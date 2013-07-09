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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.osgi.application;

import java.io.File;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.osgi.OSGiAdapter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ClassPathScanner {

    protected final Log log = LogFactory.getLog(ClassPathScanner.class);

    protected final OSGiAdapter osgi;

    protected final Callback callback;

    /**
     * If set points to a set of path prefixes to be excluded form bundle
     * processing
     */
    protected final String[] blackList;

    public ClassPathScanner(OSGiAdapter osgi, Callback callback) {
        this.osgi = osgi;
        this.callback = callback;
        blackList = new String[0];
    }

    public ClassPathScanner(OSGiAdapter osgi, Callback callback,
            String[] blackList) {
        this.osgi = osgi;
        this.callback = callback;
        this.blackList = blackList;
    }

    /**
     * FIXME: this javadoc is not correct.
     * <p>
     * Scans the given class path and put found OSGi bundles in bundles, regular
     * JARs in jars and append any nested jar or bundle into the given class
     * loader.
     *
     * @param classPath
     */
    public void scan(List<File> classPath) {
        for (File file : classPath) {
            try {
                scan(file);
            } catch (BundleException e) {
                log.error("Cannot install bundle file " + file, e);
            }
        }
    }

    public void scan(File file) throws BundleException {
        String path = file.getAbsolutePath();
        if (!(path.endsWith(".jar") || path.endsWith(".rar")
                || path.endsWith(".sar") || path.endsWith("_jar")
                || path.endsWith("_rar") || path.endsWith("_sar"))) {
            return;
        }
        if (blackList != null) {
            for (String prefix : blackList) {
                if (path.startsWith(prefix)) {
                    return;
                }
            }
        }
        callback.handleBundle(osgi.install(file.toURI()));
    }

    public interface Callback {

        /**
         * A Bundle was found on the class path. Usually a callback should
         * handle this by adding the Bundle to a class loader and installing it
         * in an OSGi framework
         *
         * @param bf the JAR found
         */
        void handleBundle(Bundle bundle);

    }

}
