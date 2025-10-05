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

/**
 * Service layer implementation using jOOQ.
 * <p>
 * In a SQL-oriented approach to database access logic implementation, jOOQ makes JPA much less
 * needed, in my opinion. In the past, JPA was needed to populate nested Java object graphs from
 * SQL query result sets, or else we were using the Spring JdbcTemplate to execute SQL, with the
 * boilerplate to compose SQL strings and to process result sets. Yet at least the JdbcTemplate
 * made it very clear which exact SQL statements were executed at runtime.
 * <p>
 * Nowadays, jOOQ makes that error-prone SQL composition and result set handling completely unnecessary,
 * while still allowing for seamless integration with Spring-managed transactions. The evolution of
 * SQL also helps (e.g. multiset, whether emulated using SQL JSON support or natively supported).
 * With jOOQ we build SQL in a type-safe way, to a large extent checked by the compiler, while using
 * SQL features (emulated or native for the dialect) that are now commonplace but still underused
 * (maybe because JPA does not support them yet, making these features less well-known).
 * <p>
 * With SQL being much more expressive than in the past, and with jOOQ being SQL-oriented in a very
 * disciplined way, exposing SQL creation as a fluent Java API, I think jOOQ is an excellent choice
 * as Java database library.
 *
 * @author Chris de Vreeze
 */
@NullMarked
package eu.cdevreeze.sudoku.service.jooqimpl;

import org.jspecify.annotations.NullMarked;
