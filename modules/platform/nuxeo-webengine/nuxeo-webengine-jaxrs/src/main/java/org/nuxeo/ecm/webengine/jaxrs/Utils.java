/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 */
public class Utils {

    /**
     * Load classes from a list of class references given as a comma separated string list.
     *
     * @param classRefs the string containing the list of class references
     * @return an array of the loaded classes
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
     */
    public static Class<?>[] loadClasses(String classRefs, char sep) throws ClassNotFoundException,
            BundleNotFoundException {
        StringBuilder buf = null;
        ArrayList<Class<?>> classes = new ArrayList<>();
        char[] chars = classRefs.toCharArray();
        for (char c : chars) {
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

        return classes.toArray(new Class<?>[0]);
    }

    /**
     * Get class instances for the given class references string
     *
     * @param componentType the type of the expected array component
     * @see #loadClasses(String)
     */
    public static <T> T[] newInstances(Class<T> componentType, String classRefs) throws ReflectiveOperationException,
            BundleNotFoundException {
        return newInstances(componentType, classRefs, ',');
    }

    /**
     * Get class instances for the given class references string
     *
     * @see #loadClasses(String, char)
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] newInstances(Class<T> componentType, String classRefs, char sep)
            throws ReflectiveOperationException, BundleNotFoundException {
        Class<?>[] classes = loadClasses(classRefs, sep);
        T[] ar = (T[]) Array.newInstance(componentType, classes.length);
        for (int i = 0; i < classes.length; i++) {
            ar[i] = (T) classes[i].getDeclaredConstructor().newInstance();
        }
        return ar;
    }

    /**
     * Load a class from a class reference string. The class reference string is in the format:
     * <code>bundleSymbolicName:className</code> or <code>className</code>. If no bundle symbolic name is given the
     * class will be loaded using the class loader of the {@link Utils} class.
     * <p>
     * The bundle will be resolved to the last version of the bundle (in case when different bundle versions are found)
     */
    public static Class<?> loadClass(String classRef) throws ClassNotFoundException, BundleNotFoundException {
        int i = classRef.indexOf(':');
        if (i == -1) {
            // use the current bundle class loader
            return Activator.getInstance().getContext().getBundle().loadClass(classRef.trim());
        } else {
            return loadClass(classRef.substring(0, i).trim(), classRef.substring(i + 1).trim());
        }
    }

    /**
     * Get a class proxy reference for the given class reference
     */
    public static ClassRef getClassRef(String classRef) throws ClassNotFoundException, BundleNotFoundException {
        return getClassRef(classRef, null);
    }

    public static ClassRef getClassRef(String classRef, Bundle bundle) throws ClassNotFoundException,
            BundleNotFoundException {
        int i = classRef.indexOf(':');
        if (i == -1) {
            // use the current bundle class loader
            if (bundle == null) {
                bundle = Activator.getInstance().getContext().getBundle();
            }
            return new ClassRef(bundle, bundle.loadClass(classRef.trim()));
        } else {
            String bundleId = classRef.substring(0, i).trim();
            String className = classRef.substring(i + 1).trim();
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
     * The bundle will be resolved to the last version of the bundle (in case when different bundle versions are found)
     */
    public static Class<?> loadClass(String bundleId, String className) throws ClassNotFoundException,
            BundleNotFoundException {
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
     * The bundle will be resolved to the last version of the bundle (in case when different bundle versions are found)
     *
     * @see #loadClass(String, String)
     */
    public static Object newInstance(String bundleId, String className) throws ReflectiveOperationException,
            BundleNotFoundException {
        return loadClass(bundleId, className).getDeclaredConstructor().newInstance();
    }

    /**
     * Create a new object of the given a class reference.
     * <p>
     * The bundle will be resolved to the last version of the bundle (in case when different bundle versions are found)
     *
     * @see #loadClass(String, String)
     */
    public static Object newInstance(String classRef) throws ReflectiveOperationException, BundleNotFoundException {
        return loadClass(classRef).getDeclaredConstructor().newInstance();
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

        public Object newInstance() throws ReflectiveOperationException {
            return clazz.getDeclaredConstructor().newInstance();
        }

        @Override
        public String toString() {
            if (bundle != null) {
                return bundle.getSymbolicName() + ":" + clazz.getName();
            }
            return clazz.getName();
        }

    }

}
