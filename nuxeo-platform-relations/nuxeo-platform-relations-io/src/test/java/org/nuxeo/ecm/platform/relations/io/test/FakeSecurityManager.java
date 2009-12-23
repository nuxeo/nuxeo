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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: FakeSecurityManager.java 25081 2007-09-18 14:57:22Z atchertchian $
 */

package org.nuxeo.ecm.platform.relations.io.test;

import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.Access;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.security.SecurityException;
import org.nuxeo.ecm.core.security.SecurityManager;

/**
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 */
public class FakeSecurityManager implements SecurityManager {

    private static final class ACPGrant extends ACPImpl {

        private static final long serialVersionUID = -5167896851016076928L;

        @Override
        public Access getAccess(String principal, String permission) {
            return Access.GRANT;
        }

        @Override
        public Access getAccess(String[] principals, String[] permissions) {
            return Access.GRANT;
        }
    }

    // always return true
    public boolean checkPermission(Document doc, String username,
            String permission) throws SecurityException {
        return true;
    }

    public Access getAccess(Document doc, String username, String permission)
            throws SecurityException {
        return Access.GRANT;
    }

    public ACP getACP(Document doc) throws SecurityException {
        return new ACPGrant();
    }

    public ACP getMergedACP(Document doc) throws SecurityException {
        return new ACPGrant();
    }

    public void invalidateCache(Session session) {
    }

    public void setACP(Document doc, ACP acp, boolean overwrite)
            throws SecurityException {
    }

}
