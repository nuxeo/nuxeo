/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.webengine.jaxrs.scan;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class AnnotationReader implements ClassVisitor {

    protected Set<String> annotations;

    protected List<String> results;

    protected String cname;

    public AnnotationReader(Set<String> annotations) throws IOException {
        results = new ArrayList<String>();
        this.annotations = annotations;
    }

    public String getClassName() {
        return cname.replace('/', '.');
    }

    public String getFileName() {
        return cname;
    }

    public List<String> getResults() {
        return results;
    }

    public boolean hasResults() {
        return !results.isEmpty();
    }

    @Override
    public void visit(int version, int access, String name, String signature,
            String superName, String[] interfaces) {
        cname = name;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String arg0, boolean arg1) {
        if (annotations.contains(arg0)) {
            results.add(arg0);
        }
        return null;
    }

    @Override
    public void visitAttribute(Attribute arg0) {
    }

    @Override
    public void visitEnd() {
    }

    @Override
    public FieldVisitor visitField(int arg0, String arg1, String arg2,
            String arg3, Object arg4) {
        return null;
    }

    @Override
    public void visitInnerClass(String arg0, String arg1, String arg2, int arg3) {
    }

    @Override
    public MethodVisitor visitMethod(int arg0, String arg1, String arg2,
            String arg3, String[] arg4) {
        return null;
    }

    @Override
    public void visitOuterClass(String arg0, String arg1, String arg2) {
    }

    @Override
    public void visitSource(String arg0, String arg1) {
    }

}
