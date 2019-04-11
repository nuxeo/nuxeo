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
import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Map;

import org.nuxeo.common.collections.ListenerList;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class FileChangeNotifier {

    private final ListenerList listeners = new ListenerList();

    private final Timer timer = new Timer("FileChangeNotifier");

    private final Map<String, FileEntry> files = new Hashtable<>();

    public void start(int startAfter, int interval) {
        timer.scheduleAtFixedRate(new WatchTask(), startAfter, interval);
    }

    public void start() {
        start(10000, 2000);
    }

    public void stop() {
        timer.cancel();
        timer.purge();
    }

    public String watch(File file) throws IOException {
        FileEntry entry = new FileEntry(file);
        files.put(entry.id, entry);
        return entry.id;
    }

    public String watch(String id, File file) throws IOException {
        files.put(id, new FileEntry(id, file));
        return id;
    }

    public void unwatch(File file) throws IOException {
        files.remove(new FileEntry(file).id);
    }

    public void unwatch(String id, File file) throws IOException {
        files.remove(new FileEntry(id, file).id);
    }

    public void addListener(FileChangeListener listener) {
        listeners.add(listener);
    }

    public void removeListener(FileChangeListener listener) {
        listeners.remove(listener);
    }

    protected void fireNotification(FileEntry entry) {
        long tm = System.currentTimeMillis();
        for (Object listener : listeners.getListeners()) {
            ((FileChangeListener) listener).fileChanged(entry, tm);
        }
    }

    class WatchTask extends TimerTask {
        @Override
        public void run() {
            // make a copy to avoid concurrent modifs if a listener is
            // unwatching a file
            FileEntry[] entries = files.values().toArray(new FileEntry[files.size()]);
            for (FileEntry entry : entries) {
                long lastModified = entry.file.lastModified();
                if (entry.lastModified < lastModified) {
                    fireNotification(entry);
                    entry.lastModified = lastModified;
                }
            }
        }
    }

    public class FileEntry {
        public final String id;

        public final File file;

        public long lastModified;

        FileEntry(String id, File file) throws IOException {
            this.file = file.getCanonicalFile();
            lastModified = file.lastModified();
            this.id = id == null ? file.getAbsolutePath() : id;
        }

        FileEntry(File file) throws IOException {
            this(null, file);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj.getClass() == FileEntry.class) {
                return id.equals(((FileEntry) obj).id);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return id != null ? id.hashCode() : 0;
        }

        @Override
        public String toString() {
            return id;
        }
    }

}
