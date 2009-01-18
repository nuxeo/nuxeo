package org.nuxeo.ecm.platform.web.common.requestcontroller.service;

import java.io.Serializable;
import java.util.regex.Pattern;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Descriptor for {@link RequestFilterConfig}
 *
 * @author tiry
 */
@XObject(value = "filterConfig")
public class FilterConfigDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

    @XNode("@name")
    protected String name;

    @XNode("@synchonize")
    protected boolean useSync;

    @XNode("@transactional")
    protected boolean useTx;

    @XNode("@grant")
    protected boolean grant = true;

    @XNode("pattern")
    protected String pattern;

    protected Pattern compiledPattern;

    public String getName() {
        if (name == null) {
            return pattern;
        }
        return name;
    }

    public boolean useSync() {
        return useSync;
    }

    public boolean useTx() {
        return useTx;
    }

    public boolean isGrantRule() {
        return grant;
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

}
