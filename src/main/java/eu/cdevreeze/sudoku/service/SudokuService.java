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

package eu.cdevreeze.sudoku.service;

import eu.cdevreeze.sudoku.model.CellPosition;
import eu.cdevreeze.sudoku.model.GameHistory;
import eu.cdevreeze.sudoku.model.Grid;
import eu.cdevreeze.sudoku.model.Sudoku;

import java.time.Instant;
import java.util.Optional;

/**
 * Service for playing Sudoku games.
 *
 * @author Chris de Vreeze
 */
public interface SudokuService {

    /**
     * Creates a new Sudoku game, storing it in the database.
     * The start grid must not yet exist in the database, and it most have no ID yet.
     */
    Sudoku createSudoku(Grid startGrid);

    /**
     * Starts a new Sudoku game, storing it in the database as new {@link GameHistory}.
     */
    GameHistory startGame(long sudokuId, String player, Instant startTime);

    /**
     * Fill in one empty cell, to proceed with the Sudoku game, storing the result in the database.
     */
    GameHistory fillInEmptyCell(long gameHistoryId, CellPosition pos, int value);

    Optional<Grid> findGrid(long gridId);

    Optional<Sudoku> findSudoku(long sudokuId);

    Optional<GameHistory> findGameHistory(long gameHistoryId);
}
