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
package org.nuxeo.runtime.test.runner;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DeployScanner {

    /**
     * the bundle symbolic names to deploy
     */
    protected Set<String> bundles;
    protected Set<String> localResources;
    

    public DeployScanner() {
        bundles = new LinkedHashSet<String>();
        localResources = new HashSet<String>();
    }

    
    public void load(Class<?> clazz) throws Exception {
        Deploy anno = clazz.getAnnotation(Deploy.class);
        if (anno != null) {
            loadAnnotation(anno);
        }
        LocalDeploy lanno = clazz.getAnnotation(LocalDeploy.class);
        if (lanno != null) {
            loadAnnotation(lanno);
        }        
        // try super classes
        Class<?> superClass = clazz.getSuperclass();
        while (superClass != null) {
            load(superClass);
            superClass = superClass.getSuperclass();
        }
        // try interfaces
        for (Class<?> itf : clazz.getInterfaces()) {
            load(itf);
        }
    }

    public void addBundle(String bundle) {
        bundles.add(bundle);
    }
    
    public void addLocalResource(String uri) {
        localResources.add(uri);
    }
    
    public Set<String> getBundles() {
        return bundles;
    }
    
    public Set<String> getLocalResources() {
        return localResources;
    }
    
    public void loadAnnotation(Deploy annotation)
    throws Exception {
        String[] bundles = annotation.value();
        if (bundles != null) {
            for (String bundle : bundles) {
                addBundle(bundle);
            }
        }
    }
    
    public void loadAnnotation(LocalDeploy annotation)
    throws Exception {
        String[] bundles = annotation.value();
        if (bundles != null) {
            for (String bundle : bundles) {
                addLocalResource(bundle);
            }
        }
    }

}
