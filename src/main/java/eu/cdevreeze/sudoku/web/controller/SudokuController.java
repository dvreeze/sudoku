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

package eu.cdevreeze.sudoku.web.controller;

import eu.cdevreeze.sudoku.model.GameHistory;
import eu.cdevreeze.sudoku.model.Sudoku;
import eu.cdevreeze.sudoku.service.SudokuService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Web MVC controller for Sudoku games.
 *
 * @author Chris de Vreeze
 */
@Controller
public class SudokuController {

    private final SudokuService sudokuService;

    public SudokuController(SudokuService sudokuService) {
        this.sudokuService = sudokuService;
    }

    @GetMapping(value = "/sudokus")
    public String getSudoku(@RequestParam(name = "id") long sudokuId, Model model) {
        Sudoku sudoku = sudokuService.findSudoku(sudokuId).orElseThrow();
        model.addAttribute("sudoku", sudoku);

        return "sudoku";
    }

    @GetMapping(value = "/games")
    public String getGameHistory(@RequestParam(name = "id") long gameHistoryId, Model model) {
        GameHistory gameHistory = sudokuService.findGameHistory(gameHistoryId).orElseThrow();
        model.addAttribute("gameHistory", gameHistory);

        return "game";
    }
}
