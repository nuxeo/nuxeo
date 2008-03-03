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
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.model.Document;

public class CorePolicyServiceImpl implements CorePolicyService {

    private static final Log log = LogFactory.getLog(CorePolicyServiceImpl.class);

    public boolean checkPolicy(Document doc, NuxeoPrincipal principal, String permission) {
        try {
            if ("Folder".equals(doc.getType().getName())) {
                Long accessLevel = (Long) principal.getModel().getProperty("user", "accessLevel");
                Long securityLevel = (Long) doc.getPropertyValue("sp:securityLevel");
                return accessLevel >= securityLevel;
            } else {
                return true;
            }
        } catch (Exception e) {
            log.debug("Test : ", e);
            return true;
        }
    }

}
