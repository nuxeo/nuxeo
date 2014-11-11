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

    private static final long serialVersionUID = 276543987313289764L;

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
        if (urlPatterns==null) {
            urlPatterns= new ArrayList<Pattern>();
            for (String url : urls) {
                urlPatterns.add(Pattern.compile(url));
            }
        }
        return urlPatterns;
    }

    public Map<String, Pattern> getHeaderPatterns() {
        if (headerPatterns==null) {
            headerPatterns = new HashMap<String, Pattern>();
            for (String headerName : headers.keySet()) {
                headerPatterns.put(headerName, Pattern.compile(headers.get(headerName)));
            }
        }
        return headerPatterns;
    }

    public List<String> computeResultingChain(List<String> defaultChain) {

        if (replacementChain!=null && replacementChain.size()>0) {
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
