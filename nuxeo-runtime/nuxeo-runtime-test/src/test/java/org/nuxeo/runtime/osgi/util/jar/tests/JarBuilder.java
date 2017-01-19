/*
 * (C) Copyright 2012-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     matic
 */
package org.nuxeo.runtime.osgi.util.jar.tests;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import org.nuxeo.runtime.api.Framework;

/**
 * @author matic
 */
public class JarBuilder {

    public static class First {
    }

    public static class Other {
    }

    protected final File bindir;

    protected File pkgdir = getResourceFile(null, JarBuilder.class.getPackage().getName().split("\\."));

    public JarBuilder() throws IOException {
        super();
        bindir = locateBinaries();
    }

    protected File locateBinaries() {
        String classpath = System.getProperty("java.class.path");
        StringTokenizer tokenizer = new StringTokenizer(classpath, File.pathSeparator);
        while (tokenizer.hasMoreElements()) {
            File bindir = new File(tokenizer.nextToken());
            if (!bindir.isDirectory()) {
                continue;
            }
            if (new File(bindir, pkgdir.getPath()).exists()) {
                return bindir;
            }
        }
        throw new IllegalStateException("cannot locate binaries");
    }

    protected static File getResourceFile(File dir, String... path) {
        File file = dir;
        for (String name : path) {
            file = new File(file, name);
        }
        return file;
    }

    protected static File getClassFile(File dir, Class<?> clazz) {
        File pkg = getResourceFile(dir, clazz.getPackage().getName().split("\\."));
        return new File(pkg, clazz.getSimpleName() + ".class");
    }

    ArrayList<File> builtFiles = new ArrayList<>();

    File rootFile = createRootFile();

    protected static File createRootFile() throws IOException {
        File tempdir = Framework.createTempFile("bundles", ".lib");
        tempdir.delete();
        tempdir.mkdir();
        return tempdir;
    }

    public File getRootFile() {
        return rootFile;
    }

    public URL buildFirst() throws FileNotFoundException, IOException {
        File file = File.createTempFile("test", ".jar", rootFile);
        builtFiles.add(file);
        JarOutputStream output = new JarOutputStream(new FileOutputStream(file));
        try {
            writeEntry(output, new File(pkgdir, "JarBuilder$First.class"));
            writeEntry(output, new File("first.marker"));
        } finally {
            output.close();
        }
        return file.toURI().toURL();
    }

    public URL buildOther() throws FileNotFoundException, IOException {
        File file = File.createTempFile("test", ".jar", rootFile);
        JarOutputStream output = new JarOutputStream(new FileOutputStream(file));
        try {
            writeEntry(output, new File(pkgdir, "JarBuilder$Other.class"));
            writeEntry(output, new File("other.marker"));
        } finally {
            output.close();
        }
        return file.toURI().toURL();
    }

    protected void writeEntry(JarOutputStream output, File file) throws IOException {
        output.putNextEntry(new ZipEntry(file.getPath().replace('\\', '/')));
        InputStream input = new FileInputStream(new File(bindir, file.getPath()));
        try {
            while (input.available() > 0) {
                output.write(input.read());
            }
        } finally {
            input.close();
        }
    }

    public void deleteBuiltFiles() {
        Iterator<File> iterator = builtFiles.iterator();
        while (iterator.hasNext()) {
            File file = iterator.next();
            file.delete();
            iterator.remove();
        }
        rootFile.delete();
    }
}
