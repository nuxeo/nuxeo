/*
 * Copyright (c) 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package org.nuxeo.common.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.ArrayList;
import java.util.Hashtable;

/**
 * Used to force installation of URLStreamHandlerFactory as the default
 * mechanism in Java is failing to set a new factory if one was already set.
 * <p>
 * This class provides the capability to stack any number of factories - each
 * factory having precedence over the last one.
 * <p>
 * Thus, when querying for a URL protocol handler all factories will be asked in
 * turn (from the newest one to the older one) until a stream handler is
 * obtained.
 * <p>
 * Contains some code from Eclipse Framework class.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class URLStreamHandlerFactoryInstaller {

    private static final FactoryStackHolder factoryStackHolder = new FactoryStackHolder();

    private static final FactoryStack stack = new FactoryStack();

    private static class FactoryStackHolder extends
            InheritableThreadLocal<FactoryStack> implements URLStreamHandlerFactory {

        @Override
        public FactoryStack initialValue() {
            return stack;
        }

        @Override
        public void remove() {
            super.remove();
        }

        @Override
        public URLStreamHandler createURLStreamHandler(String protocol) {
            return get().createURLStreamHandler(protocol);
        }
    }

    private URLStreamHandlerFactoryInstaller() {
    }

    public static void installURLStreamHandlerFactory(URLStreamHandlerFactory shf) {
        FactoryStack factoryStack = factoryStackHolder.get();
        Field factoryField = getStaticField(URL.class, URLStreamHandlerFactory.class);
        if (factoryField == null) {
            throw new IllegalArgumentException(
                    "Could not find URLStreamHandlerFactory field");
        }
        // look for a lock to synchronize on
        Object lock = getURLStreamHandlerFactoryLock();
        synchronized (lock) {
            try {
                URLStreamHandlerFactory factory = (URLStreamHandlerFactory) factoryField.get(null);
                if (factory == null) { // not installed - install it
                    factoryStack.push(shf); // push the new factory
                } else if (factory != factoryStackHolder) { // another factory is
                                                      // installed
                    factoryStack.push(factory);
                    factoryStack.push(shf); // push the new factory
                } else { // already installed
                    factoryStack.push(shf); // push the new factory
                }
                flush(factoryField);
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }

    public static void uninstallURLStreamHandlerFactory() {
        factoryStackHolder.remove();
        try {
           Field factoryField = getStaticField(URL.class, URLStreamHandlerFactory.class);
            if (factoryField == null) {
                return; // oh well, we tried
            }
            Object lock = getURLStreamHandlerFactoryLock();
            synchronized (lock) {
                URLStreamHandlerFactory factory = (URLStreamHandlerFactory) factoryField.get(null);
                if (factory == null) {
                    return;
                }
                if (factory != factoryStackHolder) {
                    return;
                }
                factoryField.set(null, null);
                resetURLStreamHandlers();
            }
        } catch (IllegalArgumentException e) {
            // ignore and continue closing the framework
        } catch (IllegalAccessException e) {
           // ignore and continue closing the framework
        }
    }

    public static void uninstallURLStreamHandlerFactory(URLStreamHandlerFactory shf) {
        try {
            Field factoryField = getStaticField(URL.class, URLStreamHandlerFactory.class);
            if (factoryField == null) {
                return; // oh well, we tried
            }
            Object lock = getURLStreamHandlerFactoryLock();
            synchronized (lock) {
                URLStreamHandlerFactory factory = (URLStreamHandlerFactory) factoryField.get(null);
                if (factory == null) {
                    return;
                }
                if (factory != factoryStackHolder) {
                    return;
                }
                FactoryStack factoryStack = factoryStackHolder.get();
                if (shf == null) {
                    factoryStack.pop();
                } else {
                    factoryStack.remove(shf);
                }
                // reinstall factory (to flush cache)
                flush(factoryField);
            }
        } catch (IllegalArgumentException e) {
            // ignore and continue closing the framework
        } catch (IllegalAccessException e) {
           // ignore and continue closing the framework
        }

    }

    protected static void flush(Field factoryField)
            throws IllegalArgumentException, IllegalAccessException {
        factoryField.set(null, null);
        resetURLStreamHandlers();
        URL.setURLStreamHandlerFactory(factoryStackHolder);
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

    public static void resetURLStreamHandlers()  {
        Field handlersField = getStaticField(URL.class, Hashtable.class);
        if (handlersField != null) {
            Hashtable<?, ?> handlers;
            try {
                handlers = (Hashtable<?, ?>) handlersField.get(null);
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException(
                        "Cannot clear URL handlers cache", e);
            }
            if (handlers != null) {
                handlers.clear();
            }
        }
    }

    private static Object getURLStreamHandlerFactoryLock() {
        Object lock;
        try {
            Field streamHandlerLockField = URL.class.getDeclaredField("streamHandlerLock");
            streamHandlerLockField.setAccessible(true);
            lock = streamHandlerLockField.get(null);
        } catch (NoSuchFieldException noField) {
            // could not find the lock, lets sync on the class object
            lock = URL.class;
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        }
        return lock;
    }

    /**
     * Get the underlying stack.
     * <p>
     * This should not be used to register/unregister factories (since it is not
     * synchronized). To install / uninstall factories use the static method of
     * that class.
     */
    public static FactoryStack getStack() {
        return factoryStackHolder.get();
    }


    public static class FactoryStack implements URLStreamHandlerFactory {

        final ArrayList<URLStreamHandlerFactory> factories = new ArrayList<URLStreamHandlerFactory>();

        @Override
        public URLStreamHandler createURLStreamHandler(String protocol) {
            for (int i = factories.size() - 1; i >= 0; i--) {
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
            return factories.remove(factories.size() - 1);
        }

        URLStreamHandlerFactory remove(URLStreamHandlerFactory shf) {
            return factories.remove(factories.indexOf(shf));
        }

        public URLStreamHandlerFactory peek() {
            if (factories.isEmpty()) {
                return null;
            }
            return factories.get(factories.size() - 1);
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
