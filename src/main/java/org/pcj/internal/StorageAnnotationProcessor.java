/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import org.pcj.Storage;

/**
 * SharedProcessor is Java Annotation Processor to process
 * {@literal @}Shared annotation.
 * <p>
 * It looks up for shared fields and checks for proper
 * declaration (field have to be non-final, non-static, and
 * Serializable).
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
@SupportedAnnotationTypes("org.pcj.Storage")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class StorageAnnotationProcessor extends javax.annotation.processing.AbstractProcessor {

    private TypeElement serializableType;
    private Types typeUtils;

//    private void note(String msg, Element e) {
//        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, msg, e);
//    }
//
    private void warn(String msg, Element e) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, msg, e);
    }

    private void error(String msg, Element e) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, msg, e);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        serializableType = processingEnv.getElementUtils().getTypeElement(Serializable.class.getCanonicalName());
        typeUtils = processingEnv.getTypeUtils();

        roundEnv.getElementsAnnotatedWith(Storage.class)
                .stream()
                .map(element -> (TypeElement) element)
                .forEach(this::process);

        return true;
    }

    private boolean isSuppressed(Element e, String warning) {
        SuppressWarnings suppressWarnings = e.getAnnotation(SuppressWarnings.class);
        if (suppressWarnings != null) {
            for (String w : suppressWarnings.value()) {
                if (w.equals(warning)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void process(TypeElement processedElement) {

        Storage annotation = processedElement.getAnnotation(Storage.class);
        Set<String> sharedEnumNames;
        try {
            Class<? extends Enum<?>> sharedEnum = annotation.value();
            if (sharedEnum.isEnum() == false) {
                error("Annotation value is not Enum.", processedElement);
                return;
            }

            sharedEnumNames = Arrays.stream(sharedEnum.getEnumConstants())
                    .map(Enum::name)
                    .collect(Collectors.toCollection(HashSet::new));
        } catch (MirroredTypeException ex) {
            DeclaredType classTypeMirror = (DeclaredType) ex.getTypeMirror();
            TypeElement classTypeElement = (TypeElement) classTypeMirror.asElement();

            if (classTypeElement.getKind().equals(ElementKind.ENUM) == false) {
                error("Annotation value is not Enum.", classTypeElement);
                return;
            }

            sharedEnumNames = classTypeElement.getEnclosedElements()
                    .stream()
                    .filter(element -> element.getKind().equals(ElementKind.ENUM_CONSTANT))
                    .map(element -> element.toString())
                    .collect(Collectors.toCollection(HashSet::new));
        }

        if (processedElement.getModifiers().contains(Modifier.ABSTRACT)) {
            error("Abstract class cannot be Storage", processedElement);
            return;
        }

        if (processedElement.getEnclosedElements()
                .stream()
                .filter(element -> element.getKind().equals(ElementKind.CONSTRUCTOR))
                .map(element -> (ExecutableElement) element)
                .noneMatch(element -> element.getParameters().isEmpty())) {
            error("No-arg constructor not found", processedElement);
            return;
        }

        Set<String> fieldNames = ElementFilter.fieldsIn(processedElement.getEnclosedElements())
                .stream()
                .map(element -> element.toString())
                .collect(Collectors.toCollection(HashSet::new));

        Optional<String> notFoundName = sharedEnumNames.stream()
                .filter(enumName -> fieldNames.contains(enumName) == false)
                .findFirst();

        if (notFoundName.isPresent()) {
            error("Field '" + notFoundName.get() + "' not found", processedElement);
        }

        ElementFilter.fieldsIn(processedElement.getEnclosedElements())
                .stream()
                .filter(element -> typeUtils.isAssignable(element.asType(), serializableType.asType()) == false)
                .forEach(element -> error("Variable type has to be serializable", element));

        ElementFilter.fieldsIn(processedElement.getEnclosedElements())
                .stream()
                .filter(element -> element.getModifiers().contains(Modifier.FINAL) == true)
                .forEach(element -> error("Variable has to be non-final", element));

        ElementFilter.fieldsIn(processedElement.getEnclosedElements())
                .stream()
                .filter(element -> element.getModifiers().contains(Modifier.STATIC) == true)
                .forEach(element -> error("Variable has to be non-static", element));

    }
}
