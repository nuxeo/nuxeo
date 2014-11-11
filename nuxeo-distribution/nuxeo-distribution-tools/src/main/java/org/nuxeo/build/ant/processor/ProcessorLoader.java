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
 */
package org.nuxeo.build.ant.processor;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ProcessorLoader extends URLClassLoader {

    
    public ProcessorLoader(ClassLoader parent) {
        super (new URL[0], parent);
    }

    public ProcessorLoader(URL[] urls, ClassLoader parent) {
        super (urls, parent);
    }

    @Override
    public void addURL(URL url) {
        super.addURL(url);
    }
    
    public void run(File wdir) throws Exception {
        Class<?> klass = loadClass("org.nuxeo.runtime.deployment.preprocessor.DeploymentPreprocessor");
        Method main = klass.getMethod("main", String[].class);
        main.invoke(null, new Object[] {new String[] {wdir.getAbsolutePath()}});
    }
    
 
    public static ProcessorLoader newInstance(ClassLoader parent, File wdir) throws IOException {
        return newInstance(parent, wdir, "bundles", "lib");
    }
    
    public static ProcessorLoader newInstance(ClassLoader parent, File wdir, String bundles, String lib) throws IOException {
        File bdir = new File(wdir, bundles);
        File ldir = new File(wdir, lib);
        ArrayList<URL> urls = new ArrayList<URL>();
        File[] files = bdir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.getPath().endsWith(".jar")) {
                    urls.add(f.toURI().toURL());
                }
            }
        }
        files = ldir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.getPath().endsWith(".jar")) {
                    urls.add(f.toURI().toURL());
                }
            }
        }
        URL[] u = urls.toArray(new URL[urls.size()]);
        return new ProcessorLoader(u, parent);
    }

    
}
