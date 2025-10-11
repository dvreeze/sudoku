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

import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.stream.IntStream;

/**
 * Column of 9 cells in a Sudoku grid.
 *
 * @author Chris de Vreeze
 */
public record Column(int columnNumber, ImmutableList<Cell> cells) implements CellGroup {

    public Column {
        Preconditions.checkArgument(columnNumber >= 0 && columnNumber < Constants.COLUMN_COUNT_IN_GRID);
        Preconditions.checkArgument(
                cells.stream().map(Cell::cellPosition).toList().equals(
                        IntStream.range(0, Constants.ROW_COUNT_IN_GRID)
                                .mapToObj(i -> new CellPosition(i, columnNumber))
                                .toList()
                )
        );
    }

    /**
     * Creates a {@link Column} from the given column number and optional values.
     * The cells in the created row have no cell IDs.
     */
    public static Column from(int columnNumber, ImmutableList<OptionalInt> optionalValues) {
        Preconditions.checkArgument(columnNumber >= 0 && columnNumber < Constants.COLUMN_COUNT_IN_GRID);
        Preconditions.checkArgument(optionalValues.size() == Constants.ROW_COUNT_IN_GRID);

        ImmutableList<Cell> cells = IntStream.range(0, optionalValues.size())
                .boxed()
                .map(idx -> new Cell(OptionalLong.empty(), idx, columnNumber, optionalValues.get(idx)))
                .collect(ImmutableList.toImmutableList());
        return new Column(columnNumber, cells);
    }

    public Cell cell(int rowNumber) {
        Preconditions.checkArgument(rowNumber >= 0 && rowNumber < Constants.ROW_COUNT_IN_GRID);
        return cells.get(rowNumber);
    }
}
