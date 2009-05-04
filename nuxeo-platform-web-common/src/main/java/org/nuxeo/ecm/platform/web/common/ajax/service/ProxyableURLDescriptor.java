package org.nuxeo.ecm.platform.web.common.ajax.service;

import java.util.regex.Pattern;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 *
 * Simple Descriptor for a proxyable URL config
 *
 * @author tiry
 *
 */
@XObject(value = "proxyableURL")
public class ProxyableURLDescriptor {

    private static final long serialVersionUID = 1L;

    @XNode("@name")
    protected String name;

    @XNode("@enabled")
    protected boolean enabled = true;

    @XNode("@userCache")
    protected boolean useCache=false;

    @XNode("@cachePerSession")
    protected boolean cachePerSession=false;

    @XNode("pattern")
    protected String pattern;

    protected Pattern compiledPattern;

    public ProxyableURLDescriptor() {
    }

    public String getName() {
        if (name == null) {
            return pattern;
        }
        return name;
    }

    public String getPatternStr() {
        return pattern;
    }

    public Pattern getCompiledPattern() {
        if (compiledPattern == null) {
            compiledPattern = Pattern.compile(pattern);
        }
        return compiledPattern;
    }


    public void merge(ProxyableURLDescriptor other) {
        // TODO
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isUseCache() {
        return useCache;
    }

    public boolean isCachePerSession() {
        return cachePerSession;
    }

}
