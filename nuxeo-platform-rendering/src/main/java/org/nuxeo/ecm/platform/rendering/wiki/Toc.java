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

package org.nuxeo.ecm.platform.rendering.wiki;

/**
 * Table of contents model.
 * <p>
 * A simple linked list of toc entries.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class Toc {

    protected final Entry head;
    protected Entry tail;

    public Toc() {
        head = new Entry();
        tail = head;
        head.title = "Table of Contents";
        head.id = null;
    }

    /**
     * Adds a heading to the TOC list and returns the ID of that heading (to be used for anchors).
     *
     * @param title the heading title
     * @param level the heading level
     * @return the heading id
     */
    public String addHeading(String title, int level) {
        Entry entry = new Entry();
        entry.title = title;
        entry.level = level;
        if (level == tail.level) { // same level
            tail.next = entry;
            entry.parent = tail.parent;
            entry.index = tail.index+1;
        } else if (level > tail.level) {
            entry.parent = tail;
            tail.firstChild = entry;
            entry.index = 1;
        } else {
            Entry prev = tail.parent;
            // FIXME: null consistency check
            while (prev.level > level && prev != null) {
                prev = prev.parent;
            }
            // FIXME: null consistency check
            if (prev == null || prev.parent == null) {
                throw new IllegalStateException("Invalid headers. Header levels underflowed");
            }
            prev.next = entry;
            entry.parent = prev.parent;
            entry.index = prev.index+1;
        }
        if (entry.parent.id != null) {
            entry.id = entry.parent.id+"."+entry.index;
        } else {
            entry.id = ""+entry.index;
        }
        tail = entry;
        return entry.id;
    }

    public static class Entry {
        public Entry parent;
        public Entry next;
        public Entry firstChild;
        public String id;
        public String title;
        public int level;
        public int index;
    }

}
