/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.runtime.launcher;

import java.io.File;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.nuxeo.common.utils.FileNamePattern;
import org.nuxeo.common.utils.JarUtils;
import org.nuxeo.runtime.launcher.FileWalker.Visitor;
import org.osgi.framework.Constants;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class BundleVisitor extends Visitor {

    public static final FileNamePattern[] DEFAULT_PATTERNS = new FileNamePattern[] {
        new FileNamePattern("*.jar"),
        new FileNamePattern("*.war"),
        new FileNamePattern("*.rar"),
        new FileNamePattern("*.sar"), // jboss sar
        new FileNamePattern("*_jar"),
        new FileNamePattern("*_war"),
        new FileNamePattern("*_rar")
    };

    private FileNamePattern[] patterns;
    private final Callback callback;

    public BundleVisitor(Callback cb) {
        this (cb, DEFAULT_PATTERNS);
    }

    public BundleVisitor(Callback cb, String[] patterns) {
        if (patterns != null) {
            this.patterns = new FileNamePattern[patterns.length];
            for (int i = 0; i < patterns.length; i++) {
                this.patterns[i] = new FileNamePattern(patterns[i]);
            }
        }
        callback = cb;
    }

    public BundleVisitor(Callback cb, FileNamePattern[] patterns) {
        this.patterns = patterns;
        callback = cb;
    }

    public void visit(File root) {
        new FileWalker().walk(root, this);
    }

    @Override
    public int visitDirectory(File file) {
        //System.out.println("###### Processing DIR: " + file);
        // first check if this is a possible bundle
        String fileName = file.getName();
        if (patterns != null) {
            if (!acceptFile(fileName, patterns)) {
                return FileWalker.CONTINUE;
            }
        }
        // check if this is an OSGi bundle
        try {
            Manifest mf = JarUtils.getDirectoryManifest(file);
            if (mf == null) {
                return FileWalker.CONTINUE;
            }
            String bundleName = mf.getMainAttributes().getValue(
                    Constants.BUNDLE_SYMBOLICNAME);
            if (bundleName != null) {
                // notify the callback about the new bundle
                callback.visitBundle(new DirectoryBundleFile(file, mf));
                // assume that a directory OSGi bundle cannot contain other bundles so skip it
                return FileWalker.BREAK;
            }
        } catch (Exception e) {
            //TODO: log?
            // ignore
        }
        return FileWalker.CONTINUE;
    }

    @Override
    public int visitFile(File file) {
        //System.out.println("###### Processing file: "+file);
        // first check if this is a possible bundle
        String fileName = file.getName();
        if (patterns != null) {
            if (!acceptFile(fileName, patterns)) {
                //System.out.println("###### Ignoring file: "+file);
                return FileWalker.CONTINUE;
            }
        }
        // check if this is an OSGi bundle
        try {
            JarFile jarFile = new JarFile(file);
            if (jarFile.getManifest() == null) {
                return FileWalker.CONTINUE;
            }
            BundleFile bundleFile = new JarBundleFile(jarFile);
            if (bundleFile.getSymbolicName() != null) {
                // notify the callback about the new bundle
                callback.visitBundle(bundleFile);
            } else {
                // notify the callback about the new jar
                callback.visitJar(bundleFile);
            }
        } catch (Exception e) {
            // ignore
        }
        return FileWalker.CONTINUE;
    }

    protected boolean acceptFile(String fileName, FileNamePattern[] patterns) {
        int i = 0;
        for (; i < patterns.length; i++) {
            if (patterns[i].match(fileName)) {
                break;
            }
        }
        return i < patterns.length;
    }

    public interface Callback {
        void visitBundle(BundleFile bundleFile) throws Exception;
        void visitJar(BundleFile bundleFile) throws Exception;
    }

}
