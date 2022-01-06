/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.runtime.test.runner;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

import org.junit.Test;

/**
 * @since 2021.15
 */
public class AnnotationScannerTest {

    protected final AnnotationScanner scanner = new AnnotationScanner();

    @Test
    public void testNoAnnotationOnClass() {
        var annotation = scanner.getAnnotation(NoAnnotationOnClass.class, SimpleAnnotation.class);
        assertNull(annotation);

        var annotations = scanner.getAnnotations(NoAnnotationOnClass.class, SimpleAnnotation.class);
        assertTrue(annotations.isEmpty());
    }

    @Test
    public void testAnnotationOnClass() {
        var annotation = scanner.getAnnotation(AnnotationOnClass.class, SimpleAnnotation.class);
        assertEquals("AnnotationOnClass", annotation.value());

        var annotations = scanner.getAnnotations(AnnotationOnClass.class, SimpleAnnotation.class);
        assertEquals(List.of("AnnotationOnClass"), getAnnotationValues(annotations));
    }

    @Test
    public void testAnnotationOnInterface() {
        var annotation = scanner.getAnnotation(AnnotationOnInterface.class, SimpleAnnotation.class);
        assertEquals("InterfaceWithAnnotation", annotation.value());

        var annotations = scanner.getAnnotations(AnnotationOnInterface.class, SimpleAnnotation.class);
        assertEquals(List.of("InterfaceWithAnnotation"), getAnnotationValues(annotations));
    }

    @Test
    public void testAnnotationOnSuperClass() {
        var annotation = scanner.getAnnotation(AnnotationOnSuperClass.class, SimpleAnnotation.class);
        assertEquals("AnnotationOnClass", annotation.value());

        var annotations = scanner.getAnnotations(AnnotationOnSuperClass.class, SimpleAnnotation.class);
        assertEquals(List.of("AnnotationOnClass"), getAnnotationValues(annotations));
    }

    @Test
    public void testAnnotationOnClassAndInterface() {
        var annotation = scanner.getAnnotation(AnnotationOnClassAndInterface.class, SimpleAnnotation.class);
        assertEquals("AnnotationOnClassAndInterface", annotation.value());

        var annotations = scanner.getAnnotations(AnnotationOnClassAndInterface.class, SimpleAnnotation.class);
        assertEquals(List.of("AnnotationOnClassAndInterface", "InterfaceWithAnnotation"),
                getAnnotationValues(annotations));
    }

    @Test
    public void testAnnotationOnClassAndSuperClass() {
        var annotation = scanner.getAnnotation(AnnotationOnClassAndSuperClass.class, SimpleAnnotation.class);
        assertEquals("AnnotationOnClassAndSuperClass", annotation.value());

        var annotations = scanner.getAnnotations(AnnotationOnClassAndSuperClass.class, SimpleAnnotation.class);
        assertEquals(List.of("AnnotationOnClassAndSuperClass", "AnnotationOnClass"), getAnnotationValues(annotations));
    }

    @Test
    public void testAnnotationOnInterfaceAndSuperClass() {
        var annotation = scanner.getAnnotation(AnnotationOnInterfaceAndSuperClass.class, SimpleAnnotation.class);
        assertEquals("InterfaceWithAnnotation", annotation.value());

        var annotations = scanner.getAnnotations(AnnotationOnInterfaceAndSuperClass.class, SimpleAnnotation.class);
        assertEquals(List.of("InterfaceWithAnnotation", "AnnotationOnClass"), getAnnotationValues(annotations));
    }

    @Test
    public void testInheritedAnnotationOnSuperClass() {
        var annotation = scanner.getAnnotation(InheritedAnnotationOnSuperClass.class, InheritedAnnotation.class);
        assertEquals("InheritedAnnotationOnClass", annotation.value());

        var annotations = scanner.getAnnotations(InheritedAnnotationOnSuperClass.class, InheritedAnnotation.class);
        assertEquals(List.of("InheritedAnnotationOnClass"), getInheritedAnnotationValues(annotations));
    }

    @Test
    public void testInheritedAnnotationOnInterfaceAndSuperClass() {
        var annotation = scanner.getAnnotation(InheritedAnnotationOnInterfaceAndSuperClass.class,
                InheritedAnnotation.class);
        assertEquals("InheritedAnnotationOnClass", annotation.value());

        var annotations = scanner.getAnnotations(InheritedAnnotationOnInterfaceAndSuperClass.class,
                InheritedAnnotation.class);
        assertEquals(List.of("InheritedAnnotationOnClass", "InterfaceWithInheritedAnnotation"),
                getInheritedAnnotationValues(annotations));
    }

    @Test
    public void testRepeatableAnnotationOnClass() {
        var annotation = scanner.getAnnotation(RepeatableAnnotationOnClass.class, RepeatableAnnotation.class);
        assertEquals("RepeatableAnnotationOnClass1", annotation.value());

        var annotations = scanner.getAnnotations(RepeatableAnnotationOnClass.class, RepeatableAnnotation.class);
        assertEquals(List.of("RepeatableAnnotationOnClass1", "RepeatableAnnotationOnClass2"),
                getRepeatableAnnotationValues(annotations));
    }

