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
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class AnnotationReader extends ClassVisitor {

    protected Set<String> annotations;

    protected List<String> results;

    protected String cname;

    public AnnotationReader(Set<String> annotations) throws IOException {
        super(Opcodes.ASM5);
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
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        cname = name;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String arg0, boolean arg1) {
        if (annotations.contains(arg0)) {
            results.add(arg0);
        }
        return null;
    }

}
