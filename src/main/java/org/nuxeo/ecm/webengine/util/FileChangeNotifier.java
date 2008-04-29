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

package org.nuxeo.ecm.webengine.util;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.collections.ListenerList;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class FileChangeNotifier {

    private static Log log = LogFactory.getLog(FileChangeNotifier.class);

    private ListenerList listeners = new ListenerList();
    private Timer timer = new Timer("FileChangeNotifier");
    private Vector<FileEntry> files = new Vector<FileEntry>();

    public void start() {
        start(10000, 2000);
    }

    public void start(int startAfter, int interval) {
        timer.scheduleAtFixedRate(new WatchTask(), startAfter, interval);
    }

    public void stop() {
        timer.cancel();
        timer.purge();
    }

    public void watch(File file) {
        files.add(new FileEntry(file));
    }

    public void unwatch(File file) {
        files.remove(new FileEntry(file));
    }

    public void addListener(FileChangeListener listener) {
        listeners.add(listener);
    }

    public void removeListener(FileChangeListener listener) {
        listeners.remove(listener);
    }

    protected void fireNotification(FileEntry entry) {
        for (Object listener : listeners.getListeners()) {
            ((FileChangeListener)listener).fileChanged(entry.file, entry.lastModified);
        }
    }

    class WatchTask extends TimerTask {
        @Override
        public void run() {
            for (FileEntry entry : files) {
                long lastModified = entry.file.lastModified();
                if ( entry.lastModified < lastModified) {
                    try {
                        fireNotification(entry);
                    } catch (Throwable t) {
                        log.error("Error while notifying file change", t);
                    }
                    entry.lastModified = lastModified;
                }
            }
        }
    }

    class FileEntry {
        File file;
        long lastModified;
        FileEntry(File file) {
            this.file = file;
            this.lastModified = file.lastModified();
        }
        @Override
        public boolean equals(Object obj) {
            if (obj == null) return false;
            if (obj.getClass() == FileEntry.class) {
                return file.equals(((FileEntry)obj).file);
            }
            return false;
        }
    }
}
