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

import java.time.Instant;
import java.util.Comparator;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.stream.Gatherers;

/**
 * Sudoku game in progress (or completed), with history. The same Sudoku (game) can be played
 * multiple times, each having its own game history.
 *
 * @author Chris de Vreeze
 */
public record GameHistory(
        OptionalLong idOption,
        String player,
        Instant startTime,
        Sudoku sudoku,
        ImmutableList<Step> steps) {

    public GameHistory {
        Preconditions.checkArgument(
                steps.stream().sorted(Comparator.comparing(Step::stepKey)).toList()
                        .equals(steps)
        );
        gridHistory(sudoku.startGrid(), steps);
    }

    public ImmutableList<Grid> gridHistory() {
        return gridHistory(sudoku().startGrid(), steps);
    }

    public Grid currentGrid() {
        return (steps.isEmpty()) ? sudoku().startGrid() : gridHistory().getLast();
    }

    public boolean isSolved() {
        return currentGrid().isSolved();
    }

    public boolean isStillValid() {
        return currentGrid().isStillValid();
    }

    public GameHistory slice(int stepToIndex) {
        Preconditions.checkArgument(stepToIndex >= 0 && stepToIndex <= steps.size());
        return new GameHistory(
                OptionalLong.empty(),
                player(),
                startTime(),
                sudoku(),
                steps().subList(0, stepToIndex)
        );
    }

    private static ImmutableList<Grid> gridHistory(Grid startGrid, ImmutableList<Step> steps) {
        return steps.stream()
                .gather(Gatherers.scan(
                        () -> startGrid,
                        (Grid accGrid, Step step) -> {
                            Optional<Grid> resultOption = accGrid.fillCellIfEmpty(step.cellPosition(), step.value());
                            Preconditions.checkArgument(resultOption.isPresent(), "Expected allowed step, but got step " + step);
                            return resultOption.orElseThrow();
                        }
                ))
                .collect(ImmutableList.toImmutableList());
    }
}
