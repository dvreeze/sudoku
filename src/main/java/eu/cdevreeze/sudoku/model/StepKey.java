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

package eu.cdevreeze.sudoku.model;

import java.time.Instant;

/**
 * Composite Step key.
 *
 * @author Chris de Vreeze
 */
public record StepKey(long gameHistoryId, Instant stepDateTime) implements Comparable<StepKey> {

    @Override
    public int compareTo(StepKey otherStepKey) {
        int gameHistoryIdComparison = Long.compare(gameHistoryId(), otherStepKey.gameHistoryId());
        return gameHistoryIdComparison == 0 ?
                stepDateTime().compareTo(otherStepKey.stepDateTime()) :
                gameHistoryIdComparison;
    }
}
