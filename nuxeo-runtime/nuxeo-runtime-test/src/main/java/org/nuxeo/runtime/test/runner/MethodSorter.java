/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

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
    public static class CV implements ClassVisitor {

        public Map<String, Integer> nameToLine;

        public CV(Map<String, Integer> nameToLine) {
            this.nameToLine = nameToLine;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        }

        @Override
        public void visitSource(String source, String debug) {
        }

        @Override
        public void visitOuterClass(String owner, String name, String desc) {
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            return null;
        }

        @Override
        public void visitAttribute(Attribute attr) {
        }

        @Override
        public void visitInnerClass(String name, String outerName, String innerName, int access) {
        }

        @Override
        public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
            return null;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            return new MV(nameToLine, name);
        }

        @Override
        public void visitEnd() {
        }
    }

    /**
     * Method Visitor that records method source line number.
     */
    public static class MV implements MethodVisitor {

        public Map<String, Integer> nameToLine;

        public final String name;

        public MV(Map<String, Integer> nameToLine, String name) {
            this.nameToLine = nameToLine;
            this.name = name;
        }

        @Override
        public AnnotationVisitor visitAnnotationDefault() {
            return null;
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            return null;
        }

        @Override
        public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
            return null;
        }

        @Override
        public void visitAttribute(Attribute attr) {
        }

        @Override
        public void visitCode() {
        }

        @Override
        public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
        }

        @Override
        public void visitInsn(int opcode) {
        }

        @Override
        public void visitIntInsn(int opcode, int operand) {
        }

        @Override
        public void visitVarInsn(int opcode, int var) {
        }

        @Override
        public void visitTypeInsn(int opcode, String desc) {
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc) {
        }

        @Override
        public void visitJumpInsn(int opcode, Label label) {
        }

        @Override
        public void visitLabel(Label label) {
        }

        @Override
        public void visitLdcInsn(Object cst) {
        }

        @Override
        public void visitIincInsn(int var, int increment) {
        }

        @Override
        public void visitTableSwitchInsn(int min, int max, Label dflt, Label[] labels) {
        }

        @Override
        public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        }

        @Override
        public void visitMultiANewArrayInsn(String desc, int dims) {
        }

        @Override
        public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
        }

        @Override
        public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
        }

        @Override
        public void visitLineNumber(int line, Label start) {
            if (nameToLine.get(name) == null) {
                nameToLine.put(name, Integer.valueOf(line));
            }
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
        }

        @Override
        public void visitEnd() {
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
