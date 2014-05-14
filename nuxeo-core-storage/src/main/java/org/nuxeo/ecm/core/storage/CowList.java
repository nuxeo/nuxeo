/*
 * Copyright (c) 2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * A copy-on-write {@link List}, with deep copy of values that are
 * {@link CowList} or {@link CowMap} themselves. Other values are assumed
 * immutable.
 * <p>
 * A {@link #deepCopy} will return fast, and a copy will be done as soon as a
 * write is done.
 */
public class CowList implements List<Serializable>, Serializable {

    private static final long serialVersionUID = 1L;

    protected List<Serializable> list;

    protected boolean shared;

    public CowList() {
        list = new ArrayList<Serializable>();
        shared = false;
    }

    public CowList(List<Serializable> other) {
        list = CopyHelper.deepCopy(other);
        shared = false;
    }

    protected CowList(CowList other) {
        list = other.list;
        shared = true;
    }

    public CowList deepCopy() {
        shared = true;
        return new CowList(this);
    }

    protected List<Serializable> unshare() {
        if (shared == true) {
            shared = false;
            list = new ArrayList<Serializable>(list);
            for (int i = 0; i < list.size(); i++) {
                Serializable elem = list.get(i);
                if (elem instanceof CowMap) {
                    list.set(i, ((CowMap) elem).deepCopy());
                } else if (elem instanceof CowList) {
                    list.set(i, ((CowList) elem).deepCopy());
                }
            }
        }
        return list;
    }

    protected List<Serializable> readonly() {
        return Collections.unmodifiableList(list);
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return list.contains(o);
    }

    @Override
    public Object[] toArray() {
        return list.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return list.toArray(a);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return list.containsAll(c);
    }

    @Override
    public Serializable get(int index) {
        return list.get(index);
    }

    @Override
    public int indexOf(Object o) {
        return list.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return list.lastIndexOf(o);
    }

    @Override
    public void clear() {
        unshare();
        list.clear();
    }

    @Override
    public boolean add(Serializable e) {
        unshare();
        return list.add(e);
    }

    @Override
    public boolean remove(Object o) {
        unshare();
        return list.remove(o);
    }

    @Override
    public boolean addAll(Collection<? extends Serializable> c) {
        unshare();
        return list.addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends Serializable> c) {
        unshare();
        return list.addAll(index, c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        unshare();
        return list.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        unshare();
        return list.retainAll(c);
    }

    @Override
    public Serializable set(int index, Serializable element) {
        unshare();
        return list.set(index, element);
    }

    @Override
    public void add(int index, Serializable element) {
        unshare();
        list.add(index, element);
    }

    @Override
    public Serializable remove(int index) {
        unshare();
        return list.remove(index);
    }

    @Override
    public Iterator<Serializable> iterator() {
        return readonly().iterator();
    }

    @Override
    public ListIterator<Serializable> listIterator() {
        return readonly().listIterator();
    }

    @Override
    public ListIterator<Serializable> listIterator(int index) {
        return readonly().listIterator(index);
    }

    @Override
    public List<Serializable> subList(int fromIndex, int toIndex) {
        return readonly().subList(fromIndex, toIndex);
    }

    @Override
    public String toString() {
        return list.toString();
    }
}
