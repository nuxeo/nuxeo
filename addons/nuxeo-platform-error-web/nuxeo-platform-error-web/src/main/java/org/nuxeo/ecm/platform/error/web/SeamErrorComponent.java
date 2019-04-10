/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.error.web;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.ScopeType.EVENT;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.RecoverableClientException;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Seam component performing errors
 *
 * @author Anahide Tchertchian
 */
@Name("errorSeamComponent")
@Scope(CONVERSATION)
public class SeamErrorComponent implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(SeamErrorComponent.class);

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    protected void createNewDocument() throws ClientException {
        if (documentManager == null) {
            log.error("********** Unexpected exception while testing "
                    + "error handling: documentManager is null ***********");
        }
        DocumentModel newDocument = documentManager.createDocumentModel("Workspace");
        String title = "Test document";
        newDocument.setProperty("dublincore", "title", "Test document");
        String parentDocumentPath = "/default-domain/workspaces";
        String name = IdUtils.generateId(title, "-", true, 24);
        newDocument.setPathInfo(parentDocumentPath, name);

        newDocument = documentManager.createDocument(newDocument);
        documentManager.save();
    }

    public void checkedErrorAfterCreation() throws ClientException {
        createNewDocument();
        throw new ClientException("Checked exception after document creation");
    }

    public void uncheckedErrorAfterCreation() {
        try {
            createNewDocument();
        } catch (ClientException e) {
            log.error("********** Unexpected exception while testing error handling ***********");
        }
        throw new NullPointerException(
                "Unchecked exception after document creation");
    }

    public String getCheckedError() throws ClientException {
        throw new ClientException("Checked error on getter");
    }

    public String getUncheckedError() {
        throw new NullPointerException("Unchecked error on getter");
    }

    public String getSecurityError() throws DocumentSecurityException {
        throw new DocumentSecurityException("Security error on getter");
    }

    @Factory(value = "checkedErrorFactoryEvent", scope = EVENT)
    public String getCheckedErrorFactoryEvent() throws ClientException {
        throw new ClientException("Checked error on factory, scope event");
    }

    @Factory(value = "uncheckedErrorFactoryEvent", scope = EVENT)
    public String getUncheckedErrorFactoryEvent() {
        throw new NullPointerException(
                "Unchecked error on factory, scope event");
    }

    @Factory(value = "securityErrorFactoryEvent", scope = EVENT)
    public String getSecurityErrorFactoryEvent()
            throws DocumentSecurityException {
        throw new DocumentSecurityException(
                "Security error on factory, scope event");
    }

    public String performCheckedError() throws ClientException {
        throw new ClientException("Checked error on action");
    }

    public String performUncheckedError() {
        throw new NullPointerException("Unchecked error on action");
    }

    public String performSecurityError() throws DocumentSecurityException {
        throw new DocumentSecurityException("Security error on action");
    }

    public String performRecoverableClientException() throws RecoverableClientException {
        throw new RecoverableClientException("Application validation failed, rollingback", "Application validation failed, rollingback", null);
    }

    public String performPureRollback(){
        TransactionHelper.setTransactionRollbackOnly();
        return null;
    }

    // methods to test concurrency issues

    protected int counter = 0;

    public String performConcurrentRequestTimeoutException() throws Exception {
        Thread.sleep(15*1000);
        counter++;
        return null;
    }

    public int getCounterValue() {
        return counter;
    }


}
