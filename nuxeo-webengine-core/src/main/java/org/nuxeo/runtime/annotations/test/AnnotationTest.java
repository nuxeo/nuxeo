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

package org.nuxeo.runtime.annotations.test;

import java.lang.annotation.Annotation;

import org.nuxeo.runtime.annotations.AnnotatedClass;
import org.nuxeo.runtime.annotations.AnnotatedMethod;
import org.nuxeo.runtime.annotations.AnnotationManager;

import edu.emory.mathcs.backport.java.util.Arrays;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class AnnotationTest {

    @Anno1("A_class_anno")
    static class A {
        @Anno1("A_test1_anno")
        public void test1() {}
        @Anno1("A_test2_anno")
        public void test2() {}
        public void test3() {}
    }

    @Anno1("B_class_anno")
    static class B extends A {
        @Anno1("B_test1_anno")
        public void test1() {}
        @Anno2("B_test2_anno2")
        public void test2() {} // inherit annotation
        public void test3() {}
    }


    @Anno2("I_class_anno")
    static interface I {
        @Anno2("I_test2_anno")
        void test1();
    }

    static class C extends B implements I {
        public void test1() {}
        public void test2() {}
        @Anno1("C_test3_anno")
        public void test3() {}
    }

    public static void main(String[] args) throws Exception {

        AnnotationManager mgr = new AnnotationManager();
        AnnotatedClass<?> acA = mgr.getAnnotatedClass(A.class);
        AnnotatedClass<?> acC = mgr.getAnnotatedClass(C.class);
        AnnotatedClass<?> acB = mgr.getAnnotatedClass(B.class);
        AnnotatedClass<?> acI = mgr.getAnnotatedClass(I.class);

        System.out.println("acA.anno1: "+acA.getAnnotation(Anno1.class).value());
        System.out.println("acB.anno1: "+acB.getAnnotation(Anno1.class).value());
        System.out.println("acC.anno1: "+acC.getAnnotation(Anno1.class).value());
        System.out.println("acC.anno2: "+acC.getAnnotation(Anno2.class).value());
        System.out.println("acI.anno2: "+acI.getAnnotation(Anno2.class).value());

        System.out.println("acA.annos: "+Arrays.asList(acA.getAnnotations()));
        System.out.println("acB.annos: "+Arrays.asList(acB.getAnnotations()));
        System.out.println("acC.annos: "+Arrays.asList(acC.getAnnotations()));
        System.out.println("acI.annos: "+Arrays.asList(acI.getAnnotations()));

        // methods
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
