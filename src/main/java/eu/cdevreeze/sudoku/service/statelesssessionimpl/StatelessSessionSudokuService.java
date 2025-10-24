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

package eu.cdevreeze.sudoku.service.statelesssessionimpl;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import eu.cdevreeze.sudoku.entity.*;
import eu.cdevreeze.sudoku.model.*;
import eu.cdevreeze.sudoku.service.SudokuService;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.Subgraph;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.internal.StatelessSessionImpl;
import org.hibernate.jpa.AvailableHints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Implementation of {@link SudokuService} using Hibernate's {@link StatelessSession}.
 * <p>
 * Note how similar this implementation is to the JPA implementation of the service.
 *
 * @author Chris de Vreeze
 */
@Service
@ConditionalOnProperty(name = "underlyingSessionType", havingValue = "org.hibernate.StatelessSession")
public class StatelessSessionSudokuService implements SudokuService {

    private static final Logger logger = LoggerFactory.getLogger(StatelessSessionSudokuService.class);

    // See https://thorben-janssen.com/hibernate-tips-how-to-bootstrap-hibernate-with-spring-boot/
    // Yet this time using a StatelessSession rather than an EntityManager

    private static final String LOAD_GRAPH_KEY = AvailableHints.HINT_SPEC_LOAD_GRAPH;

    private final DataSource dataSource;
    private final SessionFactory sessionFactory;

    public StatelessSessionSudokuService(
            DataSource dataSource,
            SessionFactory sessionFactory) {
        this.dataSource = dataSource;
        this.sessionFactory = sessionFactory;
    }

    // Below the assumption is made that it is ok to open a new StatelessSession and close it within the methods
    // marked as being transactional.

    @Override
    @Transactional
    public Sudoku createSudoku(Grid startGrid) {
        try (StatelessSession statelessSession = openStatelessSession()) {
            Preconditions.checkState(TransactionSynchronizationManager.isActualTransactionActive());
            logContext(statelessSession);

            Preconditions.checkArgument(startGrid.idOption().isEmpty());
            Preconditions.checkArgument(startGrid.cells().stream().allMatch(c -> c.idOption().isEmpty()));

            // Create new JPA entities, and persist them

            GridEntity gridEntity = new GridEntity();

            statelessSession.insert(gridEntity);

            statelessSession.refresh(gridEntity);

            startGrid.cells().forEach(cell -> {
                CellEntity cellEntity = new CellEntity();
                cellEntity.setGrid(gridEntity);
                cellEntity.setRowNumber(cell.rowNumber());
                cellEntity.setColumnNumber(cell.columnNumber());
                cellEntity.setCellValue(cell.valueOption().stream().boxed().findFirst().orElse(null));

                statelessSession.insert(cellEntity);
            });

            EntityGraph<GridEntity> gridGraph = statelessSession.createEntityGraph(GridEntity.class);
            gridGraph.addAttributeNode(GridEntity_.cells);
            GridEntity gridEntity2 = statelessSession.get(gridGraph, gridEntity.getId());

            SudokuEntity sudokuEntity = new SudokuEntity();
            sudokuEntity.setStartGrid(gridEntity2);

            statelessSession.insert(sudokuEntity);

            // Refresh, to prepare querying again
            statelessSession.refresh(sudokuEntity);
            Objects.requireNonNull(sudokuEntity.getId());

            // Query for the inserted JPA entity with all associations, and return it
            Sudoku sudoku = findSudoku(sudokuEntity.getId()).orElseThrow();

            Preconditions.checkState(sudoku.idOption().isPresent());
            Preconditions.checkState(sudoku.startGrid().idOption().isPresent());
            return sudoku;
        }
    }

    @Override
    @Transactional
    public GameHistory startGame(long sudokuId, String player, Instant startTime) {
        try (StatelessSession statelessSession = openStatelessSession()) {
            Preconditions.checkState(TransactionSynchronizationManager.isActualTransactionActive());
            logContext(statelessSession);

            // Query for associated data
            SudokuEntity sudokuEntity = findSudokuEntity(sudokuId, statelessSession).orElseThrow();

            // Create new JPA entity, filling in retrieved associated data (leaving steps field empty)
            GameHistoryEntity gameHistoryEntity = new GameHistoryEntity();
            gameHistoryEntity.setPlayer(player);
            gameHistoryEntity.setStartTime(startTime.atOffset(ZoneOffset.UTC));
            gameHistoryEntity.setSudoku(sudokuEntity);
            gameHistoryEntity.setSteps(new ArrayList<>());

            // Insert that new JPA entity (no cascading possible with StatelessSession)
            statelessSession.insert(gameHistoryEntity);

            // Refresh, to prepare querying again
            statelessSession.refresh(gameHistoryEntity); // Returns ID, and more than that
            Objects.requireNonNull(gameHistoryEntity.getId());

            // Query for the inserted JPA entity with all associations, and return it
            GameHistory gameHistory = findGameHistory(gameHistoryEntity.getId()).orElseThrow();

            Preconditions.checkState(gameHistory.idOption().isPresent());
            Preconditions.checkState(gameHistory.sudoku().idOption().isPresent());
            Preconditions.checkState(gameHistory.sudoku().startGrid().idOption().isPresent());
            Preconditions.checkState(gameHistory.isStillValid());
            return gameHistory;
        }
    }

