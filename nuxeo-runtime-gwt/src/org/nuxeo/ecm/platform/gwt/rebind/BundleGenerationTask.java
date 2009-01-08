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

package org.nuxeo.ecm.platform.gwt.rebind;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.nuxeo.ecm.platform.gwt.client.ApplicationBundle;
import org.nuxeo.ecm.platform.gwt.client.Bundle;
import org.nuxeo.ecm.platform.gwt.client.Extension;
import org.nuxeo.ecm.platform.gwt.client.ExtensionPoint;
import org.nuxeo.ecm.platform.gwt.client.Framework;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JType;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class BundleGenerationTask extends GenerationTask {

    
    
    @Override
    public void run() throws UnableToCompleteException {
        composer.addImport(Framework.class.getName());
        composer.addImport(ApplicationBundle.class.getName());
        composer.addImplementedInterface(ApplicationBundle.class.getName());
        
        // Bundles will be processed from right to root
        // Given bundles A: B, C and B: B1, B2 and C: C1, C2
        // they will be processed in the following order:
        // C2, C1, C, B2, B1, B, A
        ArrayList<JClassType> bundles = new ArrayList<JClassType>();
        bundles.add(typeInfo.getClassType());        
        collectSuperBundles(typeInfo.getClassType(), bundles);
        Collections.reverse(bundles);
        
        writer.indent();
        writer.println();
        writer.indent();
        writer.println();
        writeStartMethod();
        writer.println();
        writeStartMethod2();
        writer.println();        
        writeDeployMethod(bundles);
        writer.outdent();
        writer.outdent();
        writer.println();
        writer.commit(logger);
    }
    
    private void collectSuperBundles(JClassType type, List<JClassType> bundles) throws UnableToCompleteException {
        Bundle bundle = type.getAnnotation(Bundle.class);
        if (bundle == null) {
            logger.log(TreeLogger.ERROR, "Application Bundles must be annotated with @Bundle'"
                    + typeName + "'", null);
            throw new UnableToCompleteException();            
        }            
        Class<?>[] superBundles = bundle.value();
        for (Class<?> clazz : superBundles) {
            type = getTypeForClass(clazz);
            bundles.add(type);
            collectSuperBundles(type, bundles);
        }
    }    
    
    private void writeStartMethod() {
        writer.println("public void start() {");
        writer.indent();
        writer.println("deploy();");
        writer.println("Framework.start();");
        writer.outdent();
        writer.println("}");
    }

    private void writeStartMethod2() {
        writer.println("public void start(String url) {");
        writer.indent();
        writer.println("deploy();");
        writer.println("Framework.start(url);");
        writer.outdent();
        writer.println("}");
    }

    private void writeDeployMethod(List<JClassType> bundles) throws UnableToCompleteException { 
        // it's important to preserve order so lets use a LinkedHashSet
        Set<JMethod> extensions = new LinkedHashSet<JMethod>();
        Set<JMethod> lastExtensions = new LinkedHashSet<JMethod>();
        Set<JMethod> extPoints = new LinkedHashSet<JMethod>();
        Set<JMethod> instances = new LinkedHashSet<JMethod>();
        

        // start processing bundles
        for (JClassType type : bundles) {
            collectAnnotatedMethods(type, extensions, lastExtensions, extPoints, instances);
        }

        // write method start
        writer.println("public void deploy() {");
        writer.indent();
        // write local vars instantiation 
        writer.println();
        writeInstanceDecls(instances);
        writer.println();
        // write extension points registration
        for (JMethod method : extPoints) {
            writeExtensionPoint(method);
        }
        writer.println();
        // write extension registration
        // first order extensions given their ordering hints
        for (JMethod method : reorderExtensions(extensions)) {
            writeExtension(method);
        }
        writer.println();
        // write extension registrations for the extensions marked as ADD_IF_NOT_EXIST
        for (JMethod method : lastExtensions) {
            writeExtension(method);
        }
        writer.println();
        // close method
        writer.outdent();
        writer.println("}");
    }


    private void writeInstanceDecls(Set<JMethod> instances) {
        for (JMethod m : instances) {
            writeInstanceDecl(m.getReturnType(), m.getName());
        }
    }
    
    private void writeInstanceDecl(JType type, String name) {
        composer.addImport(type.getQualifiedSourceName());
        writer.println(type.getSimpleSourceName()+" "+name +" = new "+ type.getSimpleSourceName() +"();");
    }
    
    private void writeExtensionPoint(JMethod method) {
        ExtensionPoint point = method.getAnnotation(ExtensionPoint.class);
        for (String xp : point.value()) {
            writer.println("Framework.registerExtensionPoint(\""+xp+"\", "+method.getName()+");");
        }
    }

    private void writeExtension(JMethod method) {
        Extension ext = method.getAnnotation(Extension.class);
        for (String point : ext.targets()) {            
            writer.println("Framework.registerExtension(\""+point+"\", "+ method.getName()+");");
        }
    }

    public void collectAnnotatedMethods(JClassType type, 
            Set<JMethod> extensions, Set<JMethod> lastExtensions,
            Set<JMethod> extPoints, Set<JMethod> instances) throws UnableToCompleteException {
        for (JMethod m : type.getMethods()) {
            boolean found = false;
            Extension ext = m.getAnnotation(Extension.class);
            if (ext != null) {
                if (ext.hint() == Extension.AS_DEFAULT) { // TODO AS_DEFAULT hints are not correctly handled
                    lastExtensions.add(m);
                    found = true;
                } else {
                    extensions.add(m);
                    found = true;
                }
            }
            ExtensionPoint xp = m.getAnnotation(ExtensionPoint.class);
            if (xp != null) {
                extPoints.add(m);
                found = true;
            }
            // add method return type
            if (found) {
                JType rt = m.getReturnType();
                if (!(rt instanceof JClassType)) {
                    logger.log(TreeLogger.ERROR, 
                            "Extension point definition methods inside Application Bundles must return object instances '"
                            + m.getName() + "'", null);
                    throw new UnableToCompleteException();
                }
                instances.add(m);
            }
        }
        // collect methods from super types
//        for (JClassType t : type.getImplementedInterfaces()) {
//            collectAnnotatedMethods(t, extensions, lastExtensions, extPoints, instances);
//        }
    }

    protected List<JMethod>reorderExtensions(Set<JMethod> extensions) {
        ArrayList<JMethod> result = new ArrayList<JMethod>(extensions);
        Collections.sort(result, new ExtensionComparator());
        return result;
    }
    
    static class ExtensionComparator implements Comparator<JMethod> {
        public int compare(JMethod o1, JMethod o2) {
            int h1 = o1.getAnnotation(Extension.class).hint();            
            int h2 = o2.getAnnotation(Extension.class).hint();
            if (h1 == h2) {
                return 0;
            }
            if (h1 < 0) {
                return 1;
            }
            if (h2 < 0) {
                return -1;
            }
            return h1 - h2;
        }  
    }
}
