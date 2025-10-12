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

/**
 * One step in a Sudoku game. The row and column numbers are coordinates in the grid, and not relative
 * coordinates in a block (that would be relative to the left upper corner of the block).
 *
 * @author Chris de Vreeze
 */
public record Step(StepKey stepKey, int rowNumber, int columnNumber, int value) {

    public Step {
        Preconditions.checkArgument(rowNumber >= 0);
        Preconditions.checkArgument(rowNumber < Constants.ROW_COUNT_IN_GRID);
        Preconditions.checkArgument(columnNumber >= 0);
        Preconditions.checkArgument(columnNumber < Constants.COLUMN_COUNT_IN_GRID);
        Preconditions.checkArgument(value >= 1 && value <= 9);
    }

    public CellPosition cellPosition() {
        return new CellPosition(rowNumber(), columnNumber());
    }
}
