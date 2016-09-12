/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import org.pcj.StartPoint;
import org.pcj.Storage;
import org.pcj.RegisterStorage;

/**
 * SharedProcessor is Java Annotation Processor to process
 * {@literal @}Storage, {@literal @}RegisterStorage and
 * {@literal @}RegisterStorageRepeatableContainer annotations.
 * <p>
 * It looks up for shared fields and checks for proper
 * declaration (field have to be non-final, non-static, and
 * Serializable).
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
@SupportedAnnotationTypes({"org.pcj.Storage",
    "org.pcj.RegisterStorage", "org.pcj.internal.RegisterStorageRepeatableContainer"})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class StorageAnnotationProcessor extends javax.annotation.processing.AbstractProcessor {

    private Types typeUtils;
    private Elements elementUtils;
    private Set<Element> notSerializableStorageFields;
    private Set<Element> staticStorageFields;
    private Set<Element> finalStorageFields;
    private Map<Element, Map<Element, Set<Element>>> storageUsedFields;
    private TypeElement serializableType;
    private TypeElement startPointType;
    private TypeElement storageType;
    private TypeElement registerStorageType;
    private TypeElement registerStorageRepeatableContainerType;

    private void warning(String msg, Element e) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, msg, e);
    }

    private void error(String msg, Element e) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, msg, e);
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        elementUtils = processingEnv.getElementUtils();
        serializableType = elementUtils.getTypeElement(Serializable.class.getCanonicalName());
        startPointType = elementUtils.getTypeElement(StartPoint.class.getCanonicalName());
        storageType = elementUtils.getTypeElement(Storage.class.getCanonicalName());
        registerStorageType = elementUtils.getTypeElement(RegisterStorage.class.getCanonicalName());
        registerStorageRepeatableContainerType = elementUtils.getTypeElement(RegisterStorageRepeatableContainer.class.getCanonicalName());

        typeUtils = processingEnv.getTypeUtils();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        notSerializableStorageFields = new LinkedHashSet<>();
        staticStorageFields = new LinkedHashSet<>();
        finalStorageFields = new LinkedHashSet<>();
        storageUsedFields = new HashMap<>();

        roundEnv.getElementsAnnotatedWith(storageType)
                .stream()
                .map(element -> (TypeElement) element)
                .forEach(this::processStorage);

        roundEnv.getElementsAnnotatedWith(registerStorageType)
                .stream()
                .map(element -> (TypeElement) element)
                .forEach(this::processRegisterStorage);

        roundEnv.getElementsAnnotatedWith(registerStorageRepeatableContainerType)
                .stream()
                .map(element -> (TypeElement) element)
                .forEach(this::processRegisterStorageRepeatableContainer);

        notSerializableStorageFields.forEach(element -> error("Not serializable variable", element));

        staticStorageFields.stream()
                .filter(storageElement -> isSuppressed(storageElement, "static") == false)
                .forEach(element -> warning("[static] PCJ shared variable is static", element));

        finalStorageFields.stream()
                .filter(storageElement -> isSuppressed(storageElement, "final") == false)
                .forEach(element -> warning("[final] PCJ shared variable is final", element));

        storageUsedFields.values().stream()
                .flatMap(element -> element.entrySet().stream())
                .filter(entry -> entry.getValue().size() > 1)
                .filter(entry -> isSuppressed(entry.getKey(), "multiple") == false)
                .forEach(entry -> {
                    String enums = entry.getValue().stream().map(Element::toString).collect(Collectors.joining(", "));
                    warning("[multiple] PCJ shared variable used in multiple enums: " + enums, entry.getKey());
                });

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

    private void processStorage(TypeElement enumElement) {
        if (enumElement.getKind().equals(ElementKind.ENUM) == false) {
            error("Only enums can be annotated by @Storage", enumElement);
            return;
        }

        TypeElement storageClassElement = enumElement.getAnnotationMirrors().stream()
                .filter(element -> element.getAnnotationType().asElement().equals(storageType))
                .flatMap(element -> element.getElementValues().entrySet().stream())
                .filter(element -> element.getKey().getSimpleName().contentEquals("value"))
                .map(Map.Entry::getValue)
                .map(element -> (DeclaredType) element.getValue())
                .map(element -> (TypeElement) element.asElement())
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(enumElement + ": Not annotated with storage."));

        Map<Element, Set<Element>> usedFields
                = storageUsedFields.computeIfAbsent(storageClassElement, key -> new HashMap<>());

        if (storageClassElement.getModifiers().contains(Modifier.ABSTRACT)) {
            error("Abstract class cannot be Storage", enumElement);
            return;
        }

        if (ElementFilter.constructorsIn(storageClassElement.getEnclosedElements())
                .stream()
                .noneMatch(element -> element.getParameters().isEmpty())) {
            error("No-arg constructor not found", enumElement);
            return;
        }

        Set<String> storageNames = ElementFilter.fieldsIn(storageClassElement.getEnclosedElements()).stream()
                .map(element -> element.toString())
                .collect(Collectors.toCollection(HashSet::new));

        enumElement.getEnclosedElements().stream()
                .filter(element -> element.getKind().equals(ElementKind.ENUM_CONSTANT))
                .filter(element -> storageNames.contains(element.toString()) == false)
                .forEach(element -> error("Field " + element.toString() + " not found in Storage class", element));

        Set<String> enumNames = enumElement.getEnclosedElements().stream()
                .filter(element -> element.getKind().equals(ElementKind.ENUM_CONSTANT))
                .map(element -> element.toString())
                .collect(Collectors.toCollection(HashSet::new));

        Set<VariableElement> storageFieldsInEnum
                = ElementFilter.fieldsIn(storageClassElement.getEnclosedElements()).stream()
                .filter(element -> enumNames.contains(element.toString()))
                .collect(Collectors.toSet());

        storageFieldsInEnum.stream()
                .filter(storageElement -> typeUtils.isAssignable(storageElement.asType(), serializableType.asType()) == false)
                .forEach(notSerializableStorageFields::add);

        storageFieldsInEnum.stream()
                .filter(storageElement -> storageElement.getModifiers().contains(Modifier.STATIC) == true)
                .forEach(staticStorageFields::add);

        storageFieldsInEnum.stream()
                .filter(storageElement -> storageElement.getModifiers().contains(Modifier.FINAL) == true)
                .forEach(finalStorageFields::add);

        storageFieldsInEnum.stream()
                .forEach(element -> usedFields.computeIfAbsent(element, key -> new LinkedHashSet<>()).add(enumElement));
    }

    private void processRegisterStorage(TypeElement processedElement) {
        if (typeUtils.isAssignable(processedElement.asType(), startPointType.asType()) == false) {
            error("Only classes that implements StartPoint can be annotated by @RegisterStorage", processedElement);
            return;
        }

        TypeElement[] sharedEnumClassElements = processedElement.getAnnotationMirrors().stream()
                // get only @RegisterStorage
                .filter(element -> element.getAnnotationType().asElement().equals(registerStorageType))
                // get value of @RegisterStorage
                .flatMap(element -> element.getElementValues().entrySet().stream())
                .filter(element -> element.getKey().getSimpleName().contentEquals("value"))
                .map(Map.Entry::getValue)
                // get type object of value
                .map(element -> (DeclaredType) element.getValue())
                // get element object of value type
                .map(element -> (TypeElement) element.asElement())
                // collect values
                .toArray(TypeElement[]::new);

        for (TypeElement typeElement : sharedEnumClassElements) {
            if (typeElement.getKind().equals(ElementKind.ENUM) == false) {
                error(typeElement + ": has to be enum", typeElement);
            }

            if (typeElement.getAnnotation(Storage.class) == null) {
                error(typeElement + ": has to be annotated with @Storage", typeElement);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void processRegisterStorageRepeatableContainer(TypeElement processedElement) {
        if (typeUtils.isAssignable(processedElement.asType(), startPointType.asType()) == false) {
            error("Only classes that implements StartPoint can be annotated by @RegisterStorage", processedElement);
            return;
        }

        TypeElement[] sharedEnumClassElements = processedElement.getAnnotationMirrors().stream()
                // get only @RegisterStorageRepeatableContainer
                .filter(element -> element.getAnnotationType().asElement().equals(registerStorageRepeatableContainerType))
                // get value of @RegisterStorageRepeatableContainer
                .flatMap(element -> element.getElementValues().entrySet().stream())
                .filter(element -> element.getKey().getSimpleName().contentEquals("value"))
                .map(Map.Entry::getValue)
                // value is array: RegisterStorage[]
                .flatMap(element -> ((List<? extends AnnotationValue>) element.getValue()).stream())
                // treat value as annotation @RegisterStorage
                .map(element -> (AnnotationMirror) element.getValue())
                // get value of @RegisterStorage
                .flatMap(element -> element.getElementValues().entrySet().stream())
                .filter(element -> element.getKey().getSimpleName().contentEquals("value"))
                .map(Map.Entry::getValue)
                // get type object of value
                .map(element -> (DeclaredType) element.getValue())
                // get element object of value type
                .map(element -> (TypeElement) element.asElement())
                // collect values
                .toArray(TypeElement[]::new);

        for (TypeElement typeElement : sharedEnumClassElements) {
            if (typeElement.getKind().equals(ElementKind.ENUM) == false) {
                error(typeElement + ": has to be enum", typeElement);
            }

            if (typeElement.getAnnotation(Storage.class) == null) {
                error(typeElement + ": has to be annotated with @Storage", typeElement);
            }
        }
    }
}
