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

import com.google.common.collect.ImmutableList;

import java.util.OptionalInt;

/**
 * Either a {@link CellGroup} or a {@link Grid}.
 *
 * @author Chris de Vreeze
 */
public sealed interface CellGroupOrGrid permits CellGroup, Grid {

    /**
     * Returns all cells as optional values, whether filled or not.
     * Unlike method {@link CellGroupOrGrid#cells}, this method returns no cell coordinates.
     * Yet like method {@link CellGroupOrGrid#cells}, the optional values are returned in the
     * same order as the cells.
     */
    ImmutableList<OptionalInt> optionalValues();

    /**
     * Returns all cells (with coordinates), whether filled or not.
     */
    ImmutableList<Cell> cells();

    boolean isFilled();

    boolean isStillValid();
}
