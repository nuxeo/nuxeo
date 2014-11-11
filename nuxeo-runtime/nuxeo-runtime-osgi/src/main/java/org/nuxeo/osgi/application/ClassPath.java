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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.osgi.OSGiAdapter;
import org.nuxeo.osgi.nio.BundleWalker;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ClassPath {

    protected final Log log = LogFactory.getLog(ClassPath.class);

    protected final OSGiAdapter osgi;

    protected Bundle[] bundles;

    public ClassPath(OSGiAdapter osgi) {
        this.osgi = osgi;
    }

    public Bundle[] getBundles() {
        return bundles;
    }

    protected static class BundleCollector implements BundleWalker.Callback {

        protected final List<Bundle> bundles = new LinkedList<Bundle>();

        @Override
        public void visitBundle(Bundle bundle) {
            bundles.add(bundle);
        }

    }

    public Bundle[] scan(List<File> files) {
        BundleCollector collector = new BundleCollector();
        new BundleWalker(osgi, collector);
        return bundles = collector.bundles.toArray(new Bundle[collector.bundles.size()]);
    }

    public Bundle[] scan(List<File> files, String[] blacklist) {
        BundleCollector collector = new BundleCollector();
        new BundleWalker(osgi, collector, blacklist);
        return bundles = collector.bundles.toArray(new Bundle[collector.bundles.size()]);
    }

    public void store(File storefile) throws IOException {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(storefile));
            for (Bundle bundle : bundles) {
                URI uri = URI.create(bundle.getLocation());
                writer.append(uri.toURL().getFile());
            }
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    public void restore(File file) throws IOException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            List<Bundle> list = new LinkedList<Bundle>();
            String line = null;
            while (true) {
                line = reader.readLine();
                if (line == null) {
                    break;
                }
                if (line.startsWith("#")) {
                    continue;
                }
                File f = new File(line.trim());
                try {
                    list.add(osgi.install(f.toURI()));
                } catch (BundleException e) {
                    log.error("Cannot reload bundle file " + f, e);
                }
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

}
