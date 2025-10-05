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
import eu.cdevreeze.sudoku.jooq.tables.records.CellRecord;
import eu.cdevreeze.sudoku.jooq.tables.records.GameHistoryRecord;
import eu.cdevreeze.sudoku.jooq.tables.records.GridRecord;
import eu.cdevreeze.sudoku.jooq.tables.records.SudokuRecord;
import eu.cdevreeze.sudoku.model.*;
import eu.cdevreeze.sudoku.service.SudokuService;
import org.jooq.DSLContext;
import org.jooq.Records;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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

    private record CellDatabaseTableRow(
            @Nullable Long id,
            Long gridId,
            Integer rowNumber,
            Integer columnNumber,
            @Nullable Integer cellValue
    ) {

        public Cell toCell() {
            return new Cell(
                    rowNumber(),
                    columnNumber(),
                    Optional.ofNullable(cellValue()).stream().mapToInt(v -> v).findFirst()
            );
        }
    }

    private record GridDatabaseTableRow(
            @Nullable Long id,
            List<CellDatabaseTableRow> cells
    ) {

        public Grid toGrid() {
            return Grid.fromCells(
                    cells().stream().map(CellDatabaseTableRow::toCell).collect(ImmutableSet.toImmutableSet())
            ).withId(Objects.requireNonNull(id()));
        }
    }

    private record SudokuDatabaseTableRow(
            @Nullable Long id,
            GridDatabaseTableRow grid
    ) {

        public Sudoku toSudoku() {
            return new Sudoku(
                    OptionalLong.of(Objects.requireNonNull(id())),
                    grid.toGrid()
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

        return new GameHistory(
                OptionalLong.of(gameHistoryRecord.getGameHistoryId()),
                gameHistoryRecord.getPlayer(),
                gameHistoryRecord.getStartTime().toInstant(),
                sudoku,
                ImmutableList.of()
        );
    }

    @Override
    @Transactional
    public GameHistory fillInEmptyCell(long gameHistoryId, CellPosition pos, int value) {
        // TODO
        return null;
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
                        ).convertFrom(r -> r.map(Records.mapping(CellDatabaseTableRow::new)))
                )
                .from(GRID)
                .where(GRID.GRID_ID.eq(gridId))
                .fetchOptional()
                .map(Records.mapping(GridDatabaseTableRow::new))
                .map(GridDatabaseTableRow::toGrid);
    }

    @Override
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
                                ).convertFrom(r -> r.map(Records.mapping(CellDatabaseTableRow::new)))
                        ).convertFrom(Records.mapping(GridDatabaseTableRow::new))
                )
                .from(SUDOKU)
                .leftJoin(GRID)
                .on(SUDOKU.START_GRID_ID.eq(GRID.GRID_ID))
                .where(SUDOKU.SUDOKU_ID.eq(sudokuId))
                .fetchOptional()
                .map(Records.mapping(SudokuDatabaseTableRow::new))
                .map(SudokuDatabaseTableRow::toSudoku);
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
