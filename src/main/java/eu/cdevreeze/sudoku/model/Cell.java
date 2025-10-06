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

import java.util.OptionalInt;

/**
 * One cell in a Sudoku grid. The row and column numbers are coordinates in the grid, and not relative
 * coordinates in a block (that would be relative to the left upper corner of the block).
 *
 * @author Chris de Vreeze
 */
public record Cell(int rowNumber, int columnNumber, OptionalInt valueOption) {

    public Cell {
        Preconditions.checkArgument(rowNumber >= 0);
        Preconditions.checkArgument(rowNumber < Constants.ROW_COUNT_IN_GRID);
        Preconditions.checkArgument(columnNumber >= 0);
        Preconditions.checkArgument(columnNumber < Constants.COLUMN_COUNT_IN_GRID);
        Preconditions.checkArgument(valueOption.stream().allMatch(v -> v >= 1 && v <= 9));
    }

    public boolean isFilled() {
        return valueOption().isPresent();
    }

    public boolean isEmpty() {
        return valueOption().isEmpty();
    }

    public CellPosition cellPosition() {
        return new CellPosition(rowNumber(), columnNumber());
    }

    public String valueOptionAsString() {
        return valueOption().stream().boxed().map(String::valueOf).findFirst().orElse("");
    }
}
