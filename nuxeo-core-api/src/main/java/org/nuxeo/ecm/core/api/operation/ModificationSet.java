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

package org.nuxeo.ecm.core.api.operation;

import java.io.Serializable;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.nuxeo.ecm.core.api.DocumentRef;

/**
 * A set of modification descriptors.
 * <p>
 * This class is not thread safe.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ModificationSet implements Serializable, Iterable<Modification> {

    private static final long serialVersionUID = 6074152814184873084L;

    private static final int SIZE = 4;

    private Modification[] ar = new Modification[SIZE];

    private int length = 0;

    public int size() {
        return length;
    }

    public boolean add(DocumentRef ref, int modifType) {
        if (length == ar.length) {
            Modification[] tmp = new Modification[ar.length + SIZE];
            System.arraycopy(ar, 0, tmp, 0, ar.length);
            ar = tmp;
        }
        ar[length++] = new Modification(ref, modifType);
        return false;
    }

    public boolean add(Modification modif) {
        return add(modif.ref, modif.type);
    }

    public boolean contains(DocumentRef ref) {
        for (int i = 0; i < length; i++) {
            if (ar[i].ref.equals(ref)) {
                return true;
            }
        }
        return false;
    }

    public final Modification get(int index) {
        return ar[index];
    }

    public int indexOf(DocumentRef ref) {
        for (int i = 0; i < length; i++) {
            if (ar[i].ref.equals(ref)) {
                return i;
            }
        }
        return -1;
    }

    public Modification remove(int index) {
        if (index >= length || index < 0) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        Modification mod = ar[index];
        if (index == length - 1) {
            ar[--length] = null;
        } else {
            System.arraycopy(ar, index + 1, ar, index, ar.length - index - 1);
            length--;
        }
        return mod;
    }

    public Modification[] toArray() {
        Modification[] tmp = new Modification[length];
        System.arraycopy(ar, 0, tmp, 0, ar.length);
        return tmp;
    }

    @Override
    public Iterator<Modification> iterator() {
        return new ModifIterator();
    }

    class ModifIterator implements Iterator<Modification> {
        int index = 0;

        @Override
        public boolean hasNext() {
            return index < length;
        }

        @Override
        public Modification next() {
            try {
                return ar[index++];
            } catch (Throwable t) {
                throw new NoSuchElementException(
                        "Iterator has no more elements");
            }
        }

        @Override
        public void remove() {
            ModificationSet.this.remove(index);
        }
    }

}
