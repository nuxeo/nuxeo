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
 */
package org.nuxeo.ecm.webengine.jaxrs.scan;

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

    public AnnotationReader(Set<String> annotations) {
        super(Opcodes.ASM7);
        results = new ArrayList<>();
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
