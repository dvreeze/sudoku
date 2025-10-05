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

package eu.cdevreeze.sudoku.model;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Sudoku grid of 9 rows and columns, partially or wholly filled with numbers.
 *
 * @author Chris de Vreeze
 */
public record Grid(OptionalLong idOption, ImmutableList<Row> rows) implements CellGroupOrGrid {

    public Grid {
        Preconditions.checkArgument(rows.size() == Constants.ROW_COUNT_IN_GRID);
        Preconditions.checkArgument(
                rows.stream().map(Row::rowNumber).toList().equals(
                        IntStream.range(0, Constants.ROW_COUNT_IN_GRID).boxed().toList()
                )
        );
    }

    public Grid withId(long id) {
        return new Grid(OptionalLong.of(id), this.rows());
    }

    public static Grid fromCells(ImmutableSet<Cell> cells) {
        Map<CellPosition, Cell> cellsByPos = cells.stream()
                .collect(Collectors.toMap(Cell::cellPosition, c -> c));

        List<Row> rows = new ArrayList<>();

        for (int row = 0; row < Constants.ROW_COUNT_IN_GRID; row++) {
            List<OptionalInt> rowCellValues = new ArrayList<>();

            for (int col = 0; col < Constants.COLUMN_COUNT_IN_GRID; col++) {
                rowCellValues.add(
                        Optional.ofNullable(cellsByPos.get(CellPosition.of(row, col)))
                                .map(Cell::valueOption)
                                .orElse(OptionalInt.empty())
                );
            }

            rows.add(new Row(row, ImmutableList.copyOf(rowCellValues)));
        }

        return new Grid(OptionalLong.empty(), ImmutableList.copyOf(rows));
    }

    public ImmutableList<Column> columns() {
        List<Column> result = new ArrayList<>();

        for (int col = 0; col < Constants.COLUMN_COUNT_IN_GRID; col++) {
            List<OptionalInt> columnCellValues = new ArrayList<>();
            for (int row = 0; row < Constants.ROW_COUNT_IN_GRID; row++) {
                columnCellValues.add(cell(row, col).valueOption());
            }
            result.add(new Column(col, ImmutableList.copyOf(columnCellValues)));
        }
        return ImmutableList.copyOf(result);
    }

    public ImmutableList<Block> blocks() {
        return blockUpperLeftIndices().stream()
                .map(idx -> blockAtUpperLeftIndex(idx.rowNumber(), idx.columnNumber()))
                .collect(ImmutableList.toImmutableList());
    }

    public Row row(int rowNumber) {
        Preconditions.checkArgument(rowNumber >= 0 && rowNumber < Constants.ROW_COUNT_IN_GRID);

        return rows.get(rowNumber);
    }

    public Column column(int columnNumber) {
        Preconditions.checkArgument(columnNumber >= 0 && columnNumber < Constants.COLUMN_COUNT_IN_GRID);

        List<OptionalInt> columnCellValues = new ArrayList<>();
        for (int row = 0; row < Constants.ROW_COUNT_IN_GRID; row++) {
            columnCellValues.add(cell(row, columnNumber).valueOption());
        }
        return new Column(columnNumber, ImmutableList.copyOf(columnCellValues));
    }

    public Block containingBlock(int rowNumber, int columnNumber) {
        Preconditions.checkArgument(rowNumber >= 0 && rowNumber < Constants.ROW_COUNT_IN_GRID);
        Preconditions.checkArgument(columnNumber >= 0 && columnNumber < Constants.COLUMN_COUNT_IN_GRID);

        int upperLeftRowNumber = 3 * (rowNumber / 3);
        int upperLeftColumnNumber = 3 * (columnNumber / 3);
        return blockAtUpperLeftIndex(upperLeftRowNumber, upperLeftColumnNumber);
    }

    public Cell cell(int rowNumber, int columnNumber) {
        Preconditions.checkArgument(rowNumber >= 0 && rowNumber < Constants.ROW_COUNT_IN_GRID);
        Preconditions.checkArgument(columnNumber >= 0 && columnNumber < Constants.COLUMN_COUNT_IN_GRID);

        return row(rowNumber).cell(columnNumber);
    }

