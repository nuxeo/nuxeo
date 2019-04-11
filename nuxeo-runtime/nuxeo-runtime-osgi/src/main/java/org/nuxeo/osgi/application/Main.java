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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.osgi.application;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class Main {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("No classpath specified");
            System.exit(10);
        }
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (!(cl instanceof URLClassLoader)) {
            System.err.println("Not a valid class loader: " + cl);
            System.exit(10);
        }
        ClassLoader parent = cl.getParent();
        if (parent == null) {
            parent = ClassLoader.getSystemClassLoader();
        }
        RootClassLoader rootLoader = new RootClassLoader(parent, SharedClassLoader.class);
        SharedClassLoaderImpl classLoader = new SharedClassLoaderImpl(((URLClassLoader) cl).getURLs(), rootLoader);
        Thread.currentThread().setContextClassLoader(classLoader.getLoader());
        URL systemBundle = classLoader.getURLs()[0];
        // build the class path now
        List<File> cp = buildClassPath(classLoader, args[0]);
        // make new arguments by removing the first one to pass further
        String[] tmp = new String[args.length - 1];
        System.arraycopy(args, 1, tmp, 0, tmp.length);
        args = tmp;
        // reload classes from this JAR using a SharedClassLoader
        Class<?> me = classLoader.loadClass(StandaloneApplication.class.getName());
        Method main = me.getMethod("main", URL.class, List.class, String[].class);
        main.invoke(null, systemBundle, cp, args);
    }

    public static List<File> buildClassPath(SharedClassLoader classLoader, String rawcp) throws IOException {
        List<File> result = new ArrayList<>();
        String[] cp = rawcp.split(":");
        for (String entry : cp) {
            File entryFile;
            if (entry.endsWith("/.")) {
                entryFile = new File(entry.substring(0, entry.length() - 2));
                File[] files = entryFile.listFiles();
                if (files != null) {
                    for (File file : files) {
                        result.add(file);
                        classLoader.addURL(file.toURI().toURL());
                    }
                }
            } else {
                entryFile = new File(entry);
                result.add(entryFile);
                classLoader.addURL(entryFile.toURI().toURL());
            }
        }
        return result;
    }

    private static class RootClassLoader extends ClassLoader {

        private final Class<?> loaderClass;

        private final String loaderName;

        RootClassLoader(ClassLoader parent, Class<?> loaderClass) {
            super(parent);
            this.loaderClass = loaderClass;
            loaderName = loaderClass.getName();
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            if (loaderName.equals(name)) {
                return loaderClass;
            }
            throw new ClassNotFoundException(name);
        }

    }

}
