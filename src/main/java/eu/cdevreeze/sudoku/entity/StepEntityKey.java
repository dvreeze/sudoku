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

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * Composite Step key, to be used as an {@link jakarta.persistence.IdClass}.
 * Note that the key contains an associated entity.
 *
 * @author Chris de Vreeze
 */
public class StepEntityKey implements Serializable {

    private GameHistoryEntity gameHistory;

    private OffsetDateTime stepDateTime;

    public StepEntityKey() {
    }

    public StepEntityKey(GameHistoryEntity gameHistory, OffsetDateTime stepDateTime) {
        this.gameHistory = gameHistory;
        this.stepDateTime = stepDateTime;
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
}
