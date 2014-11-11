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
 */
package org.nuxeo.osgi.nio;

import java.io.File;
import java.io.FileFilter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class FileIterator implements Iterator<File>, Enumeration<File> {

    protected FileFilter filter;
    protected Queue<File> files;
    protected File file;
    protected boolean skipDirs = false;

    public static Iterator<URL> asUrlIterator(final Iterator<File> it) {
        return new Iterator<URL>() {
            @Override
            public boolean hasNext() {
                return it.hasNext();
            }
            @Override
            public URL next() {
                try {
                    return it.next().toURI().toURL();
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            }
            @Override
            public void remove() {
                it.remove();
            }
        };
    }

    public static Enumeration<URL> asUrlEnumeration(final Iterator<File> it) {
        return new Enumeration<URL>() {
            @Override
            public boolean hasMoreElements() {
                return it.hasNext();
            }
            @Override
            public URL nextElement() {
                try {
                    return it.next().toURI().toURL();
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    public FileIterator(File file) {
        this(file, null);
    }

    public FileIterator(File file, FileFilter filter) {
        files = new LinkedList<File>();
        this.filter = filter;
        feed(file);
    }

    public void setSkipDirs(boolean skipDirs) {
        this.skipDirs = skipDirs;
    }

    public boolean getSkipDirs() {
        return skipDirs;
    }

    @Override
    public boolean hasNext() {
        if (file != null) {
            return true;
        }
        file = files.poll();
        if (file == null) {
            return false;
        }
        if (skipDirs) { // feed and get next file
            while (file != null && file.isDirectory()) {
                feed(file);
                file = files.poll();
            }
        } else {
            feed(file);
        }
        return file != null;
    }

    @Override
    public File next() {
        if (file == null) {
            hasNext();
            if (file == null) {
                throw new NoSuchElementException();
            }
        }
        File f = file;
        file = null;
        return f;
    }


    @Override
    public void remove() {
        throw new UnsupportedOperationException("remove ot supported");
    }


    /**
     * Feed the iterator with the given directory content if any
     * @param file
     */
    public void feed(File file) {
        File[] content = file.listFiles(filter);
        if (content != null) {
            for (File f : content) {
                files.add(f);
            }
        }
    }

    /** Enumeration API */

    @Override
    public boolean hasMoreElements() {
        return hasNext();
    }

    @Override
    public File nextElement() {
        return next();
    }

}