    @Test
    public void testRepeatableAnnotationOnInterface() {
        var annotation = scanner.getAnnotation(RepeatableAnnotationOnInterface.class, RepeatableAnnotation.class);
        assertEquals("InterfaceWithRepeatableAnnotation1", annotation.value());

        var annotations = scanner.getAnnotations(RepeatableAnnotationOnInterface.class, RepeatableAnnotation.class);
        assertEquals(List.of("InterfaceWithRepeatableAnnotation1", "InterfaceWithRepeatableAnnotation2"),
                getRepeatableAnnotationValues(annotations));
    }

    @Test
    public void testRepeatableAnnotationOnClassAndSuperClass() {
        var annotation = scanner.getAnnotation(RepeatableAnnotationOnClassAndSuperClass.class,
                RepeatableAnnotation.class);
        assertEquals("RepeatableAnnotationOnClassAndSuperClass1", annotation.value());

        var annotations = scanner.getAnnotations(RepeatableAnnotationOnClassAndSuperClass.class,
                RepeatableAnnotation.class);
        assertEquals(
                List.of("RepeatableAnnotationOnClassAndSuperClass1", "RepeatableAnnotationOnClassAndSuperClass2",
                        "RepeatableAnnotationOnClass1", "RepeatableAnnotationOnClass2"),
                getRepeatableAnnotationValues(annotations));
    }

    @Test
    public void testRepeatableAnnotationOnInterfaceAndSuperClass() {
        var annotation = scanner.getAnnotation(RepeatableAnnotationOnInterfaceAndSuperClass.class,
                RepeatableAnnotation.class);
        assertEquals("InterfaceWithRepeatableAnnotation1", annotation.value());

        var annotations = scanner.getAnnotations(RepeatableAnnotationOnInterfaceAndSuperClass.class,
                RepeatableAnnotation.class);
        assertEquals(
                List.of("InterfaceWithRepeatableAnnotation1", "InterfaceWithRepeatableAnnotation2",
                        "RepeatableAnnotationOnClass1", "RepeatableAnnotationOnClass2"),
                getRepeatableAnnotationValues(annotations));
    }

    protected List<String> getAnnotationValues(List<SimpleAnnotation> annotations) {
        return annotations.stream().map(SimpleAnnotation::value).collect(toList());
    }

    protected List<String> getInheritedAnnotationValues(List<InheritedAnnotation> annotations) {
        return annotations.stream().map(InheritedAnnotation::value).collect(toList());
    }

    protected List<String> getRepeatableAnnotationValues(List<RepeatableAnnotation> annotations) {
        return annotations.stream().map(RepeatableAnnotation::value).collect(toList());
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.METHOD })
    public @interface SimpleAnnotation {

        String value() default "a default";
    }

    public static class NoAnnotationOnClass {
    }

    @SimpleAnnotation("AnnotationOnClass")
    public static class AnnotationOnClass {
    }

    @SimpleAnnotation("InterfaceWithAnnotation")
    public interface InterfaceWithAnnotation {
    }

    public static class AnnotationOnInterface implements InterfaceWithAnnotation {
    }

    public static class AnnotationOnSuperClass extends AnnotationOnClass {
    }

    @SimpleAnnotation("AnnotationOnClassAndInterface")
    public static class AnnotationOnClassAndInterface implements InterfaceWithAnnotation {
    }

    @SimpleAnnotation("AnnotationOnClassAndSuperClass")
    public static class AnnotationOnClassAndSuperClass extends AnnotationOnClass {
    }

    public static class AnnotationOnInterfaceAndSuperClass extends AnnotationOnClass
            implements InterfaceWithAnnotation {
    }

    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.METHOD })
    public @interface InheritedAnnotation {

        String value() default "a default";
    }

    @InheritedAnnotation("InheritedAnnotationOnClass")
    public static class InheritedAnnotationOnClass {
    }

    @InheritedAnnotation("InterfaceWithInheritedAnnotation")
    public interface InterfaceWithInheritedAnnotation {
    }

    public static class InheritedAnnotationOnSuperClass extends InheritedAnnotationOnClass {
    }

    public static class InheritedAnnotationOnInterfaceAndSuperClass extends InheritedAnnotationOnClass
            implements InterfaceWithInheritedAnnotation {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.METHOD })
    public @interface RepeatableAnnotations {

        RepeatableAnnotation[] value();
    }

    @Repeatable(RepeatableAnnotations.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.METHOD })
    public @interface RepeatableAnnotation {

        String value() default "a default";
    }

    @RepeatableAnnotation("RepeatableAnnotationOnClass1")
    @RepeatableAnnotation("RepeatableAnnotationOnClass2")
    public static class RepeatableAnnotationOnClass {
    }

    @RepeatableAnnotation("InterfaceWithRepeatableAnnotation1")
    @RepeatableAnnotation("InterfaceWithRepeatableAnnotation2")
    public interface InterfaceWithRepeatableAnnotation {
    }

    public static class RepeatableAnnotationOnInterface implements InterfaceWithRepeatableAnnotation {
    }

    @RepeatableAnnotation("RepeatableAnnotationOnClassAndSuperClass1")
    @RepeatableAnnotation("RepeatableAnnotationOnClassAndSuperClass2")
    public static class RepeatableAnnotationOnClassAndSuperClass extends RepeatableAnnotationOnClass {
    }

    public static class RepeatableAnnotationOnInterfaceAndSuperClass extends RepeatableAnnotationOnClass
            implements InterfaceWithRepeatableAnnotation {
    }
}
