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

package org.nuxeo.ecm.core.api.repository.cache;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;

/**
 *
 * A document list that uses document cached from the document cache.
 * This is not using the children cache. If you need to cache getChildren calls it is better to use
 * the {@link DocumentChildrenList} which is caching children lists
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class CachingDocumentList implements DocumentModelList {

    private static final long serialVersionUID = 6370206124496509919L;

    private final List<DocumentModel> list;
    private final DocumentModelCache cache;

    public CachingDocumentList(DocumentModelCache cache, List<DocumentModel> list) {
        this.list = list;
        this.cache = cache;
    }

    public boolean add(DocumentModel o) {
        return list.add(o);
    }

    public void add(int index, DocumentModel element) {
        list.add(index, element);
    }

    public boolean addAll(Collection<? extends DocumentModel> c) {
        return list.addAll(c);
    }

    // FIXME: this recurses infinitely.
    public boolean addAll(int index, Collection<? extends DocumentModel> c) {
        return addAll(index, c);
    }

    public void clear() {
        list.clear();
    }

    public boolean contains(Object o) {
        return list.contains(o);
    }

    public boolean containsAll(Collection<?> c) {
        return list.containsAll(c);
    }

    public DocumentModel get(int index) {
        return cache.cacheDocument(list.get(index));
    }

    public int indexOf(Object o) {
        return list.indexOf(o);
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }

    public Iterator<DocumentModel> iterator() {
        return list.iterator();
    }

    public int lastIndexOf(Object o) {
        return list.lastIndexOf(o);
    }

    public ListIterator<DocumentModel> listIterator() {
        return new CachingIterator(list.listIterator());
    }

    public ListIterator<DocumentModel> listIterator(int index) {
        return new CachingIterator(list.listIterator(index));
    }

    public DocumentModel remove(int index) {
        return list.remove(index);
    }

    public boolean remove(Object o) {
        return list.remove(o);
    }

    public boolean removeAll(Collection<?> c) {
        return list.removeAll(c);
    }

    public boolean retainAll(Collection<?> c) {
        return list.removeAll(c);
    }

    public DocumentModel set(int index, DocumentModel element) {
        return list.set(index, element);
    }

    public int size() {
        return list.size();
    }

    public List<DocumentModel> subList(int fromIndex, int toIndex) {
        return new CachingDocumentList(cache, list.subList(fromIndex, toIndex));
    }

    public Object[] toArray() {
        Object[] ar = list.toArray();
        for (int i=ar.length-1; i>=0; i--) {
            ar[i] = cache.cacheDocument((DocumentModel)ar[i]);
        }
        return ar;
    }

    public <T> T[] toArray(T[] a) {
        T[] ar = list.toArray(a);
        for (int i=ar.length-1; i>=0; i--) {
            ar[i] = (T) cache.cacheDocument((DocumentModel) ar[i]);
        }
        return ar;
    }

    static class CachingIterator implements ListIterator<DocumentModel> {

        final ListIterator<DocumentModel> it;

        CachingIterator(ListIterator<DocumentModel> it) {
            this.it = it;
        }

        public void add(DocumentModel o) {
            it.add(o);
        }

        public boolean hasNext() {
            return it.hasNext();
        }

        public boolean hasPrevious() {
            return it.hasPrevious();
        }

        public DocumentModel next() {
            return it.next();
        }

        public int nextIndex() {
            return it.nextIndex();
        }

        public DocumentModel previous() {
            return it.previous();
        }

        public int previousIndex() {
            return it.previousIndex();
        }

        public void remove() {
            it.remove();
        }

        public void set(DocumentModel o) {
            it.set(o);
        }
    }

}
