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
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Implementation of {@link SudokuService} using Jakarta Persistence.
 *
 * @author Chris de Vreeze
 */
@Service
@ConditionalOnBooleanProperty(name = "useJooq", havingValue = false, matchIfMissing = true)
public class JpaSudokuService implements SudokuService {

    // See https://thorben-janssen.com/hibernate-tips-how-to-bootstrap-hibernate-with-spring-boot/

    private static final String LOAD_GRAPH_KEY = "jakarta.persistence.loadgraph";

    // Shared thread-safe proxy for the actual transactional EntityManager that differs for each transaction
    @PersistenceContext
    private final EntityManager entityManager;

    public JpaSudokuService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public Sudoku createSudoku(Grid startGrid) {
        Preconditions.checkArgument(startGrid.idOption().isEmpty());

        GridEntity gridEntity = convertToGridEntity(startGrid);
        entityManager.persist(gridEntity);
        gridEntity.getCells().forEach(entityManager::persist);

        SudokuEntity sudokuEntity = new SudokuEntity();
        sudokuEntity.setStartGrid(gridEntity);

        entityManager.persist(sudokuEntity);

        entityManager.flush();
        entityManager.refresh(sudokuEntity);
        Objects.requireNonNull(sudokuEntity.getId());

        Sudoku sudoku = findSudoku(sudokuEntity.getId()).orElseThrow();

        Preconditions.checkArgument(sudoku.idOption().isPresent());
        Preconditions.checkArgument(sudoku.startGrid().idOption().isPresent());
        return sudoku;
    }

    @Override
    @Transactional
    public GameHistory startGame(long sudokuId, String player, Instant startTime) {
        Sudoku sudoku = findSudoku(sudokuId).orElseThrow();

        SudokuEntity sudokuEntity = convertToSudokuEntity(sudoku);

        GameHistoryEntity gameHistoryEntity = new GameHistoryEntity();
        gameHistoryEntity.setPlayer(player);
        gameHistoryEntity.setStartTime(startTime.atOffset(ZoneOffset.UTC));
        gameHistoryEntity.setSudoku(sudokuEntity);

        entityManager.persist(gameHistoryEntity);

        entityManager.flush();
        entityManager.refresh(gameHistoryEntity);
        Objects.requireNonNull(gameHistoryEntity.getId());

        GameHistory gameHistory = findGameHistory(gameHistoryEntity.getId()).orElseThrow();

        Preconditions.checkArgument(gameHistory.idOption().isPresent());
        Preconditions.checkArgument(gameHistory.sudoku().idOption().isPresent());
        Preconditions.checkArgument(gameHistory.sudoku().startGrid().idOption().isPresent());
        return gameHistory;
    }

