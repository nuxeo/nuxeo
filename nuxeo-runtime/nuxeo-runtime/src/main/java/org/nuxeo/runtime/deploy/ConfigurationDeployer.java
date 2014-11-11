/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.collections.ListenerList;
import org.nuxeo.runtime.model.RuntimeContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
        urls = new HashMap<String, Entry>();
        if (notifier != null) {
            notifier.addListener(this);
        }
    }

    public void deploy(RuntimeContext ctx, URL url, boolean trackChanges) throws Exception {
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
            } catch (Exception e) {
                log.error(e);
                watchFile = null;
            }
        }
        _deploy(ctx, url, watchFile, trackChanges);
    }

    public void undeploy(URL url) throws Exception {
        Entry entry = urls.remove(url.toExternalForm());
        if (entry != null) {
            _undeploy(entry);
        }
    }

    public synchronized void _undeploy(Entry entry) throws Exception {
        try {
            entry.ctx.undeploy(entry.url);
        } finally {
            if (notifier != null && entry.watchFile != null) {
                notifier.unwatch(entry.url.toExternalForm(), entry.watchFile);
            }
        }
    }

    public  void deploy(RuntimeContext ctx, File file, boolean trackChanges) throws Exception {
        _deploy(ctx, file.getCanonicalFile().toURI().toURL(), file, trackChanges);
    }

    public void undeploy(File file) throws Exception {
       undeploy(file.getCanonicalFile().toURI().toURL());
    }

    protected synchronized void _deploy(RuntimeContext ctx, URL url, File watchFile, boolean trackChanges) throws Exception {
        String id = url.toExternalForm();
        Entry entry = new Entry(ctx, url, watchFile);
        ctx.deploy(url);
        urls.put(id, entry);
        if (trackChanges && notifier != null && watchFile != null) {
            notifier.watch(id, watchFile);
        }
    }

    @Override
    public void fileChanged(FileChangeNotifier.FileEntry entry, long now) throws Exception {
        Entry e = urls.get(entry.id);
        if (e != null) {
            try {
                e.ctx.undeploy(e.url);
                e.ctx.deploy(e.url);
            } finally {
                fireConfigurationChanged(e);
            }
        }
    }

    public void addConfigurationChangedListener(
            ConfigurationChangedListener listener) {
        listeners.add(listener);
    }

    public void removeConfigurationChangedListener(
            ConfigurationChangedListener listener) {
        listeners.remove(listener);
    }

    public void fireConfigurationChanged(Entry entry) throws Exception {
        for (Object obj : listeners.getListenersCopy()) {
            ((ConfigurationChangedListener)obj).configurationChanged(entry);
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
