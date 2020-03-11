/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.runtime.test.runner;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.runners.model.FrameworkMethod;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Utility class that sorts a list of JUnit methods according to their source line number.
 *
 * @since 7.1
 */
public class MethodSorter {

    private MethodSorter() {
        // utility class
    }

    /**
     * Sorts a list of JUnit methods according to their source line number.
     *
     * @param methods the JUnit methods
     */
    public static void sortMethodsUsingSourceOrder(List<FrameworkMethod> methods) {
        if (methods.isEmpty()) {
            return;
        }
        Map<String, Integer> nameToLine = new HashMap<>();
        Class<?> cls = methods.get(0).getMethod().getDeclaringClass();
        String name = "/" + cls.getName().replace(".", "/") + ".class";
        try (InputStream is = cls.getResourceAsStream(name)) {
            ClassReader cr = new ClassReader(is);
            ClassVisitor cv = new CV(nameToLine);
            cr.accept(cv, ClassReader.SKIP_FRAMES);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse " + name, e);
        }
        Collections.sort(methods, new LineComparator(nameToLine));
    }

    /**
     * Class Visitor that constructs a map of method name to source line number.
     */
    public static class CV extends ClassVisitor {

        public Map<String, Integer> nameToLine;

        public CV(Map<String, Integer> nameToLine) {
            super(Opcodes.ASM7);
            this.nameToLine = nameToLine;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            return new MV(nameToLine, name);
        }
    }

    /**
     * Method Visitor that records method source line number.
     */
    public static class MV extends MethodVisitor {

        public Map<String, Integer> nameToLine;

        public final String name;

        public MV(Map<String, Integer> nameToLine, String name) {
            super(Opcodes.ASM7);
            this.nameToLine = nameToLine;
            this.name = name;
        }

        @Override
        public void visitLineNumber(int line, Label start) {
            if (nameToLine.get(name) == null) {
                nameToLine.put(name, Integer.valueOf(line));
            }
        }
    }

    /**
     * Comparator of methods according to their line number.
     */
    public static class LineComparator implements Comparator<FrameworkMethod> {

        public Map<String, Integer> nameToLine;

        public LineComparator(Map<String, Integer> nameToLine) {
            this.nameToLine = nameToLine;
        }

        @Override
        public int compare(FrameworkMethod fm1, FrameworkMethod fm2) {
            String name1 = fm1.getMethod().getName();
            String name2 = fm2.getMethod().getName();
            Integer pos1 = nameToLine.get(name1);
            Integer pos2 = nameToLine.get(name2);
            if (pos1 == null || pos2 == null) {
                return name1.compareTo(name2);
            } else {
                return pos1.compareTo(pos2);
            }
        }
    }

}
