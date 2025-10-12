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

import com.google.common.base.Preconditions;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

/**
 * Step JPA entity. Each instance represents a row in the corresponding table.
 *
 * @author Chris de Vreeze
 */
@Entity(name = "Step")
@IdClass(StepEntityKey.class)
public class StepEntity {

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_history_id", nullable = false)
    private GameHistoryEntity gameHistory;

    @Id
    @Column(name = "step_time", nullable = false)
    private OffsetDateTime stepDateTime;

    @Column(name = "row_number", nullable = false, columnDefinition = "ROW_IDX")
    private Integer rowNumber;

    @Column(name = "column_number", nullable = false, columnDefinition = "COL_IDX")
    private Integer columnNumber;

    @Column(name = "step_value", nullable = false, columnDefinition = "DIGIT")
    private Integer stepValue;

    public StepEntityKey getStepKey() {
        Preconditions.checkArgument(gameHistory != null);
        Preconditions.checkArgument(stepDateTime != null);
        return new StepEntityKey(gameHistory, stepDateTime);
    }

    public GameHistoryEntity getGameHistory() {
        return gameHistory;
    }

    public void setGameHistory(GameHistoryEntity gameHistory) {
        this.gameHistory = gameHistory;
    }

    public OffsetDateTime getStepDateTime() {
        return stepDateTime;
    }

    public void setStepDateTime(OffsetDateTime stepDateTime) {
        this.stepDateTime = stepDateTime;
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
