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
 * Service layer, as technology-agnostic Java interfaces, implemented by concrete Java classes
 * in a sub-package.
 * <p>
 * The concrete implementation classes take care of transaction management, database access
 * (either directly or indirectly), etc.
 * <p>
 * The service API in this package is completely technology-agnostic, and can easily be mocked in
 * unit tests. That is, the service interfaces are purely abstract, and the exchanged data objects
 * are model objects as immutable Java records. The latter objects are also technology-agnostic, in
 * contrast to JPA entities.
 * <p>
 * Finally, note that from an API point of view, the "query" functions in the service interfaces are pure
 * functions, taking immutable data arguments and returning immutable data objects as well. Of course,
 * the service implementations do have side effects, but as far as the service API is concerned those
 * are implementation details.
 *
 * @author Chris de Vreeze
 */
@NullMarked
package eu.cdevreeze.sudoku.service;

import org.jspecify.annotations.NullMarked;
