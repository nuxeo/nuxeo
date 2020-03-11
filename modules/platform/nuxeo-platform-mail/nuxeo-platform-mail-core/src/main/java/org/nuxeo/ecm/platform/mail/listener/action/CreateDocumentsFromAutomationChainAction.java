/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.mail.listener.action;

import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.ATTACHMENTS_KEY;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.CONTENT_KEY;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.PARENT_PATH_KEY;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.SUBJECT_KEY;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.ExceptionUtils;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.api.pathsegment.PathSegmentService;
import org.nuxeo.ecm.platform.mail.action.ExecutionContext;
import org.nuxeo.runtime.api.Framework;

/**
 * Creates a MailMessage document for every new email found in the INBOX. The creation is handled by an AutomationChain
 *
 * @since 6.0
 * @author tiry
 */
public class CreateDocumentsFromAutomationChainAction extends AbstractMailAction {

    private static final Log log = LogFactory.getLog(CreateDocumentsFromAutomationChainAction.class);

    protected String chainName;

    public CreateDocumentsFromAutomationChainAction(String chainName) {
        super();
        this.chainName = chainName;
    }

    public Pattern stupidRegexp = Pattern.compile("^[- .,;?!:/\\\\'\"]*$");

    protected String getChainName() {
        if (chainName == null) {
            return Framework.getProperty("org.nuxeo.mail.automation.chain", "CreateMailDocumentFromAutomation");
        }
        return chainName;
    }

    protected String generateMailName(String subject) {
        PathSegmentService pss = Framework.getService(PathSegmentService.class);
        return pss.generatePathSegment(subject + System.currentTimeMillis() % 10000);
    }

    @Override
    public boolean execute(ExecutionContext context) {
        CoreSession session = getCoreSession(context);
        if (session == null) {
            log.error("Could not open CoreSession");
            return false;
        }

        AutomationService as = Framework.getService(AutomationService.class);

        try (OperationContext automationCtx = new OperationContext(session)) {
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
                automationCtx.put(CONTENT_KEY, Collections.EMPTY_MAP);
            }

            try {
                as.run(automationCtx, getChainName());
            } catch (OperationException e) {
                throw new NuxeoException(e);
            }
        } catch (Exception e) {
            ExceptionUtils.checkInterrupt(e);
        }

        return true;
    }

}
