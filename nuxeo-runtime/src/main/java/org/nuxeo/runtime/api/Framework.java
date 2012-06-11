/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.runtime.api;

import java.io.File;
import java.net.URL;
import java.util.Properties;

import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.commons.io.FileCleaningTracker;
import org.apache.commons.io.FileDeleteStrategy;
import org.nuxeo.common.Environment;
import org.nuxeo.common.collections.ListenerList;
import org.nuxeo.runtime.RuntimeService;
import org.nuxeo.runtime.RuntimeServiceEvent;
import org.nuxeo.runtime.RuntimeServiceListener;
import org.nuxeo.runtime.ServiceManager;
import org.nuxeo.runtime.api.login.LoginAs;
import org.nuxeo.runtime.api.login.LoginService;

/**
 * This class is the main entry point to a Nuxeo runtime application.
 * <p>
 * It offers an easy way to create new sessions, to access system services and
 * other resources.
 * <p>
 * There are two type of services:
 * <ul>
 * <li>Global Services - these services are uniquely defined by a service
 * class, and there is an unique instance of the service in the system per
 * class.
 * <li>Local Services - these services are defined by a class and an URI. This
 * type of service allows multiple service instances for the same class of
 * services. Each instance is uniquely defined in the system by an URI.
 * </ul>
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public final class Framework {

    /**
     * Global dev property
     *
     * @since 5.6
     * @see #isDevModeSet()
     */
    public static final String NUXEO_DEV_SYSTEM_PROP = "org.nuxeo.dev";

    /**
     * Global testing property
     *
     * @since 5.6
     * @see #isTestModeSet()
     */
    public static final String NUXEO_TESTING_SYSTEM_PROP = "org.nuxeo.runtime.testing";

    /**
     * Global debug property
     *
     * @since 5.6
     * @see #isDebugModeSet()
     */
    public static final String NUXEO_DEBUG_SYSTEM_PROP = "org.nuxeo.debug";

    /**
     * The runtime instance.
     */
    private static RuntimeService runtime;

    private static ServiceManager serviceMgr;

    private static final ListenerList listeners = new ListenerList();

    private static final FileCleaningTracker fileCleaningTracker = new FileCleaningTracker();

    /**
     * A class loader used to share resources between all bundles.
     * <p>
     * This is useful to put resources outside any bundle (in a directory on
     * the file system) and then refer them from XML contributions.
     * <p>
     * The resource directory used by this loader is
     * ${nuxeo_data_dir}/resources whee ${nuxeo_data_dir} is usually
     * ${nuxeo_home}/data
     */
    protected static SharedResourceLoader resourceLoader;

    /**
     * Whether or not services should be exported as OSGI services. This is
     * controlled by the ${ecr.osgi.services} property. The default is false.
     */
    protected static Boolean isOSGiServiceSupported;

    // Utility class.
    private Framework() {
    }

    public static void initialize(RuntimeService runtimeService)
            throws Exception {
        if (runtime != null) {
            throw new Exception("Nuxeo Framework was already initialized");
        }
        runtime = runtimeService;
        reloadResourceLoader();
        initServiceManager();
        runtime.start();
    }

    public static void reloadResourceLoader() throws Exception {
        File rs = new File(Environment.getDefault().getData(), "resources");
        rs.mkdirs();
        resourceLoader = new SharedResourceLoader(
                new URL[] { rs.toURI().toURL() },
                Framework.class.getClassLoader());
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

    public static SharedResourceLoader getResourceLoader() {
        return resourceLoader;
    }

    private static void initServiceManager() {
        serviceMgr = org.nuxeo.runtime.api.ServiceManager.getInstance();
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
     */
    public static <T> T getService(Class<T> serviceClass) throws Exception {
        return serviceMgr.getService(serviceClass);
    }

    /**
     * Gets a service given its class and an identifier.
     */
    public static <T> T getService(Class<T> serviceClass, String name)
            throws Exception {
        return serviceMgr.getService(serviceClass, name);
    }

    /**
     * Gets a nuxeo-runtime local service.
     */
    public static <T> T getLocalService(Class<T> serviceClass) {
        ServiceProvider provider = DefaultServiceProvider.getProvider();
        if (provider != null) {
            return provider.getService(serviceClass);
        }
        // TODO impl a runtime service provider
        return runtime.getService(serviceClass);
    }

    /**
     * Lookup a registered object given its key.
     */
    public static Object lookup(String key) {
        return null; // TODO
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
     * privileges). The given username will be used to identify the user id
     * that called this method.
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
     * Login in the system as the given user without checking the password.
     *
     * @param username the user name to login as.
     * @return the login context
     * @throws LoginException if any error occurs
     * @since 5.4.2
     */
    public static LoginContext loginAsUser(String username)
            throws LoginException {
        return getLocalService(LoginAs.class).loginAs(username);
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
     * Login in the system using the given callback handler for login info
     * resolution.
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
     * @param listener the listener to remove
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
     * Gets all the framework properties. The system properties are not
     * included in the returned map.
     *
     * @return the framework properties map. Never returns null.
     */
    public static Properties getProperties() {
        return runtime.getProperties();
    }

    /**
     * Expands any variable found in the given expression with the value of the
     * corresponding framework property.
     * <p>
     * The variable format is ${property_key}.
     * <p>
     * System properties are also expanded.
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
            case '$':
                dollar = true;
                break;
            case '{':
                if (dollar) {
                    dollar = false;
                    var = true;
                } else {
                    result.append(c);
                }
                break;
            case '}':
                if (var) {
                    var = false;
                    String varName = varBuf.toString();
                    varBuf.setLength(0);
                    String varValue = getProperty(varName); // get the variable
                                                            // value
                    if (varValue != null) {
                        result.append(varValue);
                    } else { // let the variable as is
                        result.append("${").append(varName).append('}');
                    }
                } else {
                    result.append(c);
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

    public static boolean isOSGiServiceSupported() {
        if (isOSGiServiceSupported == null) {
            isOSGiServiceSupported = Boolean.valueOf(getProperty(
                    "ecr.osgi.services", "false"));
        }
        return isOSGiServiceSupported;
    }

    /**
     * Returns true id dev mode is set.
     * <p>
     * Activating this mode, the Runtime Framework will stop on low-level
     * errors, see {@link #handleDevError(Throwable)}
     */
    public static boolean isDevModeSet() {
        String dev = getProperty(NUXEO_DEV_SYSTEM_PROP);
        if (dev == null) {
            dev = System.getProperty(NUXEO_DEV_SYSTEM_PROP);
        }
        return Boolean.TRUE.equals(Boolean.valueOf(dev));
    }

    /**
     * Returns true if test mode is set.
     * <p>
     * Activating this mode, some of the code may not behave as it would in
     * production, to ease up testing.
     */
    public static boolean isTestModeSet() {
        String test = getProperty(NUXEO_TESTING_SYSTEM_PROP);
        if (test == null) {
            test = System.getProperty(NUXEO_TESTING_SYSTEM_PROP);
        }
        return Boolean.TRUE.equals(Boolean.valueOf(test));
    }

    /**
     * Returns true if debug mode is set.
     * <p>
     * Activating this mode, some of the code may not behave as it would in
     * production, to ease up debugging and working on developing the
     * application.
     * <p>
     * For instance, it'll enable hot-reload if some packages are installed
     * while the framework is running. It will also reset some caches when that
     * happens.
     *
     * @since 5.6
     */
    public static boolean isDebugModeSet() {
        String debug = getProperty(NUXEO_DEBUG_SYSTEM_PROP);
        if (debug == null) {
            debug = System.getProperty(NUXEO_DEBUG_SYSTEM_PROP);
        }
        return Boolean.TRUE.equals(Boolean.valueOf(debug));
    }

    /**
     * This method stops the application if development mode is enabled (i.e.
     * org.nuxeo.dev system property is set) and one of the following errors
     * occurs during startup:
     * <ul>
     * <li>Component XML parse error.
     * <li>Contribution to an unknown extension point.
     * <li>Component with an unknown implementation class (the implementation
     * entry exists in the XML descriptor but cannot be resolved to a class).
     * <li>Uncatched exception on extension registration / unregistration
     * (either in framework or user component code)
     * <li>Uncatched exception on component activation / desactivation (either
     * in framework or user component code)
     * <li>Broken Nuxeo-Component MANIFEST entry. (i.e. the entry cannot be
     * resolved to a resource)
     * </ul>
     *
     * @param t the exception or null if none
     */
    public static void handleDevError(Throwable t) {
        if (isDevModeSet()) {
            System.err.println("Fatal error caught in dev mode: exiting.");
            t.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Deletes the given file when the marker object is collected by GC.
     *
     * @param file The file to delete
     * @param marker the marker Object
     */
    public static void trackFile(File file, Object marker) {
        fileCleaningTracker.track(file, marker);
    }

    /**
     * Deletes the given file when the marker object is collected by GC. The
     * fileDeleteStrategy can be used for instance do delete only empty
     * directory or force deletion.
     *
     * @param file The file to delete
     * @param marker the marker Object
     * @param fileDeleteStrategy add a custom delete strategy
     */
    public static void trackFile(File file, Object marker,
            FileDeleteStrategy fileDeleteStrategy) {
        fileCleaningTracker.track(file, marker, fileDeleteStrategy);
    }

    public static void main(String[] args) {
    }

}
