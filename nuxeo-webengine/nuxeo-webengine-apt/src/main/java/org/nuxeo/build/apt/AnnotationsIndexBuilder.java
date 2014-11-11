/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.build.apt;

import static java.util.Collections.emptySet;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.mirror.apt.AnnotationProcessor;
import com.sun.mirror.apt.AnnotationProcessorEnvironment;
import com.sun.mirror.apt.AnnotationProcessorFactory;
import com.sun.mirror.apt.AnnotationProcessors;
import com.sun.mirror.apt.Filer;
import com.sun.mirror.apt.Messager;
import com.sun.mirror.declaration.AnnotationMirror;
import com.sun.mirror.declaration.AnnotationTypeDeclaration;
import com.sun.mirror.declaration.AnnotationTypeElementDeclaration;
import com.sun.mirror.declaration.AnnotationValue;
import com.sun.mirror.declaration.ClassDeclaration;
import com.sun.mirror.declaration.TypeDeclaration;
import com.sun.mirror.type.TypeMirror;
import com.sun.mirror.util.DeclarationVisitor;
import com.sun.mirror.util.DeclarationVisitors;
import com.sun.mirror.util.SimpleDeclarationVisitor;

/**
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class AnnotationsIndexBuilder implements AnnotationProcessorFactory {

    // Process any set of annotations
    private static final Set<String> supportedAnnotations = new HashSet<String>(
            Arrays.asList(
                    "org.nuxeo.ecm.webengine.model.WebObject",
                    "org.nuxeo.ecm.webengine.model.WebAdapter",
                    "org.nuxeo.ecm.webengine.model.WebModule",
                    "org.nuxeo.ecm.webengine.model.WebView",
                    "javax.ws.rs.Path"));

    private static final String EOL = System.getProperty("line.separator");

    // No supported options
    private static final Collection<String> supportedOptions = emptySet();

    public Collection<String> supportedAnnotationTypes() {
//        String str = System.getProperty("org.nuxeo.build.annotations");
//        if (str != null) {
//            String[] ar = str.split(":");
//            HashSet<String> result = new HashSet<String>(Arrays.asList(ar));
//            System.out.println("@@@>>>>>>>>>>>>> "+result);
//            return result;
//        } else {
//            System.out.println("@@@>>>>>>>>>>>>> EMPTY");
//          return emptySet();
//        }
        return supportedAnnotations;
    }

    public Collection<String> supportedOptions() {
        return supportedOptions;
    }


    protected AnnotationProcessorEnvironment env;

    public AnnotationProcessor getProcessorFor(
            Set<AnnotationTypeDeclaration> atds,
            AnnotationProcessorEnvironment env) {
        AnnotationProcessor result = null;
        if (atds.isEmpty()) {
            result = AnnotationProcessors.NO_OP;
        } else {
            result = new Processor(env);
        }
        return result;
    }

    private static class Processor implements AnnotationProcessor {
        private final AnnotationProcessorEnvironment env;
        protected PrintWriter writer;
        protected StringBuilder buf;

        Processor(AnnotationProcessorEnvironment env) {
            this.env = env;
        }

        class Visitor extends SimpleDeclarationVisitor {
            public void visitClassDeclaration(ClassDeclaration d) {
                String className = ((ClassDeclaration)d).getQualifiedName();
                for (AnnotationMirror anno : d.getAnnotationMirrors()) {
                    AnnotationTypeDeclaration adecl = anno.getAnnotationType().getDeclaration();
                    if (!supportedAnnotations.contains(adecl.getQualifiedName())) {
                        continue;
                    }
                    buf.append(className).append('|').append(adecl.getQualifiedName());
                    List<String> indexes = null;
                    for (AnnotationMirror aa : adecl.getAnnotationMirrors()) {
                        if (aa.getAnnotationType().getDeclaration().getQualifiedName().equals("org.nuxeo.runtime.annotations.loader.Indexable")) {
                            List<AnnotationValue> values = (List<AnnotationValue>)aa.getElementValues().values().iterator().next().getValue();
                            indexes = new ArrayList<String>(values.size());
                            for (AnnotationValue v : values) {
                                indexes.add((String)v.getValue());
                            }
                            break;
                        }
                    }
                    if (indexes != null) {
                        buf.append('|');
                        for (Map.Entry<AnnotationTypeElementDeclaration, AnnotationValue> entry : anno.getElementValues().entrySet()) {
                            AnnotationTypeElementDeclaration akey = entry.getKey();
                            String key = akey.getSimpleName();
                            if (!indexes.contains(key)) {
                                continue;
                            }
                            AnnotationValue aval = entry.getValue();
                            if (aval == null) {
                                aval = akey.getDefaultValue();
                            }
                            TypeMirror type = akey.getReturnType();
                            Object value = aval.getValue();
                            buf.append(key).append('|').append(encode(type, value)).append('|');
                        }
                        buf.setLength(buf.length()-1); // remove trailing &
                    }
                    buf.append(EOL);
                }
            }
        }

        //TODO: encode value
        public final static String encode(TypeMirror type, Object value) {
            return value.toString();
        }

        public void process() {
            try {
                buf = new StringBuilder();
                Messager log = env.getMessager();

                DeclarationVisitor scanner = DeclarationVisitors.getDeclarationScanner(
                        new Visitor(),
                        DeclarationVisitors.NO_OP);

                for (TypeDeclaration typeDecl : env.getSpecifiedTypeDeclarations()) {
                    typeDecl.accept(scanner);
                }

                if (buf.length() > 0) {
                    writer = env.getFiler().createTextFile(Filer.Location.CLASS_TREE, "",
                            new File("OSGI-INF/annotations"), "UTF-8");
                    writer.print(buf.toString());
                    writer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
