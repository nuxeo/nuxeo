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
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.nuxeo.ecm.webengine.config.FileChangeListener;
import org.nuxeo.ecm.webengine.config.FileChangeNotifier;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DirectoryStack implements FileChangeListener {

    protected List<Entry> dirs;
    protected ConcurrentMap<String, File> cache;
    protected long lastNotifFlush = 0;

    public DirectoryStack() throws IOException {
        dirs = new ArrayList<Entry>();
        this.cache = new ConcurrentHashMap<String, File>();
    }

    public List<Entry> getEntries() {
        return dirs;
    }

    public boolean isEmpty() {
        return dirs.isEmpty();
    }

    public void addDirectory(File dir, int priority) throws IOException {
        dirs.add(new Entry(dir.getCanonicalFile(), priority));
    }

    public void trackFileChanges(FileChangeNotifier notifier) throws IOException {
        for (Entry entry : dirs) {
            notifier.watch(entry.file);
        }
    }

    /**
     * Gets the file given its name in this virtual directory.
     * <p>
     * The canonical file is returned if any file is found
     *
     * @param name the file name to lookup
     * @return the file in the canonical form
     *
     * @throws IOException
     */
    public File getFile(String name) throws IOException {
        File file = cache.get(name);
        if (file != null) {
            return file;
        }
        for (Entry entry : dirs) {
            file = new File(entry.file, name);
            if (file.exists()) {
                file = file.getCanonicalFile();
                cache.put(name, file);
                return file;
            }
        }
        return null;
    }

    public File[] listFiles() {
        ArrayList<File> result =new ArrayList<File>();
        for (Entry entry : dirs) {
            File[] files = entry.file.listFiles();
            result.addAll(Arrays.asList(files));
        }
        return result.toArray(new File[result.size()]);
    }

    public File[] listFiles(FileFilter filter) {
        ArrayList<File> result =new ArrayList<File>();
        for (Entry entry : dirs) {
            File[] files = entry.file.listFiles(filter);
            result.addAll(Arrays.asList(files));
        }
        return result.toArray(new File[result.size()]);
    }

    /**
     * Flush cache
     */
    public void flush() {
        cache.clear();
    }

    public void fileChanged(FileChangeNotifier.FileEntry entry, long now) {
        if (now == lastNotifFlush) return;
        for (Entry dir : dirs) {
            if (dir.file.getPath().equals(entry.file.getPath())) {
                lastNotifFlush = now;
                flush(); // TODO optimize this do not flush entire cache
            }
        }
    }

    public static class Entry implements Comparable<Entry> {
        public File file;
        public int priority;
        public Entry(File file, int priority) {
            this.file = file;
            this.priority = priority;
        }
        public int compareTo(Entry o) {
            return o.priority-priority; // put at the begining the bigest priorities (e.g. 10, 9, 8 ...)
        }
        @Override
        public String toString() {
            return file.toString();
        }
    }

    public static void main(String[] args) {

        try {
            DirectoryStack vd = new DirectoryStack();
            vd.addDirectory(new File("/home/bstefanescu/Desktop"), 1);
            vd.addDirectory(new File("/home/bstefanescu/src"), 1);

            for (File file : vd.listFiles()) {
                System.out.println("> "+file);
            }
            System.out.println("dummy: "+vd.getFile("dummy"));
            System.out.println("dev: "+vd.getFile("dev"));
            System.out.println("dummy: "+vd.getFile("dummy"));
            System.out.println("dev: "+vd.getFile("dev"));
            vd.flush();
            System.out.println("dummy: "+vd.getFile("dummy"));
            System.out.println("dev: "+vd.getFile("dev"));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
