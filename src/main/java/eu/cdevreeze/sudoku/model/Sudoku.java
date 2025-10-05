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

import java.util.OptionalLong;

/**
 * A Sudoku game as it could be found in a puzzle book. It contains a start grid. This class is not
 * used for X-Sudoku games.
 *
 * @author Chris de Vreeze
 */
public record Sudoku(OptionalLong idOption, Grid startGrid) {
}
