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

package org.nuxeo.ecm.webengine.model.impl;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.WebService;
import org.nuxeo.runtime.annotations.loader.AnnotationLoader;
import org.nuxeo.runtime.annotations.loader.BundleAnnotationsLoader;
import org.osgi.framework.Bundle;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class GlobalTypesLoader  implements AnnotationLoader {
    
    protected WebEngine engine;
    
    protected TypeConfigurationProvider mainProvider;
    protected Map<String, TypeConfigurationProvider> providers;
    
    public GlobalTypesLoader(WebEngine engine) {
        this.engine = engine;
        this.providers = new ConcurrentHashMap<String, TypeConfigurationProvider>();
        this.mainProvider = new TypeConfigurationProvider();
        BundleAnnotationsLoader.getInstance().addLoader(WebObject.class.getName(), this);
        BundleAnnotationsLoader.getInstance().addLoader(WebService.class.getName(), this);
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
        int extlen = ".groovy".length(); 
        for (File file : dir.listFiles()) {
            String name = file.getName();
            if (name.endsWith(".groovy")) {
                loadClassFile(provider, new StringBuilder()
                    .append(pkgName).append('.')
                    .append(name.substring(0, name.length()-extlen)).toString());
            }
        }
        return provider;
    }
    
    protected void loadClassFile(TypeConfigurationProvider provider, String className) throws ClassNotFoundException {
        Class<?> clazz = this.engine.getScripting().loadClass(className);
        WebObject type = clazz.getAnnotation(WebObject.class);
        if (type != null) {
            provider.registerType(TypeDescriptor.fromAnnotation(clazz, type));
        } else {            
            WebService ws = clazz.getAnnotation(WebService.class);
            if (ws != null) {
                provider.registerType(ServiceDescriptor.fromAnnotation(clazz, ws));    
            }
        }
    }
    

    // support for loading annotations from descriptor files 
    public void loadAnnotation(Bundle bundle, String annotationType, String className, String[] args) throws Exception {
        // args are ignored for now
        Class<?> clazz = bundle.loadClass(className);
        if (annotationType.equals(WebObject.class.getName())) {
            WebObject type = clazz.getAnnotation(WebObject.class);                 
            mainProvider.registerType(TypeDescriptor.fromAnnotation(clazz, type));
        } else if (annotationType.equals(WebService.class.getName())) {
            //TODO: avoid loading clazz here - use the data from annotation ?
            WebService service = clazz.getAnnotation(WebService.class);
            mainProvider.registerType(ServiceDescriptor.fromAnnotation(clazz, service));
        } else {
            throw new IllegalArgumentException(annotationType);
        }
    }
    
}
