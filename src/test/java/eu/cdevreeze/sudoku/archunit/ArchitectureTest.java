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

package eu.cdevreeze.sudoku.archunit;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Unit test testing the application architecture, using <a href="https://www.archunit.org/">ArchUnit</a>.
 * <p>
 * Note that the ArchUnit library is also endorsed by Spring, even to the extent that
 * <a href="https://spring.io/projects/spring-modulith">Spring Modulith</a> is built on top of it.
 * This project does not use Modulith, however, since it uses "traditional" application layering.
 *
 * @author Chris de Vreeze
 */
@AnalyzeClasses(packages = "eu.cdevreeze.sudoku")
public class ArchitectureTest {

    // No cycles allowed

    @ArchTest
    public static final ArchRule noCyclesRule =
            slices().matching("eu.cdevreeze.sudoku.(*)..").should().beFreeOfCycles();

    // Strict layering enforced

    @ArchTest
    public static final ArchRule simpleLayeringRule =
            layeredArchitecture()
                    .consideringOnlyDependenciesInLayers()
                    .layer("Model").definedBy("..model..")
                    .layer("Service").definedBy("..service..")
                    .layer("Web").definedBy("..web..")
                    .whereLayer("Web").mayNotBeAccessedByAnyLayer()
                    .whereLayer("Service").mayOnlyBeAccessedByLayers("Web")
                    .whereLayer("Model").mayNotAccessAnyLayer();

    @ArchTest
    public static final ArchRule layeringRule =
            layeredArchitecture()
                    .consideringOnlyDependenciesInLayers()
                    .layer("Model").definedBy("..model..")
                    .layer("Entity").definedBy("..entity..")
                    .layer("Service").definedBy("..service..")
                    .layer("Web").definedBy("..web..")
                    .layer("Wiring").definedBy("..wiring..")
                    .whereLayer("Wiring").mayNotBeAccessedByAnyLayer()
                    .whereLayer("Web").mayOnlyBeAccessedByLayers("Wiring")
                    .whereLayer("Web").mayOnlyAccessLayers("Model", "Service")
                    .whereLayer("Service").mayOnlyBeAccessedByLayers("Web", "Wiring")
                    .whereLayer("Entity").mayOnlyBeAccessedByLayers("Service")
                    .whereLayer("Entity").mayOnlyAccessLayers("Model")
                    .whereLayer("Model").mayNotAccessAnyLayer();

    // Fine-grained dependency checks

    @ArchTest
    public static final ArchRule jpaEntityDependentsRule =
            classes()
                    .that().areAnnotatedWith(Entity.class)
                    .should().resideInAPackage("..entity..");

    @ArchTest
    public static final ArchRule jpaEntityManagerDependentsRule =
            classes()
                    .that(useClass(EntityManager.class))
                    .should().resideInAPackage("..service..");

    @ArchTest
    public static final ArchRule jpaEntityManagerFactoryDependentsRule =
            classes()
                    .that(useClass(EntityManagerFactory.class))
                    .should().resideInAPackage("..service..")
                    .allowEmptyShould(true);

    private static DescribedPredicate<JavaClass> useClass(Class<?> clazz) {
        return new DescribedPredicate<>(String.format("use class %s", clazz)) {

            @Override
            public boolean test(JavaClass javaClass) {
                return javaClass.getAllAccessesFromSelf()
                        .stream()
                        .anyMatch(ja -> ja.getTargetOwner().isAssignableTo(clazz));
            }
        };
    }
}
