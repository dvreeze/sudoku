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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.util.Map;
import java.util.OptionalInt;

/**
 * Row, column or block in a Sudoku grid. It always contains 9 cells.
 *
 * @author Chris de Vreeze
 */
public sealed interface CellGroup extends CellGroupOrGrid permits Row, Column, Block {

    @Override
    ImmutableList<Cell> cells();

    @Override
    default ImmutableList<OptionalInt> optionalValues() {
        return cells().stream()
                .map(Cell::valueOption)
                .collect(ImmutableList.toImmutableList());
    }

    @Override
    default boolean isFilled() {
        return cells().stream().allMatch(Cell::isFilled);
    }

    @Override
    default boolean isStillValid() {
        return containsNoDuplicates();
    }

    default boolean containsNoDuplicates() {
        return duplicates().isEmpty();
    }

    default ImmutableMap<Integer, ImmutableSet<Cell>> duplicates() {
        return cells()
                .stream()
                .filter(Cell::isFilled)
                .collect(
                        ImmutableMap.toImmutableMap(
                                c -> c.valueOption().orElseThrow(),
                                ImmutableSet::of,
                                (cells1, cells2) -> ImmutableSet.<Cell>builder().addAll(cells1).addAll(cells2).build()
                        )
                )
                .entrySet()
                .stream()
                .filter(kv -> kv.getValue().size() >= 2)
                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
