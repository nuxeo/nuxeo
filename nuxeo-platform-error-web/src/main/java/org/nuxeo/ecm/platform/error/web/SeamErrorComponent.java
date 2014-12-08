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

import static org.jboss.seam.ScopeType.EVENT;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.RecoverableClientException;
import org.nuxeo.ecm.core.persistence.PersistenceProviderFactory;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.audit.api.AuditReader;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.api.Logs;
import org.nuxeo.ecm.platform.audit.service.LogEntryProvider;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Seam component performing errors
 *
 * @author Anahide Tchertchian
 */
@Name("errorSeamComponent")
@Scope(ScopeType.CONVERSATION)
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
        throw new NullPointerException("Unchecked exception after document creation");
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
        throw new NullPointerException("Unchecked error on factory, scope event");
    }

    @Factory(value = "securityErrorFactoryEvent", scope = EVENT)
    public String getSecurityErrorFactoryEvent() throws DocumentSecurityException {
        throw new DocumentSecurityException("Security error on factory, scope event");
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

    /**
     * @since 5.8
     */
    public String performRecoverableClientException() throws RecoverableClientException {
        throw new RecoverableClientException("Application validation failed, rollingback",
                "Application validation failed, rollingback", null);
    }

    /**
     * @since 5.8
     */
    public String performPureRollback() {
        TransactionHelper.setTransactionRollbackOnly();
        return null;
    }

    /**
     * @since 5.9.5
     */
    public void performDistributedRollback() throws ClientException {
        createDummyUser();
        createDummyLogEntry();
        createDummyDoc();
        TransactionHelper.setTransactionRollbackOnly();
    }

    /**
     * @since 5.9.5
     */
    public void clearDistributedRollbackEnv() throws ClientException {
        clearDummyUser();
        clearDummyDoc();
        clearDummyLogEntries();
    }

    protected DocumentModel createDummyUser() throws ClientException {
        DirectoryService directories = Framework.getLocalService(DirectoryService.class);
        org.nuxeo.ecm.directory.Session userDir = directories.getDirectory("userDirectory").getSession();
        try {
            Map<String, Object> user = new HashMap<String, Object>();
            user.put("username", "dummy");
            user.put("password", "dummy");
            user.put("firstName", "dummy");
            user.put("lastName", "dummy");
            return userDir.createEntry(user);
        } finally {
            userDir.close();
        }
    }

    protected void clearDummyUser() throws DirectoryException {
        DirectoryService directories = Framework.getLocalService(DirectoryService.class);
        org.nuxeo.ecm.directory.Session userDir = directories.getDirectory("userDirectory").getSession();
        try {
            userDir.deleteEntry("dummy");
        } catch (Exception e) {
            ;
        } finally {
            userDir.close();
        }
    }

    /**
     * @since 5.9.5
     */
    @Factory(scope = ScopeType.EVENT)
    public boolean isDummyUserExists() throws DirectoryException {
        DirectoryService directories = Framework.getLocalService(DirectoryService.class);
        org.nuxeo.ecm.directory.Session userDir = directories.getDirectory("userDirectory").getSession();
        try {
            DocumentModel user = userDir.getEntry("dummy");
            return user != null;
        } catch (DirectoryException cause) {
            return false;
        } finally {
            userDir.close();
        }
    }

    protected LogEntry createDummyLogEntry() {
        Logs logs = Framework.getLocalService(Logs.class);
        LogEntry entry = logs.newLogEntry();
        entry.setEventId("dummy");
        entry.setDocUUID("dummy");
        entry.setCategory("dummy");
        entry.setComment("dummy");
        logs.addLogEntries(Collections.singletonList(entry));
        return entry;
    }

    /**
     * @since 5.9.5
     */
    public void clearDummyLogEntries() {
        PersistenceProviderFactory pf = Framework.getService(PersistenceProviderFactory.class);
        EntityManager em = pf.newProvider("nxaudit-logs").acquireEntityManager();
        LogEntryProvider provider = LogEntryProvider.createProvider(em);
        provider.removeEntries("dummy", null);
    }

    /**
     * @since 5.9.5
     */
    @Factory(scope = ScopeType.EVENT)
    public boolean isDummyAuditExists() {
        AuditReader reader = Framework.getLocalService(AuditReader.class);
        List<LogEntry> entries = reader.getLogEntriesFor("dummy");
        return !entries.isEmpty();
    }

    protected DocumentModel createDummyDoc() throws ClientException {
        DocumentModel doc = documentManager.createDocumentModel("/", "dummy", "Document");
        doc = documentManager.createDocument(doc);
        documentManager.save();
        return doc;
    }

    /**
     * @since 5.9.5
     */
    public void clearDummyDoc() throws ClientException {
        PathRef ref = new PathRef("/dummy");
        if (documentManager.exists(ref)) {
            documentManager.removeDocument(ref);
        }
    }

    /**
     * @since 5.9.5
     */
    @Factory(scope = ScopeType.EVENT)
    public boolean isDummyDocExists() throws ClientException {
        return documentManager.exists(new PathRef("/dummy"));
    }

    // methods to test concurrency issues

    protected int counter = 0;

    /**
     * @since 5.8
     */
    public String performConcurrentRequestTimeoutException() throws Exception {
        Thread.sleep(15 * 1000);
        counter++;
        return null;
    }

    public int getCounterValue() {
        return counter;
    }

}