    @Override
    @Transactional
    public GameHistory fillInEmptyCell(long gameHistoryId, CellPosition pos, int value, Instant stepTime) {
        try (StatelessSession statelessSession = openStatelessSession()) {
            Preconditions.checkState(TransactionSynchronizationManager.isActualTransactionActive());
            logContext(statelessSession);

            // Query for the JPA entity before updating it
            GameHistoryEntity gameHistoryEntity =
                    findGameHistoryEntity(gameHistoryId, statelessSession).orElseThrow();

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

            // Insert that new JPA entity (no cascading)
            statelessSession.insert(stepEntity);

            // Refresh, to prepare querying again
            statelessSession.refresh(gameHistoryEntity);
            Objects.requireNonNull(gameHistoryEntity.getId());

            // Query for the inserted JPA entity with all associations, and return it
            GameHistory gameHistory = findGameHistory(gameHistoryEntity.getId()).orElseThrow();

            Preconditions.checkState(gameHistory.idOption().isPresent());
            Preconditions.checkState(gameHistory.sudoku().idOption().isPresent());
            Preconditions.checkState(gameHistory.sudoku().startGrid().idOption().isPresent());
            Preconditions.checkState(gameHistory.isStillValid());
            return gameHistory;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Grid> findGrid(long gridId) {
        try (StatelessSession statelessSession = openStatelessSession()) {
            Preconditions.checkState(TransactionSynchronizationManager.isActualTransactionActive());
            logContext(statelessSession);

            return findGridEntity(gridId, statelessSession).map(this::convertToGrid);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Sudoku> findSudoku(long sudokuId) {
        try (StatelessSession statelessSession = openStatelessSession()) {
            Preconditions.checkState(TransactionSynchronizationManager.isActualTransactionActive());
            logContext(statelessSession);

            return findSudokuEntity(sudokuId, statelessSession).map(this::convertToSudoku);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GameHistory> findGameHistory(long gameHistoryId) {
        try (StatelessSession statelessSession = openStatelessSession()) {
            Preconditions.checkState(TransactionSynchronizationManager.isActualTransactionActive());
            logContext(statelessSession);

            return findGameHistoryEntity(gameHistoryId, statelessSession).map(this::convertToGameHistory);
        }
    }

    // Private query methods

    private Optional<GridEntity> findGridEntity(long gridId, StatelessSession statelessSession) {
        // First build up the query (without worrying about the load/fetch graph)
        CriteriaBuilder cb = statelessSession.getCriteriaBuilder();
        CriteriaQuery<GridEntity> cq = cb.createQuery(GridEntity.class);

        Root<GridEntity> gridRoot = cq.from(GridEntity.class);
        cq.where(cb.equal(gridRoot.get(GridEntity_.id), gridId));
        cq.select(gridRoot);

        // Next build up the entity graph, to specify which associated data should be fetched
        // At the same time, this helps achieve good performance, by solving the N + 1 problem
        EntityGraph<GridEntity> gridGraph = statelessSession.createEntityGraph(GridEntity.class);
        gridGraph.addAttributeNode(GridEntity_.cells);

        // Run the query, providing the load graph as query hint
        // Note that JPA entities do not escape the stateless session (when looking at the callers of this private method)
        // Note that method getResultStream was avoided; thus I appear to avoid some data loss in the query
        return statelessSession.createQuery(cq)
                .setHint(LOAD_GRAPH_KEY, gridGraph)
                .getResultList()
                .stream()
                .findFirst();
    }

    private Optional<SudokuEntity> findSudokuEntity(long sudokuId, StatelessSession statelessSession) {
        // First build up the query (without worrying about the load/fetch graph)
        CriteriaBuilder cb = statelessSession.getCriteriaBuilder();
        CriteriaQuery<SudokuEntity> cq = cb.createQuery(SudokuEntity.class);

        Root<SudokuEntity> sudokuRoot = cq.from(SudokuEntity.class);
        cq.where(cb.equal(sudokuRoot.get(SudokuEntity_.id), sudokuId));
        cq.select(sudokuRoot);

        // Next build up the entity graph, to specify which associated data should be fetched
        // At the same time, this helps achieve good performance, by solving the N + 1 problem
        EntityGraph<SudokuEntity> sudokuGraph = statelessSession.createEntityGraph(SudokuEntity.class);
        sudokuGraph.addAttributeNode(SudokuEntity_.startGrid);
        Subgraph<GridEntity> gridSubgraph = sudokuGraph.addSubgraph(SudokuEntity_.startGrid);
        gridSubgraph.addAttributeNode(GridEntity_.cells);

        // Run the query, providing the load graph as query hint
        // Note that JPA entities do not escape the stateless session (when looking at the callers of this private method)
        // Note that method getResultStream was avoided; thus I appear to avoid some data loss in the query
        return statelessSession.createQuery(cq)
                .setHint(LOAD_GRAPH_KEY, sudokuGraph)
                .getResultList()
                .stream()
                .findFirst();
    }

    private Optional<GameHistoryEntity> findGameHistoryEntity(long gameHistoryId, StatelessSession statelessSession) {
        // Avoiding a cartesian product ("MultiBagFetchException") by using 2 criteria queries

        Optional<GameHistoryEntity> partialGameHistoryOption =
                findGameHistoryEntityWithoutGridCells(gameHistoryId, statelessSession);

        // Seems to retrieve the Sudoku twice, once implicitly and once explicitly
        Optional<SudokuEntity> sudokuOption = partialGameHistoryOption.flatMap(gameHist ->
                findSudokuEntity(Objects.requireNonNull(gameHist.getSudoku().getId()), statelessSession)
        );

        return partialGameHistoryOption
                .flatMap(gameHist ->
                        sudokuOption.map(sudoku -> {
                            gameHist.setSudoku(sudoku);
                            return gameHist;
                        }));
    }

    private Optional<GameHistoryEntity> findGameHistoryEntityWithoutGridCells(long gameHistoryId, StatelessSession statelessSession) {
        // First build up the query (without worrying about the load/fetch graph)
        CriteriaBuilder cb = statelessSession.getCriteriaBuilder();
        CriteriaQuery<GameHistoryEntity> cq = cb.createQuery(GameHistoryEntity.class);

        Root<GameHistoryEntity> gameHistoryRoot = cq.from(GameHistoryEntity.class);
        cq.where(cb.equal(gameHistoryRoot.get(GameHistoryEntity_.id), gameHistoryId));
        cq.select(gameHistoryRoot);

        // Next build up the entity graph, to specify which associated data should be fetched
        // At the same time, this helps achieve good performance, by solving the N + 1 problem
        EntityGraph<GameHistoryEntity> gameHistoryGraph = statelessSession.createEntityGraph(GameHistoryEntity.class);
        gameHistoryGraph.addAttributeNode(GameHistoryEntity_.steps);
        gameHistoryGraph.addAttributeNode(GameHistoryEntity_.sudoku);
        Subgraph<SudokuEntity> sudokuSubgraph = gameHistoryGraph.addSubgraph(GameHistoryEntity_.sudoku);
        sudokuSubgraph.addAttributeNode(SudokuEntity_.startGrid);

        // Run the query, providing the load graph as query hint
        // Note that JPA entities do not escape the stateless session (when looking at the callers of this private method)
        // Note that method getResultStream was avoided; thus I appear to avoid some data loss in the query
        return statelessSession.createQuery(cq)
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

    // Other private methods

    private StatelessSession openStatelessSession() {
        // This should open a StatelessSession that participates in Spring-managed transactions
        Connection conn = DataSourceUtils.getConnection(dataSource);
        Preconditions.checkState(DataSourceUtils.isConnectionTransactional(conn, dataSource));
        return sessionFactory.openStatelessSession(conn);
    }

    private void logContext(StatelessSession statelessSession) {
        logger.debug("Used StatelessSession: {}", statelessSession);
        logger.debug("Used StatelessSession ID: {}", Objects.toIdentityString(statelessSession));
        logger.debug("Hibernate StatelessSessionImpl: {}", statelessSession.unwrap(StatelessSessionImpl.class));
        logger.debug("Hibernate StatelessSessionImpl ID: {}", Objects.toIdentityString(statelessSession.unwrap(StatelessSessionImpl.class)));
        logger.debug("Transactional resource map: {}", TransactionSynchronizationManager.getResourceMap());
    }
}
