/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import static org.nuxeo.ecm.permissions.Constants.ACE_KEY;
import static org.nuxeo.ecm.permissions.Constants.ACL_NAME_KEY;
import static org.nuxeo.ecm.permissions.Constants.PERMISSION_NOTIFICATION_EVENT;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.runtime.api.Framework;

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

    public static void firePermissionNotificationEvent(CoreSession session, DocumentModel doc, String aclName, ACE ace) {
        DocumentEventContext docCtx = new DocumentEventContext(session, session.getPrincipal(), doc);
        docCtx.setProperty(ACE_KEY, ace);
        docCtx.setProperty(ACL_NAME_KEY, aclName);
        EventService eventService = Framework.getService(EventService.class);
        eventService.fireEvent(PERMISSION_NOTIFICATION_EVENT, docCtx);
    }
}
