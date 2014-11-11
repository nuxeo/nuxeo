package org.nuxeo.ecm.platform.ui.web.auth.service;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.ui.web.auth.NuxeoAuthenticationFilter;

@XObject(value = "preFilter")
public class AuthPreFilterDescriptor implements Comparable<AuthPreFilterDescriptor> {

    private static final long serialVersionUID = 237654398643289764L;

    @XNode("@name")
    protected String name;

    @XNode("@enabled")
    protected boolean enabled = true;

    @XNode("@class")
    protected Class<NuxeoAuthenticationFilter> className;

    @XNode("@order")
    protected int order = 10;

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Class<NuxeoAuthenticationFilter> getClassName() {
        return className;
    }

    public Integer getOrder() {
        return order;
    }

    @Override
    public int compareTo(AuthPreFilterDescriptor o) {
        return this.getOrder().compareTo(o.getOrder());
    }

}
