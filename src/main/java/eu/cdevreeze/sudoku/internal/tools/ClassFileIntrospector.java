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
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdSerializer;
import tools.jackson.datatype.guava.GuavaModule;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.classfile.*;
import java.lang.classfile.attribute.*;
import java.lang.classfile.constantpool.Utf8Entry;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.AccessFlag;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Gatherer;
import java.util.stream.Gatherers;
import java.util.stream.Stream;

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

    private final JsonMapper jsonMapper;

    public ClassFileIntrospector() {
        var simpleModule = new SimpleModule();
        simpleModule.addSerializer(ClassModel.class, new CustomClassModelSerializer());
        simpleModule.addSerializer(FieldModel.class, new CustomFieldModelSerializer());
        simpleModule.addSerializer(MethodModel.class, new CustomMethodModelSerializer());
        simpleModule.addSerializer(CodeModel.class, new CustomCodeModelSerializer());
        simpleModule.addSerializer(RuntimeVisibleAnnotationsAttribute.class, new CustomRuntimeVisibleAnnotationsAttributeSerializer());
        simpleModule.addSerializer(RuntimeInvisibleAnnotationsAttribute.class, new CustomRuntimeInvisibleAnnotationsAttributeSerializer());
        simpleModule.addSerializer(AccessFlags.class, new CustomAccessFlagsSerializer());

        this.jsonMapper = JsonMapper.builder()
                .addModule(new GuavaModule())
                .addModule(simpleModule)
                .enable(SerializationFeature.INDENT_OUTPUT)
                .build();
    }

    public void writeClassModel(ClassModel classModel, OutputStream os) {
        jsonMapper.writer().writeValue(os, classModel);
    }

    public void writeClassModel(ClassModel classModel) {
        writeClassModel(classModel, System.out);
    }

    static void main(String[] args) throws IOException {
        Objects.checkIndex(0, args.length);
        Path classFilePath = Path.of(args[0]);
        ClassModel classModel = ClassFile.of().parse(classFilePath);
        Preconditions.checkArgument(classModel.thisClass().asSymbol().isClassOrInterface());

        ClassFileIntrospector classFileIntrospector = new ClassFileIntrospector();
        classFileIntrospector.writeClassModel(classModel);
    }

    public static class CustomClassModelSerializer extends StdSerializer<ClassModel> {

        public CustomClassModelSerializer() {
            this(null);
        }

        public CustomClassModelSerializer(@Nullable Class<ClassModel> t) {
            super(t);
        }

        @Override
        public void serialize(ClassModel value, JsonGenerator gen, SerializationContext provider) throws JacksonException {
            gen.writeStartObject();
            gen.writeObjectPropertyStart(getClassFileLibraryInterface(value).getSimpleName());
            gen.writeStringProperty("symbol", ClassOrInterfaceDescData.from(value.thisClass().asSymbol()).toString());

            gen.writeArrayPropertyStart("elementList");

            value.elementStream().forEach(classElement -> {
                switch (classElement) {
                    case FieldModel fieldModel -> gen.writePOJO(fieldModel);
                    case MethodModel methodModel -> gen.writePOJO(methodModel);
                    case AccessFlags accessFlags -> gen.writePOJO(accessFlags);
                    case SourceFileAttribute sourceFileAttribute ->
                            serializeSourceFileAttribute(sourceFileAttribute, gen, provider);
                    case RuntimeVisibleAnnotationsAttribute attr -> gen.writePOJO(attr);
                    case RuntimeInvisibleAnnotationsAttribute attr -> gen.writePOJO(attr);
                    case NestMembersAttribute nestMembersAttribute ->
                            serializeNestMembersAttribute(nestMembersAttribute, gen, provider);
                    case InnerClassesAttribute innerClassesAttribute ->
                            serializeInnerClassesAttribute(innerClassesAttribute, gen, provider);
                    default -> serializeAnyClassElement(classElement, gen, provider);
                }
            });

            gen.writeEndArray();

            gen.writeEndObject();
            gen.writeEndObject();
        }

        private void serializeSourceFileAttribute(SourceFileAttribute value, JsonGenerator gen, SerializationContext provider) {
            gen.writeStartObject();
            gen.writeObjectPropertyStart(getClassFileLibraryInterface(value).getSimpleName());
            gen.writeStringProperty("value", value.toString());
            gen.writeStringProperty("file", value.sourceFile().stringValue());
            gen.writeEndObject();
            gen.writeEndObject();
        }

        private void serializeNestMembersAttribute(NestMembersAttribute value, JsonGenerator gen, SerializationContext provider) {
            gen.writeStartObject();
            gen.writeObjectPropertyStart(getClassFileLibraryInterface(value).getSimpleName());
            gen.writeStringProperty("value", value.toString());

            gen.writeArrayPropertyStart("nestMembers");
            value.nestMembers().forEach(v -> gen.writeString(v.toString()));
            gen.writeEndArray();

            gen.writeEndObject();
            gen.writeEndObject();
        }

        private void serializeInnerClassesAttribute(InnerClassesAttribute value, JsonGenerator gen, SerializationContext provider) {
            gen.writeStartObject();
            gen.writeObjectPropertyStart(getClassFileLibraryInterface(value).getSimpleName());
            gen.writeStringProperty("value", value.toString());

            gen.writeArrayPropertyStart("classes");
            value.classes().forEach(v -> gen.writeString(v.toString()));
            gen.writeEndArray();

            gen.writeEndObject();
            gen.writeEndObject();
        }

        private void serializeAnyClassElement(ClassElement value, JsonGenerator gen, SerializationContext provider) {
            gen.writeStartObject();
            gen.writeObjectPropertyStart(getClassFileLibraryInterface(value).getSimpleName());
            gen.writeStringProperty("value", value.toString());
            gen.writeEndObject();
            gen.writeEndObject();
        }
    }

    public static class CustomFieldModelSerializer extends StdSerializer<FieldModel> {

        public CustomFieldModelSerializer() {
            this(null);
        }

        public CustomFieldModelSerializer(@Nullable Class<FieldModel> t) {
            super(t);
        }

        @Override
        public void serialize(FieldModel value, JsonGenerator gen, SerializationContext provider) throws JacksonException {
            gen.writeStartObject();
            gen.writeObjectPropertyStart(getClassFileLibraryInterface(value).getSimpleName());
            gen.writeStringProperty("name", value.fieldName().stringValue());
            gen.writeStringProperty("type", ClassOrInterfaceDescData.from(value.fieldTypeSymbol()).toString());

            gen.writeArrayPropertyStart("elementList");

            value.elementStream().forEach(fieldElement -> {
                switch (fieldElement) {
                    case AccessFlags accessFlags -> gen.writePOJO(accessFlags);
                    case RuntimeVisibleAnnotationsAttribute attr -> gen.writePOJO(attr);
                    case RuntimeInvisibleAnnotationsAttribute attr -> gen.writePOJO(attr);
                    default -> serializeAnyFieldElement(fieldElement, gen, provider);
                }
            });

            gen.writeEndArray();

            gen.writeEndObject();
            gen.writeEndObject();
        }

        private void serializeAnyFieldElement(FieldElement value, JsonGenerator gen, SerializationContext provider) {
            gen.writeStartObject();
            gen.writeObjectPropertyStart(getClassFileLibraryInterface(value).getSimpleName());
            gen.writeStringProperty("value", value.toString());
            gen.writeEndObject();
            gen.writeEndObject();
        }
    }

    public static class CustomMethodModelSerializer extends StdSerializer<MethodModel> {

        public CustomMethodModelSerializer() {
            this(null);
        }

        public CustomMethodModelSerializer(@Nullable Class<MethodModel> t) {
            super(t);
        }

        @Override
        public void serialize(MethodModel value, JsonGenerator gen, SerializationContext provider) throws JacksonException {
            gen.writeStartObject();
            gen.writeObjectPropertyStart(getClassFileLibraryInterface(value).getSimpleName());
            gen.writeStringProperty("name", value.methodName().stringValue());
            gen.writeStringProperty("type", MethodTypeDescData.from(value.methodTypeSymbol()).toString());

            gen.writeArrayPropertyStart("elementList");

            value.elementStream().forEach(methodElement -> {
                switch (methodElement) {
                    case CodeModel codeModel -> gen.writePOJO(codeModel);
                    case AccessFlags accessFlags -> gen.writePOJO(accessFlags);
                    case MethodParametersAttribute methodParametersAttribute ->
                            serializeMethodParametersAttribute(methodParametersAttribute, gen, provider);
                    case SignatureAttribute signatureAttribute ->
                            serializeSignatureAttribute(signatureAttribute, gen, provider);
                    case RuntimeVisibleAnnotationsAttribute attr -> gen.writePOJO(attr);
                    case RuntimeInvisibleAnnotationsAttribute attr -> gen.writePOJO(attr);
                    default -> serializeAnyMethodElement(methodElement, gen, provider);
                }
            });

            gen.writeEndArray();

            gen.writeEndObject();
            gen.writeEndObject();
        }

        private void serializeMethodParametersAttribute(MethodParametersAttribute value, JsonGenerator gen, SerializationContext provider) {
            gen.writeStartObject();
            gen.writeObjectPropertyStart(getClassFileLibraryInterface(value).getSimpleName());
            gen.writeStringProperty("value", value.toString());

            gen.writeArrayPropertyStart("parameters");
            MethodParametersInfoData.from(value.parameters()).parameters()
                    .forEach(v -> gen.writeString(v.toString()));
            gen.writeEndArray();

            gen.writeEndObject();
            gen.writeEndObject();
        }

        private void serializeSignatureAttribute(SignatureAttribute value, JsonGenerator gen, SerializationContext provider) {
            gen.writeStartObject();
            gen.writeObjectPropertyStart(getClassFileLibraryInterface(value).getSimpleName());
            gen.writeStringProperty("value", value.toString());
            gen.writeStringProperty("signature", value.asMethodSignature().toString());
            gen.writeEndObject();
            gen.writeEndObject();
        }

        private void serializeAnyMethodElement(MethodElement value, JsonGenerator gen, SerializationContext provider) {
            gen.writeStartObject();
            gen.writeObjectPropertyStart(getClassFileLibraryInterface(value).getSimpleName());
            gen.writeStringProperty("value", value.toString());
            gen.writeEndObject();
            gen.writeEndObject();
        }
    }

    public static class CustomCodeModelSerializer extends StdSerializer<CodeModel> {

        public CustomCodeModelSerializer() {
            this(null);
        }

        public CustomCodeModelSerializer(@Nullable Class<CodeModel> t) {
            super(t);
        }

        @Override
        public void serialize(CodeModel value, JsonGenerator gen, SerializationContext provider) throws JacksonException {
            gen.writeStartObject();
            gen.writeObjectPropertyStart(getClassFileLibraryInterface(value).getSimpleName());

            gen.writeArrayPropertyStart("elementList");

            value.elementStream().forEach(codeElement -> {
                gen.writeStartObject();
                gen.writeObjectPropertyStart(getClassFileLibraryInterface(codeElement).getSimpleName());
                gen.writeStringProperty("value", codeElement.toString());
                gen.writeEndObject();
                gen.writeEndObject();
            });

            gen.writeEndArray();

            gen.writeEndObject();
            gen.writeEndObject();
        }
    }

    private static class CustomRuntimeVisibleAnnotationsAttributeSerializer extends StdSerializer<RuntimeVisibleAnnotationsAttribute> {

        public CustomRuntimeVisibleAnnotationsAttributeSerializer() {
            this(null);
        }

        public CustomRuntimeVisibleAnnotationsAttributeSerializer(@Nullable Class<RuntimeVisibleAnnotationsAttribute> t) {
            super(t);
        }

        @Override
        public void serialize(RuntimeVisibleAnnotationsAttribute value, JsonGenerator gen, SerializationContext provider) throws JacksonException {
            gen.writeStartObject();
            gen.writeObjectPropertyStart(getClassFileLibraryInterface(value).getSimpleName());
            gen.writeStringProperty("value", value.toString());

            gen.writeArrayPropertyStart("annotations");
            value.annotations().forEach(v -> gen.writeString(v.toString()));
            gen.writeEndArray();

            gen.writeEndObject();
            gen.writeEndObject();
        }
    }

    private static class CustomRuntimeInvisibleAnnotationsAttributeSerializer extends StdSerializer<RuntimeInvisibleAnnotationsAttribute> {

        public CustomRuntimeInvisibleAnnotationsAttributeSerializer() {
            this(null);
        }

        public CustomRuntimeInvisibleAnnotationsAttributeSerializer(@Nullable Class<RuntimeInvisibleAnnotationsAttribute> t) {
            super(t);
        }

        @Override
        public void serialize(RuntimeInvisibleAnnotationsAttribute value, JsonGenerator gen, SerializationContext provider) throws JacksonException {
            gen.writeStartObject();
            gen.writeObjectPropertyStart(getClassFileLibraryInterface(value).getSimpleName());
            gen.writeStringProperty("value", value.toString());

            gen.writeArrayPropertyStart("annotations");
            value.annotations().forEach(v -> gen.writeString(v.toString()));
            gen.writeEndArray();

            gen.writeEndObject();
            gen.writeEndObject();
        }
    }

    private static class CustomAccessFlagsSerializer extends StdSerializer<AccessFlags> {

        public CustomAccessFlagsSerializer() {
            this(null);
        }

        public CustomAccessFlagsSerializer(@Nullable Class<AccessFlags> t) {
            super(t);
        }

        @Override
        public void serialize(AccessFlags value, JsonGenerator gen, SerializationContext provider) throws JacksonException {
            gen.writeStartObject();
            gen.writeObjectPropertyStart(getClassFileLibraryInterface(value).getSimpleName());
            gen.writeStringProperty("value", value.toString());

            gen.writeArrayPropertyStart("flags");
            value.flags().forEach(v -> gen.writeString(v.toString()));
            gen.writeEndArray();

            gen.writeEndObject();
            gen.writeEndObject();
        }
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

    // Private static helper methods

    private static Class<? extends ClassFileElement> getClassFileLibraryInterface(ClassFileElement element) {
        Set<Class<? extends ClassFileElement>> filteredClasses =
                getAllClassFileLibraryInterfaces(element);

        Preconditions.checkState(!filteredClasses.isEmpty());
        return getMostSpecificClass(filteredClasses);
    }

    private static Set<Class<? extends ClassFileElement>> getAllClassFileLibraryInterfaces(ClassFileElement element) {
        return getAllClassFileLibraryInterfaces(element.getClass());
    }

    private static Set<Class<? extends ClassFileElement>> getAllClassFileLibraryInterfaces(Class<? extends ClassFileElement> clazz) {
        Set<Class<?>> startClasses = new HashSet<>();
        startClasses.add(clazz);
        startClasses.addAll(findAllSupertypes(clazz).stream().filter(Class::isInterface).toList());

        return startClasses.stream()
                .filter(Class::isInterface)
                .filter(ClassFileElement.class::isAssignableFrom)
                .filter(cls -> cls.getPackage().equals(ClassFileElement.class.getPackage()) ||
                        cls.getPackage().equals(RecordAttribute.class.getPackage()) ||
                        cls.getPackage().equals(InvokeInstruction.class.getPackage()))
                .map(cls -> (Class<? extends ClassFileElement>) cls)
                .collect(Collectors.toSet());
    }

    private static Class<? extends ClassFileElement> getMostSpecificClass(Set<Class<? extends ClassFileElement>> classes) {
        Preconditions.checkArgument(!classes.isEmpty());
        Preconditions.checkArgument(classes.stream().allMatch(Class::isInterface));
        Preconditions.checkArgument(classes.stream().allMatch(ClassFileElement.class::isAssignableFrom));

        Gatherer<Class<? extends ClassFileElement>, ?, Class<? extends ClassFileElement>> gatherer =
                Gatherers.fold(
                        () -> List.copyOf(classes).getFirst(),
                        (acc, currElem) -> {
                            if (acc.isAssignableFrom(currElem)) {
                                return currElem;
                            } else if (currElem.isAssignableFrom(acc)) {
                                return acc;
                            } else {
                                var accInterfaces = getAllClassFileLibraryInterfaces(acc);
                                var currElemInterfaces = getAllClassFileLibraryInterfaces(currElem);
                                return accInterfaces.size() < currElemInterfaces.size() ? currElem : acc;
                            }
                        }
                );

        return classes.stream()
                .gather(gatherer)
                .findFirst()
                .orElse(List.copyOf(classes).getFirst());
    }

    private static Set<Class<?>> findAllSupertypes(Class<?> cls) {
        return findAllSupertypesOrSelf(cls).stream().skip(1).collect(Collectors.toSet());
    }

    private static Set<Class<?>> findAllSupertypesOrSelf(Class<?> cls) {
        Set<Class<?>> result = new HashSet<>();
        result.add(cls);
        // Recursion
        result.addAll(
                Stream.concat(Arrays.stream(cls.getInterfaces()), Optional.ofNullable(cls.getSuperclass()).stream())
                        .flatMap(c -> findAllSupertypesOrSelf(c).stream())
                        .collect(Collectors.toSet())
        );
        return Set.copyOf(result);
    }
}
