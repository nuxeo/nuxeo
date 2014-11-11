/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

    public int compareTo(SecurityPolicyDescriptor anotherPolicy) {
        return order - anotherPolicy.order;
    }

}
