/*
 * (C) Copyright 2006-2014 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.platform.mail.listener.action;

import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.ATTACHMENTS_KEY;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.PARENT_PATH_KEY;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.SUBJECT_KEY;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.platform.mail.action.ExecutionContext;
import org.nuxeo.ecm.platform.mail.listener.action.AbstractMailAction;
import org.nuxeo.runtime.api.Framework;

/**
 * Creates a MailMessage document for every new email found in the INBOX.
 * 
 * The creation is handled by an AutomationChain
 * 
 * @since 6.0
 * @author tiry
 * 
 */
public class CreateDocumentsFromAutomationChainAction extends
        AbstractMailAction {

    private static final Log log = LogFactory.getLog(CreateDocumentsFromAutomationChainAction.class);

    protected String chainName;

    public CreateDocumentsFromAutomationChainAction(String chainName) {
        super();
        this.chainName = chainName;
    }

    public Pattern stupidRegexp = Pattern.compile("^[- .,;?!:/\\\\'\"]*$");

    protected String getChainName() {
        if (chainName == null) {
            return Framework.getProperty("org.nuxeo.mail.automation.chain",
                    "CreateMailDocumentFromAutomation");
        }
        return chainName;
    }

    protected final int maxSize = Integer.parseInt(Framework.getProperty(
            "nuxeo.path.segment.maxsize", "24"));

    protected String generatePathSegment(String s) {
        if (s == null) {
            s = "";
        }
        s = s.trim();
        if (s.length() > maxSize) {
            s = s.substring(0, maxSize).trim();
        }
        s = s.replace('/', '-');
        s = s.replace('\\', '-');
        if (stupidRegexp.matcher(s).matches()) {
            return IdUtils.generateStringId();
        }
        return s;
    }

    protected String generateMailName(String subject) {
        return generatePathSegment(subject + System.currentTimeMillis() % 10000);
    }

    @Override
    public boolean execute(ExecutionContext context) throws Exception {
        CoreSession session = getCoreSession(context);
        if (session == null) {
            log.error("Could not open CoreSession");
            return false;
        }

        AutomationService as = Framework.getService(AutomationService.class);

        OperationContext automationCtx = new OperationContext(session);
        automationCtx.putAll(context);

        ExecutionContext initialContext = context.getInitialContext();
        String parentPath = (String) initialContext.get(PARENT_PATH_KEY);
        DocumentModel mailFolder = session.getDocument(new PathRef(parentPath));
        automationCtx.put("mailFolder", mailFolder);
        automationCtx.put("executionContext", initialContext);
        String subject = (String) context.get(SUBJECT_KEY);
        automationCtx.put("mailDocumentName", generateMailName(subject));

        @SuppressWarnings("unchecked")
        List<FileBlob> attachments = (List<FileBlob>) context.get(ATTACHMENTS_KEY);
        if (attachments == null) {
            automationCtx.put(ATTACHMENTS_KEY, Collections.EMPTY_LIST);
        }

        as.run(automationCtx, getChainName());

        return true;
    }

}
