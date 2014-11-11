/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.runtime.annotations;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class AnnotationTest extends TestCase {

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
        assertTrue(("[" + a1 + ", " + a2 + "]").equals(actual.toString()) ||
                ("[" + a2 + ", " + a1 + "]").equals(actual.toString()));

        assertEquals("[@org.nuxeo.runtime.annotations.Anno2(value=I_class_anno)]",
                Arrays.asList(acI.getAnnotations()).toString());

        // methods
        if (false) {
            System.out.println("#################methods##########");
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
        System.out.println("Annotation "+annoClass+" on class "+ac.getAnnotatedClass()+" is present on methods:");
        if (ams.length == 0) {
            System.out.println("N/A");
        } else {
            for (AnnotatedMethod am : ams) {
                System.out.println(am.getMethod() +" => "+ am.getAnnotation(annoClass));
            }
        }
    }

}

@Anno1("A_class_anno")
class A {
    @Anno1("A_test1_anno")
    public void test1() {}
    @Anno1("A_test2_anno")
    public void test2() {}
    public void test3() {}
}

@Anno1("B_class_anno")
class B extends A {
    @Override
    @Anno1("B_test1_anno")
    public void test1() {}
    @Override
    @Anno2("B_test2_anno2")
    public void test2() {} // inherit annotation
    @Override
    public void test3() {}
}

@Anno2("I_class_anno")
interface I {
    @Anno2("I_test2_anno")
    void test1();
}

class C extends B implements I {
    @Override
    public void test1() {}
    @Override
    public void test2() {}
    @Override
    @Anno1("C_test3_anno")
    public void test3() {}
}
