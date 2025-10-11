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

import java.util.ArrayList;
import java.util.List;

/**
 * Grid JPA entity. Each instance represents a row in the corresponding table.
 *
 * @author Chris de Vreeze
 */
@Entity(name = "Grid")
public class GridEntity {

    @Id
    @Column(name = "grid_id", nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "Grid_seq_gen")
    @SequenceGenerator(name = "Grid_seq_gen", sequenceName = "grid_id_seq", allocationSize = 1)
    private Long id;

    @OneToMany(mappedBy = "grid")
    private List<CellEntity> cells = new ArrayList<>();

    public void addCell(CellEntity cell) {
        cells.add(cell);
        cell.setGrid(this);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<CellEntity> getCells() {
        return cells;
    }

    public void setCells(List<CellEntity> cells) {
        this.cells = cells;
    }
}
