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

import static org.apache.commons.logging.LogFactory.getLog;
import static org.nuxeo.ecm.permissions.Constants.ACE_GRANTED_TEMPLATE;
import static org.nuxeo.ecm.permissions.Constants.ACE_KEY;
import static org.nuxeo.ecm.permissions.Constants.COMMENT_KEY;
import static org.nuxeo.ecm.permissions.Constants.PERMISSION_NOTIFICATION_EVENT;

import java.util.Collections;

import org.apache.commons.logging.Log;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.operations.notification.SendMail;
import org.nuxeo.ecm.automation.core.scripting.Expression;
import org.nuxeo.ecm.automation.core.scripting.Scripting;
import org.nuxeo.ecm.automation.core.util.StringList;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.PostCommitFilteringEventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.ec.notification.service.NotificationService;
import org.nuxeo.ecm.platform.ec.notification.service.NotificationServiceHelper;
import org.nuxeo.ecm.platform.ui.web.tag.fn.Functions;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Listener sending an email notification for a given ACE.
 *
 * @since 7.4
 */
public class PermissionNotificationListener implements PostCommitFilteringEventListener {

    private static final Log log = getLog(PermissionNotificationListener.class);

    public static final String SUBJECT_FORMAT = "%s %s %s";

    @Override
    public void handleEvent(EventBundle events) throws ClientException {
        if (events.containsEventName(PERMISSION_NOTIFICATION_EVENT)) {
            for (Event event : events) {
                if (PERMISSION_NOTIFICATION_EVENT.equals(event.getName())) {
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
        if (ace == null || ace.isDenied()) {
            return;
        }

        UserManager userManager = Framework.getService(UserManager.class);
        OperationContext ctx = new OperationContext(doc.getCoreSession());
        ctx.setInput(doc);
        ctx.put("ace", ace);
        ctx.put("comment", ace.getContextData(COMMENT_KEY));
        String aceCreator = ace.getCreator();
        if (aceCreator != null) {
            NuxeoPrincipal principal = userManager.getPrincipal(aceCreator);
            if (principal != null) {
                ctx.put("aceCreator",
                        String.format("%s (%s)", Functions.principalFullName(principal), principal.getName()));
            }
        }

        NuxeoPrincipal principal = userManager.getPrincipal(ace.getUsername());
        if (principal == null) {
            return;
        }

        StringList to = new StringList(Collections.singletonList(principal.getEmail()));
        Expression from = Scripting.newExpression("Env[\"mail.from\"]");
        NotificationService notificationService = NotificationServiceHelper.getNotificationService();
        String subject = String.format(SUBJECT_FORMAT, notificationService.getEMailSubjectPrefix(),
                "New permission on", doc.getTitle());
        try {
            OperationChain chain = new OperationChain("SendMail");
            chain.add(SendMail.ID)
                 .set("from", from)
                 .set("to", to)
                 .set("HTML", true)
                 .set("subject", subject)
                 .set("message", ACE_GRANTED_TEMPLATE);
            Framework.getService(AutomationService.class)
                     .run(ctx, chain);
        } catch (OperationException e) {
            log.warn("Unable to notify user", e);
            log.debug(e, e);
        }
    }

    @Override
    public boolean acceptEvent(Event event) {
        return PERMISSION_NOTIFICATION_EVENT.equals(event.getName());
    }
}
