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
 * Cell JPA entity. Each instance represents a row in the corresponding table.
 *
 * @author Chris de Vreeze
 */
@Entity(name = "Cell")
public class CellEntity {

    @Id
    @Column(name = "cell_id", nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "Cell_seq_gen")
    @SequenceGenerator(name = "Cell_seq_gen", sequenceName = "cell_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "grid_id")
    private GridEntity grid;

    @Column(name = "row_number", nullable = false, columnDefinition = "ROW_IDX")
    private Integer rowNumber;

    @Column(name = "column_number", nullable = false, columnDefinition = "COL_IDX")
    private Integer columnNumber;

    @Column(name = "cell_value", columnDefinition = "DIGIT")
    private Integer cellValue;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public GridEntity getGrid() {
        return grid;
    }

    public void setGrid(GridEntity grid) {
        this.grid = grid;
    }

    public Integer getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(Integer rowNumber) {
        this.rowNumber = rowNumber;
    }

    public Integer getColumnNumber() {
        return columnNumber;
    }

    public void setColumnNumber(Integer columnNumber) {
        this.columnNumber = columnNumber;
    }

    public Integer getCellValue() {
        return cellValue;
    }

    public void setCellValue(Integer cellValue) {
        this.cellValue = cellValue;
    }
}
