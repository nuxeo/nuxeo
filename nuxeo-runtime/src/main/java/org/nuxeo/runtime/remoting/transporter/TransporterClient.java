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

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.remoting.CannotConnectException;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.detection.Detector;
import org.jboss.remoting.detection.multicast.MulticastDetector;
import org.jboss.remoting.invocation.NameBasedInvocation;
import org.jboss.remoting.network.NetworkRegistry;

/**
 * Class to be used as a factory via static method calls to get remote proxy to
 * POJO that exists within a external process. Note, if using clustered, will
 * use the multicast detector by default.
 * <p>
 * bs@nuxeo.com - Fixed bugs related to client subsystem initialization
 *
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class TransporterClient implements InvocationHandler, Serializable {

    private static final long serialVersionUID = 3894857374386677211L;

    private static final Log log = LogFactory.getLog(TransporterClient.class);

    // detector variables (only needed when clustering)
    private static MBeanServer server;

    private static Detector detector;

    private static NetworkRegistry registry;

    private final Client remotingClient;

//    private boolean isClustered = false;

    private final String subSystem;

    /**
     * Creates the remoting client to server POJO. Is clustered.
     */
    private TransporterClient(InvokerLocator locator, String targetSubsystem)
            throws Exception {
        remotingClient = new Client(locator, targetSubsystem);
        //this.isClustered = true;
        subSystem = targetSubsystem;
        remotingClient.connect();
    }

    /**
     * Disconnects the remoting client.
     */
    private void disconnect() {
        if (remotingClient != null) {
            remotingClient.disconnect();
        }
    }

    /**
     * Will set up network registry and detector for clustering (to identify
     * other remoting servers running on network).
     */
    private static void setupDetector() throws Exception {
        server = MBeanServerFactory.createMBeanServer();

        // the registry will house all remoting servers discovered
        registry = NetworkRegistry.getInstance();
        server.registerMBean(registry, new ObjectName(
                "remoting:type=NetworkRegistry"));

        // multicast detector will detect new network registries that come
        // online
        detector = new MulticastDetector();
        server.registerMBean(detector,
                new ObjectName("remoting:type=MulticastDetector"));
        detector.start();
    }

    /**
     * Creates a remote proxy to a POJO on a remote server.
     *
     * @param locatorURI
     *            the remoting locator URI to the target server where the target
     *            POJO exists.
     * @param targetClass
     *            the interface class of the POJO will be calling upon.
     * @param clustered
     *            true will cause the transporter to look for other remoting
     *            serves that have the POJO running and include it in the
     *            client's target list. If a call on first target fails, will
     *            seamlessly failover to one of the other discovered targets.
     * @return dynamic remote proxy typed to the interface specified by the
     *         targetClass param
     * @throws Exception
     */
    public static Object createTransporterClient(String locatorURI,
            Class targetClass, boolean clustered) throws Exception {
        if (!clustered) {
            return createTransporterClient(locatorURI, targetClass);
        } else {
            if (registry == null) {
                setupDetector();
            }
            InvokerLocator locator = new InvokerLocator(locatorURI);
            TransporterClient client = new TransporterClient(locator,
                    targetClass.getName());
            return Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                    new Class[] { targetClass }, client);
        }
    }

    /**
     * Creates a remote proxy to a POJO on a remote server.
     *
     * @param locatorURI
     *            the remoting locator URI to the target server where the target
     *            POJO exists
     * @param targetClass
     *            the interface class of the POJO will be calling upon.
     * @return dynamic remote proxy typed to the interface specified by the
     *         targetClass parameter
     * @throws Exception
     */
    public static Object createTransporterClient(String locatorURI,
            Class targetClass) throws Exception {
        InvokerLocator locator = new InvokerLocator(locatorURI);
        return createTransporterClient(locator, targetClass);
    }

    /**
     * Creates a remote proxy to a POJO on a remote server.
     *
     * @param locator
     *            the remoting locator to the target server where the target
     *            POJO exists
     * @param targetClass
     *            the interface class of the POJO will be calling upon.
     * @return dynamic remote proxy typed to the interface specified by the
     *         targetClass parameter
     * @throws Exception
     */
    public static Object createTransporterClient(InvokerLocator locator,
            Class targetClass) throws Exception {
        TransporterClient client = new TransporterClient(locator, targetClass.getName());
        return Proxy.newProxyInstance(Thread.currentThread()
                .getContextClassLoader(), new Class[] { targetClass }, client);
    }

    /**
     * Needs to be called by user when no longer need to make calls on remote
     * POJO. Otherwise will maintain remote connection until this is called.
     */
    public static void destroyTransporterClient(Object transporterClient) {
        if (transporterClient instanceof Proxy) {
            InvocationHandler handler = Proxy
                    .getInvocationHandler(transporterClient);
            if (handler instanceof TransporterClient) {
                TransporterClient client = (TransporterClient) handler;
                client.disconnect();
            } else {
                throw new IllegalArgumentException(
                        "Object is not a transporter client.");
            }
        } else {
            throw new IllegalArgumentException(
                    "Object is not a transporter client.");
        }
    }

    /**
     * The method called when anyone calls on the dynamic proxy returned by
     * getProcessor(). This method will simply convert the proxy call info into
     * a remoting invocation on the target remoting server (using a
     * NameBaseInvocation).
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable {
        String methodName = method.getName();
        String[] paramSig = createParamSignature(method.getParameterTypes());

        NameBasedInvocation request = new NameBasedInvocation(methodName, args,
                paramSig);
        Object response = null;

        boolean failOver = false;

        do {
            try {
                failOver = false;
                response = remotingClient.invoke(request);
            } catch (CannotConnectException cnc) {
                //failOver = findAlternativeTarget();
                if (!failOver) {
                    throw cnc;
                }
            } catch (InvocationTargetException itex) {
                throw itex.getCause();
            }
        } while (failOver);

        return response;
    }

    /**
     * Will check to see if the network registry has found any other remoting
     * servers. Then will check to see if any of them contain the subsystem we
     * are interested in (which will correspond to the proxy type we are using).
     * If one is found, will try to create a remoting client and connect to it.
     * If can't find one, will return false.
     */
