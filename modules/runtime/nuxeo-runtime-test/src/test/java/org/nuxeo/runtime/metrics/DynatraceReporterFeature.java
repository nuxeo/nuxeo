/*
 * (C) Copyright 2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.runtime.metrics;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.runtime.metrics.reporter.patch.NuxeoDynatraceReporter;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;

import org.junit.runners.model.FrameworkMethod;

import io.dropwizard.metrics5.MetricAttribute;
import io.dropwizard.metrics5.MetricFilter;
import io.dropwizard.metrics5.MetricRegistry;

/**
 * Nuxeo test feature that provides a UDP server to capture Dynatrace metrics and a metric registry.
 * <p>
 * Use with {@code @RunWith(FeaturesRunner.class)} and {@code @Features(DynatraceReporterFeature.class)}, then
 * {@code @Inject DynatraceReporterFeature feature} in your test.
 *
 * @since 2025.15
 */
public class DynatraceReporterFeature implements RunnerFeature {

    private MetricRegistry registry;

    private DatagramSocket serverSocket;

    private int serverPort;

    private List<String> receivedMessages;

    @Override
    public void start(FeaturesRunner runner) throws Exception {
        registry = new MetricRegistry();
        receivedMessages = Collections.synchronizedList(new ArrayList<>());
        serverSocket = new DatagramSocket(0);
        serverPort = serverSocket.getLocalPort();
        serverSocket.setSoTimeout(100);
    }

    @Override
    public void stop(FeaturesRunner runner) throws Exception {
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }
    }

    @Override
    public void beforeSetup(FeaturesRunner runner, FrameworkMethod method, Object test) throws Exception {
        receivedMessages.clear();
        registry.getNames().forEach(registry::remove);
    }

    public MetricRegistry getRegistry() {
        return registry;
    }

    public int getServerPort() {
        return serverPort;
    }

    public List<String> getReceivedMessages() {
        return receivedMessages;
    }

    /**
     * Receives one batch of messages from the UDP socket and appends each non-empty line to
     * {@link #getReceivedMessages()}.
     */
    public void receiveMessages() throws IOException {
        byte[] buffer = new byte[8192];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        try {
            serverSocket.receive(packet);
            String message = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
            for (String line : message.split("\n")) {
                if (!line.isEmpty()) {
                    receivedMessages.add(line);
                }
            }
        } catch (SocketTimeoutException e) {
            // No more messages
        }
    }

    /**
     * Creates a reporter with default test settings (localhost, testhost, nuxeo prefix, no dimensions).
     */
    public NuxeoDynatraceReporter createReporter() {
        return NuxeoDynatraceReporter.builder(registry)
                                     .withHost("localhost")
                                     .withPort(serverPort)
                                     .withPrefix("nuxeo")
                                     .withHostname("testhost")
                                     .withDimensions(Collections.emptyMap())
                                     .withFilter(MetricFilter.ALL)
                                     .withDeniedExpansions(Collections.emptySet())
                                     .build();
    }

    /**
     * Creates a reporter with custom dimensions.
     */
    public NuxeoDynatraceReporter createReporter(Map<String, String> dimensions) {
        return NuxeoDynatraceReporter.builder(registry)
                                     .withHost("localhost")
                                     .withPort(serverPort)
                                     .withPrefix("nuxeo")
                                     .withHostname("testhost")
                                     .withDimensions(dimensions != null ? dimensions : Collections.emptyMap())
                                     .withFilter(MetricFilter.ALL)
                                     .withDeniedExpansions(Collections.emptySet())
                                     .build();
    }

    /**
     * Creates a reporter with custom options (e.g. denied expansions, emptyTimerAsCount).
     */
    public NuxeoDynatraceReporter createReporter(Map<String, String> dimensions, Set<MetricAttribute> deniedExpansions,
            boolean emptyTimerAsCount) {
        return NuxeoDynatraceReporter.builder(registry)
                                     .withHost("localhost")
                                     .withPort(serverPort)
                                     .withPrefix("nuxeo")
                                     .withHostname("testhost")
                                     .withDimensions(dimensions != null ? dimensions : Collections.emptyMap())
                                     .withFilter(MetricFilter.ALL)
                                     .withDeniedExpansions(deniedExpansions != null ? deniedExpansions : Collections.emptySet())
                                     .withEmptyTimerAsCount(emptyTimerAsCount)
                                     .build();
    }
}