    @Override
    public ImmutableList<OptionalInt> optionalValues() {
        return rows.stream()
                .flatMap(row -> row.optionalValues().stream())
                .collect(ImmutableList.toImmutableList());
    }

    @Override
    public ImmutableList<Cell> cells() {
        return rows.stream()
                .flatMap(row -> row.cells().stream())
                .collect(ImmutableList.toImmutableList());
    }

    @Override
    public boolean isFilled() {
        return rows().stream().allMatch(Row::isFilled);
    }

    @Override
    public boolean isStillValid() {
        return rows().stream().allMatch(Row::isStillValid) &&
                columns().stream().allMatch(Column::isStillValid) &&
                blocks().stream().allMatch(Block::isStillValid);
    }

    public ImmutableSet<CellPosition> filledCellPositions() {
        return cells().stream()
                .filter(Cell::isFilled)
                .map(Cell::cellPosition)
                .collect(ImmutableSet.toImmutableSet());
    }

    public ImmutableList<CellPosition> blockUpperLeftIndices() {
        List<CellPosition> result = new ArrayList<>();

        for (int x = 0; x < Constants.NUMBER_OF_BLOCK_ROWS; x++) {
            for (int y = 0; y < Constants.NUMBER_OF_BLOCK_COLUMNS; y++) {
                result.add(new CellPosition(x * 3, y * 3));
            }
        }
        return ImmutableList.copyOf(result);
    }

    /**
     * Returns true if the grid is a solved Sudoku. This does not necessarily mean that this
     * solution is the only solution.
     */
    public boolean isSolved() {
        return isFilled() && isStillValid();
    }

    /**
     * Fills the cell at the given position with the given value, returning the resulting grid wrapped in an Optional.
     * If the given cell was already filled, nothing changes to the grid, and an empty Optional
     * is returned.
     */
    public Optional<Grid> fillCellIfEmpty(CellPosition pos, int value) {
        Preconditions.checkArgument(value >= 1 && value <= 9);

        return cell(pos.rowNumber(), pos.columnNumber()).valueOption().isPresent() ?
                Optional.empty() :
                Optional.of(this.updateCell(pos, OptionalInt.of(value)));
    }

    public Grid updateCell(CellPosition pos, OptionalInt valueOption) {
        Preconditions.checkArgument(valueOption.stream().allMatch(v -> v >= 1 && v <= 9));

        Row rowToUpdate = row(pos.rowNumber());
        Row updatedRow = new Row(
                pos.rowNumber(),
                ImmutableList.<OptionalInt>builder()
                        .addAll(rowToUpdate.optionalValues().subList(0, pos.columnNumber()))
                        .add(valueOption)
                        .addAll(rowToUpdate.optionalValues().subList(pos.columnNumber() + 1, rowToUpdate.cells().size()))
                        .build()
        );
        return new Grid(
                OptionalLong.empty(),
                ImmutableList.<Row>builder()
                        .addAll(rows.subList(0, pos.rowNumber()))
                        .add(updatedRow)
                        .addAll(rows.subList(pos.rowNumber() + 1, rows.size()))
                        .build()
        );
    }

    public String show() {
        String fmt =
                """
                        %s
                        %s
                        %s
                        ---------------------
                        %s
                        %s
                        %s
                        ---------------------
                        %s
                        %s
                        %s""";
        return String.format(
                fmt,
                rows.get(0).show(),
                rows.get(1).show(),
                rows.get(2).show(),
                rows.get(3).show(),
                rows.get(4).show(),
                rows.get(5).show(),
                rows.get(6).show(),
                rows.get(7).show(),
                rows.get(8).show()
        );
    }

    private Block blockAtUpperLeftIndex(int rowNumber, int columnNumber) {
        Preconditions.checkArgument(rowNumber % 3 == 0);
        Preconditions.checkArgument(columnNumber % 3 == 0);

        List<OptionalInt> optValues = new ArrayList<>();
        for (int x = 0; x < Constants.ROW_COUNT_IN_BLOCK; x++) {
            for (int y = 0; y < Constants.COLUMN_COUNT_IN_BLOCK; y++) {
                optValues.add(cell(rowNumber + x, columnNumber + y).valueOption());
            }
        }
        return new Block(rowNumber, columnNumber, ImmutableList.copyOf(optValues));
    }
}
