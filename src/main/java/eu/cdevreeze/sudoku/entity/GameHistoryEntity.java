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

package eu.cdevreeze.sudoku.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * GameHistory JPA entity. Each instance represents a row in the corresponding table.
 *
 * @author Chris de Vreeze
 */
@Entity(name = "GameHistory")
public class GameHistoryEntity {

    @Id
    @Column(name = "game_history_id", nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "Game_history_seq_gen")
    @SequenceGenerator(name = "Game_history_seq_gen", sequenceName = "game_history_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sudoku_id")
    private SudokuEntity sudoku;

    @Column(name = "player", nullable = false)
    private String player;

    @Column(name = "start_time", nullable = false)
    private OffsetDateTime startTime;

    @OneToMany(mappedBy = "gameHistory")
    private List<StepEntity> steps = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SudokuEntity getSudoku() {
        return sudoku;
    }

    public void setSudoku(SudokuEntity sudoku) {
        this.sudoku = sudoku;
    }

    public String getPlayer() {
        return player;
    }

    public void setPlayer(String player) {
        this.player = player;
    }

    public OffsetDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(OffsetDateTime startTime) {
        this.startTime = startTime;
    }

    public List<StepEntity> getSteps() {
        return steps;
    }

    public void setSteps(List<StepEntity> steps) {
        this.steps = steps;
    }
}
