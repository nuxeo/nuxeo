/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thierry Delprat
 */

package org.nuxeo.ecm.platform.ui.web.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.seam.Component;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.util.EJB;

/**
 * This class provides helper methods for accessing a Seam Component
 * <p>
 * Why this class?
 * <p>
 * At startup time, Seam generates CGLib Wrappers around each Seam component. This wrapper holds all interceptors that
 * are used for bijection. Because of that, accessing a Seam component by its reference will lead to call a disinjected
 * instance (all @In member variables are null).
 * <p>
 * Seam components are usually accessed via EL or via injected references, in this cases, Seam takes care of everything
 * and you get a functional instance. But in some cases, you need to access a Seam component:
 * <ul>
 * <li>from an object that has no direct access to Seam (ie: can't use injection),
 * <li>from an object that stored a call-back reference (storing the 'this' of the Seam component).
 * </ul>
 * In these cases, this helper class is useful. This class provides helper functions for :
 * <ul>
 * <li>getting a Wrapped Seam component via its name or its reference,
 * <li>executing a method via a method name on a Seam component.
 * </ul>
 *
 * @author tiry
 */
public final class SeamComponentCallHelper {

    // This is an utility class.
    private SeamComponentCallHelper() {
    }

    /**
     * Gets the CGLib-wrapped Seam component from its name.
     *
     * @param seamName the name of the Seam component
     * @return the Wrapped Seam component
     */
    public static Object getSeamComponentByName(String seamName) {
        // Find the component we're calling
        Component component = Component.forName(seamName);

        if (component == null) {
            throw new RuntimeException("No such component: " + seamName);
        }

        // Create an instance of the component
        Object seamComponent = Component.getInstance(seamName, true);

        return seamComponent;
    }

    /**
     * Gets the CGLib-wrapped Seam component from a reference.
     *
     * @param seamRef reference of the object behind the Seam component
     * @return the Wrapped Seam component
     */
    public static Object getSeamComponentByRef(Object seamRef) {
        String seamName = getSeamComponentName(seamRef);
        if (seamName == null) {
            return null;
        }
        return getSeamComponentByName(seamName);
    }

    /**
     * Calls a Seam component by name.
     *
     * @param seamName the name of the Seam component
     * @param methodName the method name (for ejb3 method must be exposed in the local interface)
     * @param params parameters as Object[]
     * @return the result of the call
     * @throws RuntimeException
     */
    public static Object callSeamComponentByName(String seamName, String methodName, Object[] params) {

        Object seamComponent = getSeamComponentByName(seamName);
        Component component = Component.forName(seamName);

        Class type = null;
        if (component.getType().isSessionBean() && !component.getBusinessInterfaces().isEmpty()) {
            for (Class c : component.getBusinessInterfaces()) {
                if (c.isAnnotationPresent(EJB.LOCAL)) {
                    type = component.getBusinessInterfaces().iterator().next();
                    break;
                }
            }

            if (type == null) {
                throw new RuntimeException("Type cannot be determined for component [" + component
                        + "]. Please ensure that it has a local interface.");
            }
        }

        if (type == null) {
            type = component.getBeanClass();
        }

        Method m = findMethod(methodName, type, params);

        if (m == null) {
            throw new RuntimeException("No compatible method found.");
        }

        try {
            Object result = m.invoke(seamComponent, params);
            return result;
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Error calling method " + e.getMessage(), e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Error calling method " + e.getMessage(), e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Error calling method " + e.getMessage(), e);
        }
    }

    /**
     * Calls a Seam component by reference.
     *
     * @param seamRef the reference on the object behind the Seam component
     * @param methodName the method name (for ejb3 method must be exposed in the local interface)
     * @param params parameters as Object[]
     * @return the result of the call
     * @throws RuntimeException
     */
    public static Object callSeamComponentByRef(Object seamRef, String methodName, Object[] params) {
        String seamName = getSeamComponentName(seamRef);
        return callSeamComponentByName(seamName, methodName, params);
    }

    /**
     * Calls a Seam component by reference.
     *
     * @param seamRef the reference on the object behind the Seam component
     * @param methodName the method name (for ejb3 method must be exposed in the local interface)
     * @param param parameter as Object
     * @return the result of the call
     * @throws RuntimeException
     */
    public static Object callSeamComponentByRef(Object seamRef, String methodName, Object param) {
        List<Object> params = new ArrayList<Object>();
        params.add(param);
        return callSeamComponentByRef(seamRef, methodName, params.toArray());
    }

    /**
     * Calls a Seam component by name.
     *
     * @param seamName the name of the Seam component
     * @param methodName the method name (for ejb3 method must be exposed in the local interface)
     * @param param parameters as Object[]
     * @return the result of the call
     * @throws RuntimeException
     */
    public static Object callSeamComponentByName(String seamName, String methodName, Object param) {
        List<Object> params = new ArrayList<Object>();
        params.add(param);
        return callSeamComponentByName(seamName, methodName, params.toArray());
    }

    // Internal methods

    /**
     * Extracts the Seam name from annotation given a reference.
     */
    private static String getSeamComponentName(Object seamRef) {
        Name componentName = seamRef.getClass().getAnnotation(Name.class);

        if (componentName == null) {
            return null;
        }

        return componentName.value();
    }

    /**
     * Finds the method in the local interface of the Seam component.
     */
    private static Method findMethod(String name, Class cls, Object[] params) {
        Map<Method, Integer> candidates = new HashMap<Method, Integer>();

        // for (Method m : cls.getDeclaredMethods()) {
        for (Method method : cls.getMethods()) {

            if (name.equals(method.getName()) && method.getParameterTypes().length == params.length) {
                int score = 0;
                // XXX should do better check !!!
                candidates.put(method, score);
            }
        }

        Method bestMethod = null;
        int bestScore = 0;

        for (Method method : candidates.keySet()) {
            int thisScore = candidates.get(method);
            if (bestMethod == null || thisScore > bestScore) {
                bestMethod = method;
                bestScore = thisScore;
            }
        }

        return bestMethod;
    }

}
