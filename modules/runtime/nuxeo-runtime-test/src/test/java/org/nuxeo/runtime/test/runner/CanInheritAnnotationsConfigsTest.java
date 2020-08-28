/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.runtime.test.runner;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hamcrest.Matchers;
import org.hamcrest.MatcherAssert;
import org.junit.Test;

public class CanInheritAnnotationsConfigsTest {

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Annotation {
        String noDefault();

        String withDefault() default "default";
    }

    @Annotation(noDefault = "base", withDefault = "base")
    public static class Base {

    }

    @Annotation(noDefault = "specialized")
    public static class Specialized extends Base {

    }

    @Annotation(noDefault = "another")
    public static class Another {

    }

    @Test
    public void inheritsDefaultsFromBase() {
        AnnotationScanner scanner = new AnnotationScanner();
        Annotation annotation = scanner.getAnnotation(Specialized.class, Annotation.class);
        MatcherAssert.assertThat("no default", annotation.noDefault(), Matchers.is("specialized"));
        MatcherAssert.assertThat("with default", annotation.withDefault(), Matchers.is("base"));
    }

    @Test
    public void inheritsFromMultiple() {
        AnnotationScanner scanner = new AnnotationScanner();
        Annotation base = scanner.getAnnotation(Base.class, Annotation.class);
        Annotation another = scanner.getAnnotation(Another.class, Annotation.class);
        Annotation annotation = Defaults.of(Annotation.class, another, base);
        MatcherAssert.assertThat("no default", annotation.noDefault(), Matchers.is("another"));
        MatcherAssert.assertThat("with default", annotation.withDefault(), Matchers.is("base"));
    }
}
