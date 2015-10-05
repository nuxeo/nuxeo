/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thomas Roger
 */

package org.nuxeo.ecm.permissions;

import static org.nuxeo.ecm.permissions.Constants.ACE_INFO_ACE_ID;
import static org.nuxeo.ecm.permissions.Constants.ACE_INFO_ACL_NAME;
import static org.nuxeo.ecm.permissions.Constants.ACE_INFO_COMMENT;
import static org.nuxeo.ecm.permissions.Constants.ACE_INFO_DOC_ID;
import static org.nuxeo.ecm.permissions.Constants.ACE_INFO_ID;
import static org.nuxeo.ecm.permissions.Constants.ACE_INFO_NOTIFY;
import static org.nuxeo.ecm.permissions.Constants.ACE_INFO_REPOSITORY_NAME;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.security.ACE;

/**
 * @since 7.4
 */
public class PermissionHelper {

    private PermissionHelper() {
        // helper class
    }

    public static String computeDirectoryId(DocumentModel doc, String aclName, String aceId) {
        return String.format("%s:%s:%s:%s", doc.getId(), doc.getRepositoryName(), aclName, aceId);
    }

    public static Map<String, Object> createDirectoryEntry(DocumentModel doc, String aclName, ACE ace, boolean notify,
            String comment) {
        Map<String, Object> m = new HashMap<>();
        m.put(ACE_INFO_ID, computeDirectoryId(doc, aclName, ace.getId()));
        m.put(ACE_INFO_REPOSITORY_NAME, doc.getRepositoryName());
        m.put(ACE_INFO_DOC_ID, doc.getId());
        m.put(ACE_INFO_ACL_NAME, aclName);
        m.put(ACE_INFO_ACE_ID, ace.getId());
        m.put(ACE_INFO_NOTIFY, notify);
        m.put(ACE_INFO_COMMENT, comment);
        return m;
    }
}
