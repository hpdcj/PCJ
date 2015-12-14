/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal.storage;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import org.pcj.Shared;

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
@SupportedAnnotationTypes("org.pcj.Shared")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class SharedProcessor extends javax.annotation.processing.AbstractProcessor {

    private TypeElement serializableType;
    private TypeElement storageType;
    private Types typeUtils;
    private Map<String, Set<String>> sharedFields;

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
        storageType = processingEnv.getElementUtils().getTypeElement(InternalStorage.class.getCanonicalName());
        typeUtils = processingEnv.getTypeUtils();
        sharedFields = new HashMap<>();

        roundEnv.getElementsAnnotatedWith(Shared.class).stream()
                .forEach(element -> process(element));

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
    
    private void process(Element e) {
        String className = e.getEnclosingElement().toString();
        String sharedName = e.toString();

        Set<String> classFields;
        if (sharedFields.containsKey(className) == false) {
            Element storage = e.getEnclosingElement();
            if (typeUtils.isAssignable(storage.asType(), storageType.asType()) == false) {
                error("Class does not implement Storage interface", storage);
            }

            storage.getEnclosedElements().stream()
                    .filter(element -> element.getKind().equals(ElementKind.METHOD))
                    .filter(element -> isSuppressed(element, "method") == false)
                    .forEach(element -> warn("[method] Be sure that method does not use @Shared fields directly", element));

            classFields = new HashSet<>();
            sharedFields.put(className, classFields);
        } else {
            classFields = sharedFields.get(className);
        }

        if (classFields.contains(sharedName)) {
            error("Duplicate shared field '" + sharedName + "'!", e);
        } else {
            classFields.add(sharedName);
        }

        if (e.getModifiers().contains(Modifier.PRIVATE) == false) {
            if (isSuppressed(e, "nonprivate") == false) {
                warn("[nonprivate] Object is not private!", e);
            }
        }

        if (e.getModifiers().contains(Modifier.FINAL) == true) {
            error("Object is final!", e);
        }

        if (typeUtils.isAssignable(e.asType(), serializableType.asType()) == false) {
            error("Object is not serializable!", e);
        }
    }
}
