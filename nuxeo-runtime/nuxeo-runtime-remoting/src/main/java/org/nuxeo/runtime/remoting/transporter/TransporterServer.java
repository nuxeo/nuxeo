/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.nuxeo.runtime.remoting.transporter;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.detection.Detector;
import org.jboss.remoting.detection.multicast.MulticastDetector;
import org.jboss.remoting.transport.Connector;

/**
 * Modified to be compatible with 2.0.0.GA - added addHandler() method - as in JBossRemoting 2.0.0.GA
 *
 * The remoting server to expose the target POJO. This should be called on as a
 * factory via static methods.
 *
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class TransporterServer {

    private static MBeanServer server;

    private static Detector detector;

    private final Connector connector;

    /**
     * Creates a remoting server using the provided locator and subsytem and
     * creating a TransporterHandler which takes the specified target object.
     *
     * @param locator
     * @param target
     * @param subsystem
     * @throws Exception
     */
    public TransporterServer(InvokerLocator locator, Object target,
            String subsystem) throws Exception {
        connector = new Connector();
        connector.setInvokerLocator(locator.getLocatorURI());
        connector.create();
        ServerInvocationHandler handler = new TransporterHandler(target);
        connector.addInvocationHandler(subsystem, handler);
    }

    public Connector getConnector() {
        return connector;
    }

    /**
     * Adds a transporter handler to receive remote invocations on the target
     * object passed.
     *
     * @param target
     *            the target implementation to call on
     * @param proxyclassname
     *            the fully qualified classname of the interface that clients
     *            will use to call on
     */
    public void addHandler(Object target, String proxyclassname)
            throws Exception {
        if (connector != null) {
            connector.addInvocationHandler(proxyclassname,
                    new TransporterHandler(target));
        } else {
            throw new Exception(
                    "Can not add handler to transporter server as has not be initialized yet.");
        }
    }

    public String getLocatorURI() {
        try {
            return connector.getInvokerLocator();
        } catch (Exception e) {
            throw new Error("Failed to get locator URI", e);
        }
    }

    /**
     * Starts the remoting server. This is called automatically upon any of the
     * static createTransporterServer() methods.
     *
     * @throws Exception
     */
    public void start() throws Exception {
        connector.start();
    }

    /**
     * Stops the remoting server. This must be called when no longer want to
     * expose the target POJO for remote method calls.
     */
    public void stop() {
        connector.stop();
    }

    /**
     * Creates a MBeanServer and MulticastDetector to start publishing detection
     * messages so other detectors will be aware this server is available.
     *
     * @throws Exception
     */
    private static void setupDetector() throws Exception {
        // we need an MBeanServer to store our network registry and multicast
        // detector services
        server = MBeanServerFactory.createMBeanServer();

        // multicast detector will detect new network registries that come
        // online
        detector = new MulticastDetector();
        server.registerMBean(detector, new ObjectName(
                "remoting:type=MulticastDetector"));
        detector.start();
    }

    /**
     * Creates a remoting server based on given locator. Will convert any remote
     * invocation requests into method calls on the given target object.
     *
     * @param locator -
     *            specifies what transport, host and port binding, etc. to use
     *            by the remoting server.
     * @param target -
     *            the target POJO to receive the method call upon getting remote
     *            invocation requests.
     * @param subsystem -
     *            the name under which to register the handler within the
     *            remoting server. <b>This must be the fully qualified name of
     *            the interface for clients to use a the remote proxy to the
     *            target POJO. Otherwise, clustering will not work, as this is
     *            the value used to identifiy remote POJOs on the client side.</b>
     *            If not clustered, this is not as critical, and simply use the
     *            fully qualified class name of the POJO if desired.
     * @param isClustered -
     *            true indicates that would like this server to be considered
     *            available for failover from clients calling on the same
     *            interface as exposed by the subsystem value. False will only
     *            allow those client that explicitly targeting this server to
     *            make calls on it.
     * @return TransporterServer. Note, it will already be started upon return.
     * @throws Exception
     */
    public static TransporterServer createTransporterServer(
            InvokerLocator locator, Object target, String subsystem,
            boolean isClustered) throws Exception {
        if (isClustered && detector == null) {
            setupDetector();
        }

        TransporterServer server = new TransporterServer(locator, target,
                subsystem);
        server.start();
        return server;
    }

    /**
     * Creates a remoting server based on given locator. Will convert any remote
     * invocation requests into method calls on the given target object.
     *
     * @param locatorURI -
     *            specifies what transport, host and port binding, etc. to use
     *            by the remoting server.
     * @param target -
     *            the target POJO to receive the method call upon getting remote
     *            invocation requests.
     * @param subsystem -
     *            the name under which to register the handler within the
     *            remoting server. <b>This must be the fully qualified name of
     *            the interface for clients to use a the remote proxy to the
     *            target POJO. Otherwise, clustering will not work, as this is
     *            the value used to identifiy remote POJOs on the client side.</b>
     *            If not clustered, this is not as critical, and simply use the
     *            fully qualified class name of the POJO if desired.
     * @param isClustered -
     *            true indicates that would like this server to be considered
     *            available for failover from clients calling on the same
     *            interface as exposed by the subsystem value. False will only
     *            allow those client that explicitly targeting this server to
     *            make calls on it.
     * @return TransporterServer. Note, it will already be started upon return.
     * @throws Exception
     */
    public static TransporterServer createTransporterServer(String locatorURI,
            Object target, String subsystem, boolean isClustered)
            throws Exception {
        return createTransporterServer(new InvokerLocator(locatorURI), target,
                subsystem, isClustered);
    }

    /**
     * Creates a remoting server based on given locator. Will convert any remote
     * invocation requests into method calls on the given target object.
     *
     * @param locator -
     *            specifies what transport, host and port binding, etc. to use
     *            by the remoting server.
     * @param target -
     *            the target POJO to receive the method call upon getting remote
     *            invocation requests.
     * @param subsystem -
     *            the name under which to register the handler within the
     *            remoting server. Can simply use the fully qualified class name
     *            of the POJO if desired.
     * @return TransporterServer. Note, it will already be started upon return.
     * @throws Exception
     */
    public static TransporterServer createTransporterServer(
            InvokerLocator locator, Object target, String subsystem)
            throws Exception {
        return createTransporterServer(locator, target, subsystem, false);
    }

    /**
     * Creates a remoting server based on given locator. Will convert any remote
     * invocation requests into method calls on the given target object.
     *
     * @param locatorURI -
     *            specifies what transport, host and port binding, etc. to use
     *            by the remoting server.
     * @param target -
     *            the target POJO to receive the method call upon getting remote
     *            invocation requests.
     * @param subsystem -
     *            the name under which to register the handler within the
     *            remoting server. Can simply use the fully qualified class name
     *            of the POJO if desired.
     * @return TransporterServer. Note, it will already be started upon return.
     * @throws Exception
     */
    public static TransporterServer createTransporterServer(String locatorURI,
            Object target, String subsystem) throws Exception {
        return createTransporterServer(new InvokerLocator(locatorURI), target,
                subsystem, false);
    }

    /**
     * Creates a remoting server based on given locator. Will convert any remote
     * invocation requests into method calls on the given target object.
     *
     * @param locator -
     *            specifies what transport, host and port binding, etc. to use
     *            by the remoting server.
     * @param target -
     *            the target POJO to receive the method call upon getting remote
     *            invocation requests.
     * @return TransporterServer. Note, it will already be started upon return.
     * @throws Exception
     */
    public static TransporterServer createTransporterServer(
            InvokerLocator locator, Object target) throws Exception {
        return createTransporterServer(locator, target, target.getClass()
                .getName());
    }

    /**
     * Creates a remoting server based on given locator. Will convert any remote
     * invocation requests into method calls on the given target object.
     *
     * @param locatorURI -
     *            specifies what transport, host and port binding, etc. to use
     *            by the remoting server.
     * @param target -
     *            the target POJO to receive the method call upon getting remote
     *            invocation requests.
     * @return TransporterServer. Note, it will already be started upon return.
     * @throws Exception
     */
    public static TransporterServer createTransporterServer(String locatorURI,
            Object target) throws Exception {
        return createTransporterServer(new InvokerLocator(locatorURI), target);
    }

}
