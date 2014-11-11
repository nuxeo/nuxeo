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
 */
package org.nuxeo.common.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.ArrayList;

/**
 * Used to force installation of URLStreamHandlerFactory as the default mechanism in Java
 * is failing to set a new factory if one was already set.
 * <p>
 * This class provides the capability to stack any number of factories - each factory having
 * precedence over the last one.
 * <p>
 * Thus, when querying for a URL protocol handler all factories will be asked in turn
 * (from the newest one to the older one) until a stream handler is obtained.
 * <p>
 * Contains some code from Eclipse Framework class.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class URLStreamHandlerFactoryInstaller {

    private static final FactoryStack factoryStack = new FactoryStack();

    public static void installURLStreamHandlerFactory(URLStreamHandlerFactory shf) throws Exception {
        Field factoryField = getStaticField(URL.class, URLStreamHandlerFactory.class);
        if (factoryField == null) {
            throw new Exception("Could not find URLStreamHandlerFactory field");
        }
        // look for a lock to synchronize on
        Object lock = getURLStreamHandlerFactoryLock();
        synchronized (lock) {
            URLStreamHandlerFactory factory = (URLStreamHandlerFactory) factoryField.get(null);
            if (factory == null) { // not installed - install it
                factoryStack.push(shf); // push the new factory
            } else if (factory != factoryStack) { // another factory is installed
                factoryStack.push(factory);
                factoryStack.push(shf); // push the new factory
            } else { // already installed
                factoryStack.push(shf); // push the new factory
            }
            // install it
            factoryField.set(null, null);
            URL.setURLStreamHandlerFactory(factoryStack);
        }
    }

    public static void uninstallURLStreamHandlerFactory() {
        try {
            Field factoryField = getStaticField(URL.class, URLStreamHandlerFactoryInstaller.class);
            if (factoryField == null) {
                return; // oh well, we tried
            }
            Object lock = getURLStreamHandlerFactoryLock();
            synchronized (lock) {
                URLStreamHandlerFactory factory = (URLStreamHandlerFactory) factoryField.get(null);
                if (factory != null && factory == factoryStack) {
                    factoryStack.pop();
                }
                // reinstall factory (to flush cache)
                factoryField.set(null, null);
                URL.setURLStreamHandlerFactory(factoryStack);
            }
        } catch (Exception e) {
            // ignore and continue closing the framework
        }
    }

    private static Field getStaticField(Class<?> clazz, Class<?> type) {
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers()) && field.getType().equals(type)) {
                field.setAccessible(true);
                return field;
            }
        }
        return null;
    }

    private static Object getURLStreamHandlerFactoryLock() throws IllegalAccessException {
        Object lock;
        try {
            Field streamHandlerLockField = URL.class.getDeclaredField("streamHandlerLock");
            streamHandlerLockField.setAccessible(true);
            lock = streamHandlerLockField.get(null);
        } catch (NoSuchFieldException noField) {
            // could not find the lock, lets sync on the class object
            lock = URL.class;
        }
        return lock;
    }

    /**
     * Get the underlying stack.
     * <p>
     * This should not be used to register/unregister factories (since it is not synchronized).
     * To install / uninstall factories use the static method of that class.
     */
    public static FactoryStack getStack() {
        return factoryStack;
    }

    public static class FactoryStack implements URLStreamHandlerFactory {
        ArrayList<URLStreamHandlerFactory> factories = new ArrayList<URLStreamHandlerFactory>();
        public URLStreamHandler createURLStreamHandler(String protocol) {
            for (int i = factories.size()-1; i>=0; i--) {
                URLStreamHandler h = factories.get(i).createURLStreamHandler(protocol);
                if (h != null) {
                    return h;
                }
            }
            return null;
        }
        public void push(URLStreamHandlerFactory factory) {
            factories.add(factory);
        }
        public URLStreamHandlerFactory pop() {
            if (factories.isEmpty()) {
                return null;
            }
            return factories.remove(factories.size()-1);
        }
        public URLStreamHandlerFactory peek() {
            if (factories.isEmpty()) {
                return null;
            }
            return factories.get(factories.size()-1);
        }

        public boolean isEmpty() {
            return factories.isEmpty();
        }
        public int size() {
            return factories.size();
        }
        public void clear() {
            factories.clear();
        }
    }

}
