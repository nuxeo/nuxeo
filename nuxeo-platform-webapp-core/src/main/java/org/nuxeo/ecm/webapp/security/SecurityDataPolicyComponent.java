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
 *     George Lefter
 *
 * $Id$
 */

package org.nuxeo.ecm.webapp.security;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.webapp.security.policies.DefaultSecurityDataPolicy;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Enables users to adapt the way of transforming a security data web form
 * into a  list of user entries. Just contributes to the service by defining a
 * new policy class.
 *
 */

public class SecurityDataPolicyComponent extends DefaultComponent {

    private static final Log log = LogFactory.getLog(SecurityDataPolicyComponent.class);

    protected SecurityDataPolicy policy = new DefaultSecurityDataPolicy();

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        if (extensionPoint.equals("policy")) {
            policy = ((SecurityDataPolicyDescriptor)contribution).newPolicy();
        } else {
            log.error("unknown extension point: " + extensionPoint);
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        if (extensionPoint.equals("policy")) {
                policy = new DefaultSecurityDataPolicy();
        } else {
            log.error("unknown extension point: " + extensionPoint);
        }
    }

    public SecurityDataPolicy getPolicy() {
        return policy;
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(SecurityDataPolicy.class)){
            return adapter.cast(policy);
        }
        return super.getAdapter(adapter);
    }
}
