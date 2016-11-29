/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.runtime.annotations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class AnnotationTest {

    static final Log log = LogFactory.getLog(AnnotationTest.class);

    @Test
    public void testAnnotations() {
        AnnotationManager mgr = new AnnotationManager();
        AnnotatedClass<?> acA = mgr.getAnnotatedClass(A.class);
        AnnotatedClass<?> acC = mgr.getAnnotatedClass(C.class);
        AnnotatedClass<?> acB = mgr.getAnnotatedClass(B.class);
        AnnotatedClass<?> acI = mgr.getAnnotatedClass(I.class);

        assertEquals("A_class_anno", acA.getAnnotation(Anno1.class).value());
        assertEquals("B_class_anno", acB.getAnnotation(Anno1.class).value());
        assertEquals("B_class_anno", acC.getAnnotation(Anno1.class).value());
        assertEquals("I_class_anno", acC.getAnnotation(Anno2.class).value());
        assertEquals("I_class_anno", acI.getAnnotation(Anno2.class).value());

        assertEquals("[@org.nuxeo.runtime.annotations.Anno1(value=A_class_anno)]",
                Arrays.asList(acA.getAnnotations()).toString());
        assertEquals("[@org.nuxeo.runtime.annotations.Anno1(value=B_class_anno)]",
                Arrays.asList(acB.getAnnotations()).toString());

        List<Annotation> actual = Arrays.asList(acC.getAnnotations());
        assertEquals(2, actual.size());
        String a1 = "@org.nuxeo.runtime.annotations.Anno2(value=I_class_anno)";
        String a2 = "@org.nuxeo.runtime.annotations.Anno1(value=B_class_anno)";
        assertTrue(("[" + a1 + ", " + a2 + "]").equals(actual.toString())
                || ("[" + a2 + ", " + a1 + "]").equals(actual.toString()));

        assertEquals("[@org.nuxeo.runtime.annotations.Anno2(value=I_class_anno)]",
                Arrays.asList(acI.getAnnotations()).toString());

        // methods
        if (log.isDebugEnabled()) {
            log.debug("#################methods##########");
            printMethodAnnos(acA, Anno1.class);
            printMethodAnnos(acA, Anno2.class);
            printMethodAnnos(acB, Anno1.class);
            printMethodAnnos(acB, Anno2.class);
            printMethodAnnos(acC, Anno1.class);
            printMethodAnnos(acC, Anno2.class);
            printMethodAnnos(acI, Anno1.class);
            printMethodAnnos(acI, Anno2.class);
        }
    }

    static void printMethodAnnos(AnnotatedClass<?> ac, Class<? extends Annotation> annoClass) {
        AnnotatedMethod[] ams = ac.getAnnotatedMethods(annoClass);
        log.debug("Annotation " + annoClass + " on class " + ac.getAnnotatedClass() + " is present on methods:");
        if (ams.length == 0) {
            log.debug("N/A");
        } else {
            for (AnnotatedMethod am : ams) {
                log.debug(am.getMethod() + " => " + am.getAnnotation(annoClass));
            }
        }
    }
}

@Anno1("A_class_anno")
class A {
    @Anno1("A_test1_anno")
    public void test1() {
    }

    @Anno1("A_test2_anno")
    public void test2() {
    }

    public void test3() {
    }
}

@Anno1("B_class_anno")
class B extends A {
    @Override
    @Anno1("B_test1_anno")
    public void test1() {
    }

    @Override
    @Anno2("B_test2_anno2")
    public void test2() {
    } // inherit annotation

    @Override
    public void test3() {
    }
}

@Anno2("I_class_anno")
interface I {
    @Anno2("I_test2_anno")
    void test1();
}

class C extends B implements I {
    @Override
    public void test1() {
    }

    @Override
    public void test2() {
    }

    @Override
    @Anno1("C_test3_anno")
    public void test3() {
    }
}
