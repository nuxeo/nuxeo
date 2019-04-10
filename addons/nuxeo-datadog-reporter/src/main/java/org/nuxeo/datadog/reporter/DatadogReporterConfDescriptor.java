/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     dmetzler
 */
package org.nuxeo.datadog.reporter;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.api.Framework;

@XObject("configuration")
public class DatadogReporterConfDescriptor {

    @XNode("apiKey")
    String apiKey;

    @XNode("pollInterval")
    int pollInterval;

    @XNode("host")
    String host;

    @XNode("tags")
    String tags;

    @XNode("filter")
    FilterDescriptor filter = new FilterDescriptor();

    public long getPollInterval() {
        return pollInterval;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getHost() {
        if (StringUtils.isNotBlank(host)) {
            return host;
        } else {
            return computeHostFromNuxeoUrl();
        }
    }

    private String computeHostFromNuxeoUrl() {
        try {
            String url = Framework.getProperty("nuxeo.url");
            if (StringUtils.isBlank(url)) {
                return "";
            }

            URI uri = new URI(url);

            String domain = uri.getHost();
            if (StringUtils.isBlank(domain)) {
                return "";
            }

            return domain.startsWith("www.") ? domain.substring(4) : domain;

        } catch (URISyntaxException e) {
            return "";
        }
    }

    public List<String> getTags() {
        if (StringUtils.isBlank(tags)) {
            return Collections.emptyList();
        } else {
            List<String> result = new ArrayList<>();

            for (String tag : Arrays.asList(tags.split(","))) {
                result.add(tag.trim());
            }
            return result;
        }
    }
}
