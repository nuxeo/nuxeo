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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.security.PolicyService;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Extension;

@Deprecated
public class PolicyServiceImpl extends DefaultComponent implements
        PolicyService {

    public static final ComponentName NAME = new ComponentName(
            "org.nuxeo.ecm.core.security.PolicyService");

    private static final Log log = LogFactory.getLog(PolicyServiceImpl.class);

    protected Object corePolicy;

    protected Object searchPolicy;

    public Object getCorePolicy() {
        return corePolicy;
    }

    public Object getSearchPolicy() {
        return searchPolicy;
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        corePolicy = null;
        searchPolicy = null;
    }

    @Override
    public void registerExtension(Extension extension) {
        Object[] contribs = extension.getContributions();
        if (contribs == null) {
            return;
        }
        for (Object contrib : contribs) {
            if (contrib instanceof PolicyDescriptor) {
                PolicyDescriptor policyDescriptor = (PolicyDescriptor) contrib;
                if ("core".equals(policyDescriptor.getType())) {
                    try {
                        corePolicy = policyDescriptor.getPolicy().newInstance();
                    } catch (Exception e) {
                        log.debug(e);
                    }
                } else if ("search".equals(policyDescriptor.getType())) {
                    try {
                        searchPolicy = policyDescriptor.getPolicy()
                                .newInstance();
                    } catch (Exception e) {
                        log.debug(e);
                    }
                }
            }
        }
    }

}
