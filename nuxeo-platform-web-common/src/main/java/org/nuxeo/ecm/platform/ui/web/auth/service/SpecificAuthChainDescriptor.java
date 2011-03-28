/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.ui.web.auth.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject(value = "specificAuthenticationChain")
public class SpecificAuthChainDescriptor {

    @XNode(value="@name")
    protected String name;

    @XNodeList(value = "replacementChain/plugin", type = ArrayList.class, componentType = String.class)
    private List<String> replacementChain;

    public List<String> getReplacementChain() {
        return replacementChain;
    }

    @XNodeList(value = "allowedPlugins/plugin", type = ArrayList.class, componentType = String.class)
    private List<String> allowedPlugins;

    public List<String> getAllowedPlugins() {
        return allowedPlugins;
    }

    @XNodeList(value = "urlPatterns/url", type = ArrayList.class, componentType = String.class)
    private List<String> urls;

    private List<Pattern> urlPatterns;

    @XNodeMap(value = "headers/header", key = "@name", type = HashMap.class, componentType = String.class)
    private Map<String, String> headers;

    private Map<String, Pattern> headerPatterns;

    public List<Pattern> getUrlPatterns() {
        if (urlPatterns == null) {
            List<Pattern> patterns = new ArrayList<Pattern>();
            for (String url : urls) {
                patterns.add(Pattern.compile(url));
            }
            urlPatterns = patterns;
        }
        return urlPatterns;
    }

    public Map<String, Pattern> getHeaderPatterns() {
        if (headerPatterns == null) {
            headerPatterns = new HashMap<String, Pattern>();
            for (String headerName : headers.keySet()) {
                headerPatterns.put(headerName, Pattern.compile(headers.get(headerName)));
            }
        }
        return headerPatterns;
    }

    public List<String> computeResultingChain(List<String> defaultChain) {
        if (replacementChain != null && !replacementChain.isEmpty()) {
            return replacementChain;
        }

        List<String> filtredChain = new ArrayList<String>();
        for (String pluginName : defaultChain) {
            if (allowedPlugins.contains(pluginName)) {
                filtredChain.add(pluginName);
            }
        }
        return filtredChain;
    }

}
