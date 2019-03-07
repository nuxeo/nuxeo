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
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParsingException;
import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.commons.logging.LogFactory;
import org.nuxeo.connect.tools.report.ReportServer;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

/**
 * @since 8.3
 */
public class ReportConnector {

    static final ObjectName NAME = name();

    static ObjectName name() {
        try {
            return new ObjectName("org.nuxeo:type=service,name=connect-report");
        } catch (MalformedObjectNameException cause) {
            throw new AssertionError("Cannot name report", cause);
        }
    }

    public static ReportConnector of() {
        return new ReportConnector();
    }

    public JsonGenerator feed(JsonGenerator generator) throws IOException, InterruptedException, ExecutionException {
        class Feeder implements Consumer {
            StreamFeeder feeder = new StreamFeeder();

            @Override
            public void consume(JsonParser stream) {
                feeder.feed(generator, stream);
            }
        }
        connect(new Feeder());
        return generator;
    }

    public JsonObjectBuilder feed(JsonObjectBuilder builder)
            throws IOException, InterruptedException, ExecutionException {
        class Feeder implements Consumer {
            ObjectFeeder feeder = new ObjectFeeder();

            @Override
            public void consume(JsonParser stream) {
                feeder.feed(builder, stream);
            }
        }
        connect(new Feeder());
        return builder;
    }

    static class Discovery implements Iterable<ReportServer> {
        @Override
        public Iterator<ReportServer> iterator() {
            return new Iterator<ReportServer>() {

                final Iterator<VirtualMachineDescriptor> source = VirtualMachine.list().iterator();

                ReportServer next = fetchNext();

                ReportServer fetchNext() {
                    if (!source.hasNext()) {
                        return null;
                    }
                    while (source.hasNext()) {
                        VirtualMachineDescriptor pid = source.next();
                        try {
                            MBeanServerConnection connection = new Management(pid).connect();
                            if (!connection.isRegistered(NAME)) {
                                continue;
                            }
                            return JMX.newMXBeanProxy(connection, NAME, ReportServer.class);
                        } catch (IOException cause) {
                            LogFactory.getLog(Discovery.class).error("Cannot connect to " + pid, cause);
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
                        boolean isRemote = Boolean.parseBoolean(
                                vm.getSystemProperties().getProperty("com.sun.management.jmxremote", "false"));
                        if (!isRemote) {
                            return null;
                        }
                        int port = Integer.parseInt(
                                vm.getSystemProperties().getProperty("com.sun.management.jmxremote.port", "1089"));
                        return new JMXServiceURL(
                                String.format("service:jmx:rmi:///jndi/rmi://localhost:%d/jmxrmi", port));
                    }

                    JMXServiceURL lookupAgent(VirtualMachine vm) throws IOException {
                        String address = vm.getAgentProperties()
                                           .getProperty("com.sun.management.jmxremote.localConnectorAddress");
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

                        String agent = home + File.separator + "jre" + File.separator + "lib" + File.separator
                                + "management-agent.jar";
                        File f = new File(agent);
                        if (!f.exists()) {
                            agent = home + File.separator + "lib" + File.separator + "management-agent.jar";
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
                public ReportServer next() {
                    try {
                        return next;
                    } finally {
                        next = fetchNext();
                    }
                }

            };
        }
    }

    interface Consumer {
        void consume(JsonParser stream);
    }

    void connect(Consumer consumer) throws IOException, InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newCachedThreadPool(target -> {
            Thread thread = new Thread(target, "connect-report");
            thread.setDaemon(true);
            return thread;
        });
        try {
            for (ReportServer server : new Discovery()) {
                try (ServerSocket callback = new ServerSocket(0)) {
                    final Future<?> consumed = executor.submit(() -> {

                        String name = Thread.currentThread().getName();
                        Thread.currentThread().setName("connect-report-consumer-" + server);
                        try (InputStream source = callback.accept().getInputStream()) {
                            consumer.consume(Json.createParser(source));
                        } catch (IOException | JsonParsingException cause) {
                            throw new AssertionError("Cannot consume connect report", cause);
                        } finally {
                            Thread.currentThread().setName(name);
                        }
                        LogFactory.getLog(ReportConnector.class).info("Consumed " + server);
                    });
                    final Future<?> served = executor.submit(() -> {
                        String name = Thread.currentThread().getName();
                        Thread.currentThread().setName("connect-report-server-" + server);
                        InetSocketAddress address = (InetSocketAddress) callback.getLocalSocketAddress();
                        try {
                            server.run(address.getHostName(), address.getPort());
                        } catch (IOException cause) {
                            throw new AssertionError("Cannot run connect report", cause);
                        } finally {
                            Thread.currentThread().setName(name);
                        }
                    });
                    ExecutionException consumerError = null;
                    try {
                        consumed.get();
                    } catch (ExecutionException cause) {
                        consumerError = cause;
                    }
                    try {
                        served.get();
                    } catch (ExecutionException cause) {
                        if (consumerError != null) {
                            consumerError.addSuppressed(cause);
                            throw consumerError;
                        }
                        throw cause;
                    }
                }
            }
        } finally {
            executor.shutdownNow();
        }
    }

    public Iterable<ReportServer> discover() {
        class ToolsRunner {

            @SuppressWarnings("unchecked")
            Iterable<ReportServer> discover() {
                try {
                    ReportConnector.class.getClassLoader().loadClass("com.sun.tools.attach.VirtualMachine");
                } catch (ClassNotFoundException cause) {
                    class Loader extends URLClassLoader {
                        Loader(Path path) {
                            super(new URL[] { fileof(path) });
                        }

                        @Override
                        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                            if (name.equals(Discovery.class.getName())) {
                                return findClass(name);
                            }
                            return super.loadClass(name, resolve);
                        }
                    }
                    ClassLoader previous = Thread.currentThread().getContextClassLoader();
                    ClassLoader loader = new Loader(findTools());
                    Thread.currentThread().setContextClassLoader(loader);
                    try {
                        return (Iterable<ReportServer>) loader.loadClass(Discovery.class.getName()).newInstance();
                    } catch (ReflectiveOperationException cause1) {
                        throw new AssertionError("Cannot discover servers", cause1);
                    } finally {
                        Thread.currentThread().setContextClassLoader(previous);
                    }
                }
                return new Discovery();
            }

            URL fileof(Path path) {
                try {
                    return new URL("file://".concat(path.toString()));
                } catch (MalformedURLException cause) {
                    throw new AssertionError("Cannot create url for " + path, cause);
                }
            }

            Path findTools() {
                Path home = Paths.get(System.getProperty("java.home"));
                for (Path path : new Path[] { Paths.get("../lib/tools.jar"), Paths.get("../Classes/classes.jar") }) {
                    Path tools = home.resolve(path);
                    if (Files.exists(tools)) {
                        return tools;
                    }
                }
                throw new AssertionError("Cannot find tools in system");
            }

        }
        try {
            return new ToolsRunner().discover();
        } catch (Exception cause) {
            throw new AssertionError("Cannot discover servers", cause);
        }
    }

}
