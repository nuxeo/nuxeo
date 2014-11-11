/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.runtime.api;

import java.util.Properties;

import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.nuxeo.common.collections.ListenerList;
import org.nuxeo.runtime.RuntimeService;
import org.nuxeo.runtime.RuntimeServiceEvent;
import org.nuxeo.runtime.RuntimeServiceListener;
import org.nuxeo.runtime.ServiceManager;
import org.nuxeo.runtime.api.login.LoginService;

/**
 * This class is the main entry point to a Nuxeo runtime application.
 * <p>
 * It offers an easy way to create new sessions, to access system services and
 * other resources.
 * <p>
 * There are two type of services:
 * <ul>
 * <li> Global Services - these services are uniquely defined by a service
 * class, and there is an unique instance of the service in the system per
 * class.
 * <li> Local Services - these services are defined by a class and an URI.
 * This type of service allows multiple service instances for the same class of
 * services. Each instance is uniquely defined in the system by an URI.
 * </ul>
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public final class Framework {

    /**
     * The runtime instance.
     */
    private static RuntimeService runtime;

    private static ServiceManager serviceMgr;

    private static final ListenerList listeners = new ListenerList();

    // Utility class.
    private Framework() { }


    // FIXME: this method can't work as it is implemented here.
    public static void initialize(RuntimeService runtimeService) throws Exception {
        if (runtime != null) {
            throw new Exception("Nuxeo Framework was already initialized");
        }
        runtime = runtimeService;
        initServiceManager();
        runtime.start();
    }

    public static void shutdown() throws Exception {
        if (runtime != null) {
            runtime.stop();
            runtime = null;
        }
    }

    /**
     * Tests whether or not the runtime was initialized.
     *
     * @return true if the runtime was initialized, false otherwise
     */
    public static synchronized boolean isInitialized() {
        return runtime != null;
    }

    private static void initServiceManager() {
        String sm = getProperty("org.nuxeo.runtime.ServiceManager");
        if (sm == null) { // compatibility mode
            serviceMgr = org.nuxeo.runtime.api.ServiceManager.getInstance();
        } else {
            try {
                serviceMgr = (ServiceManager) Class.forName(sm).newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize service manager", e);
            }
        }
    }

    /**
     * Gets the runtime service instance.
     *
     * @return the runtime service instance
     */
    public static RuntimeService getRuntime() {
        return runtime;
    }

    /**
     * Gets a service given its class.
     *
     * @param <T>
     * @param serviceClass
     * @return
     */
    public static <T> T getService(Class<T> serviceClass) throws Exception {
        return serviceMgr.getService(serviceClass);
    }

    /**
     * Gets a service given its class and an identifier.
     *
     * @param <T>
     * @param serviceClass
     * @param name
     * @return
     */
    public static <T> T getService(Class<T> serviceClass, String name)
            throws Exception {
        return serviceMgr.getService(serviceClass, name);
    }

    /**
     * Gets a nuxeo-runtime local service.
     *
     * @param <T>
     * @param serviceClass
     * @return
     */
    public static <T> T getLocalService(Class<T> serviceClass) {
        //TODO: obsolete code remove it
        ServiceProvider provider = DefaultServiceProvider.getProvider();
        if (provider != null) {
            return provider.getService(serviceClass);
        }
        // TODO impl a runtime service provider
        return runtime.getService(serviceClass);
    }

    /**
     * Lookup a registered object given its key.
     *
     * @param key
     * @return
     */
    public static Object lookup(String key) {
        return null; //TODO
    }

    /**
     * Login in the system as the system user (a pseudo-user having all
     * privileges).
     *
     * @return the login session if successful. Never returns null.
     * @throws LoginException on login failure
     */
    public static LoginContext login() throws LoginException {
        if (null == runtime) {
            throw new IllegalStateException("runtime not initialized");
        }
        LoginService loginService = runtime.getService(LoginService.class);
        if (loginService != null) {
            return loginService.login();
        }
        return null;
    }

    /**
     * Login in the system as the system user (a pseudo-user having all
     * privileges). The given username will be used to identify the user
     * id that called this method.
     *
     * @param username the originating user id
     * @return the login session if successful. Never returns null.
     * @throws LoginException on login failure
     */
    public static LoginContext loginAs(String username) throws LoginException {
        if (null == runtime) {
            throw new IllegalStateException("runtime not initialized");
        }
        LoginService loginService = runtime.getService(LoginService.class);
        if (loginService != null) {
            return loginService.loginAs(username);
        }
        return null;
    }

    /**
     * Login in the system as the given user using the given password.
     *
     * @param username the username to login
     * @param password the password
     * @return a login session if login was successful. Never returns null.
     * @throws LoginException if login failed
     */
    public static LoginContext login(String username, Object password)
            throws LoginException {
        LoginService loginService = runtime.getService(LoginService.class);
        if (loginService != null) {
            return loginService.login(username, password);
        }
        return null;
    }

    /**
     * Login in the system using the given callback handler for login info resolution.
     *
     * @param cbHandler used to fetch the login info
     * @return the login context
     * @throws LoginException
     */
    public static LoginContext login(CallbackHandler cbHandler)
            throws LoginException {
        LoginService loginService = runtime.getService(LoginService.class);
        if (loginService != null) {
            return loginService.login(cbHandler);
        }
        return null;
    }

    public static void sendEvent(RuntimeServiceEvent event) {
        Object[] listenersArray = listeners.getListeners();
        for (Object listener : listenersArray) {
            ((RuntimeServiceListener) listener).handleEvent(event);
        }
    }

    /**
     * Registers a listener to be notified about runtime events.
     * <p>
     * If the listener is already registered, do nothing.
     *
     * @param listener the listener to register
     */
    public static void addListener(RuntimeServiceListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes the given listener.
     * <p>
     * If the listener is not registered, do nothing.
     *
     * @param listener
     */
    public static void removeListener(RuntimeServiceListener listener) {
        listeners.remove(listener);
    }

    /**
     * Gets the given property value if any, otherwise null.
     * <p>
     * The framework properties will be searched first then if any matching
     * property is found the system properties are searched too.
     *
     * @param key the property key
     * @return the property value if any or null otherwise
     */
    public static String getProperty(String key) {
        return getProperty(key, null);
    }

    /**
     * Gets the given property value if any, otherwise returns the given
     * default value.
     * <p>
     * The framework properties will be searched first then if any matching
     * property is found the system properties are searched too.
     *
     * @param key the property key
     * @param defValue the default value to use
     * @return the property value if any otherwise the default value
     */
    public static String getProperty(String key, String defValue) {
        return runtime.getProperty(key, defValue);
    }

    /**
     * Gets all the framework properties. The system properties are not included
     * in the returned map.
     *
     * @return the framework properties map. Never returns null.
     */
    public static Properties getProperties() {
        return runtime.getProperties();
    }

    /**
     * Expands any variable found in the given expression with the value
     * of the corresponding framework property.
     * <p>
     * The variable format is ${property_key}.
     * <p>
     * System properties are also expanded.
     *
     * @param expression
     * @return
     */
    public static String expandVars(String expression) {
        int p = expression.indexOf("${");
        if (p == -1) {
            return expression; // do not expand if not needed
        }

        char[] buf = expression.toCharArray();
        StringBuilder result = new StringBuilder(buf.length);
        if (p > 0) {
            result.append(expression.substring(0, p));
        }
        StringBuilder varBuf = new StringBuilder();
        boolean dollar = false;
        boolean var = false;
        for (int i = p; i < buf.length; i++) {
            char c = buf[i];
            switch (c) {
            case '$' :
                dollar = true;
                break;
            case '{' :
                if (dollar) {
                    dollar = false;
                    var = true;
                }
                break;
            case '}':
                if (var) {
                  var = false;
                  String varName = varBuf.toString();
                  varBuf.setLength(0);
                  String varValue = getProperty(varName); // get the variable value
                  if (varValue != null) {
                      result.append(varValue);
                  } else { // let the variable as is
                      result.append("${").append(varName).append('}');
                  }
                }
                break;
            default:
                if (var) {
                  varBuf.append(c);
                } else {
                    result.append(c);
                }
                break;
            }
        }
        return result.toString();
    }

    public static void main(String[] args) {
    }

}
