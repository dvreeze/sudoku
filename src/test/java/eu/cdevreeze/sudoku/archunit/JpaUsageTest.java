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

import com.google.common.base.Preconditions;
import com.tngtech.archunit.core.domain.*;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.*;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * Unit test testing the "correct" use of JPA, using <a href="https://www.archunit.org/">ArchUnit</a>.
 *
 * @author Chris de Vreeze
 */
@AnalyzeClasses(packages = "eu.cdevreeze.sudoku")
public class JpaUsageTest {

    // Only lazy fetching

    @ArchTest
    public static final ArchRule lazyFetchingRule =
            classes()
                    .that().areAnnotatedWith(Entity.class)
                    .or().areAnnotatedWith(MappedSuperclass.class)
                    .or().areAnnotatedWith(Embeddable.class)
                    .should(useLazyFetchingOnly());

    private static ArchCondition<JavaClass> useLazyFetchingOnly() {
        return new ArchCondition<>("only use lazy fetching in associations") {

            @Override
            public void check(JavaClass javaClass, ConditionEvents conditionEvents) {
                useLazyFetchingOnly(javaClass, conditionEvents, OneToOne.class);
                useLazyFetchingOnly(javaClass, conditionEvents, OneToMany.class);
                useLazyFetchingOnly(javaClass, conditionEvents, ManyToOne.class);
                useLazyFetchingOnly(javaClass, conditionEvents, ManyToMany.class);
            }

            private void useLazyFetchingOnly(
                    JavaClass javaClass,
                    ConditionEvents conditionEvents,
                    Class<? extends Annotation> jpaAssociationAnnotationClass) {
                Preconditions.checkArgument(jpaAssociationAnnotationClass.getPackage().equals(Entity.class.getPackage()));

                List<JavaField> fieldsWithTheAnnotation = javaClass.getAllFields()
                        .stream()
                        .filter(fld -> fld.isAnnotatedWith(jpaAssociationAnnotationClass))
                        .toList();

                fieldsWithTheAnnotation
                        .stream()
                        .map(fld -> fld.getAnnotationOfType(jpaAssociationAnnotationClass.getCanonicalName()))
                        .filter(this::usesEagerFetching)
                        .forEach(ann -> {
                            String msg = String.format(
                                    "Field %s is annotated with %s which does not use lazy fetching",
                                    ann.getOwner().getFullName(),
                                    jpaAssociationAnnotationClass.getCanonicalName()
                            );
                            conditionEvents.add(
                                    SimpleConditionEvent.violated(ann, msg)
                            );
                        });

                List<JavaMethod> methodsWithTheAnnotation = javaClass.getAllMethods()
                        .stream()
                        .filter(meth -> meth.isAnnotatedWith(jpaAssociationAnnotationClass))
                        .toList();

                methodsWithTheAnnotation
                        .stream()
                        .map(meth -> meth.getAnnotationOfType(jpaAssociationAnnotationClass.getCanonicalName()))
                        .filter(this::usesEagerFetching)
                        .forEach(ann -> {
                            String msg = String.format(
                                    "Method %s is annotated with %s which does not use lazy fetching",
                                    ann.getOwner().getFullName(),
                                    jpaAssociationAnnotationClass.getCanonicalName()
                            );
                            conditionEvents.add(
                                    SimpleConditionEvent.violated(ann, msg)
                            );
                        });
            }

            private boolean usesEagerFetching(JavaAnnotation<?> jpaAssociationAnnotation) {
                Preconditions.checkArgument(
                        Set.of("OneToOne", "OneToMany", "ManyToOne", "ManyToMany")
                                .contains(jpaAssociationAnnotation.getRawType().getSimpleName())
                );
                Optional<FetchType> explicitFetchType =
                        jpaAssociationAnnotation.tryGetExplicitlyDeclaredProperty("fetch")
                                .map(v -> (JavaEnumConstant) v)
                                .map(JavaEnumConstant::name)
                                .map(FetchType::valueOf);
                FetchType fetch = explicitFetchType
                        .orElse(getDefaultFetchType(jpaAssociationAnnotation));
                return FetchType.EAGER.equals(fetch);
            }

            private FetchType getDefaultFetchType(JavaAnnotation<?> jpaAssociationAnnotation) {
                Preconditions.checkArgument(
                        Set.of("OneToOne", "OneToMany", "ManyToOne", "ManyToMany")
                                .contains(jpaAssociationAnnotation.getRawType().getSimpleName())
                );
                return jpaAssociationAnnotation.getRawType().getSimpleName().endsWith("ToOne") ?
                        FetchType.EAGER :
                        FetchType.LAZY;
            }
        };
    }
}
