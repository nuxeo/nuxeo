/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.runtime.jboss.util;

import java.lang.reflect.Proxy;
import java.util.Hashtable;
import java.util.Map;

import javax.management.DynamicMBean;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.jboss.mx.util.JMXInvocationHandler;
import org.jboss.mx.util.MBeanProxyCreationException;
import org.jboss.mx.util.MBeanProxyExt;
import org.jboss.mx.util.MBeanServerLocator;
import org.jboss.mx.util.ProxyContext;
import org.jboss.system.ServiceControllerMBean;
import org.jboss.system.server.ServerConfig;
import org.jboss.system.server.ServerConfigLocator;

/**
 * Helper class to locate MBean services.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public final class ServiceLocator {

    // the current jboss server
    private static MBeanServer jboss;
    private static final Map<String, ObjectName> nameCache = new Hashtable<String, ObjectName>();

    // by default use the class loader of this class (to avoid isolation class loader problems)
    private static ClassLoader classLoader = ServiceLocator.class.getClassLoader();


    // Utility class
    private ServiceLocator() {
    }

    public static void setClassLoader(ClassLoader cl) {
        classLoader = cl;
    }

    public static ClassLoader getClassLoader() {
        return classLoader != null ? classLoader : Thread.currentThread().getContextClassLoader();
    }

    /**
     * Gets the JBoss MBeanServer.
     *
     * @return the MBean JBoss server
     */
    public static MBeanServer getJBoss() {
        if (jboss == null) {
            jboss = MBeanServerLocator.locateJBoss();
        }
        return jboss;
    }

    public static ServiceControllerMBean getServiceController() {
        return (ServiceControllerMBean) MBeanProxyExt
                .create(ServiceControllerMBean.class,
                        ServiceControllerMBean.OBJECT_NAME, getJBoss());
    }

    /**
     * Locates and return the named MBean service.
     *
     * @param name the service name
     * @return A proxy to the named Mbean
     * @throws MBeanProxyCreationException
     */
    public static Object getService(ObjectName name) throws MBeanProxyCreationException {
        return get(name, getJBoss());
    }

    public static Object getService(String name)
            throws MBeanProxyCreationException, MalformedObjectNameException {
        ObjectName oName = nameCache.get(name);
        if (oName == null) {
            oName = new ObjectName(name);
            nameCache.put(name, oName);
        }
        return get(oName, getJBoss());
    }

    public static Object getService(Class itf, String name)
            throws MBeanProxyCreationException, MalformedObjectNameException {
        ObjectName oName = nameCache.get(name);
        if (oName == null) {
            oName = new ObjectName(name);
            nameCache.put(name, oName);
        }
        return get(itf, oName, getJBoss());
    }

    /**
     * Locates the given service and return it as the given interface.
     *
     * @param itf the interface
     * @param name the MBean name
     * @return a proxy to the named MBean implementing the given interface
     * @throws MBeanProxyCreationException
     */
    public static Object getService(Class itf, ObjectName name) throws MBeanProxyCreationException {
        return get(itf, name, getJBoss());
    }

    /**
     * Gets the JBoss configuration service.
     *
     * @return the jboss config service
     */
    public static ServerConfig getJBossConfiguration() {
        return ServerConfigLocator.locate();
    }


    /**
     * The following methods are copied from JBoss org.jboss.mx.util.MBeanProxy
     * We cannot use directly this class because of class loader issues when running in an isolated env.
     */


    // Static --------------------------------------------------------

    /**
     * Creates a proxy to an MBean in the given MBean server.
     *
     * @param   intrface    the interface this proxy implements
     * @param   name        object name of the MBean this proxy connects to
     * @param   agentID     agent ID of the MBean server this proxy connects to
     *
     * @return  proxy instance
     *
     * @throws MBeanProxyCreationException if the proxy could not be created
     */
    public static Object get(Class intrface, ObjectName name, String agentID)
            throws MBeanProxyCreationException {
        return get(
                intrface,
                name,
                (MBeanServer) MBeanServerFactory.findMBeanServer(agentID).get(0));
    }

    /**
     * Creates a proxy to an MBean in the given MBean server.
     *
     * @param   intrface the interface this proxy implements
     * @param   name     object name of the MBean this proxy connects to
     * @param   server   MBean server this proxy connects to
     *
     * @return proxy instance
     *
     * @throws MBeanProxyCreationException if the proxy could not be created
     */
    public static Object get(Class intrface, ObjectName name, MBeanServer server)
            throws MBeanProxyCreationException {
        return get(new Class[] { intrface, ProxyContext.class,
                DynamicMBean.class }, name, server);
    }

    public static Object get(ObjectName name, MBeanServer server)
            throws MBeanProxyCreationException {
        return get(new Class[] { ProxyContext.class, DynamicMBean.class },
                name, server);
    }

    private static Object get(Class[] interfaces, ObjectName name,
            MBeanServer server) throws MBeanProxyCreationException {
        return Proxy.newProxyInstance(getClassLoader(), interfaces,
                new JMXInvocationHandler(server, name));
    }

    /**
     * Convenience method for registering an MBean and retrieving a proxy for
     * it.
     *
     * @param instance MBean instance to be registered
     * @param intrface the interface this proxy implements
     * @param name object name of the MBean
     * @param agentID agent ID of the MBean server this proxy connects to
     *
     * @return proxy instance
     *
     * @throws MBeanProxyCreationException if the proxy could not be created
     */
    public static Object create(Class instance, Class intrface,
            ObjectName name, String agentID) throws MBeanProxyCreationException {
        return create(
                instance,
                intrface,
                name,
                (MBeanServer) MBeanServerFactory.findMBeanServer(agentID).get(0));
    }

    /**
     * Convenience method for registering an MBean and retrieving a proxy for it.
     *
     * @param   instance MBean instance to be registered
     * @param   intrface the interface this proxy implements
     * @param   name     object name of the MBean
     * @param   server   MBean server this proxy connects to
     *
     * @throws MBeanProxyCreationException if the proxy could not be created
     */
    public static Object create(Class instance, Class intrface,
            ObjectName name, MBeanServer server)
            throws MBeanProxyCreationException {
        try {
            server.createMBean(instance.getName(), name);
            return get(intrface, name, server);
        } catch (ReflectionException e) {
            throw new MBeanProxyCreationException("Creating the MBean failed: "
                    + e.toString());
        } catch (InstanceAlreadyExistsException e) {
            throw new MBeanProxyCreationException("Instance already exists: "
                    + name);
        } catch (MBeanRegistrationException e) {
            throw new MBeanProxyCreationException(
                    "Error registering the MBean to the server: "
                            + e.toString());
        } catch (MBeanException e) {
            throw new MBeanProxyCreationException(e.toString());
        } catch (NotCompliantMBeanException e) {
            throw new MBeanProxyCreationException("Not a compliant MBean "
                    + instance.getClass().getName() + ": " + e.toString());
        }
    }

}
