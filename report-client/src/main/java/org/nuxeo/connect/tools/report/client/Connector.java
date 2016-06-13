/*
 * (C) Copyright 2012-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.connect.tools.report.client;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

/**
 *
 * @since 8.3
 */
public class Connector {

    static final ObjectName NAME = name();

    static ObjectName name() {
        try {
            return new ObjectName("org.nuxeo:type=service,name=connect-report");
        } catch (MalformedObjectNameException cause) {
            throw new AssertionError("Cannot name report", cause);
        }
    }

    public static Connector connector() {
        return new Connector();
    }

    public Iterable<Provider> connect() {
        return connect(VirtualMachine.list());
    }

    Iterable<Provider> connect(Iterable<VirtualMachineDescriptor> pids) {
        return new Iterable<Provider>() {

            @Override
            public Iterator<Provider> iterator() {
                return new Iterator<Provider>() {

                    final Iterator<VirtualMachineDescriptor> source = pids.iterator();

                    Provider next = fetchNext();

                    Provider fetchNext() {
                        if (!source.hasNext()) {
                            return null;
                        }
                        while (source.hasNext()) {
                            try {
                                VirtualMachineDescriptor pid = source.next();
                                MBeanServerConnection connection = new Management(pid).connect();
                                if (!connection.isRegistered(NAME)) {
                                    continue;
                                }
                                return JMX.newMXBeanProxy(connection, NAME, Provider.class);
                            } catch (IOException cause) {
                                ;
                            }
                        }
                        return null;
                    }

                    class Management {

                        Management(VirtualMachineDescriptor anIdentifier) {
                            pid = anIdentifier;
                        }

                        final VirtualMachineDescriptor pid;

                        MBeanServerConnection connect() throws IOException {
                            VirtualMachine vm;
                            try {
                                vm = pid.provider().attachVirtualMachine(pid);
                            } catch (AttachNotSupportedException cause) {
                                throw new IOException("Cannot attach to " + pid, cause);
                            }
                            try {
                                return connect(lookup(vm));
                            } finally {
                                vm.detach();
                            }
                        }

                        JMXServiceURL lookup(VirtualMachine vm) throws IOException {
                            JMXServiceURL url = lookupRemote(vm);
                            if (url != null) {
                                return url;
                            }
                            return lookupAgent(vm);
                        }

                        JMXServiceURL lookupRemote(VirtualMachine vm) throws IOException {
                            boolean isRemote =
                                    Boolean.valueOf(vm.getSystemProperties().getProperty("com.sun.management.jmxremote", "false")).booleanValue();
                            if (!isRemote) {
                                return null;
                            }
                            int port = Integer.valueOf(vm.getSystemProperties().getProperty("com.sun.management.jmxremote.port", "1089")).intValue();
                            return new JMXServiceURL(String.format("service:jmx:rmi:///jndi/rmi://localhost:%d/jmxrmi", port));
                        }

                        JMXServiceURL lookupAgent(VirtualMachine vm) throws IOException {
                            String address = vm.getAgentProperties().getProperty("com.sun.management.jmxremote.localConnectorAddress");
                            if (address != null) {
                                return new JMXServiceURL(address);
                            }
                            startAgent(vm);
                            return lookupAgent(vm);
                        }

                        void startAgent(VirtualMachine vm) throws IOException {
                            String home = vm.getSystemProperties().getProperty("java.home");

                            // Normally in
                            // ${java.home}/jre/lib/management-agent.jar but
                            // might
                            // be in ${java.home}/lib in build environments.

                            String agent = home + File.separator + "jre" + File.separator +
                                    "lib" + File.separator + "management-agent.jar";
                            File f = new File(agent);
                            if (!f.exists()) {
                                agent = home + File.separator + "lib" + File.separator +
                                        "management-agent.jar";
                                f = new File(agent);
                                if (!f.exists()) {
                                    throw new IOException("Management agent not found");
                                }
                            }

                            agent = f.getCanonicalPath();
                            try {
                                vm.loadAgent(agent, "com.sun.management.jmxremote");
                            } catch (AgentLoadException x) {
                                IOException ioe = new IOException(x.getMessage());
                                ioe.initCause(x);
                                throw ioe;
                            } catch (AgentInitializationException x) {
                                IOException ioe = new IOException(x.getMessage());
                                ioe.initCause(x);
                                throw ioe;
                            }
                        }

                        MBeanServerConnection connect(JMXServiceURL url) throws IOException {
                            return JMXConnectorFactory.connect(url).getMBeanServerConnection();
                        }
                    }

                    @Override
                    public boolean hasNext() {
                        return next != null;
                    }

                    @Override
                    public Provider next() {
                        try {
                            return next;
                        } finally {
                            next = fetchNext();
                        }
                    }

                };
            }

        };
    }
}
