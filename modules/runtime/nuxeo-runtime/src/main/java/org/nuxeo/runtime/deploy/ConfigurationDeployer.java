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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.runtime.deploy;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.collections.ListenerList;
import org.nuxeo.runtime.model.RuntimeContext;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ConfigurationDeployer implements FileChangeListener {

    private static final Log log = LogFactory.getLog(ConfigurationDeployer.class);

    protected final Map<String, Entry> urls;

    protected final FileChangeNotifier notifier;

    protected final ListenerList listeners = new ListenerList();

    public ConfigurationDeployer() {
        this(null);
    }

    public ConfigurationDeployer(FileChangeNotifier notifier) {
        this.notifier = notifier;
        urls = new HashMap<>();
        if (notifier != null) {
            notifier.addListener(this);
        }
    }

    public void deploy(RuntimeContext ctx, URL url, boolean trackChanges) throws IOException {
        File watchFile = null;
        if (trackChanges) {
            try {
                String str = url.toExternalForm();
                if (str.startsWith("file:")) {
                    watchFile = new File(url.toURI());
                } else if (str.startsWith("jar:file:")) {
                    int p = str.lastIndexOf('!');
                    if (p > -1) {
                        str = str.substring(4, p);
                    } else {
                        str = str.substring(4);
                    }
                    watchFile = new File(new URI(str));
                }
            } catch (URISyntaxException e) {
                log.error(e);
                watchFile = null;
            }
        }
        _deploy(ctx, url, watchFile, trackChanges);
    }

    public void undeploy(URL url) throws IOException {
        Entry entry = urls.remove(url.toExternalForm());
        if (entry != null) {
            _undeploy(entry);
        }
    }

    public synchronized void _undeploy(Entry entry) throws IOException {
        try {
            entry.ctx.undeploy(entry.url);
        } finally {
            if (notifier != null && entry.watchFile != null) {
                notifier.unwatch(entry.url.toExternalForm(), entry.watchFile);
            }
        }
    }

    public void deploy(RuntimeContext ctx, File file, boolean trackChanges) throws IOException {
        _deploy(ctx, file.getCanonicalFile().toURI().toURL(), file, trackChanges);
    }

    public void undeploy(File file) throws IOException {
        undeploy(file.getCanonicalFile().toURI().toURL());
    }

    protected synchronized void _deploy(RuntimeContext ctx, URL url, File watchFile, boolean trackChanges)
            throws IOException {
        String id = url.toExternalForm();
        Entry entry = new Entry(ctx, url, watchFile);
        ctx.deploy(url);
        urls.put(id, entry);
        if (trackChanges && notifier != null && watchFile != null) {
            notifier.watch(id, watchFile);
        }
    }

    @Override
    public void fileChanged(FileChangeNotifier.FileEntry entry, long now) {
        Entry e = urls.get(entry.id);
        if (e != null) {
            try {
                e.ctx.undeploy(e.url);
                e.ctx.deploy(e.url);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            } finally {
                fireConfigurationChanged(e);
            }
        }
    }

    public void addConfigurationChangedListener(ConfigurationChangedListener listener) {
        listeners.add(listener);
    }

    public void removeConfigurationChangedListener(ConfigurationChangedListener listener) {
        listeners.remove(listener);
    }

    public void fireConfigurationChanged(Entry entry) {
        for (Object obj : listeners.getListenersCopy()) {
            ((ConfigurationChangedListener) obj).configurationChanged(entry);
        }
    }

    public static class Entry {
        final RuntimeContext ctx;

        final URL url;

        File watchFile;

        Entry(RuntimeContext ctx, URL config, File watchFile) throws IOException {
            url = config;
            this.ctx = ctx;
            if (watchFile != null) {
                this.watchFile = watchFile.getCanonicalFile();
            }
        }

        public RuntimeContext getContext() {
            return ctx;
        }

        public File getWatchFile() {
            return watchFile;
        }

        public URL getUrl() {
            return url;
        }

        @Override
        public String toString() {
            return url.toExternalForm();
        }
    }

}
