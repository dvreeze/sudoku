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

/**
 * Constants, such as row count and column count in na grid.
 *
 * @author Chris de Vreeze
 */
public class Constants {

    private Constants() {
    }

    public static final int ROW_COUNT_IN_GRID = 9;
    public static final int COLUMN_COUNT_IN_GRID = 9;

    public static final int CELL_COUNT_IN_BLOCK = 9;

    public static final int ROW_COUNT_IN_BLOCK = 3;
    public static final int COLUMN_COUNT_IN_BLOCK = 3;

    public static final int NUMBER_OF_BLOCK_ROWS = 3;
    public static final int NUMBER_OF_BLOCK_COLUMNS = 3;
}
