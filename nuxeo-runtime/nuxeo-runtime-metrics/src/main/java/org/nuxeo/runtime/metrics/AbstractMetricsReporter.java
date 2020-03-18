/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Map;

import org.nuxeo.runtime.api.Framework;

/**
 * @since 11.1
 */
public abstract class AbstractMetricsReporter implements MetricsReporter {

    protected Map<String, String> options;

    protected long pollInterval;

    @Override
    public void init(long pollInterval, Map<String, String> options) {
        this.options = options;
        this.pollInterval = pollInterval;
    }

    protected String getCurrentHostname() {
        try {
            return InetAddress.getLocalHost().getHostName().split("\\.")[0];
        } catch (UnknownHostException e) {
            return "unknown";
        }
    }

    protected long getPollInterval() {
        return pollInterval;
    }

    protected String getHostnameFromNuxeoUrl() {
        try {
            String url = Framework.getProperty("nuxeo.url");
            if (isBlank(url)) {
                return "";
            }
            URI uri = new URI(url);
            String domain = uri.getHost();
            if (isBlank(domain)) {
                return "";
            }
            return domain.startsWith("www.") ? domain.substring(4) : domain;
        } catch (URISyntaxException e) {
            return "";
        }
    }

    protected String getOption(String name, String defaultValue) {
        String value = options.get(name);
        if (isBlank(value)) {
            return defaultValue;
        }
        return value;
    }

    protected String requireOption(String name) {
        return requireOption(name, null);
    }

    protected String requireOption(String name, String errorMessage) {
        String value = options.get(name);
        if (isBlank(value)) {
            throw new IllegalArgumentException("Metric Reporter configuration requires option: " + name
                    + (isBlank(errorMessage) ? "" : " " + errorMessage));
        }
        return value;
    }

    protected int getOptionAsInt(String name, int defaultValue) {
        String value = options.get(name);
        if (isBlank(value)) {
            return defaultValue;
        }
        return Integer.valueOf(value);
    }

    protected boolean getOptionAsBoolean(String name, boolean defaultValue) {
        String value = options.get(name);
        if (isBlank(value)) {
            return defaultValue;
        }
        return Boolean.valueOf(value);
    }

}
