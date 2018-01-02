/*
 * (C) Copyright 2015-2018 Nuxeo (http://nuxeo.com/) and others.
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

import static org.nuxeo.ecm.permissions.Constants.ACE_GRANTED_TEMPLATE;
import static org.nuxeo.ecm.permissions.Constants.ACE_INFO_COMMENT;
import static org.nuxeo.ecm.permissions.Constants.ACE_INFO_DIRECTORY;
import static org.nuxeo.ecm.permissions.Constants.ACE_KEY;
import static org.nuxeo.ecm.permissions.Constants.ACL_NAME_KEY;
import static org.nuxeo.ecm.permissions.Constants.PERMISSION_NOTIFICATION_EVENT;

import java.util.Collections;
import java.util.Locale;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.i18n.I18NUtils;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.operations.notification.SendMail;
import org.nuxeo.ecm.automation.core.scripting.Expression;
import org.nuxeo.ecm.automation.core.scripting.Scripting;
import org.nuxeo.ecm.automation.core.util.StringList;
import org.nuxeo.ecm.automation.features.PlatformFunctions;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoGroup;
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
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.tokenauth.service.TokenAuthenticationService;
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

    public static final String LABEL_SUBJECT_NEW_PERMISSION = "label.subject.new.permission";

    private static final Log log = LogFactory.getLog(PermissionGrantedNotificationListener.class);

    public static final String SUBJECT_FORMAT = "%s %s";

    @Override
    public void handleEvent(EventBundle events) {
        for (Event event : events) {
            handleEvent(event);
        }
    }

    protected void handleEvent(Event event) {
        EventContext eventCtx = event.getContext();
        if (!(eventCtx instanceof DocumentEventContext)) {
            return;
        }

        DocumentEventContext docCtx = (DocumentEventContext) eventCtx;
        CoreSession coreSession = docCtx.getCoreSession();
        DocumentModel doc = docCtx.getSourceDocument();
        if (doc == null || !coreSession.exists(doc.getRef())) {
            return;
        }

        ACE ace = (ACE) docCtx.getProperty(ACE_KEY);
        String aclName = (String) docCtx.getProperty(ACL_NAME_KEY);
        if (ace == null || ace.isDenied() || aclName == null) {
            return;
        }

        String username = ace.getUsername();
        StringList to = getRecipients(username);
        if (to == null) {
            // no recipient
            return;
        }

        Expression from = Scripting.newExpression("Env[\"mail.from\"]");
        NotificationService notificationService = NotificationServiceHelper.getNotificationService();
        String subject = String.format(SUBJECT_FORMAT, notificationService.getEMailSubjectPrefix(),
                I18NUtils.getMessageString("messages", LABEL_SUBJECT_NEW_PERMISSION, new Object[] { doc.getTitle() },
                        Locale.ENGLISH));

        try (OperationContext ctx = new OperationContext(coreSession)) {
            ctx.setInput(doc);
            ctx.put("ace", ace);

            Framework.doPrivileged(() -> {
                DirectoryService directoryService = Framework.getService(DirectoryService.class);
                try (Session session = directoryService.open(ACE_INFO_DIRECTORY)) {
                    String id = PermissionHelper.computeDirectoryId(doc, aclName, ace.getId());
                    DocumentModel entry = session.getEntry(id);
                    if (entry != null) {
                        String comment = (String) entry.getPropertyValue(ACE_INFO_COMMENT);
                        if (comment != null) {
                            comment = StringEscapeUtils.escapeHtml(comment);
                            comment = comment.replaceAll("\n", "<br/>");
                            ctx.put("comment", comment);
                        }
                    }
                }
            });

            String aceCreator = ace.getCreator();
            if (aceCreator != null) {
                UserManager userManager = Framework.getService(UserManager.class);
                NuxeoPrincipal creator = userManager.getPrincipal(aceCreator);
                if (creator != null) {
                    ctx.put("aceCreator", String.format("%s (%s)", principalFullName(creator), creator.getName()));
                }
            }

            if (NuxeoPrincipal.isTransientUsername(username)) {
                TokenAuthenticationService tokenAuthenticationService = Framework.getService(
                        TokenAuthenticationService.class);
                String token = tokenAuthenticationService.getToken(username, doc.getRepositoryName(), doc.getId());
                if (token != null) {
                    ctx.put("token", token);
                }
            }

            OperationChain chain = new OperationChain("SendMail");

            // if there is more than one recipient use "bcc:" instead of "to:" for avoiding email sharing
            final boolean useBcc = to.size() > 1;
            chain.add(SendMail.ID)
                 .set("from", from)
                 .set("to", useBcc ? null : to)
                 .set("bcc", useBcc ? to : null)
                 .set("HTML", true)
                 .set("subject", subject)
                 .set("message", ACE_GRANTED_TEMPLATE);
            Framework.getService(AutomationService.class).run(ctx, chain);
        } catch (OperationException e) {
            log.warn("Unable to notify user", e);
            log.debug(e, e);
        }
    }

    // copied from org.nuxeo.ecm.platform.ui.web.tag.fn.Functions which lives in nuxeo-platform-ui-web
    public static String principalFullName(NuxeoPrincipal principal) {
        String first = principal.getFirstName();
        String last = principal.getLastName();
        return userDisplayName(principal.getName(), first, last);
    }

    public static String userDisplayName(String id, String first, String last) {
        if (first == null || first.length() == 0) {
            if (last == null || last.length() == 0) {
                return id;
            } else {
                return last;
            }
        } else {
            if (last == null || last.length() == 0) {
                return first;
            } else {
                return first + ' ' + last;
            }
        }
    }

    protected StringList getRecipients(String username) {
        UserManager userManager = Framework.getService(UserManager.class);
        NuxeoPrincipal principal = userManager.getPrincipal(username);
        StringList to = null;
        if (principal != null) {
            to = new StringList(Collections.singletonList(principal.getEmail()));
        } else {
            NuxeoGroup group = userManager.getGroup(username);
            if (group != null) {
                PlatformFunctions platformFunctions = new PlatformFunctions();
                to = platformFunctions.getEmailsFromGroup(group.getName());
            }
        }
        return to;
    }

    @Override
    public boolean acceptEvent(Event event) {
        String eventName = event.getName();
        return PERMISSION_NOTIFICATION_EVENT.equals(eventName);
    }
}
