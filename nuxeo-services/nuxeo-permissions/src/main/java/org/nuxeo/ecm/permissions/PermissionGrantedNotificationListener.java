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

import static org.nuxeo.ecm.permissions.Constants.ACE_GRANTED_TEMPLATE;
import static org.nuxeo.ecm.permissions.Constants.ACE_INFO_COMMENT;
import static org.nuxeo.ecm.permissions.Constants.ACE_INFO_DIRECTORY;
import static org.nuxeo.ecm.permissions.Constants.ACE_INFO_NOTIFIED;
import static org.nuxeo.ecm.permissions.Constants.ACE_KEY;
import static org.nuxeo.ecm.permissions.Constants.ACL_NAME_KEY;
import static org.nuxeo.ecm.permissions.Constants.PERMISSION_NOTIFICATION_EVENT;

import java.util.Collections;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.operations.notification.SendMail;
import org.nuxeo.ecm.automation.core.scripting.Expression;
import org.nuxeo.ecm.automation.core.scripting.Scripting;
import org.nuxeo.ecm.automation.core.util.StringList;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.PostCommitFilteringEventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.ec.notification.service.NotificationService;
import org.nuxeo.ecm.platform.ec.notification.service.NotificationServiceHelper;
import org.nuxeo.ecm.platform.ui.web.tag.fn.Functions;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Listener sending an email notification for a granted ACE.
 * <p>
 * This listener checks only if the ACE is granted. It assumes that other checks (such as the ACE becomes effective)
 * have been done before.
 *
 * @since 7.4
 */
public class PermissionGrantedNotificationListener implements PostCommitFilteringEventListener {

    private static final Log log = LogFactory.getLog(PermissionGrantedNotificationListener.class);

    public static final String SUBJECT_FORMAT = "%s %s %s";

    @Override
    public void handleEvent(EventBundle events) {
        if (events.containsEventName(PERMISSION_NOTIFICATION_EVENT)) {
            for (Event event : events) {
                String eventName = event.getName();
                if (PERMISSION_NOTIFICATION_EVENT.equals(eventName)) {
                    handleEvent(event);
                }
            }
        }
    }

    protected void handleEvent(Event event) {
        EventContext eventCtx = event.getContext();
        if (!(eventCtx instanceof DocumentEventContext)) {
            return;
        }

        DocumentEventContext docCtx = (DocumentEventContext) eventCtx;
        DocumentModel doc = docCtx.getSourceDocument();
        ACE ace = (ACE) docCtx.getProperty(ACE_KEY);
        String aclName = (String) docCtx.getProperty(ACL_NAME_KEY);
        if (ace == null || ace.isDenied() || aclName == null) {
            return;
        }

        UserManager userManager = Framework.getService(UserManager.class);
        NuxeoPrincipal principal = userManager.getPrincipal(ace.getUsername());
        if (principal == null) {
            return;
        }

        StringList to = new StringList(Collections.singletonList(principal.getEmail()));
        Expression from = Scripting.newExpression("Env[\"mail.from\"]");
        NotificationService notificationService = NotificationServiceHelper.getNotificationService();
        String subject = String.format(SUBJECT_FORMAT, notificationService.getEMailSubjectPrefix(), "New permission on",
                doc.getTitle());

        DirectoryService directoryService = Framework.getService(DirectoryService.class);
        try (Session session = directoryService.open(ACE_INFO_DIRECTORY)) {
            String id = PermissionHelper.computeDirectoryId(doc, aclName, ace.getId());
            DocumentModel entry = session.getEntry(id);

            OperationContext ctx = new OperationContext(doc.getCoreSession());
            ctx.setInput(doc);
            ctx.put("ace", ace);
            if (entry != null) {
                ctx.put("comment", entry.getPropertyValue(ACE_INFO_COMMENT));
            }
            String aceCreator = ace.getCreator();
            if (aceCreator != null) {
                NuxeoPrincipal creator = userManager.getPrincipal(aceCreator);
                if (creator != null) {
                    ctx.put("aceCreator",
                            String.format("%s (%s)", Functions.principalFullName(principal), principal.getName()));
                }
            }

            OperationChain chain = new OperationChain("SendMail");
            chain.add(SendMail.ID).set("from", from).set("to", to).set("HTML", true).set("subject", subject).set(
                    "message", ACE_GRANTED_TEMPLATE);
            Framework.getService(AutomationService.class).run(ctx, chain);

            if (entry != null) {
                entry.setPropertyValue(ACE_INFO_NOTIFIED, true);
                session.updateEntry(entry);
            }
        } catch (OperationException e) {
            log.warn("Unable to notify user", e);
            log.debug(e, e);
        }
    }

    @Override
    public boolean acceptEvent(Event event) {
        String eventName = event.getName();
        return PERMISSION_NOTIFICATION_EVENT.equals(eventName);
    }
}
