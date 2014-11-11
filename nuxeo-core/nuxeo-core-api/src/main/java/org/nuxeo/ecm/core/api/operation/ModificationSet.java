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

package org.nuxeo.ecm.core.api.operation;

import java.io.Serializable;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;

/**
 * A set of modification descriptors.
 * <p/>
 * This class is not thread safe
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ModificationSet implements Serializable, Iterable<Modification> {

    private static final long serialVersionUID = 6074152814184873084L;

    private final static int SIZE = 4;

    private Modification[] ar = new Modification[SIZE];
    private int length = 0;

    public int size() {
        return length;
    }

    public boolean add(DocumentRef ref, int modifType) {
        for (int i=0; i<length; i++) {
            if (ar[i].ref.equals(ref)) {
                ar[i].type |= modifType;
                return true;
            }
        }
        if (length == ar.length) {
            Modification[] tmp = new Modification[ar.length+SIZE];
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
        for (int i=0; i<length; i++) {
            if (ar[i].ref.equals(ref)) {
                return true;
            }
        }
        return false;
    }

    public final Modification get(int index) {
        return ar[index];
    }

    public Modification getModifcation(DocumentRef ref) {
        for (int i=0; i<length; i++) {
            if (ar[i].ref.equals(ref)) {
                return ar[i];
            }
        }
        return null;
    }

    public int indexOf(DocumentRef ref) {
        for (int i=0; i<length; i++) {
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
        if (index == length -1) {
            ar[--length] = null;
        } else {
            System.arraycopy(ar, index+1, ar, index, ar.length-index-1);
            length--;
        }
        return mod;
    }

    public Modification removeModification(DocumentRef ref) {
        int index = indexOf(ref);
        return remove(index);
    }

    public Modification[] toArray() {
        Modification[] tmp = new Modification[length];
        System.arraycopy(ar, 0, tmp, 0, ar.length);
        return tmp;
    }

    public Iterator<Modification> iterator() {
        return new ModifIterator();
    }

    class ModifIterator implements Iterator<Modification> {
        int index = 0;
        public boolean hasNext() {
            return index < length;
        }
        public Modification next() {
            try {
                return ar[index++];
            } catch (Throwable t) {
                throw new NoSuchElementException("Iterator has no more elements");
            }
        }
        public void remove() {
            ModificationSet.this.remove(index);
        }
    }

    public static void main(String[] args) {
        ModificationSet set = new ModificationSet();
        set.add(new IdRef("a"), Modification.CREATE);
        set.add(new IdRef("b"), Modification.REMOVE);
        set.add(new IdRef("c"), Modification.CONTENT);
        set.add(new IdRef("e"), Modification.SECURITY);
        set.add(new IdRef("d"), Modification.STATE);
        for (Modification m : set) {
            System.out.println("> "+m);
        }
        System.out.println("===========================");
        set.add(new IdRef("c"), Modification.STATE | Modification.SECURITY);
        for (int i=0; i<set.size(); i++) {
            System.out.println("> "+set.get(i));
        }
        System.out.println("===========================");
        Modification mod = set.getModifcation(new IdRef("c"));
        System.out.println(mod+" = "+Modification.UPDATE_MODIFICATION);
        System.out.println("===========================");
        set.remove(2);
        for (Modification m : set) {
            System.out.println("> "+m);
        }
    }

}
