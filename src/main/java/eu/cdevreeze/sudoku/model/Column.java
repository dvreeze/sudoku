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
public record Column(int columnNumber, ImmutableList<OptionalInt> optionalValues) implements CellGroup {

    public Column {
        Preconditions.checkArgument(columnNumber >= 0 && columnNumber < Constants.COLUMN_COUNT_IN_GRID);
        Preconditions.checkArgument(optionalValues.size() == Constants.ROW_COUNT_IN_GRID);
    }

    /**
     * Returns all {@link Cell} instances, in the correct order.
     * The database IDs of the cells, if any, are lost.
     */
    public ImmutableList<Cell> cells() {
        return IntStream.range(0, optionalValues().size())
                .boxed()
                .map(idx -> new Cell(OptionalLong.empty(), idx, columnNumber(), optionalValues().get(idx)))
                .collect(ImmutableList.toImmutableList());
    }

    /**
     * Returns the {@link Cell} at the given row number (0-based).
     * The database ID of the cell, if any, is lost.
     */
    public Cell cell(int rowNumber) {
        Preconditions.checkArgument(rowNumber >= 0 && rowNumber < Constants.ROW_COUNT_IN_GRID);
        return new Cell(OptionalLong.empty(), rowNumber, columnNumber(), optionalValues().get(rowNumber));
    }
}
