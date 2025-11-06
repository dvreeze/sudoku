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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.classfile.*;
import java.lang.classfile.attribute.*;
import java.lang.classfile.constantpool.Utf8Entry;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.AccessFlag;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Class file introspector, using the
 * <a href="https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/classfile/package-summary.html">classfile</a>
 * API. That API requires Java 24 or later.
 * <p>
 * This does not require loading of the class. Just parsing the class file suffices.
 * <p>
 * The program output is comparable to what the "javap" command outputs (when outputting the byte code
 * instructions as well). Mostly this program is about getting to know the JVM a bit better (its data
 * structures, and what happens inside a stack frame of a thread's call stack). This program is also
 * about somewhat getting to know the Java Class File API, and understanding about how this beautiful API
 * can inspire other Java APIs that model a specific domain as sealed Java interface hierarchies, leading
 * to executable code as well.
 *
 * @author Chris de Vreeze
 */
public class ClassFileIntrospector {

    private static final String MIN_INDENT = "    ";

    static void main(String[] args) throws IOException {
        Objects.checkIndex(0, args.length);
        Path classFilePath = Path.of(args[0]);
        ClassModel classModel = ClassFile.of().parse(classFilePath);
        Preconditions.checkArgument(classModel.thisClass().asSymbol().isClassOrInterface());

        showClassModel(classModel, "", System.out);
    }

    public static void showClassModel(ClassModel classModel, String indent, PrintStream out) {
        out.print(indent);
        out.printf("ClassModel (symbol: %s)%n", ClassOrInterfaceDescData.from(classModel.thisClass().asSymbol()));

        classModel.elementStream().forEach(classElement -> {
            switch (classElement) {
                case FieldModel fieldModel -> showFieldModel(fieldModel, indent + MIN_INDENT, out);
                case MethodModel methodModel -> showMethodModel(methodModel, indent + MIN_INDENT, out);
                case AccessFlags accessFlags -> showAccessFlags(accessFlags, indent + MIN_INDENT, out);
                case SourceFileAttribute sourceFileAttribute -> {
                    out.print(indent + MIN_INDENT);
                    out.print(sourceFileAttribute);
                    out.print(" ");
                    out.printf("(file: %s)", sourceFileAttribute.sourceFile());
                    out.println();
                }
                case RuntimeVisibleAnnotationsAttribute attr ->
                        showRuntimeVisibleAnnotationsAttribute(attr, indent + MIN_INDENT, out);
                case RuntimeInvisibleAnnotationsAttribute attr ->
                        showRuntimeInvisibleAnnotationsAttribute(attr, indent + MIN_INDENT, out);
                case NestMembersAttribute nestMembersAttribute -> {
                    out.print(indent + MIN_INDENT);
                    out.print(nestMembersAttribute);
                    out.print(" ");
                    out.printf("(nestMembers: %s)", nestMembersAttribute.nestMembers());
                    out.println();
                }
                case InnerClassesAttribute innerClassesAttribute -> {
                    out.print(indent + MIN_INDENT);
                    out.print(innerClassesAttribute);
                    out.print(" ");
                    out.printf("(classes: %s)", innerClassesAttribute.classes());
                    out.println();
                }
                default -> {
                    out.print(indent + MIN_INDENT);
                    out.print(classElement);
                    out.println();
                }
            }
        });
    }

    public static void showFieldModel(FieldModel fieldModel, String indent, PrintStream out) {
        out.print(indent);
        out.printf(
                "FieldModel (name: %s, type: %s)%n",
                fieldModel.fieldName(),
                ClassOrInterfaceDescData.from(fieldModel.fieldTypeSymbol())
        );

        fieldModel.elementStream().forEach(fieldElement -> {
            switch (fieldElement) {
                case AccessFlags accessFlags -> showAccessFlags(accessFlags, indent + MIN_INDENT, out);
                case RuntimeVisibleAnnotationsAttribute attr ->
                        showRuntimeVisibleAnnotationsAttribute(attr, indent + MIN_INDENT, out);
                case RuntimeInvisibleAnnotationsAttribute attr ->
                        showRuntimeInvisibleAnnotationsAttribute(attr, indent + MIN_INDENT, out);
                default -> {
                    out.print(indent + MIN_INDENT);
                    out.print(fieldElement);
                    out.println();
                }
            }
        });
    }

    public static void showMethodModel(MethodModel methodModel, String indent, PrintStream out) {
        out.print(indent);
        out.printf(
                "MethodModel (name: %s, type: %s)%n",
                methodModel.methodName(),
                MethodTypeDescData.from(methodModel.methodTypeSymbol())
        );

        methodModel.elementStream().forEach(methodElement -> {
            switch (methodElement) {
                case CodeModel codeModel -> showCodeModel(codeModel, indent + MIN_INDENT, out);
                case AccessFlags accessFlags -> showAccessFlags(accessFlags, indent + MIN_INDENT, out);
                case MethodParametersAttribute methodParametersAttribute ->
                        showMethodParametersAttribute(methodParametersAttribute, indent + MIN_INDENT, out);
                case SignatureAttribute signatureAttribute -> {
                    out.print(indent + MIN_INDENT);
                    out.print(signatureAttribute);
                    out.print(" ");
                    out.printf("(signature: %s)", signatureAttribute.asMethodSignature());
                    out.println();
                }
                case RuntimeVisibleAnnotationsAttribute attr ->
                        showRuntimeVisibleAnnotationsAttribute(attr, indent + MIN_INDENT, out);
                case RuntimeInvisibleAnnotationsAttribute attr ->
                        showRuntimeInvisibleAnnotationsAttribute(attr, indent + MIN_INDENT, out);
                default -> {
                    out.print(indent + MIN_INDENT);
                    out.print(methodElement);
                    out.println();
                }
            }
        });
    }

    public static void showCodeModel(CodeModel codeModel, String indent, PrintStream out) {
        out.print(indent);
        out.println("CodeModel");

        codeModel.elementStream().forEach(codeElement -> {
            out.print(indent + MIN_INDENT);
            out.print(codeElement);
            out.println();
        });
    }

    private static void showAccessFlags(AccessFlags accessFlags, String indent, PrintStream out) {
        out.print(indent);
        out.print(accessFlags);
        out.print(" (flags: " + accessFlags.flags() + ")");
        out.println();
    }

    private static void showMethodParametersAttribute(MethodParametersAttribute attr, String indent, PrintStream out) {
        out.print(indent);
        out.print(attr);
        out.print(" (info: " + MethodParametersInfoData.from(attr.parameters()) + ")");
        out.println();
    }

    private static void showRuntimeVisibleAnnotationsAttribute(RuntimeVisibleAnnotationsAttribute attr, String indent, PrintStream out) {
        out.print(indent);
        out.print(attr);
        out.printf(" (annotations: %s)", attr.annotations());
        out.println();
    }

    private static void showRuntimeInvisibleAnnotationsAttribute(RuntimeInvisibleAnnotationsAttribute attr, String indent, PrintStream out) {
        out.print(indent);
        out.print(attr);
        out.printf(" (annotations: %s)", attr.annotations());
        out.println();
    }

    public record ClassOrInterfaceDescData(String displayName, String packageName) {

        public static ClassOrInterfaceDescData from(ClassDesc classDesc) {
            return new ClassOrInterfaceDescData(classDesc.displayName(), classDesc.packageName());
        }
    }

    public record MethodTypeDescData(String displayDescriptor, String descriptorString) {

        public static MethodTypeDescData from(MethodTypeDesc methodTypeDesc) {
            return new MethodTypeDescData(methodTypeDesc.displayDescriptor(), methodTypeDesc.descriptorString());
        }
    }

    public record MethodParameterInfoData(Optional<String> name, ImmutableSet<AccessFlag> accessFlags) {

        public static MethodParameterInfoData from(MethodParameterInfo parInfo) {
            return new MethodParameterInfoData(
                    parInfo.name().map(Utf8Entry::stringValue),
                    ImmutableSet.copyOf(parInfo.flags())
            );
        }
    }

    public record MethodParametersInfoData(ImmutableList<MethodParameterInfoData> parameters) {

        public static MethodParametersInfoData from(List<MethodParameterInfo> pars) {
            return new MethodParametersInfoData(
                    pars.stream().map(MethodParameterInfoData::from).collect(ImmutableList.toImmutableList())
            );
        }
    }
}
