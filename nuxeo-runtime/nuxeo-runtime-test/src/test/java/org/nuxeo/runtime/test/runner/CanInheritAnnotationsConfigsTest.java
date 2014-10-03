/*******************************************************************************
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.nuxeo.runtime.test.runner;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

public class CanInheritAnnotationsConfigsTest {

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Annotation {
        String noDefault();
        String withDefault() default "default";
    }

    @Annotation(noDefault="base", withDefault="base")
    public static class Base {

    }

    @Annotation(noDefault="specialized")
    public static class Specialized extends Base {

    }

    @Annotation(noDefault="another")
    public static class Another {

    }

    @Test public void inheritsDefaultsFromBase() {
        AnnotationScanner scanner = new AnnotationScanner();
        Annotation annotation = scanner.getAnnotation(Specialized.class, Annotation.class);
        Assert.assertThat("no default", annotation.noDefault(), Matchers.is("specialized"));
        Assert.assertThat("with default", annotation.withDefault(), Matchers.is("base"));
    }

    @Test public void inheritsFromMultiple() {
        AnnotationScanner scanner = new AnnotationScanner();
        Annotation base = scanner.getAnnotation(Base.class, Annotation.class);
        Annotation another = scanner.getAnnotation(Another.class, Annotation.class);
        Annotation annotation = Defaults.of(Annotation.class, another, base);
        Assert.assertThat("no default", annotation.noDefault(), Matchers.is("another"));
        Assert.assertThat("with default", annotation.withDefault(), Matchers.is("base"));
    }
}
