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

/**
 * Sudoku JPA entity. Each instance represents a row in the corresponding table.
 *
 * @author Chris de Vreeze
 */
@Entity(name = "Sudoku")
public class SudokuEntity {

    @Id
    @Column(name = "sudoku_id", nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "Sudoku_seq_gen")
    @SequenceGenerator(name = "Sudoku_seq_gen", sequenceName = "sudoku_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "start_grid_id")
    private GridEntity startGrid;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public GridEntity getStartGrid() {
        return startGrid;
    }

    public void setStartGrid(GridEntity startGrid) {
        this.startGrid = startGrid;
    }
}
