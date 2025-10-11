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

package eu.cdevreeze.sudoku.service.jooqimpl;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import eu.cdevreeze.sudoku.jooq.tables.records.*;
import eu.cdevreeze.sudoku.model.*;
import eu.cdevreeze.sudoku.service.SudokuService;
import org.jooq.DSLContext;
import org.jooq.Records;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static eu.cdevreeze.sudoku.jooq.Tables.*;
import static org.jooq.impl.DSL.*;

/**
 * Implementation of {@link SudokuService} using jOOQ.
 *
 * @author Chris de Vreeze
 */
@Service
@ConditionalOnBooleanProperty(name = "useJooq")
public class JooqSudokuService implements SudokuService {

    private record CellTableRow(
            Long id,
            Long gridId,
            Integer rowNumber,
            Integer columnNumber,
            @Nullable Integer cellValue
    ) {

        public Cell toCell() {
            return new Cell(
                    OptionalLong.of(id),
                    rowNumber(),
                    columnNumber(),
                    Optional.ofNullable(cellValue()).stream().mapToInt(v -> v).findFirst()
            );
        }
    }

    private record GridTableRow(
            Long id,
            List<CellTableRow> cells
    ) {

        public Grid toGrid() {
            return Grid.fromCells(
                    cells().stream().map(CellTableRow::toCell).collect(ImmutableSet.toImmutableSet())
            ).withId(Objects.requireNonNull(id()));
        }
    }

    private record SudokuTableRow(
            Long id,
            GridTableRow grid
    ) {

        public Sudoku toSudoku() {
            return new Sudoku(
                    OptionalLong.of(Objects.requireNonNull(id())),
                    grid.toGrid()
            );
        }
    }

    private record StepTableRow(
            Long gameHistoryId,
            Integer stepSeqNumber,
            Integer rowNumber,
            Integer columnNumber,
            Integer stepValue
    ) {

        public Step toStep() {
            return new Step(
                    Optional.of(new StepKey(gameHistoryId, stepSeqNumber)),
                    rowNumber(),
                    columnNumber(),
                    stepValue
            );
        }
    }

    private record GameHistoryTableRow(
            Long id,
            String player,
            OffsetDateTime startTime,
            SudokuTableRow sudoku,
            List<StepTableRow> steps
    ) {

        public GameHistory toGameHistory() {
            return new GameHistory(
                    Optional.of(id()).stream().mapToLong(v -> v).findFirst(),
                    player(),
                    startTime().toInstant(),
                    sudoku().toSudoku(),
                    steps().stream()
                            .map(StepTableRow::toStep)
                            .collect(ImmutableList.toImmutableList())
            );
        }
    }

    private final DSLContext dsl;

