/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.security;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Pluggable policy descriptor for core security
 *
 * @author Anahide Tchertchian
 */
@XObject("policy")
public class SecurityPolicyDescriptor implements
        Comparable<SecurityPolicyDescriptor> {

    @XNode("@name")
    private String name;

    @XNode("@class")
    private Class<Object> policy;

    @XNode("@enabled")
    private boolean enabled = true;

    @XNode("@order")
    private int order = 0;

    public String getName() {
        return name;
    }

    public Class<Object> getPolicy() {
        return policy;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getOrder() {
        return order;
    }

    @Override
    public int compareTo(SecurityPolicyDescriptor anotherPolicy) {
        return order - anotherPolicy.order;
    }

}
