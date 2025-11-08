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

package eu.cdevreeze.sudoku.internal.tools;

import com.google.common.base.Preconditions;
import com.google.common.io.ByteStreams;

import java.io.IOException;
import java.io.InputStream;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.util.Objects;

/**
 * Reflective class file introspector, using the
 * <a href="https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/classfile/package-summary.html">classfile</a>
 * API. That API requires Java 24 or later.
 * <p>
 * This program delegates to {@link ClassFileIntrospector} for the {@link ClassFile}
 * implementation.
 * <p>
 * To run the program, pass the fully qualified class name as program argument. The program then tries
 * to load the class using method {@link Class##forName}, before delegating to {@link ClassFileIntrospector}.
 * The class must be on the class path. This program can not load classes in named modules.
 * <p>
 * Also see <a href="https://www.baeldung.com/java-reflection-instantiate-inner-class">this Baeldung page</a>.
 *
 * @author Chris de Vreeze
 */
public class ReflectiveClassFileIntrospector {

    static void main(String[] args) throws IOException, ClassNotFoundException {
        Objects.checkIndex(0, args.length);
        String className = args[0];

        Class<?> clazz = Class.forName(className);
        String classNameAsPath = clazz.getName().replace(".", "/") + ".class";
        ClassLoader classLoader = Objects.requireNonNull(clazz.getClassLoader());

        byte[] classAsByteArray;
        try (InputStream is = Objects.requireNonNull(classLoader.getResourceAsStream(classNameAsPath))) {
            // See https://www.baeldung.com/convert-input-stream-to-array-of-bytes
            classAsByteArray = ByteStreams.toByteArray(is);
        }

        ClassModel classModel = ClassFile.of().parse(classAsByteArray);
        Preconditions.checkArgument(classModel.thisClass().asSymbol().isClassOrInterface());

        ClassFileIntrospector classFileIntrospector = new ClassFileIntrospector();
        classFileIntrospector.writeClassModel(classModel);
    }
}
