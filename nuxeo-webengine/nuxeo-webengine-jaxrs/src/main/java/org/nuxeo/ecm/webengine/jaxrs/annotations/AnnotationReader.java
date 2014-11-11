/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.ecm.webengine.jaxrs.annotations;




/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class AnnotationReader {/*implements ClassVisitor {

    protected boolean found = false;

    public AnnotationReader(InputStream in) throws IOException {
        ClassReader reader = new ClassReader(in);
        reader.accept(this, null, ClassReader.SKIP_DEBUG | ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES);
        //reader.accept(this, Attributes.getDefaultAttributes(), ClassReader.SKIP_DEBUG);
    }


    @Override
    public void visit(int arg0, int arg1, String arg2, String arg3,
            String arg4, String[] arg5) {
    }

    @Override
    public AnnotationVisitor visitAnnotation(String arg0, boolean arg1) {
        if ("Ljavax/ws/rs/Path;".equals(arg0)) {
            found = true;
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


    public static void main(String[] args) throws Exception {
        System.out.println(new AnnotationReader(AnnotationReader.class.getResourceAsStream("Test.class")).found);
    }
    */
}
