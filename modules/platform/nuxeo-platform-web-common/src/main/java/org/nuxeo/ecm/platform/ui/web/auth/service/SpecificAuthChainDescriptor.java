/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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

@XObject("specificAuthenticationChain")
public class SpecificAuthChainDescriptor {

    public static final boolean DEFAULT_HANDLE_PROMPT_VALUE = true;

    @XNode("@name")
    protected String name;

    @XNode("@handlePrompt")
    private boolean handlePrompt = DEFAULT_HANDLE_PROMPT_VALUE;

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
            List<Pattern> patterns = new ArrayList<>();
            for (String url : urls) {
                patterns.add(Pattern.compile(url));
            }
            urlPatterns = patterns;
        }
        return urlPatterns;
    }

    public Map<String, Pattern> getHeaderPatterns() {
        if (headerPatterns == null) {
            headerPatterns = new HashMap<>();
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

        List<String> filteredChain = new ArrayList<>();
        for (String pluginName : defaultChain) {
            if (allowedPlugins.contains(pluginName)) {
                filteredChain.add(pluginName);
            }
        }
        return filteredChain;
    }

    /**
     * Return if the auth filter has to handle prompt or return 401
     *
     * @since 8.2
     */
    public boolean doHandlePrompt() {
        return handlePrompt;
    }

}
