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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.common.utils.Path;

/**
 * A map to store bindings between paths and random objects. Lookup by path prefixes are supported.
 * This map is not synchronized.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
// TODO: not used. Remove?
public class PathMap<T> {

    protected final Entry<T> root;

    public PathMap() {
        root = new Entry<T>();
    }

    public void put(String path, T value) {
        root.put(new Path(path).segments(), value);
    }

    public void put(Path path, T value) {
        root.put(path.segments(), value);
    }

    public void put(String[] segments, T value) {
        root.put(segments, value);
    }

    public T remove(String path) {
        return remove(new Path(path).segments());
    }

    public T remove(Path path) {
        return remove(path.segments());
    }

    public T remove(String[] segments) {
        Entry<T> entry = root.remove(segments);
        return entry != null ? entry.value : null;
    }

    public T get(String path) {
        return get(new Path(path).segments());
    }

    public T get(Path path) {
        return get(path.segments());
    }

    public T get(String[] segments) {
        Entry<T> entry = root.lookup(segments, true);
        return entry != null ? entry.value : null;
    }

    public T match(String[] segments) {
        Entry<T> entry = root.lookup(segments, false);
        return entry != null ? entry.value : null;
    }

    public T match(Path path) {
        return match(path.segments());
    }

    public T match(String path) {
        return match(new Path(path).segments());
    }

    public static class Entry<T> {
        Map<String, Entry<T>> entries;
        T value;

        public Entry<T> lookup(String[] segments, boolean exactMatch) {
            Entry<T> entry = this;
            Entry<T> match = null;
            for (String segment : segments) {
                if (entry.value != null) {
                    match = entry;
                }
                if (entry.entries == null) {
                    return exactMatch ? null : match;
                }
                entry = entry.entries.get(segment);
                if (entry == null) {
                    return exactMatch ? null : match;
                }
            }
            if (entry.value != null) {
                match = entry;
            }
            return match;
        }

        public void put(String[] segments, T value) {
            Entry<T> entry = this;
            for (String segment : segments) {
                Entry<T> newEntry;
                if (entry.entries == null) {
                    entry.entries = new HashMap<String, Entry<T>>();
                    newEntry = new Entry<T>();
                    entry.entries.put(segment, newEntry);
                } else {
                    newEntry = entry.entries.get(segment);
                    if (newEntry == null) {
                        newEntry = new Entry<T>();
                        entry.entries.put(segment, newEntry);
                    }
                }
                entry = newEntry;
            }
            entry.value = value;
        }

        public Entry<T> remove(String[] segments) {
            if (segments == null || segments.length == 0) {
                return null;
            }
            Entry<T> entry = this;
            int len = segments.length - 1;
            for (int i=0; i<len; i++) {
                if (entry.entries == null) {
                    return null;
                }
                entry = entry.entries.get(segments[i]);
                if (entry == null) {
                    return null;
                }
            }
            if (entry.entries == null) {
                return null;
            }
            return entry.entries.remove(segments[len]);
        }

    }

    public Collection<T> getValues() {
        List<T> list = new ArrayList<T>();
        collectValues(root, list);
        return list;
    }

    protected void collectValues(Entry<T> entry, Collection<T> list) {
        if (entry.value != null) {
            list.add(entry.value);
        }
        if (entry.entries != null) {
            for (Entry<T> e : entry.entries.values()) {
                collectValues(e, list);
            }
        }
    }

    public static void main(String[] args) {
        PathMap<Path> pm = new PathMap<Path>();

        List<Path> paths = new ArrayList<Path>();
        paths.add(new Path("/a/b/c"));
        paths.add(new Path("/a/b"));
        paths.add(new Path("/a"));
        paths.add(new Path("/a/b/c/d"));
        paths.add(new Path("/a/c"));
        paths.add(new Path("/a/c/d"));
        paths.add(new Path("/a/c/e"));

        // pm put
        double s = System.currentTimeMillis();
        for (Path p : paths) {
            pm.put(p.toString(), p);
        }
        System.out.println("pm.put: "+((System.currentTimeMillis()-s)/1000));

        // pm get
        s = System.currentTimeMillis();
        for (int i=0; i<1000; i++) {
            for (Path p : paths) {
                Path path = pm.get(p);
                if (path != p) {
                    throw new IllegalArgumentException(path.toString());
                }
            }
        }
        System.out.println("pm.get : "+((System.currentTimeMillis()-s)/1000));
    }

}
