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

package org.nuxeo.ecm.webengine.rest.impl;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.nuxeo.ecm.webengine.rest.WebEngine2;
import org.nuxeo.ecm.webengine.rest.annotations.Action;
import org.nuxeo.ecm.webengine.rest.annotations.Type;
import org.nuxeo.runtime.annotations.loader.AnnotationLoader;
import org.nuxeo.runtime.annotations.loader.BundleAnnotationsLoader;
import org.osgi.framework.Bundle;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class GlobalTypesLoader  implements AnnotationLoader {
    
    protected WebEngine2 engine;
    
    protected TypeConfigurationProvider mainProvider;
    protected Map<String, TypeConfigurationProvider> providers;

    public GlobalTypesLoader(WebEngine2 engine) {
        this.engine = engine;
        this.providers = new ConcurrentHashMap<String, TypeConfigurationProvider>();
        this.mainProvider = new TypeConfigurationProvider();
        BundleAnnotationsLoader.getInstance().addLoader(Type.class.getName(), this);
        BundleAnnotationsLoader.getInstance().addLoader(Action.class.getName(), this);
    }
    
    public static final String fcToUpperCase(String str) {
        if (Character.isLowerCase(str.charAt(0))) {
            str = new StringBuilder().append(Character.toUpperCase(str.charAt(0))).append(str.substring(1)).toString();
        }
        return str;
    }
 
    public TypeConfigurationProvider getMainProvider() {
        return mainProvider;
    }
    
    public TypeConfigurationProvider getProvider(String dirName) {
        TypeConfigurationProvider provider = providers.get(dirName);
        if (provider == null) {
            File root = new File(engine.getRootDirectory(), dirName);
            for (File typeDir : root.listFiles()) {
                if (!typeDir.isDirectory()) {
                    continue;
                }
                try {
                    provider = scanResourceDirectory(dirName, typeDir);
                    providers.put(dirName, provider);
                } catch (Throwable e) {
                    e.printStackTrace(); //TODO
                }
            }
        }
        return provider;
    }
    

    public TypeConfigurationProvider scanResourceDirectory(String parentName, File dir) throws ClassNotFoundException {
        TypeConfigurationProvider provider = new TypeConfigurationProvider();
        String pkgName = new StringBuilder().append(parentName).append('.').append(dir.getName()).toString();
        String typeName = fcToUpperCase(dir.getName());
        int extlen = ".groovy".length(); 
        for (File file : dir.listFiles()) {
            String name = file.getName();
            if (name.endsWith(".groovy") && name.startsWith(typeName)) {
                loadClassFile(provider, new StringBuilder()
                    .append(pkgName).append('.')
                    .append(name.substring(0, name.length()-extlen)).toString());
            }
        }
        return provider;
    }
    
    protected void loadClassFile(TypeConfigurationProvider provider, String className) throws ClassNotFoundException {
        Class<?> clazz = this.engine.getScripting().loadClass(className);
        Type type = clazz.getAnnotation(Type.class);
        if (type != null) {
            provider.registerType(TypeDescriptor.fromAnnotation(clazz, type));
        } else {            
            Action action = clazz.getAnnotation(Action.class);
            if (action != null) {
                provider.registerAction(ActionDescriptor.fromAnnotation(clazz, action));    
            }
        }
    }
    

    // support for loading annotations from descriptor files 
    public void loadAnnotation(Bundle bundle, String annotationType, String className, String[] args) throws Exception {
        // args are ignored for now
        Class<?> clazz = bundle.loadClass(className);
        if (annotationType.equals(Type.class.getName())) {
            Type type = clazz.getAnnotation(Type.class);                 
            mainProvider.registerType(TypeDescriptor.fromAnnotation(clazz, type));
        } else if (annotationType.equals(Action.class.getName())) {
            //TODO: avoid loading clazz here - use the data from annotation ?
            Action action = clazz.getAnnotation(Action.class);
            mainProvider.registerAction(ActionDescriptor.fromAnnotation(clazz, action));
        } else {
            throw new IllegalArgumentException(annotationType);
        }
    }
    
}
