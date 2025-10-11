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
 * Step JPA entity. Each instance represents a row in the corresponding table.
 *
 * @author Chris de Vreeze
 */
@Entity(name = "Step")
public class StepEntity {

    @EmbeddedId
    private StepEntityKey stepKey;

    @MapsId("gameHistoryId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_history_id", nullable = false)
    private GameHistoryEntity gameHistory;

    @Column(name = "row_number", nullable = false, columnDefinition = "ROW_IDX")
    private Integer rowNumber;

    @Column(name = "column_number", nullable = false, columnDefinition = "COL_IDX")
    private Integer columnNumber;

    @Column(name = "step_value", nullable = false, columnDefinition = "DIGIT")
    private Integer stepValue;

    public StepEntityKey getStepKey() {
        return stepKey;
    }

    public void setStepKey(StepEntityKey stepKey) {
        this.stepKey = stepKey;
    }

    public Long getGameHistoryId() {
        return stepKey.gameHistoryId();
    }

    public Integer getStepSeqNumber() {
        return stepKey.stepSeqNumber();
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

    public Integer getStepValue() {
        return stepValue;
    }

    public void setStepValue(Integer stepValue) {
        this.stepValue = stepValue;
    }
}
