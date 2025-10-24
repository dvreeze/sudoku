/*
 * Copyright 2025-2025 Chris de Vreeze
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.cdevreeze.sudoku.service.jpaimpl;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import eu.cdevreeze.sudoku.entity.*;
import eu.cdevreeze.sudoku.model.*;
import eu.cdevreeze.sudoku.service.SudokuService;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Subgraph;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.hibernate.internal.SessionImpl;
import org.hibernate.jpa.AvailableHints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.orm.jpa.EntityManagerProxy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;

/**
 * Implementation of {@link SudokuService} using Jakarta Persistence.
 *
 * @author Chris de Vreeze
 */
@Service
@ConditionalOnProperty(name = "underlyingSessionType", havingValue = "jakarta.persistence.EntityManager", matchIfMissing = true)
public class JpaSudokuService implements SudokuService {

    private static final Logger logger = LoggerFactory.getLogger(JpaSudokuService.class);

    // See https://thorben-janssen.com/hibernate-tips-how-to-bootstrap-hibernate-with-spring-boot/

    private static final String LOAD_GRAPH_KEY = AvailableHints.HINT_SPEC_LOAD_GRAPH;

    // Shared thread-safe proxy for the actual transactional EntityManager that differs for each transaction
    @PersistenceContext
    private final EntityManager entityManager;

    public JpaSudokuService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public Sudoku createSudoku(Grid startGrid) {
        Preconditions.checkState(TransactionSynchronizationManager.isActualTransactionActive());
        Preconditions.checkState(entityManager instanceof EntityManagerProxy);
        logContext();

        Preconditions.checkArgument(startGrid.idOption().isEmpty());
        Preconditions.checkArgument(startGrid.cells().stream().allMatch(c -> c.idOption().isEmpty()));

        // Create new JPA entities, and persist them

        GridEntity gridEntity = new GridEntity();

        entityManager.persist(gridEntity);

        entityManager.flush();
        entityManager.refresh(gridEntity);

        startGrid.cells().forEach(cell -> {
            CellEntity cellEntity = new CellEntity();
            cellEntity.setGrid(gridEntity);
            cellEntity.setRowNumber(cell.rowNumber());
            cellEntity.setColumnNumber(cell.columnNumber());
            cellEntity.setCellValue(cell.valueOption().stream().boxed().findFirst().orElse(null));

            entityManager.persist(cellEntity);
        });

        entityManager.flush();

        EntityGraph<GridEntity> gridGraph = entityManager.createEntityGraph(GridEntity.class);
        gridGraph.addAttributeNode(GridEntity_.cells);
        Map<String, Object> props = Map.of(LOAD_GRAPH_KEY, gridGraph);
        // While in the same open EntityManager, this is the same Java object as gridEntity
        GridEntity gridEntity2 = entityManager.find(GridEntity.class, gridEntity.getId(), props);

        SudokuEntity sudokuEntity = new SudokuEntity();
        sudokuEntity.setStartGrid(gridEntity2);

        entityManager.persist(sudokuEntity);

        // Flush and refresh, to prepare querying again
        entityManager.flush();
        entityManager.refresh(sudokuEntity);
        Objects.requireNonNull(sudokuEntity.getId());

        // Query for the persisted JPA entity with all associations, and return it
        Sudoku sudoku = findSudoku(sudokuEntity.getId()).orElseThrow();

        Preconditions.checkState(sudoku.idOption().isPresent());
        Preconditions.checkState(sudoku.startGrid().idOption().isPresent());
        return sudoku;
    }

    @Override
    @Transactional
    public GameHistory startGame(long sudokuId, String player, Instant startTime) {
        Preconditions.checkState(TransactionSynchronizationManager.isActualTransactionActive());
        Preconditions.checkState(entityManager instanceof EntityManagerProxy);
        logContext();

        // Query for associated data
        SudokuEntity sudokuEntity = findSudokuEntity(sudokuId).orElseThrow();

        // Create new JPA entity, filling in retrieved associated data (leaving steps field empty)
        GameHistoryEntity gameHistoryEntity = new GameHistoryEntity();
        gameHistoryEntity.setPlayer(player);
        gameHistoryEntity.setStartTime(startTime.atOffset(ZoneOffset.UTC));
        gameHistoryEntity.setSudoku(sudokuEntity);
        gameHistoryEntity.setSteps(new ArrayList<>());

        // Persist that new JPA entity (no cascading)
        entityManager.persist(gameHistoryEntity);

        // Flush and refresh, to prepare querying again
        entityManager.flush();
        entityManager.refresh(gameHistoryEntity); // Returns ID, and more than that
        Objects.requireNonNull(gameHistoryEntity.getId());

        // Query for the persisted JPA entity with all associations, and return it
        GameHistory gameHistory = findGameHistory(gameHistoryEntity.getId()).orElseThrow();

        Preconditions.checkState(gameHistory.idOption().isPresent());
        Preconditions.checkState(gameHistory.sudoku().idOption().isPresent());
        Preconditions.checkState(gameHistory.sudoku().startGrid().idOption().isPresent());
        Preconditions.checkState(gameHistory.isStillValid());
        return gameHistory;
    }

