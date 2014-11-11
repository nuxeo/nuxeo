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
package org.nuxeo.ecm.automation.core.impl;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.nuxeo.ecm.automation.OutputCollector;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class IterableInputHelper {

    // protected static ConcurrentMap<String,String> cache;

    private IterableInputHelper () {
    }

    public static Class<?> getIterableType(Class<?> cl) {
        // TODO first look into a cache
        // Class<?> cl = cache.get(cl.getName());
        return findIterableType(cl);
    }

    @SuppressWarnings("rawtypes")
    public static Type[] findCollectorTypes(Class<? extends OutputCollector> cl) {
        for (Type itf : cl.getGenericInterfaces()) {
            if (itf instanceof ParameterizedType) {
                ParameterizedType ptype = (ParameterizedType) itf;
                if (ptype.getRawType() == OutputCollector.class) {
                    return ptype.getActualTypeArguments();
                }
            }
        }
        throw new IllegalArgumentException(
                "Invalid output collector class: "
                        + cl
                        + ". The class must explicitely impelement the OutputCollector interface.");
    }

    public static Class<?> findIterableType(Class<?> cl) {
        if (!Iterable.class.isAssignableFrom(cl)) {
            return null;
        }
        // try generic super class
        Type superType = cl.getGenericSuperclass();
        if (superType instanceof ParameterizedType) {
            ParameterizedType ptype = (ParameterizedType) superType;
            return (Class<?>) ptype.getActualTypeArguments()[0];
        }
        // try generic interfaces
        for (Type itf : cl.getGenericInterfaces()) {
            if (itf instanceof ParameterizedType) {
                ParameterizedType ptype = (ParameterizedType) itf;
                if (ptype.getRawType() == Iterable.class
                        || ptype.getRawType() == Collection.class
                        || ptype.getRawType() == List.class) {
                    return (Class<?>) ptype.getActualTypeArguments()[0];
                }
            }
        }
        // if not descend into the super type and continue.
        if (superType != null) {
            Class<?> superClass = cl.getSuperclass();
            if (superClass != null) {
                return getIterableType(superClass);
            }
        }
        return null;
    }

    public static void main(String[] args) {
        DocumentModelListImpl o1 = new DocumentModelListImpl();
        DocumentModelCollector o2 = new DocumentModelCollector();
        MyIt o3 = new MyIt();
        MyList o4 = new MyList();
        MyCol o5 = new MyCol();
        System.out.println(getIterableType(o1.getClass()));
        System.out.println(getIterableType(o2.getClass()));
        System.out.println(getIterableType(o3.getClass()));
        System.out.println(getIterableType(o4.getClass()));
        System.out.println(getIterableType(o5.getClass()));
    }

    static class MyIt implements Iterable<String> {
        @Override
        public Iterator<String> iterator() {
            return null;
        }
    }

    static class MyList implements List<String> {

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public boolean contains(Object o) {
            return false;
        }

        @Override
        public Iterator<String> iterator() {
            return null;
        }

        @Override
        public Object[] toArray() {
            return null;
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return null;
        }

        @Override
        public boolean add(String e) {
            return false;
        }

        @Override
        public boolean remove(Object o) {
            return false;
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return false;
        }

        @Override
        public boolean addAll(Collection<? extends String> c) {
            return false;
        }

        @Override
        public boolean addAll(int index, Collection<? extends String> c) {
            return false;
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            return false;
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            return false;
        }

        @Override
        public void clear() {
        }

        @Override
        public String get(int index) {
            return null;
        }

        @Override
        public String set(int index, String element) {
            return null;
        }

        @Override
        public void add(int index, String element) {
        }

        @Override
        public String remove(int index) {
            return null;
        }

        @Override
        public int indexOf(Object o) {
            return 0;
        }

        @Override
        public int lastIndexOf(Object o) {
            return 0;
        }

        @Override
        public ListIterator<String> listIterator() {
            return null;
        }

        @Override
        public ListIterator<String> listIterator(int index) {
            return null;
        }

        @Override
        public List<String> subList(int fromIndex, int toIndex) {
            return null;
        }

    }

    static class MyCol implements Collection<String> {

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public boolean contains(Object o) {
            return false;
        }

        @Override
        public Iterator<String> iterator() {
            return null;
        }

        @Override
        public Object[] toArray() {
            return null;
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return null;
        }

        @Override
        public boolean add(String e) {
            return false;
        }

        @Override
        public boolean remove(Object o) {
            return false;
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return false;
        }

        @Override
        public boolean addAll(Collection<? extends String> c) {
            return false;
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            return false;
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            return false;
        }

        @Override
        public void clear() {
        }
    }

}
