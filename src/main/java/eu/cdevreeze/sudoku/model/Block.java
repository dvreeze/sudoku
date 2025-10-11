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

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.stream.IntStream;

/**
 * Block of 3 rows of 3 cells in a Sudoku grid.
 *
 * @author Chris de Vreeze
 */
public record Block(
        int upperLeftRowNumber,
        int upperLeftColumnNumber,
        ImmutableList<OptionalInt> optionalValues
) implements CellGroup {

    public Block {
        Preconditions.checkArgument(upperLeftRowNumber >= 0 && upperLeftRowNumber < Constants.ROW_COUNT_IN_GRID);
        Preconditions.checkArgument(upperLeftColumnNumber >= 0 && upperLeftColumnNumber < Constants.COLUMN_COUNT_IN_GRID);
        Preconditions.checkArgument(upperLeftRowNumber % 3 == 0);
        Preconditions.checkArgument(upperLeftColumnNumber % 3 == 0);
        Preconditions.checkArgument(optionalValues.size() == Constants.CELL_COUNT_IN_BLOCK);
    }

    public ImmutableList<Block.Row> blockRows() {
        List<Block.Row> blockRows = new ArrayList<>();
        for (int r = 0; r < Constants.ROW_COUNT_IN_BLOCK; r++) {
            List<Cell> blockRowCells = new ArrayList<>();
            for (int c = 0; c < Constants.COLUMN_COUNT_IN_BLOCK; c++) {
                blockRowCells.add(cell(r, c));
            }
            blockRows.add(new Block.Row(ImmutableList.copyOf(blockRowCells)));
        }
        return ImmutableList.copyOf(blockRows);
    }

    public ImmutableList<Block.Column> blockColumns() {
        List<Block.Column> result = new ArrayList<>();

        for (int c = 0; c < Constants.COLUMN_COUNT_IN_BLOCK; c++) {
            List<Cell> columnCells = new ArrayList<>();
            for (int r = 0; r < Constants.ROW_COUNT_IN_BLOCK; r++) {
                columnCells.add(cell(r, c));
            }
            result.add(new Block.Column(ImmutableList.copyOf(columnCells)));
        }
        return ImmutableList.copyOf(result);
    }

    public Block.Row blockRow(int blockRowNumber) {
        Preconditions.checkArgument(blockRowNumber >= 0 && blockRowNumber < Constants.ROW_COUNT_IN_BLOCK);

        List<Cell> blockRowCells = new ArrayList<>();
        for (int col = 0; col < Constants.COLUMN_COUNT_IN_BLOCK; col++) {
            blockRowCells.add(cell(blockRowNumber, col));
        }
        return new Block.Row(ImmutableList.copyOf(blockRowCells));
    }

    public Block.Column blockColumn(int blockColumnNumber) {
        Preconditions.checkArgument(blockColumnNumber >= 0 && blockColumnNumber < Constants.COLUMN_COUNT_IN_BLOCK);

        List<Cell> blockColumnCells = new ArrayList<>();
        for (int row = 0; row < Constants.ROW_COUNT_IN_BLOCK; row++) {
            blockColumnCells.add(cell(row, blockColumnNumber));
        }
        return new Block.Column(ImmutableList.copyOf(blockColumnCells));
    }

    /**
     * Returns the {@link Cell} at the given row and column numbers (0-based).
     * The database ID of the cell, if any, is lost.
     */
    public Cell cell(int blockRowNumber, int blockColumnNumber) {
        Preconditions.checkArgument(blockRowNumber >= 0 && blockRowNumber < Constants.ROW_COUNT_IN_BLOCK);
        Preconditions.checkArgument(blockColumnNumber >= 0 && blockColumnNumber < Constants.COLUMN_COUNT_IN_BLOCK);

        return new Cell(
                OptionalLong.empty(),
                upperLeftRowNumber + blockRowNumber,
                upperLeftColumnNumber + blockColumnNumber,
                optionalValues().get((3 * blockRowNumber) + blockColumnNumber)
        );
    }

    @Override
    public ImmutableList<Cell> cells() {
        return blockRows().stream()
                .flatMap(blockRow -> blockRow.cells().stream())
                .collect(ImmutableList.toImmutableList());
    }

    public record Row(ImmutableList<Cell> cells) {

        public Row {
            Preconditions.checkArgument(cells.size() == Constants.COLUMN_COUNT_IN_BLOCK);
            Preconditions.checkArgument(cells.stream().map(Cell::rowNumber).distinct().count() == 1);
            Preconditions.checkArgument(cells.stream().map(Cell::columnNumber).toList()
                    .equals(
                            IntStream.range(cells.getFirst().columnNumber(), cells.getFirst().columnNumber() + 3)
                                    .boxed()
                                    .toList()));
        }

        public Cell cell(int index) {
            Preconditions.checkArgument(index >= 0 && index < Constants.COLUMN_COUNT_IN_BLOCK);
            return cells.get(index);
        }
    }

    public record Column(ImmutableList<Cell> cells) {

        public Column {
            Preconditions.checkArgument(cells.size() == Constants.ROW_COUNT_IN_BLOCK);
            Preconditions.checkArgument(cells.stream().map(Cell::rowNumber).toList()
                    .equals(
                            IntStream.range(cells.getFirst().rowNumber(), cells.getFirst().rowNumber() + 3)
                                    .boxed()
                                    .toList()));
            Preconditions.checkArgument(cells.stream().map(Cell::columnNumber).distinct().count() == 1);
        }

        public Cell cell(int index) {
            Preconditions.checkArgument(index >= 0 && index < Constants.ROW_COUNT_IN_BLOCK);
            return cells.get(index);
        }
    }
}
