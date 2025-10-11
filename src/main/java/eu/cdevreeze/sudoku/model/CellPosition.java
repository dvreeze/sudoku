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
 * Pair of row number and column number. It can only be used to point at cells in the grid, not at cells
 * in a block.
 *
 * @author Chris de Vreeze
 */
public record CellPosition(int rowNumber, int columnNumber) implements Comparable<CellPosition> {

    public CellPosition {
        Preconditions.checkArgument(rowNumber >= 0 && rowNumber < Constants.ROW_COUNT_IN_GRID);
        Preconditions.checkArgument(columnNumber >= 0 && columnNumber < Constants.COLUMN_COUNT_IN_GRID);
    }

    public static CellPosition of(int rowNumber, int columnNumber) {
        return new CellPosition(rowNumber, columnNumber);
    }

    @Override
    public int compareTo(CellPosition otherPos) {
        int rowComparison = Integer.compare(this.rowNumber(), otherPos.rowNumber());

        return rowComparison == 0 ?
                Integer.compare(this.columnNumber(), otherPos.columnNumber()) :
                rowComparison;
    }
}
