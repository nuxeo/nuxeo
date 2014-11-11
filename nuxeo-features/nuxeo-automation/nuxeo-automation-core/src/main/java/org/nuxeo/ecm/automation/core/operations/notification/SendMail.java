/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.operations.notification;

import java.util.Map;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.mail.Composer;
import org.nuxeo.ecm.automation.core.mail.Mailer;
import org.nuxeo.ecm.automation.core.scripting.Scripting;
import org.nuxeo.ecm.automation.core.util.StringList;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;

/**
 * Save the session - TODO remove this?
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Operation(id = SendMail.ID, category = Constants.CAT_NOTIFICATION, label = "Send E-Mail", description = "Send an email using the input document to the specified recipients.")
public class SendMail {

    public static final Composer COMPOSER = new Composer();

    public static final String ID = "Notification.SendMail";

    @Context
    protected OperationContext ctx;

    @Param(name = "message", widget = Constants.W_MULTILINE_TEXT)
    protected String message;

    @Param(name = "subject")
    protected String subject;

    @Param(name = "from")
    protected String from;

    @Param(name = "to")
    protected StringList to; // a comma separated list of emails

    @OperationMethod
    public DocumentModel run(DocumentModel doc) throws Exception {
        send(getRecipients());
        return doc;
    }

    @OperationMethod
    public DocumentModelList run(DocumentModelList docs) throws Exception {
        send(getRecipients());
        return docs;
    }

    protected StringList getRecipients() {
        return to;
    }

    protected void send(StringList sendTo) throws Exception {
        Map<String, Object> map = Scripting.initBindings(ctx);
        Mailer.Message msg = COMPOSER.newTextMessage(message, map);
        msg.setFrom(from);
        msg.setSubject(subject);
        for (String r : sendTo) {
            msg.addTo(r);
        }
        msg.send();
    }
}
