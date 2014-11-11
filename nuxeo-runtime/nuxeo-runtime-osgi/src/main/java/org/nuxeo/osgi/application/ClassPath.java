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

package org.nuxeo.osgi.application;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.osgi.BundleFile;
import org.nuxeo.osgi.DirectoryBundleFile;
import org.nuxeo.osgi.JarBundleFile;
import org.osgi.framework.BundleException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ClassPath implements ClassPathScanner.Callback {

    protected final List<BundleFile> bundles;
    protected final List<BundleFile> jars;
    protected final List<BundleFile> nestedJars;
    protected final SharedClassLoader loader;
    protected final File nestedJARsDir;

    public ClassPath(SharedClassLoader loader, File nestedJARsDir) {
        bundles = new ArrayList<BundleFile>();
        jars = new ArrayList<BundleFile>();
        nestedJars = new ArrayList<BundleFile>();
        this.loader = loader;
        this.nestedJARsDir = nestedJARsDir;
        nestedJARsDir.mkdirs();
    }

    public List<BundleFile> getBundles() {
        return bundles;
    }

    public List<BundleFile> getJars() {
        return jars;
    }

    public List<BundleFile> getNestedJars() {
        return nestedJars;
    }

    public void scan(List<File> files, boolean scanForNestedJARs, String[] blacklist) {
        new ClassPathScanner(this, scanForNestedJARs, blacklist).scan(files);
    }

    @Override
    public File handleBundle(BundleFile bf) {
        bundles.add(bf);
        loader.addURL(bf.getURL());
        return nestedJARsDir;
    }

    @Override
    public File handleJar(BundleFile bf) {
        jars.add(bf);
        loader.addURL(bf.getURL());
        return nestedJARsDir;
    }

    @Override
    public void handleNestedJar(BundleFile bf) {
        nestedJars.add(bf);
        loader.addURL(bf.getURL());
    }

    public void store(File file) throws BundleException {
        Throwable error = null;
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(file));
            for (BundleFile bf : bundles) {
                writer.append(bf.getFile().getAbsolutePath());
                writer.newLine();
            }
            writer.append("#");
            writer.newLine();
            for (BundleFile bf : jars) {
                writer.append(bf.getFile().getAbsolutePath());
                writer.newLine();
            }
            writer.append("#");
            writer.newLine();
            for (BundleFile bf : nestedJars) {
                writer.append(bf.getFile().getAbsolutePath());
                writer.newLine();
            }
        } catch (Throwable e) {
            error = e;
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception e) {
                    error = e;
                }
            }
            if (error != null) {
                file.delete();
                throw new BundleException("failed to write cache file", error);
            }
        }
    }

    public void restore(File file) throws BundleException {
        Throwable error = null;
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            List<BundleFile> list = bundles;
            String line = null;
            while (true) {
                line = reader.readLine();
                if (line == null) {
                    break;
                }
                if (line.startsWith("#")) {
                    if (list == bundles) {
                        list = jars;
                    } else if (list == jars) {
                        list = nestedJars;
                    }
                    continue;
                }
                BundleFile bf = null;
                File f = new File(line.trim());
                if (f.isDirectory()) {
                    bf = new DirectoryBundleFile(f);
                } else {
                    bf = new JarBundleFile(f);
                }
                loader.addURL(bf.getURL());
                list.add(bf);
            }
        } catch (Throwable t) {
            error = t;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                    error = e;
                }
            }
            if (error != null) {
                throw new BundleException("Failed to load runtime application from cache info", error);
            }
        }
    }

}
