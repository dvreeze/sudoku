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
 * Domain as JPA entity classes. These entity classes are great as Java representations
 * of database data, and can easily be synchronized with the database (in both directions).
 * <p>
 * Yet they are very poor data transfer objects. After all, they are highly mutable, are often
 * proxied (as instances of generated proxy subclasses), and looking at entities in isolation
 * does not tell us to what extent associations have been loaded. Hence, the existence of a
 * separate immutable model that represents exactly the same domain.
 * <p>
 * Note that associations between entities have been made lazy in this model. See
 * <a href="https://docs.jboss.org/hibernate/orm/7.0/introduction/html_single/Hibernate_Introduction.html#many-to-one">many-to-one</a>.
 * Note that one-to-many and many-to-many associations are already lazy by default in JPA.
 * It is only for one-to-one and many-to-one associations that we override the default (of eager fetching).
 * In other words, this practice embraces the flexibility of the relational model, joining tables on an ad-hoc basis.
 * That is, in this way we work with the database and not against it. Combining this with the use
 * of entity graphs during ad-hoc querying our JPQL/Criteria queries are much closer to the generated
 * SQL, while avoiding the N + 1 problem.
 * <p>
 * Indeed, JPQL can be seen conceptually as an object-oriented SQL dialect, making it more pleasant
 * to access the database from Java code. Joins are witten as "path navigation", and result sets are
 * "automatically" converted to nested entity object graphs (especially when using entity graphs).
 * <p>
 * Also note that our criteria queries allow us to work with the database in a fully type-safe way.
 * If the database schema changes, the validation of the JPA entity classes against the schema likely
 * fails. After fixing that, an updated JPA metamodel is generated, and the criteria queries are checked
 * by the Java compiler against that updated metamodel.
 *
 * @author Chris de Vreeze
 */
@NullUnmarked
package eu.cdevreeze.sudoku.entity;

import org.jspecify.annotations.NullUnmarked;
