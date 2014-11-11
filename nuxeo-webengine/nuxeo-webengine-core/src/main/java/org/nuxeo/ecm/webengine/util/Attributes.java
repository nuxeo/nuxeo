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

import java.util.Arrays;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
// TODO: Not used. Remove?
public class Attributes implements Cloneable {

    public static final Attributes EMPTY_ATTRS = new Attributes(0);

    protected static final int GROW_SIZE = 8;

    protected String[] ar;
    protected int size;

    public Attributes(String[] ar) {
        this.ar = ar;
    }

    public Attributes(int capacity) {
        ar = new String[capacity<<1];
    }

    public Attributes() {
        this(4);
    }

    public int size() {
        return size>>1;
    }

    public int indexOf(String name) {
        for (int i = 0; i < size; i += 2) {
            if (name.equals(ar[i])) {
                return i >> 1;
            }
        }
        return -1;
    }

    public String getKey(int off) {
        return ar[off<<1];
    }

    public String getValue(int off) {
        return ar[(off<<1)+1];
    }

    public String getValue(String name) {
        for (int i = 0; i < size; i += 2) {
            if (name.equals(ar[i])) {
                return ar[i + 1];
            }
        }
        return null;
    }

    public void add(String name, String value) {
        if (size == ar.length) { // resize
            String[] newVars = new String[size+GROW_SIZE];
            System.arraycopy(ar, 0, newVars, 0, size);
            ar = newVars;
        }
        ar[size++] = name;
        ar[size++] = value;
    }

    public void set(String name, String value) {
        for (int i = 0; i < size; i += 2) {
            if (name.equals(ar[i])) {
                ar[i + 1] = value;
            }
        }
    }

    public void set(int off, String value) {
        ar[(off<<1)+1] = value;
    }

    @Override
    public String toString() {
        return Arrays.toString(ar);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj  == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj instanceof Attributes) {
            Attributes attrs = (Attributes) obj;
            if (attrs.size != size) {
                return false;
            }
            for (int i=0; i<size; i++) {
                if (!attrs.ar[i].equals(ar[i])) {
                    return false;
                }
            }
            return true;
        }
        return super.equals(obj);
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

}
