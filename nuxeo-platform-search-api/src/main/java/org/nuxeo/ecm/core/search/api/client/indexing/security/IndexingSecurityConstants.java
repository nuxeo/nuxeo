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

package org.nuxeo.ecm.core.search.api.client.indexing.security;

import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;

public final class IndexingSecurityConstants {

    private static ACP openAcp;

    public static final String SEARCH_PERMISSION = SecurityConstants.BROWSE;

    private IndexingSecurityConstants() {
    }

    /**
     * @return an ACP that grants search-based read without restriction
     */
    public static ACP getOpenAcp() {
        if (openAcp != null) {
            return openAcp;
        }
        openAcp = new ACPImpl();
        ACLImpl acl = new ACLImpl("open", true);
        acl.add(new ACE(
                SecurityConstants.EVERYONE, SEARCH_PERMISSION,true));
        openAcp.addACL(acl);
        return openAcp;
    }

}
