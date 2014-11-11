/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
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

        /*
         * (non-Javadoc)
         *
         * @see java.util.List#size()
         */
        @Override
        public int size() {
            // TODO Auto-generated method stub
            return 0;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.List#isEmpty()
         */
        @Override
        public boolean isEmpty() {
            // TODO Auto-generated method stub
            return false;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.List#contains(java.lang.Object)
         */
        @Override
        public boolean contains(Object o) {
            // TODO Auto-generated method stub
            return false;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.List#iterator()
         */
        @Override
        public Iterator<String> iterator() {
            // TODO Auto-generated method stub
            return null;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.List#toArray()
         */
        @Override
        public Object[] toArray() {
            // TODO Auto-generated method stub
            return null;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.List#toArray(T[])
         */
        @Override
        public <T> T[] toArray(T[] a) {
            // TODO Auto-generated method stub
            return null;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.List#add(java.lang.Object)
         */
        @Override
        public boolean add(String e) {
            // TODO Auto-generated method stub
            return false;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.List#remove(java.lang.Object)
         */
        @Override
        public boolean remove(Object o) {
            // TODO Auto-generated method stub
            return false;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.List#containsAll(java.util.Collection)
         */
        @Override
        public boolean containsAll(Collection<?> c) {
            // TODO Auto-generated method stub
            return false;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.List#addAll(java.util.Collection)
         */
        @Override
        public boolean addAll(Collection<? extends String> c) {
            // TODO Auto-generated method stub
            return false;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.List#addAll(int, java.util.Collection)
         */
        @Override
        public boolean addAll(int index, Collection<? extends String> c) {
            // TODO Auto-generated method stub
            return false;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.List#removeAll(java.util.Collection)
         */
        @Override
        public boolean removeAll(Collection<?> c) {
            // TODO Auto-generated method stub
            return false;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.List#retainAll(java.util.Collection)
         */
        @Override
        public boolean retainAll(Collection<?> c) {
            // TODO Auto-generated method stub
            return false;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.List#clear()
         */
        @Override
        public void clear() {
            // TODO Auto-generated method stub

        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.List#get(int)
         */
        @Override
        public String get(int index) {
            // TODO Auto-generated method stub
            return null;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.List#set(int, java.lang.Object)
         */
        @Override
        public String set(int index, String element) {
            // TODO Auto-generated method stub
            return null;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.List#add(int, java.lang.Object)
         */
        @Override
        public void add(int index, String element) {
            // TODO Auto-generated method stub

        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.List#remove(int)
         */
        @Override
        public String remove(int index) {
            // TODO Auto-generated method stub
            return null;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.List#indexOf(java.lang.Object)
         */
        @Override
        public int indexOf(Object o) {
            // TODO Auto-generated method stub
            return 0;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.List#lastIndexOf(java.lang.Object)
         */
        @Override
        public int lastIndexOf(Object o) {
            // TODO Auto-generated method stub
            return 0;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.List#listIterator()
         */
        @Override
        public ListIterator<String> listIterator() {
            // TODO Auto-generated method stub
            return null;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.List#listIterator(int)
         */
        @Override
        public ListIterator<String> listIterator(int index) {
            // TODO Auto-generated method stub
            return null;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.List#subList(int, int)
         */
        @Override
        public List<String> subList(int fromIndex, int toIndex) {
            // TODO Auto-generated method stub
            return null;
        }

    }

    static class MyCol implements Collection<String> {

        /*
         * (non-Javadoc)
         *
         * @see java.util.Collection#size()
         */
        @Override
        public int size() {
            // TODO Auto-generated method stub
            return 0;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.Collection#isEmpty()
         */
        @Override
        public boolean isEmpty() {
            // TODO Auto-generated method stub
            return false;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.Collection#contains(java.lang.Object)
         */
        @Override
        public boolean contains(Object o) {
            // TODO Auto-generated method stub
            return false;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.Collection#iterator()
         */
        @Override
        public Iterator<String> iterator() {
            // TODO Auto-generated method stub
            return null;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.Collection#toArray()
         */
        @Override
        public Object[] toArray() {
            // TODO Auto-generated method stub
            return null;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.Collection#toArray(T[])
         */
        @Override
        public <T> T[] toArray(T[] a) {
            // TODO Auto-generated method stub
            return null;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.Collection#add(java.lang.Object)
         */
        @Override
        public boolean add(String e) {
            // TODO Auto-generated method stub
            return false;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.Collection#remove(java.lang.Object)
         */
        @Override
        public boolean remove(Object o) {
            // TODO Auto-generated method stub
            return false;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.Collection#containsAll(java.util.Collection)
         */
        @Override
        public boolean containsAll(Collection<?> c) {
            // TODO Auto-generated method stub
            return false;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.Collection#addAll(java.util.Collection)
         */
        @Override
        public boolean addAll(Collection<? extends String> c) {
            // TODO Auto-generated method stub
            return false;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.Collection#removeAll(java.util.Collection)
         */
        @Override
        public boolean removeAll(Collection<?> c) {
            // TODO Auto-generated method stub
            return false;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.Collection#retainAll(java.util.Collection)
         */
        @Override
        public boolean retainAll(Collection<?> c) {
            // TODO Auto-generated method stub
            return false;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.Collection#clear()
         */
        @Override
        public void clear() {
            // TODO Auto-generated method stub

        }

    }
}
