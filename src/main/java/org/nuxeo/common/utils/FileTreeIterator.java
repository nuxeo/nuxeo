/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.common.utils;

import java.io.File;
import java.io.FileFilter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class FileTreeIterator implements Iterator<File> {

    private final Queue<Iterator<File>> queue = new LinkedList<Iterator<File>>();
    private File file; // last iterated file

    private FileFilter filter;


    public FileTreeIterator(File root) {
        queue.add(new OneFileIterator(root));
    }

    public FileTreeIterator(File root, boolean excludeRoot) {
        if (excludeRoot) {
            queue.add(new LazyChildrenIterator(root));
        } else {
            queue.add(new OneFileIterator(root));
        }
    }

    /**
     * @param filter the filter to set.
     */
    public void setFilter(FileFilter filter) {
        this.filter = filter;
    }

    /**
     * @return the filter.
     */
    public FileFilter getFilter() {
        return filter;
    }

    public boolean hasNext() {
        if (queue.isEmpty()) {
            return false;
        }
        Iterator<File> it = queue.peek();
        if (it.hasNext()) {
            return true;
        } else {
            queue.poll();
            return hasNext();
        }
    }

    public File next() {
        if (!hasNext()) {
            throw new NoSuchElementException("No more files to iterate over");
        }
        file = queue.peek().next();
        if (file.isDirectory()) {
            queue.add(new LazyChildrenIterator(file));
        }
        return file;
    }

    public void remove() {
        if (file == null) {
            throw new IllegalStateException("there is no current file to delete");
        }
        if (file.isDirectory()) {
            FileUtils.deleteTree(file);
        } else {
            file.delete();
        }
    }

    // we don't fulfill iterator contract - we don't need a real iterator,
    // this is used only internally
    private static class OneFileIterator implements Iterator<File> {

        private File file;

        private OneFileIterator(File file) {
            this.file = file;
        }
        public boolean hasNext() {
            return file != null;
        }
        public File next() {
            File next = file;
            file = null;
            return next;
        }
        public void remove() {
            throw new UnsupportedOperationException("remove not supported");
        }
    }

    // we don't fulfill iterator contract - we don't need a real iterator,
    // this is used only internally
    private class LazyChildrenIterator implements Iterator<File> {

        private final File dir;
        private File[] children;
        private int pos = -1; // last pos

        private LazyChildrenIterator(File dir) {
            this.dir = dir;
        }

        public boolean hasNext() {
            if (children == null) {
                children = filter == null ? dir.listFiles() : dir.listFiles(filter);
                if (children == null) {
                    return false; // not a dir
                }
                return children.length > 0;
            } else {
                return pos < children.length - 1;
            }
        }

        public File next() {
            return children[++pos];
        }

        public void remove() {
            throw new UnsupportedOperationException("remove not supported");
        }
    }


    public static void main(String[] args) {
        FileTreeIterator it =  new FileTreeIterator(new File("/root/kits"), false);
        while (it.hasNext()) {
            System.out.println(">> " + it.next());
        }
    }

}
