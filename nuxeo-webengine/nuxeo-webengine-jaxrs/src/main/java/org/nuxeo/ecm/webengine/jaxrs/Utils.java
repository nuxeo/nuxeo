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
package org.nuxeo.ecm.webengine.jaxrs;

import java.lang.reflect.Array;
import java.util.ArrayList;

import org.osgi.framework.Bundle;

/**
 * Some helper methods for parsing configuration and loading contributed servlets or filters.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Utils {

    /**
     * Load classes from a list of class references given as a comma separated string list.
     *
     * @param classRefs the string containing the list of class references
     * @return an array of the loaded classes
     * @throws ClassNotFoundException
     * @throws BundleNotFoundException
     */
    public static Class<?>[] loadClasses(String classRefs) throws ClassNotFoundException, BundleNotFoundException {
        return loadClasses(classRefs, ',');
    }

    /**
     * Load classes from a list of class references given as a 'sep' separated string list.
     *
     * @param classRefs the string containing the list of class references
     * @param sep the separator character used to separate class references in the string.
     * @return an array of the loaded classes
     *
     * @throws ClassNotFoundException
     * @throws BundleNotFoundException
     */
    public static Class<?>[] loadClasses(String classRefs, char sep) throws ClassNotFoundException, BundleNotFoundException {
        StringBuilder buf = null;
        ArrayList<Class<?>> classes = new ArrayList<Class<?>>();
        char[] chars = classRefs.toCharArray();
        for (int i=0; i<chars.length; i++) {
            char c = chars[i];
            if (c <= ' ') {
                continue;
            } else if (c == sep) {
                if (buf != null) {
                    classes.add(loadClass(buf.toString()));
                    buf = null;
                }
            } else {
                if (buf == null) {
                    buf = new StringBuilder();
                }
                buf.append(c);
            }
        }

        if (buf != null) {
            classes.add(loadClass(buf.toString()));
        }


        return classes.toArray(new Class<?>[classes.size()]);
    }

    /**
     * Get class instances for the given class references string
     * @param <T>
     * @param componentType the type of the expected array component
     * @param classRefs
     * @return
     * @throws Exception
     *
     * @see {@link #loadClasses(String)}
     */
    public static <T> T[] newInstances(Class<T> componentType, String classRefs) throws Exception {
        return newInstances(componentType, classRefs, ',');
    }

    /**
     * Get class instances for the given class references string
     * @param <T>
     * @param componentType
     * @param classRefs
     * @param sep
     * @return
     * @throws Exception
     *
     * @see {@link #loadClasses(String, char)}
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] newInstances(Class<T> componentType, String classRefs, char sep) throws Exception {
        Class<?>[] classes = loadClasses(classRefs, sep);
        T[] ar = (T[])Array.newInstance(componentType, classes.length);
        for (int i=0; i<classes.length; i++) {
            ar[i] = (T)classes[i].newInstance();
        }
        return ar;
    }

    /**
     * Load a class from a class reference string. The class reference string is in the format:
     * <code>bundleSymbolicName:className</code> or <code>className</code>.
     * If no bundle symbolic name is given the class will be loaded using the class loader of the {@link Utils} class.
     * <p>
     * The bundle will be resolved to the last version of the bundle
     * (in case when different bundle versions are found)
     *
     * @param classRef
     * @return
     * @throws ClassNotFoundException
     * @throws BundleNotFoundException
     */
    public static Class<?> loadClass(String classRef) throws ClassNotFoundException, BundleNotFoundException {
        int i = classRef.indexOf(':');
        if (i == -1) {
            // use the current bundle class loader
            return Activator.getInstance().getContext().getBundle().loadClass(classRef.trim());
        } else {
            return loadClass(classRef.substring(0, i).trim(), classRef.substring(i+1).trim());
        }
    }

    /**
     * Get a class proxy reference for the given class reference
     * @param classRef
     * @return
     * @throws Exception
     */
    public static ClassRef getClassRef(String classRef) throws ClassNotFoundException, BundleNotFoundException {
        int i = classRef.indexOf(':');
        if (i == -1) {
            // use the current bundle class loader
            Bundle bundle = Activator.getInstance().getContext().getBundle();
            return new ClassRef(bundle, bundle.loadClass(classRef.trim()));
        } else {
            String bundleId = classRef.substring(0, i).trim();
            String className = classRef.substring(i+1).trim();
            Bundle[] bundles = Activator.getInstance().getPackageAdmin().getBundles(bundleId, null);
            if (bundles != null) {
                return new ClassRef(bundles[0], bundles[0].loadClass(className));
            } else {
                throw new BundleNotFoundException(bundleId);
            }
        }
    }


    /**
     * Load a class given the owner bundle and the class name.
     * <p>
     * The bundle will be resolved to the last version of the bundle
     * (in case when different bundle versions are found)
     *
     * @param bundleId
     * @param className
     * @return
     * @throws ClassNotFoundException
     * @throws BundleNotFoundException
     */
    public static Class<?> loadClass(String bundleId, String className) throws ClassNotFoundException, BundleNotFoundException {
        Bundle[] bundles = Activator.getInstance().getPackageAdmin().getBundles(bundleId, null);
        if (bundles != null) {
            return bundles[0].loadClass(className);
        } else {
            throw new BundleNotFoundException(bundleId);
        }
    }


    /**
     * Create a new object of the given class in the given bundle. The class should provide a no-args empty constructor.
     * <p>
     * The bundle will be resolved to the last version of the bundle
     * (in case when different bundle versions are found)
     *
     * @param bundleId
     * @param className
     * @return
     * @throws Exception
     *
     * @see {@link #loadClass(String, String)}
     */
    public static Object newInstance(String bundleId, String className) throws Exception {
        return loadClass(bundleId, className).newInstance();
    }

    /**
     * Create a new object of the given a class reference.
     * <p>
     * The bundle will be resolved to the last version of the bundle
     * (in case when different bundle versions are found)
     *
     * @param classRef
     * @return
     * @throws Exception
     *
     * @see {@link #loadClass(String, String)}
     */
    public static Object newInstance(String classRef) throws Exception {
        return loadClass(classRef).newInstance();
    }


    public static class ClassRef {
        protected Bundle bundle;
        protected Class<?> clazz;

        public ClassRef(Bundle bundle, Class<?> clazz) {
            this.bundle = bundle;
            this.clazz = clazz;
        }

        public Class<?> get() {
            return clazz;
        }

        public Bundle bundle() {
            return bundle;
        }

        public Object newInstance() throws Exception {
            return clazz.newInstance();
        }

        @Override
        public String toString() {
            if (bundle != null) {
                return bundle.getSymbolicName()+":"+clazz.getName();
            }
            return clazz.getName();
        }

    }

}
