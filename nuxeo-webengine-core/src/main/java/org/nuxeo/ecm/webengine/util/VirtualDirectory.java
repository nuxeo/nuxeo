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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class VirtualDirectory implements FileChangeListener {

    protected File[] dirs;
    protected ConcurrentMap<String, File> cache;
    protected long lastNotifFlush = 0;

    public VirtualDirectory(File ... dirs) throws IOException {
        assert dirs != null;
        this.dirs = new File[dirs.length];
        for (int i=0; i<dirs.length; i++) {
            this.dirs[i] = dirs[i].getCanonicalFile();
        }
        this.cache = new ConcurrentHashMap<String, File>();
    }

    public File[] getDirectories() {
        return dirs;
    }

    public void trackFileChanges(FileChangeNotifier notifier) throws IOException {
        for (int i=0; i<dirs.length; i++) {
            notifier.watch(dirs[i]);
        }
    }

    /**
     * Get the file given its name in this virtual directory.
     * The canonical file is returned if any file is found
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
        for (int i=0; i<dirs.length; i++) {
            file = new File(dirs[i], name);
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
        for (int i=0; i<dirs.length; i++) {
            File[] files = dirs[i].listFiles();
            for (int k=0; k<files.length; k++) {
                result.add(files[k]);
            }
        }
        return result.toArray(new File[result.size()]);
    }

    public File[] listFiles(FileFilter filter) {
        ArrayList<File> result =new ArrayList<File>();
        for (int i=0; i<dirs.length; i++) {
            File[] files = dirs[i].listFiles(filter);
            for (int k=0; k<files.length; k++) {
                result.add(files[k]);
            }
        }
        return result.toArray(new File[result.size()]);
    }

    /**
     * Flush cache
     */
    public void flush() {
        cache.clear();
    }


    public void fileChanged(File file, long since, long now) {
        if (now == lastNotifFlush) return;
        for (int i=dirs.length-1; i>=0; i--) {
            if (dirs[i].getPath().equals(file.getPath())) {
                lastNotifFlush = now;
                flush(); // TODO optimize this do not flush entire cache
            }
        }
    }

    public static void main(String[] args) {

        try {
            VirtualDirectory vd = new VirtualDirectory(new File("/home/bstefanescu/Desktop"), new File("/home/bstefanescu/src"));
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
