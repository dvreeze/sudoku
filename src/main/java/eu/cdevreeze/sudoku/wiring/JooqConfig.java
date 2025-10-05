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

package eu.cdevreeze.sudoku.wiring;

import org.jooq.impl.DefaultConfiguration;
import org.springframework.boot.jooq.autoconfigure.DefaultConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Minor changes to the jOOQ Configuration construction.
 * <p>
 * See <a href="https://blog.jooq.org/how-to-customise-a-jooq-configuration-that-is-injected-using-spring-boot/">customize jOOQ in a Spring Boot application</a>.
 * for how to customize a jOOQ Configuration in a Spring Boot application.
 * <p>
 * See <a href="https://www.jooq.org/doc/latest/manual/sql-building/dsl-context/custom-settings/settings-fetching-trimmed-strings/">jOOQ fetchTrimmedCharValues setting</a>
 * for the "fetchTrimmedCharValues" setting.
 * <p>
 * See <a href="https://www.jooq.org/doc/latest/manual/sql-building/dsl-context/custom-settings/settings-in-list-padding/">IN list padding</a>
 * for "IN list padding", to reduce the number of SQL query strings.
 * <p>
 * See <a href="https://www.jooq.org/doc/latest/manual/sql-building/dsl-context/custom-settings/settings-map-jpa/">map JPA annotations</a>
 * for mapping of JPA annotations.
 *
 * @author Chris de Vreeze
 */
@Configuration
public class JooqConfig {

    @Bean
    public DefaultConfigurationCustomizer configurationCustomizer() {
        return (DefaultConfiguration c) -> c.settings()
                .withFetchTrimmedCharValues(true)
                .withInListPadding(true)
                .withInListPadBase(4)
                .withMapJPAAnnotations(false)
                .withRenderFormatted(true);
    }
}
