/*
 * (C) Copyright 2006-2015 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     dmetzler
 */
package org.nuxeo.datadog.reporter;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.lang.StringUtils;
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
}
