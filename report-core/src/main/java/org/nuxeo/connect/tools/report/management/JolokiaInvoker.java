/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Stephane Lacoin at Nuxeo (aka matic)
 */
package org.nuxeo.connect.tools.report.management;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.management.JMException;
import javax.management.MalformedObjectNameException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jolokia.backend.BackendManager;
import org.jolokia.config.ConfigKey;
import org.jolokia.config.Configuration;
import org.jolokia.request.JmxRequest;
import org.jolokia.request.JmxRequestFactory;
import org.jolokia.util.LogHandler;
import org.jolokia.util.RequestType;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

/**
 *
 *
 * @since 8.3
 */
public class JolokiaInvoker implements MXComponent.Invoker {

    final Configuration config = new Configuration(ConfigKey.AGENT_ID, "false", ConfigKey.IGNORE_ERRORS, "true");

    final BackendManager manager = new BackendManager(config, new LogHandler() {

        final Log log = LogFactory.getLog(MXComponent.class);

        @Override
        public void info(String message) {
            log.info(message);
        }

        @Override
        public void error(String message, Throwable t) {
            log.error(message, t);
        }

        @Override
        public void debug(String message) {
            log.debug(message);
        }
    });

    @Override
    public void destroy() {
        manager.destroy();
    }

    class RequestBuilder {

        public RequestBuilder(RequestType oftype) {
            pRequestMap.put("type", oftype.getName());
        }

        @SuppressWarnings("unchecked")
        final Map<String, Object> pRequestMap = new JSONObject();

        RequestBuilder withMbean(String value) throws MalformedObjectNameException {
            pRequestMap.put("mbean", value);
            return this;
        }

        RequestBuilder withOperation(String value, Object... arguments) {
            pRequestMap.put("operation", value);
            pRequestMap.put("arguments", Arrays.asList(arguments));
            return this;
        }

        final Map<String, String> pParams = new HashMap<>();

        JmxRequest build() {
            return JmxRequestFactory.createPostRequest(pRequestMap, config.getProcessingParameters(pParams));
        }
    }

    JsonObject invoke(RequestBuilder builder)
            throws IOException, JMException {
        return toJsonObject(manager.handleRequest(builder.build()));
    }

    JsonObject invoke(RequestType type, Map<String, String> params)
            throws IOException, JMException {
        Map<String, String> pRequestMap = new HashMap<>();
        pRequestMap.put("type", type.getName());
        pRequestMap.putAll(params);
        Map<String, String> pParams = new HashMap<>();
        JmxRequest request = JmxRequestFactory.createPostRequest(pRequestMap, config.getProcessingParameters(pParams));
        return toJsonObject(manager.handleRequest(request));
    }

    @Override
    public JsonObject list()
            throws IOException, JMException {
        return invoke(new RequestBuilder(RequestType.LIST));
    }

    @Override
    public JsonObject search(String pattern)
            throws IOException, JMException {
        return invoke(new RequestBuilder(RequestType.SEARCH)
                .withMbean(pattern));
    }

    @Override
    public JsonObject read(String pattern)
            throws IOException, JMException {
        return invoke(new RequestBuilder(RequestType.READ)
                .withMbean(pattern));
    }

    @Override
    public JsonObject exec(String pattern, String operation, Object... arguments)
            throws IOException, JMException {
        return invoke(new RequestBuilder(RequestType.EXEC)
                .withMbean(pattern)
                .withOperation(operation, arguments));
    }

    JsonObject toJsonObject(JSONStreamAware json) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (Writer writer = new OutputStreamWriter(bytes)) {
            json.writeJSONString(writer);
        }
        JsonReader reader = Json.createReader(new ByteArrayInputStream(bytes.toByteArray()));
        return (JsonObject) reader.read();
    }

}