    public JooqSudokuService(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    @Transactional
    public Sudoku createSudoku(Grid startGrid) {
        Preconditions.checkArgument(startGrid.idOption().isEmpty());

        Grid grid = createGrid(startGrid);

        Preconditions.checkArgument(grid.idOption().isPresent());

        SudokuRecord sudokuRecord = dsl.insertInto(SUDOKU)
                .columns(SUDOKU.START_GRID_ID)
                .values(grid.idOption().orElseThrow())
                .returning()
                .fetchOne();

        Preconditions.checkArgument(sudokuRecord != null);

        long sudokuId = Objects.requireNonNull(sudokuRecord.getSudokuId());
        return new Sudoku(OptionalLong.of(sudokuId), grid);
    }

    @Override
    @Transactional
    public GameHistory startGame(long sudokuId, String player, Instant startTime) {
        Sudoku sudoku = findSudoku(sudokuId).orElseThrow();

        GameHistoryRecord gameHistoryRecord = dsl.insertInto(GAME_HISTORY)
                .columns(GAME_HISTORY.SUDOKU_ID, GAME_HISTORY.PLAYER, GAME_HISTORY.START_TIME)
                .values(
                        sudoku.idOption().orElseThrow(),
                        player,
                        startTime.atOffset(ZoneOffset.UTC)
                )
                .returning()
                .fetchOne();
        Preconditions.checkArgument(gameHistoryRecord != null);

        GameHistory gameHistory = new GameHistory(
                OptionalLong.of(gameHistoryRecord.getGameHistoryId()),
                gameHistoryRecord.getPlayer(),
                gameHistoryRecord.getStartTime().toInstant(),
                sudoku,
                ImmutableList.of()
        );

        Preconditions.checkArgument(gameHistory.currentGrid().isStillValid());

        return gameHistory;
    }

    @Override
    @Transactional
    public GameHistory fillInEmptyCell(long gameHistoryId, CellPosition pos, int value) {
        GameHistory gameHistory = findGameHistory(gameHistoryId).orElseThrow();

        Preconditions.checkArgument(gameHistory.currentGrid().isStillValid());
        Preconditions.checkArgument(
                gameHistory.currentGrid().fillCellIfEmpty(pos, value).stream().anyMatch(Grid::isStillValid));

        StepRecord stepRecord = dsl.insertInto(STEP)
                .columns(STEP.GAME_HISTORY_ID, STEP.STEP_SEQ_NUMBER, STEP.ROW_NUMBER, STEP.COLUMN_NUMBER, STEP.STEP_VALUE)
                .values(
                        gameHistory.idOption().orElseThrow(),
                        1 + Objects.requireNonNull(
                                dsl.selectCount().from(STEP).where(STEP.GAME_HISTORY_ID.eq(gameHistoryId)).fetchOne()
                        ).value1(),
                        pos.rowNumber(),
                        pos.columnNumber(),
                        value
                )
                .returning()
                .fetchOne();
        Preconditions.checkArgument(stepRecord != null);

        GameHistory resultGameHistory = new GameHistory(
                gameHistory.idOption(),
                gameHistory.player(),
                gameHistory.startTime(),
                gameHistory.sudoku(),
                ImmutableList.<Step>builder()
                        .addAll(gameHistory.steps())
                        .add(
                                new Step(
                                        Optional.of(new StepKey(stepRecord.getGameHistoryId(), stepRecord.getStepSeqNumber())),
                                        stepRecord.getRowNumber(),
                                        stepRecord.getColumnNumber(),
                                        stepRecord.getStepValue()
                                )
                        )
                        .build()
        );

        Preconditions.checkArgument(resultGameHistory.currentGrid().isStillValid());

        return resultGameHistory;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Grid> findGrid(long gridId) {
        return dsl.selectDistinct(
                        GRID.GRID_ID,
                        multiset(
                                select(
                                        CELL.CELL_ID,
                                        CELL.GRID_ID,
                                        CELL.ROW_NUMBER,
                                        CELL.COLUMN_NUMBER,
                                        CELL.CELL_VALUE
                                ).from(CELL)
                                        .where(CELL.GRID_ID.eq(GRID.GRID_ID))
                        ).convertFrom(r -> r.map(Records.mapping(CellTableRow::new)))
                )
                .from(GRID)
                .where(GRID.GRID_ID.eq(gridId))
                .fetchOptional()
                .map(Records.mapping(GridTableRow::new))
                .map(GridTableRow::toGrid);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Sudoku> findSudoku(long sudokuId) {
        return dsl.selectDistinct(
                        SUDOKU.SUDOKU_ID,
                        row(
                                GRID.GRID_ID,
                                multiset(
                                        select(
                                                CELL.CELL_ID,
                                                CELL.GRID_ID,
                                                CELL.ROW_NUMBER,
                                                CELL.COLUMN_NUMBER,
                                                CELL.CELL_VALUE
                                        ).from(CELL)
                                                .where(CELL.GRID_ID.eq(GRID.GRID_ID))
                                ).convertFrom(r -> r.map(Records.mapping(CellTableRow::new)))
                        ).convertFrom(Records.mapping(GridTableRow::new))
                )
                .from(SUDOKU)
                .leftJoin(GRID)
                .on(SUDOKU.START_GRID_ID.eq(GRID.GRID_ID))
                .where(SUDOKU.SUDOKU_ID.eq(sudokuId))
                .fetchOptional()
                .map(Records.mapping(SudokuTableRow::new))
                .map(SudokuTableRow::toSudoku);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GameHistory> findGameHistory(long gameHistoryId) {
        return dsl.selectDistinct(
                        GAME_HISTORY.GAME_HISTORY_ID,
                        GAME_HISTORY.PLAYER,
                        GAME_HISTORY.START_TIME,
                        row(
                                SUDOKU.SUDOKU_ID,
                                row(
                                        GRID.GRID_ID,
                                        multiset(
                                                select(
                                                        CELL.CELL_ID,
                                                        CELL.GRID_ID,
                                                        CELL.ROW_NUMBER,
                                                        CELL.COLUMN_NUMBER,
                                                        CELL.CELL_VALUE
                                                ).from(CELL)
                                                        .where(CELL.GRID_ID.eq(GRID.GRID_ID))
                                        ).convertFrom(r -> r.map(Records.mapping(CellTableRow::new)))
                                ).convertFrom(Records.mapping(GridTableRow::new))
                        ).convertFrom(Records.mapping(SudokuTableRow::new)),
                        multiset(
                                select(
                                        STEP.GAME_HISTORY_ID,
                                        STEP.STEP_SEQ_NUMBER,
                                        STEP.ROW_NUMBER,
                                        STEP.COLUMN_NUMBER,
                                        STEP.STEP_VALUE
                                ).from(STEP)
                                        .where(STEP.GAME_HISTORY_ID.eq(GAME_HISTORY.GAME_HISTORY_ID))
                                        .orderBy(STEP.STEP_SEQ_NUMBER)
                        ).convertFrom(r -> r.map(Records.mapping(StepTableRow::new)))
                )
                .from(GAME_HISTORY)
                .leftJoin(SUDOKU)
                .on(GAME_HISTORY.SUDOKU_ID.eq(SUDOKU.SUDOKU_ID))
                .leftJoin(GRID)
                .on(SUDOKU.START_GRID_ID.eq(GRID.GRID_ID))
                .where(GAME_HISTORY.GAME_HISTORY_ID.eq(gameHistoryId))
                .fetchOptional()
                .map(Records.mapping(GameHistoryTableRow::new))
                .map(GameHistoryTableRow::toGameHistory);
    }

    private Grid createGrid(Grid grid) {
        Preconditions.checkArgument(grid.idOption().isEmpty());

        GridRecord gridRecord = dsl.insertInto(GRID)
                .columns()
                .values()
                .returning()
                .fetchOne();
        long gridId = Objects.requireNonNull(gridRecord).getGridId();

        List<Cell> cells = new ArrayList<>();

        // Maybe a batch insert is better
        for (Cell cell : grid.cells()) {
            CellRecord cellRecord = dsl.insertInto(CELL)
                    .columns(CELL.GRID_ID, CELL.ROW_NUMBER, CELL.COLUMN_NUMBER, CELL.CELL_VALUE)
                    .values(
                            gridId,
                            cell.rowNumber(),
                            cell.columnNumber(),
                            cell.valueOption().stream().boxed().findFirst().orElse(null)
                    )
                    .returning()
                    .fetchOne();
            Preconditions.checkArgument(cellRecord != null);
            cells.add(
                    new Cell(
                            OptionalLong.of(cellRecord.getCellId()),
                            cellRecord.getRowNumber(),
                            cellRecord.getColumnNumber(),
                            Optional.ofNullable(cellRecord.getCellValue()).stream().mapToInt(v -> v).findFirst()
                    )
            );
        }

        return Grid.fromCells(ImmutableSet.copyOf(cells))
                .withId(gridId);
    }
}