    @Override
    @Transactional
    public GameHistory fillInEmptyCell(long gameHistoryId, CellPosition pos, int value) {
        GameHistory gameHistory = findGameHistory(gameHistoryId).orElseThrow();

        Preconditions.checkArgument(gameHistory.currentGrid().isStillValid());
        Preconditions.checkArgument(
                gameHistory.currentGrid().fillCellIfEmpty(pos, value).stream().anyMatch(Grid::isStillValid));

        GameHistoryEntity gameHistoryEntity = convertToGameHistoryEntity(gameHistory);

        StepEntity stepEntity = new StepEntity();
        stepEntity.setStepKey(
                new StepEntityKey(
                        gameHistoryId,
                        1 + gameHistory.steps()
                                .stream()
                                .flatMap(step -> step.stepKeyOption().stream())
                                .map(StepKey::stepSeqNumber)
                                .mapToInt(v -> v)
                                .max()
                                .orElse(0)
                )
        );
        stepEntity.setRowNumber(pos.rowNumber());
        stepEntity.setColumnNumber(pos.columnNumber());
        stepEntity.setStepValue(value);

        entityManager.persist(stepEntity);

        entityManager.flush();
        entityManager.refresh(gameHistoryEntity);
        Objects.requireNonNull(gameHistoryEntity.getId());

        GameHistory gameHistory2 = findGameHistory(gameHistoryEntity.getId()).orElseThrow();

        Preconditions.checkArgument(gameHistory2.idOption().isPresent());
        Preconditions.checkArgument(gameHistory2.sudoku().idOption().isPresent());
        Preconditions.checkArgument(gameHistory2.sudoku().startGrid().idOption().isPresent());
        return gameHistory2;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Grid> findGrid(long gridId) {
        Preconditions.checkArgument(TransactionSynchronizationManager.isActualTransactionActive());
        System.out.println("Hibernate SessionImpl: " + entityManager.unwrap(SessionImpl.class));

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
        // Note that JPA entities do not escape the persistence context
        // It is not efficient to first retrieve entities and then convert them to DTOs, but it is practical
        // Note that method getResultStream was avoided; thus I appear to avoid some data loss in the query
        return entityManager.createQuery(cq)
                .setHint(LOAD_GRAPH_KEY, gridGraph)
                .getResultList()
                .stream()
                .map(this::convertToGrid)
                .findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Sudoku> findSudoku(long sudokuId) {
        Preconditions.checkArgument(TransactionSynchronizationManager.isActualTransactionActive());
        System.out.println("Hibernate SessionImpl: " + entityManager.unwrap(SessionImpl.class));

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
        // Note that JPA entities do not escape the persistence context
        // It is not efficient to first retrieve entities and then convert them to DTOs, but it is practical
        // Note that method getResultStream was avoided; thus I appear to avoid some data loss in the query
        return entityManager.createQuery(cq)
                .setHint(LOAD_GRAPH_KEY, sudokuGraph)
                .getResultList()
                .stream()
                .map(this::convertToSudoku)
                .findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GameHistory> findGameHistory(long gameHistoryId) {
        Preconditions.checkArgument(TransactionSynchronizationManager.isActualTransactionActive());
        System.out.println("Hibernate SessionImpl: " + entityManager.unwrap(SessionImpl.class));

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
        Subgraph<GridEntity> gridSubgraph = sudokuSubgraph.addSubgraph(SudokuEntity_.startGrid);
        gridSubgraph.addAttributeNode(GridEntity_.cells);

        // Run the query, providing the load graph as query hint
        // Note that JPA entities do not escape the persistence context
        // It is not efficient to first retrieve entities and then convert them to DTOs, but it is practical
        // Note that method getResultStream was avoided; thus I appear to avoid some data loss in the query
        return entityManager.createQuery(cq)
                .setHint(LOAD_GRAPH_KEY, gameHistoryGraph)
                .getResultList()
                .stream()
                .map(this::convertToGameHistory)
                .findFirst();
    }

    // Private conversion methods, encapsulating assumptions about loaded associations

    private Grid convertToGrid(GridEntity gridEntity) {
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

    private Sudoku convertToSudoku(SudokuEntity sudokuEntity) {
        return new Sudoku(
                OptionalLong.of(sudokuEntity.getId()),
                convertToGrid(sudokuEntity.getStartGrid())
        );
    }

    private GameHistory convertToGameHistory(GameHistoryEntity gameHistoryEntity) {
        return new GameHistory(
                OptionalLong.of(gameHistoryEntity.getId()),
                gameHistoryEntity.getPlayer(),
                gameHistoryEntity.getStartTime().toInstant(),
                convertToSudoku(gameHistoryEntity.getSudoku()),
                gameHistoryEntity.getSteps()
                        .stream()
                        .map(step -> new Step(
                                Optional.of(new StepKey(
                                        gameHistoryEntity.getId(),
                                        step.getStepSeqNumber()
                                )),
                                step.getRowNumber(),
                                step.getColumnNumber(),
                                step.getStepValue()
                        ))
                        .collect(ImmutableList.toImmutableList())
        );
    }

    private GridEntity convertToGridEntity(Grid grid) {
        GridEntity gridEntity = new GridEntity();
        grid.idOption().ifPresent(gridEntity::setId);
        grid.cells().forEach(cell -> {
            CellEntity cellEntity = new CellEntity();
            cellEntity.setRowNumber(cell.rowNumber());
            cellEntity.setColumnNumber(cell.columnNumber());
            cellEntity.setCellValue(cell.valueOption().stream().boxed().findFirst().orElse(null));
            gridEntity.addCell(cellEntity);
        });
        return gridEntity;
    }

    private SudokuEntity convertToSudokuEntity(Sudoku sudoku) {
        SudokuEntity sudokuEntity = new SudokuEntity();
        sudoku.idOption().ifPresent(sudokuEntity::setId);
        sudokuEntity.setStartGrid(convertToGridEntity(sudoku.startGrid()));
        return sudokuEntity;
    }

    private GameHistoryEntity convertToGameHistoryEntity(GameHistory gameHistory) {
        GameHistoryEntity gameHistoryEntity = new GameHistoryEntity();
        gameHistory.idOption().ifPresent(gameHistoryEntity::setId);
        gameHistoryEntity.setPlayer(gameHistoryEntity.getPlayer());
        gameHistoryEntity.setStartTime(gameHistory.startTime().atOffset(ZoneOffset.UTC));
        gameHistoryEntity.setSudoku(convertToSudokuEntity(gameHistory.sudoku()));
        gameHistory.steps().forEach(step -> {
            StepEntity stepEntity = new StepEntity();
            gameHistory.idOption().ifPresent(gameHistoryId ->
                    stepEntity.setStepKey(
                            new StepEntityKey(gameHistoryId, step.stepKeyOption().orElseThrow().stepSeqNumber())
                    )
            );
            stepEntity.setRowNumber(step.rowNumber());
            stepEntity.setColumnNumber(step.columnNumber());
            stepEntity.setStepValue(step.value());
            gameHistoryEntity.addStep(stepEntity);
        });
        return gameHistoryEntity;
    }
}
