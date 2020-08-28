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
 */
package org.nuxeo.osgi.util;

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
 */
public class FileIterator implements Iterator<File>, Enumeration<File> {

    protected FileFilter filter;

    protected Queue<File> files;

    protected File file;

    protected boolean skipDirs = false;

    public static Iterator<URL> asUrlIterator(final Iterator<File> it) {
        return new Iterator<>() {
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
        return new Enumeration<>() {
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
        this.files = new LinkedList<>();
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
     *
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
