/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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

package org.nuxeo.osgi.nio;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.osgi.OSGiAdapter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class BundleWalker {

    private static final Log log = LogFactory.getLog(BundleWalker.class);

    public interface Callback {
        void visitBundle(Bundle bundle);
    }

    public static final String[] DEFAULT_PATTERNS = new String[] {
            "*.jar", "*.war", "*.rar", "*.sar",
            "*_jar", "*_war", "*_rar" };

    protected final OSGiAdapter osgi;

    protected final String[] patterns;

    protected final Callback callback;

    public BundleWalker(OSGiAdapter osgi, Callback cb) {
        this(osgi, cb, DEFAULT_PATTERNS);
    }

    public BundleWalker(OSGiAdapter osgi, Callback cb, String[] patterns) {
        this.osgi = osgi;
        this.patterns = patterns;
        callback = cb;
    }

    public void visit(File root) throws IOException {
        Path rootPath = root.toPath();
        CompoundIOExceptionBuilder errorsBuilder = new CompoundIOExceptionBuilder();
        FilterBuilder<Path> filterBuilder = new FilterBuilder<Path>(rootPath.getFileSystem());
        for (Path memberPath : new RecursiveDirectoryStream<Path>(rootPath,
                filterBuilder.newOrFilter(patterns))) {
            try {
                visitBundleFile(memberPath.toFile());
            } catch (IOException error) {
                errorsBuilder.add(error);
            }
        }
        errorsBuilder.throwOnError();
    }

    public void visit(Collection<File> roots) throws IOException {
        CompoundIOExceptionBuilder errorsBuilder = new CompoundIOExceptionBuilder();
        for (File root : roots) {
            try {
                visit(root);
            } catch (IOException error) {
                errorsBuilder.add(error);
            }
        }
        errorsBuilder.throwOnError();
    }

    public void visit(File... roots) throws IOException {
        CompoundIOExceptionBuilder errorsBuilder = new CompoundIOExceptionBuilder();
        for (File root : roots) {
            try {
                visit(root);
            } catch (IOException error) {
                errorsBuilder.add(error);
            }
        }
        errorsBuilder.throwOnError();
    }

    protected boolean visitBundleFile(File file) throws IOException {
        Bundle bundle;
        try {
            bundle = osgi.install(file.toURI());
        } catch (BundleException e) {
            throw new IOException("Cannot install bundle file " + file, e);
        }
        callback.visitBundle(bundle);
        return true;
    }

}
