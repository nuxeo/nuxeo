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

package org.nuxeo.runtime.tomcat;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.NoSuchElementException;

import org.apache.catalina.loader.WebappClassLoader;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class NuxeoWebappClassLoader extends WebappClassLoader  {

    protected ClassLoader osgiLoader;

    public NuxeoWebappClassLoader() {
    }

    public NuxeoWebappClassLoader(ClassLoader parent) {
        super(parent);
    }

    @Override
    public void addURL(URL url) {
        super.addURL(url);
    }

    @Override
    public void setParentClassLoader(ClassLoader pcl) {
        super.setParentClassLoader(pcl);
    }

    public ClassLoader getParentClassLoader() {
        return parent;
    }

    @Override
    // synchronized to avoid some race conditions
    // see https://issues.apache.org/bugzilla/show_bug.cgi?id=44041
    public synchronized Class<?> loadClass(String name, boolean resolve)
            throws ClassNotFoundException {
        return super.loadClass(name, resolve);
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            return super.findClass(name);
        } catch (ClassNotFoundException e) {
            try {
                return osgiLoader.loadClass(name);
            } catch (Throwable oe) {
                throw e;
            }
        }
    }

    @Override
    public URL findResource(String name) {
        URL url = super.findResource(name);
        if (url != null) {
            return url;
        }
        return osgiLoader.getResource(name);
    }

    protected static class CompoundResourcesEnumerationBuilder {

        protected final ArrayList<Enumeration<URL>> collected = new ArrayList<Enumeration<URL>>();

        public CompoundResourcesEnumerationBuilder() {
        }

        public CompoundResourcesEnumerationBuilder add(Enumeration<URL> e) {
            collected.add(e);
            return this;
        }

        @SuppressWarnings("unchecked")
        public Enumeration<URL> build() {
            return new CompoundResourcesEnumeration(collected.toArray(new Enumeration[collected.size()]));
        }
    }

    protected static class CompoundResourcesEnumeration implements Enumeration<URL> {

        protected final Enumeration<URL>[] enums;

        protected int index = 0;

        public CompoundResourcesEnumeration(Enumeration<URL>[] enums) {
            this.enums = enums;
        }

        private boolean next() {
            while (index < enums.length) {
                if (enums[index].hasMoreElements()) {
                    return true;
                }
                index++;
            }
            return false;
        }

        @Override
        public boolean hasMoreElements() {
            return next();
        }

        @Override
        public URL nextElement() {
            if (!next()) {
                throw new NoSuchElementException();
            }
            return enums[index].nextElement();
        }

    }

    @SuppressWarnings("unchecked")
    @Override
    public Enumeration<URL> findResources(String name) throws IOException {
        CompoundResourcesEnumerationBuilder builder = new CompoundResourcesEnumerationBuilder();
        builder.add(super.findResources(name));
        builder.add(osgiLoader.getResources(name));
        return  builder.build();
    }

    public void setOSGiLoader(ClassLoader loader) throws Exception {
        osgiLoader = loader;
    }

}