    @Override
    @Transactional
    public GameHistory fillInEmptyCell(long gameHistoryId, CellPosition pos, int value, Instant stepTime) {
        Preconditions.checkState(TransactionSynchronizationManager.isActualTransactionActive());
        Preconditions.checkState(entityManager instanceof EntityManagerProxy);
        logContext();

        // Query for the JPA entity before updating it
        GameHistoryEntity gameHistoryEntity = findGameHistoryEntity(gameHistoryId).orElseThrow();

        Preconditions.checkState(gameHistoryId == Objects.requireNonNull(gameHistoryEntity.getId()));
        GameHistory tempGameHistory = convertToGameHistoryWithoutIds(gameHistoryEntity);
        Preconditions.checkState(tempGameHistory.currentGrid().isStillValid());
        Preconditions.checkState(
                tempGameHistory
                        .currentGrid()
                        .fillCellIfEmpty(pos, value)
                        .stream()
                        .anyMatch(Grid::isStillValid)
        );

        // Create new JPA entity, filling in retrieved associated data
        StepEntity stepEntity = new StepEntity();
        stepEntity.setGameHistory(gameHistoryEntity);
        stepEntity.setStepDateTime(stepTime.atOffset(ZoneOffset.UTC));
        stepEntity.setRowNumber(pos.rowNumber());
        stepEntity.setColumnNumber(pos.columnNumber());
        stepEntity.setStepValue(value);

        // Persist that new JPA entity (no cascading)
        entityManager.persist(stepEntity);

        // Flush and refresh, to prepare querying again
        entityManager.flush();
        entityManager.refresh(gameHistoryEntity);
        Objects.requireNonNull(gameHistoryEntity.getId());

        // Query for the persisted JPA entity with all associations, and return it
        GameHistory gameHistory = findGameHistory(gameHistoryEntity.getId()).orElseThrow();

        Preconditions.checkState(gameHistory.idOption().isPresent());
        Preconditions.checkState(gameHistory.sudoku().idOption().isPresent());
        Preconditions.checkState(gameHistory.sudoku().startGrid().idOption().isPresent());
        Preconditions.checkState(gameHistory.isStillValid());
        return gameHistory;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Grid> findGrid(long gridId) {
        Preconditions.checkState(TransactionSynchronizationManager.isActualTransactionActive());
        Preconditions.checkState(entityManager instanceof EntityManagerProxy);
        logContext();

        return findGridEntity(gridId).map(this::convertToGrid);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Sudoku> findSudoku(long sudokuId) {
        Preconditions.checkState(TransactionSynchronizationManager.isActualTransactionActive());
        Preconditions.checkState(entityManager instanceof EntityManagerProxy);
        logContext();

        return findSudokuEntity(sudokuId).map(this::convertToSudoku);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GameHistory> findGameHistory(long gameHistoryId) {
        Preconditions.checkState(TransactionSynchronizationManager.isActualTransactionActive());
        Preconditions.checkState(entityManager instanceof EntityManagerProxy);
        logContext();

        return findGameHistoryEntity(gameHistoryId).map(this::convertToGameHistory);
    }

    // Private query methods

    private Optional<GridEntity> findGridEntity(long gridId) {
        // First build up the query (without worrying about the load/fetch graph)
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<GridEntity> cq = cb.createQuery(GridEntity.class);

        Root<GridEntity> gridRoot = cq.from(GridEntity.class);
        cq.where(cb.equal(gridRoot.get(GridEntity_.id), gridId));
        cq.select(gridRoot);

        // Next build up the entity graph, to specify which associated data should be fetched
        // At the same time, this helps achieve good performance, by solving the N + 1 problem
        EntityGraph<GridEntity> gridGraph = entityManager.createEntityGraph(GridEntity.class);
        gridGraph.addAttributeNode(GridEntity_.cells);

        // Run the query, providing the load graph as query hint
        // Note that JPA entities do not escape the persistence context (when looking at the callers of this private method)
        // It is not efficient to first retrieve entities and then convert them to DTOs, but it is practical
        // Note that method getResultStream was avoided; thus I appear to avoid some data loss in the query
        return entityManager.createQuery(cq)
                .setHint(LOAD_GRAPH_KEY, gridGraph)
                .getResultList()
                .stream()
                .findFirst();
    }

    private Optional<SudokuEntity> findSudokuEntity(long sudokuId) {
        // First build up the query (without worrying about the load/fetch graph)
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<SudokuEntity> cq = cb.createQuery(SudokuEntity.class);

        Root<SudokuEntity> sudokuRoot = cq.from(SudokuEntity.class);
        cq.where(cb.equal(sudokuRoot.get(SudokuEntity_.id), sudokuId));
        cq.select(sudokuRoot);

        // Next build up the entity graph, to specify which associated data should be fetched
        // At the same time, this helps achieve good performance, by solving the N + 1 problem
        EntityGraph<SudokuEntity> sudokuGraph = entityManager.createEntityGraph(SudokuEntity.class);
        sudokuGraph.addAttributeNode(SudokuEntity_.startGrid);
        Subgraph<GridEntity> gridSubgraph = sudokuGraph.addSubgraph(SudokuEntity_.startGrid);
        gridSubgraph.addAttributeNode(GridEntity_.cells);

        // Run the query, providing the load graph as query hint
        // Note that JPA entities do not escape the persistence context (when looking at the callers of this private method)
        // It is not efficient to first retrieve entities and then convert them to DTOs, but it is practical
        // Note that method getResultStream was avoided; thus I appear to avoid some data loss in the query
        return entityManager.createQuery(cq)
                .setHint(LOAD_GRAPH_KEY, sudokuGraph)
                .getResultList()
                .stream()
                .findFirst();
    }

    private Optional<GameHistoryEntity> findGameHistoryEntity(long gameHistoryId) {
        // Avoiding a cartesian product ("MultiBagFetchException") by using 2 criteria queries

        Optional<GameHistoryEntity> partialGameHistoryOption = findGameHistoryEntityWithoutGridCells(gameHistoryId);

        // Seems to retrieve the Sudoku twice, once implicitly and once explicitly
        Optional<SudokuEntity> sudokuOption = partialGameHistoryOption.flatMap(gameHist ->
                findSudokuEntity(Objects.requireNonNull(gameHist.getSudoku().getId()))
        );

        return partialGameHistoryOption
                .flatMap(gameHist ->
                        sudokuOption.map(sudoku -> {
                            gameHist.setSudoku(sudoku);
                            return gameHist;
                        }));
    }

    private Optional<GameHistoryEntity> findGameHistoryEntityWithoutGridCells(long gameHistoryId) {
        // First build up the query (without worrying about the load/fetch graph)
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<GameHistoryEntity> cq = cb.createQuery(GameHistoryEntity.class);

        Root<GameHistoryEntity> gameHistoryRoot = cq.from(GameHistoryEntity.class);
        cq.where(cb.equal(gameHistoryRoot.get(GameHistoryEntity_.id), gameHistoryId));
        cq.select(gameHistoryRoot);

        // Next build up the entity graph, to specify which associated data should be fetched
        // At the same time, this helps achieve good performance, by solving the N + 1 problem
        EntityGraph<GameHistoryEntity> gameHistoryGraph = entityManager.createEntityGraph(GameHistoryEntity.class);
        gameHistoryGraph.addAttributeNode(GameHistoryEntity_.steps);
        gameHistoryGraph.addAttributeNode(GameHistoryEntity_.sudoku);
        Subgraph<SudokuEntity> sudokuSubgraph = gameHistoryGraph.addSubgraph(GameHistoryEntity_.sudoku);
        sudokuSubgraph.addAttributeNode(SudokuEntity_.startGrid);

        // Run the query, providing the load graph as query hint
        // Note that JPA entities do not escape the persistence context (when looking at the callers of this private method)
        // It is not efficient to first retrieve entities and then convert them to DTOs, but it is practical
        // Note that method getResultStream was avoided; thus I appear to avoid some data loss in the query
        return entityManager.createQuery(cq)
                .setHint(LOAD_GRAPH_KEY, gameHistoryGraph)
                .getResultList()
                .stream()
                .findFirst();
    }

    // Private conversion methods, encapsulating assumptions about loaded associations
    // The conversions from JPA entities to model objects should be rather safe, but expect fully filled data, unless specified otherwise
    // Note that there are only conversions from JPA entities to model objects, and not the other way around

    private Grid convertToGrid(GridEntity gridEntity) {
        Preconditions.checkArgument(gridEntity.getId() != null);
        Preconditions.checkArgument(gridEntity.getCells().stream().allMatch(c -> c.getId() != null));

        return Grid.fromCells(
                gridEntity.getCells()
                        .stream()
                        .map(cell -> new Cell(
                                OptionalLong.of(cell.getId()),
                                cell.getRowNumber(),
                                cell.getColumnNumber(),
                                Optional.ofNullable(cell.getCellValue()).stream().mapToInt(v -> v).findFirst()
                        ))
                        .collect(ImmutableSet.toImmutableSet())
        ).withId(gridEntity.getId());
    }

    private Grid convertToGridWithoutIds(GridEntity gridEntity) {
        return Grid.fromCells(
                gridEntity.getCells()
                        .stream()
                        .map(cell -> new Cell(
                                OptionalLong.empty(),
                                cell.getRowNumber(),
                                cell.getColumnNumber(),
                                Optional.ofNullable(cell.getCellValue()).stream().mapToInt(v -> v).findFirst()
                        ))
                        .collect(ImmutableSet.toImmutableSet())
        );
    }

    private Sudoku convertToSudoku(SudokuEntity sudokuEntity) {
        Preconditions.checkArgument(sudokuEntity.getId() != null);
        Preconditions.checkArgument(sudokuEntity.getStartGrid() != null);

        return new Sudoku(
                OptionalLong.of(sudokuEntity.getId()),
                convertToGrid(sudokuEntity.getStartGrid())
        );
    }

    private Sudoku convertToSudokuWithoutIds(SudokuEntity sudokuEntity) {
        return new Sudoku(
                OptionalLong.empty(),
                convertToGridWithoutIds(sudokuEntity.getStartGrid())
        );
    }

    private GameHistory convertToGameHistory(GameHistoryEntity gameHistoryEntity) {
        Preconditions.checkArgument(gameHistoryEntity.getId() != null);
        Preconditions.checkArgument(gameHistoryEntity.getSudoku() != null);
        Preconditions.checkArgument(gameHistoryEntity.getSteps() != null);

        return new GameHistory(
                OptionalLong.of(gameHistoryEntity.getId()),
                gameHistoryEntity.getPlayer(),
                gameHistoryEntity.getStartTime().toInstant(),
                convertToSudoku(gameHistoryEntity.getSudoku()),
                gameHistoryEntity.getSteps()
                        .stream()
                        .map(step -> new Step(
                                new StepKey(
                                        gameHistoryEntity.getId(),
                                        step.getStepDateTime().toInstant()
                                ),
                                step.getRowNumber(),
                                step.getColumnNumber(),
                                step.getStepValue()
                        ))
                        .collect(ImmutableList.toImmutableList())
        );
    }

    private GameHistory convertToGameHistoryWithoutIds(GameHistoryEntity gameHistoryEntity) {
        return new GameHistory(
                OptionalLong.empty(),
                gameHistoryEntity.getPlayer(),
                gameHistoryEntity.getStartTime().toInstant(),
                convertToSudokuWithoutIds(gameHistoryEntity.getSudoku()),
                gameHistoryEntity.getSteps()
                        .stream()
                        .map(step -> new Step(
                                new StepKey(0, step.getStepDateTime().toInstant()), // Cheating a bit
                                step.getRowNumber(),
                                step.getColumnNumber(),
                                step.getStepValue()
                        ))
                        .collect(ImmutableList.toImmutableList())
        );
    }

    private void logContext() {
        logger.debug("Injected EntityManager proxy: {}", entityManager);
        logger.debug("Injected EntityManager proxy ID: {}", Objects.toIdentityString(entityManager));
        logger.debug("Hibernate SessionImpl: {}", entityManager.unwrap(SessionImpl.class));
        logger.debug("Hibernate SessionImpl ID: {}", Objects.toIdentityString(entityManager.unwrap(SessionImpl.class)));
        logger.debug("Transactional resource map: {}", TransactionSynchronizationManager.getResourceMap());
    }
}
