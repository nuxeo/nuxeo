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

package org.nuxeo.ecm.webengine.notifier;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.collections.ListenerList;
import org.nuxeo.runtime.api.Framework;
import static org.nuxeo.ecm.webengine.notifier.FileChangeListener.*;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class FileChangeNotifier implements FileChangeListener {

    private static final Log log = LogFactory.getLog(FileChangeNotifier.class);

    private final ListenerList listeners = new ListenerList();
    private final Timer timer = new Timer("FileChangeNotifier");
    private final Hashtable<String, FileEntry> roots = new Hashtable<String, FileEntry>();

    public void start() {
        String interval = Framework.getProperty("org.nuxeo.ecm.webengine.fileChangeNotifierInterval", "2000");
        start(2000, Integer.parseInt(interval));
    }

    public void start(int startAfter, int interval) {
        timer.scheduleAtFixedRate(new WatchTask(), startAfter, interval);
    }

    public void stop() {
        timer.cancel();
        timer.purge();
    }

    public void watch(File file) throws IOException {
        FileEntry entry = new FileEntry(file);
        roots.put(entry.file.getPath(), entry);
    }

    public void unwatch(File file) throws IOException {
        roots.remove(file.getCanonicalFile().getPath());
    }

    public void addListener(FileChangeListener listener) {
        listeners.add(listener);
    }

    public void removeListener(FileChangeListener listener) {
        listeners.remove(listener);
    }

    public void fileChanged(FileEntry entry, int type, long tm) {
        for (Object listener : listeners.getListeners()) {
            try {
                ((FileChangeListener) listener).fileChanged(entry, type, tm);
            } catch (Throwable t) {
                log.error("Error while to notifying file change for: "+entry.file, t);
            }
        }
    }

    class WatchTask extends TimerTask {
        @Override
        public void run() {
            try {
                Collection<FileEntry> entries = ((Map<String, FileEntry>) roots.clone()).values();
                for (FileEntry entry : entries) {
                    entry.scanForChanges(FileChangeNotifier.this);
                }
            } catch (Throwable t) {
                log.error("Error while to notifying file change", t);
            }
        }
    }

    public class FileEntry implements Comparable<FileEntry> {
        public File file;
        public boolean isDirectory;
        public long lastModified;
        public HashMap<File, FileEntry> children;

        public FileEntry(File file) throws IOException {
            this.file = file.getCanonicalFile();
            lastModified = file.lastModified();
            isDirectory = file.isDirectory();
            if (isDirectory) {
                File[] files = file.listFiles();
                if (files != null) {
                    children = new HashMap<File, FileEntry>();
                    for (File f : files) {
                        children.put(f, new FileEntry(f));
                    }
                }
            }
        }

        public int compareTo(FileEntry o) {
            return file.compareTo(o.file);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj.getClass() == FileEntry.class) {
                return file.equals(((FileEntry) obj).file);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return file.hashCode();
        }

        @Override
        public String toString() {
            return file.toString();
        }

        public void scanForChanges(FileChangeListener listener) throws IOException {
            long tm = file.lastModified();
            if (tm > lastModified) { // handle REMOVE and CREATE
                // this file changed
                if (isDirectory != file.isDirectory()) {
                    if (isDirectory) {
                        //TODO this directory was removed and recreated as a file
                    } else {
                        //TODO this file was removed and recreated as a directory
                    }
                } else if (isDirectory) {
                    // find out which files changed in that directory
                    Set<File> checkedFiles = new HashSet<File>();
                    File[] files = file.listFiles();
                    for (File f : files) {
                        checkedFiles.add(f);
                        FileEntry entry = children.get(f);
                        if (entry == null) { // a new file
                            entry = new FileEntry(f);
                            children.put(f, entry);
                            fileChanged(entry, CREATED, tm);
                        } else {
                            entry.scanForChanges(listener);
                        }
                    }
                    // look for deleted files
                    Set<File> clone = ((Map<File, FileEntry>) children.clone()).keySet();
                    clone.removeAll(checkedFiles);
                    for (File f : clone) {
                        FileEntry entry = children.remove(f);
                        fileChanged(entry, DELETED, tm);
                    }
                } else {
                    fileChanged(this, MODIFIED, tm);
                }
                lastModified = tm;
            }
            if (isDirectory) { // descend into
                Map<File,FileEntry> clone = (Map<File,FileEntry>) children.clone();
                for (FileEntry entry : clone.values()) {
                    if (entry.isDirectory) {
                        entry.scanForChanges(listener);
                    }
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        FileChangeNotifier fcn = new FileChangeNotifier();
        fcn.watch(new File("/home/bstefanescu/tmp/test"));
        fcn.addListener(new FileChangeListener() {
            public void fileChanged(FileEntry entry, int type, long now)
                    throws Exception {
                System.out.println("FILE CHANGED: "+entry.file.getName()+" - "+type);
            }
        });
        fcn.start();
        System.out.println("Watching ...");
        Thread.sleep(1000*60*10);
        System.out.println("Done.");
    }

}
