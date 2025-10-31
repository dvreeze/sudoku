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
import com.tngtech.archunit.core.domain.*;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

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
                    .whereLayer("Service").mayOnlyAccessLayers("Model", "Entity")
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

    // Other checks

    @ArchTest
    public static final ArchRule staticMemberClassesRule =
            classes()
                    .that().areMemberClasses()
                    .and(doNotDependOnOuterClassInstance())
                    .should().notBeInnerClasses()
                    .allowEmptyShould(true);

    // Private methods

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

    private static DescribedPredicate<JavaClass> doNotDependOnOuterClassInstance() {
        return new DescribedPredicate<>("do not depend on outer class instance") {

            @Override
            public boolean test(JavaClass javaClass) {
                Set<JavaClass> enclosingClasses = findEnclosingClasses(javaClass);
                Set<JavaAccess<?>> outerInstanceDependencies =
                        findInstanceFieldAndMethodAccesses(javaClass)
                                .stream()
                                .filter(ja -> enclosingClasses.contains(ja.getTargetOwner()))
                                .collect(Collectors.toSet());
                return outerInstanceDependencies.isEmpty();
            }
        };
    }

    private static Set<JavaClass> findEnclosingClasses(JavaClass cls) {
        Set<JavaClass> enclosingClassesOrSelf = new HashSet<>(Set.of(cls));
        findEnclosingClassesOrSelf(enclosingClassesOrSelf);
        return enclosingClassesOrSelf.stream()
                .filter(c -> !c.equals(cls))
                .collect(Collectors.toSet());
    }

    private static void findEnclosingClassesOrSelf(Set<JavaClass> acc) {
        Set<JavaClass> directlyEnclosingClasses = acc.stream()
                .flatMap(c -> c.getEnclosingClass().stream())
                .collect(Collectors.toSet());

        if (!acc.containsAll(directlyEnclosingClasses)) {
            acc.addAll(directlyEnclosingClasses);
            // Recursion
            findEnclosingClassesOrSelf(acc);
        }
    }

    private static Set<JavaAccess<?>> findInstanceFieldAndMethodAccesses(JavaClass cls) {
        return findFieldAndMethodAccesses(cls)
                .stream()
                .filter(ja ->
                        ja.getTarget().resolveMember().stream().anyMatch(ArchitectureTest::isNonStatic))
                .collect(Collectors.toSet());
    }

    private static boolean isNonStatic(JavaMember javaMember) {
        // The assumption is that nested records and enums are considered static
        return javaMember.getModifiers().contains(JavaModifier.STATIC);
    }

    private static Set<JavaAccess<?>> findFieldAndMethodAccesses(JavaClass cls) {
        return cls.getAllAccessesFromSelf()
                .stream()
                .filter(ja -> switch (ja) {
                    case JavaCall<?> _ -> true; // constructor call or method call
                    case JavaFieldAccess _ -> true;
                    case JavaCodeUnitReference<?> _ -> true; // constructor reference or method reference
                    default -> false;
                })
                .collect(Collectors.toSet());
    }
}
