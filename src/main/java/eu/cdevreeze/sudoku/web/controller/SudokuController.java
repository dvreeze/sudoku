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

import eu.cdevreeze.sudoku.model.CellPosition;
import eu.cdevreeze.sudoku.model.GameHistory;
import eu.cdevreeze.sudoku.model.Sudoku;
import eu.cdevreeze.sudoku.service.SudokuService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Instant;

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
        model.addAttribute("title", String.format("Sudoku game %s", gameHistory.idOption().orElseThrow()));

        return "game";
    }

    @GetMapping(value = "/startSampleGame")
    public String startGame(@RequestParam(name = "id") long sudokuId, Model model) {
        GameHistory gameHistory = sudokuService.startGame(sudokuId, "Sample", Instant.now());
        model.addAttribute("gameHistory", gameHistory);
        model.addAttribute("title", String.format("Sudoku game %s", gameHistory.idOption().orElseThrow()));

        return "game";
    }

    @GetMapping(value = "/sampleGame")
    public String nextStep(
            @RequestParam(name = "id") long gameHistoryId,
            @RequestParam(name = "row") int row,
            @RequestParam(name = "col") int col,
            @RequestParam(name = "value") int value,
            Model model
    ) {
        GameHistory gameHistory = sudokuService.fillInEmptyCell(gameHistoryId, CellPosition.of(row, col), value);
        model.addAttribute("gameHistory", gameHistory);
        model.addAttribute("title", String.format("Sudoku game %s", gameHistory.idOption().orElseThrow()));

        return "game";
    }
}
