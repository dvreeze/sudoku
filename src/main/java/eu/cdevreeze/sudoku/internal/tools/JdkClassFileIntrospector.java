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

import java.io.IOException;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Objects;

/**
 * JDK class file introspector, using the
 * <a href="https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/classfile/package-summary.html">classfile</a>
 * API. That API requires Java 24 or later.
 * <p>
 * This program delegates to {@link ClassFileIntrospector} for the {@link ClassFile}
 * implementation.
 * <p>
 * To run the program, pass JDK module name and "class file" as program arguments. The "class file" is like
 * the fully qualified class name, replacing dots by slashes and adding the ".class" suffix. In other words,
 * the "class file" is the class file path (relative to the root of the "system" class path). The most commonly
 * used module name is "java.base".
 * <p>
 * Example program arguments: "java.base java/lang/Object.class".
 * <p>
 * See <a href="https://stackoverflow.com/questions/1240387/where-are-the-java-system-packages-stored/53897006#53897006">this stackoverflow page</a>.
 * <p>
 * Also see <a href="https://www.baeldung.com/java-reflection-instantiate-inner-class">this Baeldung page</a>.
 *
 * @author Chris de Vreeze
 */
public class JdkClassFileIntrospector {

    static void main(String[] args) throws IOException {
        Objects.checkIndex(1, args.length);
        String moduleName = args[0];
        String className = args[1];

        FileSystem fs = FileSystems.getFileSystem(URI.create("jrt:/"));
        Path classFilePath = fs.getPath("modules", moduleName, className);

        ClassModel classModel = ClassFile.of().parse(classFilePath);
        Preconditions.checkArgument(classModel.thisClass().asSymbol().isClassOrInterface());

        ClassFileIntrospector classFileIntrospector = new ClassFileIntrospector();
        classFileIntrospector.writeClassModel(classModel);
    }
}
