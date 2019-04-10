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

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.management.JMException;

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
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Platform mbean server invoker which output results in json based on jolokia.
 *
 * @since 8.3
 */
public class MXComponent extends DefaultComponent {

    static MXComponent instance;

    public MXComponent() {
        instance = this;
    }

    // jolokia configuration
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

    /**
     * Jolokia request builder
     *
     *
     */
    class RequestBuilder {

        RequestBuilder(RequestType oftype) {
            pRequestMap.put("type", oftype.getName());
        }

        @SuppressWarnings("unchecked")
        final Map<String, Object> pRequestMap = new JSONObject();

        RequestBuilder withMbean(String value) {
            pRequestMap.put("mbean", value);
            return this;
        }

        RequestBuilder withOperation(String value, Object... arguments) {
            pRequestMap.put("operation", value);
            pRequestMap.put("arguments", Arrays.asList(arguments));
            return this;
        }

        final Map<String, String> pParams = new HashMap<>();

        void run(OutputStream sink) {
            try {
                JmxRequest request = JmxRequestFactory.createPostRequest(pRequestMap, config.getProcessingParameters(pParams));
                JSONObject json = manager.handleRequest(request);
                OutputStreamWriter writer = new OutputStreamWriter(sink);
                json.writeJSONString(writer);
                writer.flush();
            } catch (JMException | IOException cause) {
                throw new AssertionError("Cannot invoke jolokia", cause);
            }
        }

    }

    RequestBuilder list() {
        return new RequestBuilder(RequestType.LIST);
    }

    RequestBuilder search(String pattern) {
        return new RequestBuilder(RequestType.SEARCH)
                .withMbean(pattern);
    }

    RequestBuilder read(String pattern) {
        return new RequestBuilder(RequestType.READ)
                .withMbean(pattern);
    }

    RequestBuilder exec(String pattern, String operation, Object... arguments) {
        return new RequestBuilder(RequestType.EXEC)
                .withMbean(pattern)
                .withOperation(operation, arguments);
    }

}