//    private boolean findAlternativeTarget() {
//        boolean failover = false;
//
//        if (registry != null) {
//            NetworkInstance[] instances = registry.getServers();
//            if (instances != null) {
//                for (int x = 0; x < instances.length; x++) {
//                    NetworkInstance netInstance = instances[x];
//                    ServerInvokerMetadata[] metadata = netInstance
//                            .getServerInvokers();
//                    for (int i = 0; i < metadata.length; i++) {
//                        ServerInvokerMetadata data = metadata[i];
//                        String[] subsystems = data.getSubSystems();
//                        for (int z = 0; z < subsystems.length; z++) {
//                            if (subSystem.equalsIgnoreCase(subsystems[z])) {
//                                // finally found server with target handler
//                                InvokerLocator newLocator = data
//                                        .getInvokerLocator();
//                                if (!remotingClient.getInvoker().getLocator()
//                                        .equals(newLocator)) {
//                                    try {
//                                        remotingClient = new Client(newLocator);
//                                        remotingClient.connect();
//                                        return true;
//                                    } catch (Exception e) {
//                                        log
//                                                .warn(
//                                                        "Problem connecting to newly found alternate target.",
//                                                        e);
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
//        return failover;
//
//    }

    /**
     * Converts the Class array supplied via the dynamic proxy to a String array
     * of the respective class names, which is need by the NameBasedInvocation
     * object.
     */
    private static String[] createParamSignature(Class[] args) {
        if (args == null || args.length == 0) {
            return new String[] {};
        }
        String[] paramSig = new String[args.length];
        for (int x = 0; x < args.length; x++) {
            paramSig[x] = args[x].getName();
        }
        return paramSig;
    }

}
