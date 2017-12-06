/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and others.
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
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.RecoverableClientException;
import org.nuxeo.ecm.core.persistence.PersistenceProviderFactory;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
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

    protected void createNewDocument() {
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

    public void checkedErrorAfterCreation() {
        createNewDocument();
        throw new NuxeoException("Checked exception after document creation");
    }

    public void uncheckedErrorAfterCreation() {
        try {
            createNewDocument();
        } catch (NuxeoException e) {
            log.error("********** Unexpected exception while testing error handling ***********");
        }
        throw new NullPointerException("Unchecked exception after document creation");
    }

    public String getCheckedError() {
        throw new NuxeoException("Checked error on getter");
    }

    public String getUncheckedError() {
        throw new NullPointerException("Unchecked error on getter");
    }

    public String getSecurityError() throws DocumentSecurityException {
        throw new DocumentSecurityException("Security error on getter");
    }

    @Factory(value = "checkedErrorFactoryEvent", scope = EVENT)
    public String getCheckedErrorFactoryEvent() {
        throw new NuxeoException("Checked error on factory, scope event");
    }

    @Factory(value = "uncheckedErrorFactoryEvent", scope = EVENT)
    public String getUncheckedErrorFactoryEvent() {
        throw new NullPointerException("Unchecked error on factory, scope event");
    }

    @Factory(value = "securityErrorFactoryEvent", scope = EVENT)
    public String getSecurityErrorFactoryEvent() throws DocumentSecurityException {
        throw new DocumentSecurityException("Security error on factory, scope event");
    }

    public String performCheckedError() {
        throw new NuxeoException("Checked error on action");
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
    public void performDistributedRollback() {
        createDummyUser();
        createDummyLogEntry();
        createDummyDoc();
        TransactionHelper.setTransactionRollbackOnly();
    }

    /**
     * @since 5.9.5
     */
    public void clearDistributedRollbackEnv() {
        clearDummyUser();
        clearDummyDoc();
        clearDummyLogEntries();
    }

    protected DocumentModel createDummyUser() {
        DirectoryService directories = Framework.getService(DirectoryService.class);
        try (Session userDir = directories.getDirectory("userDirectory").getSession()) {
            Map<String, Object> user = new HashMap<>();
            user.put("username", "dummy");
            user.put("password", "dummy");
            user.put("firstName", "dummy");
            user.put("lastName", "dummy");
            return userDir.createEntry(user);
        }
    }

    protected void clearDummyUser() throws DirectoryException {
        DirectoryService directories = Framework.getService(DirectoryService.class);
        try (Session userDir = directories.open("userDirectory")) {
            userDir.deleteEntry("dummy");
        }
    }

    /**
     * @since 5.9.5
     */
    @Factory(scope = ScopeType.EVENT)
    public boolean isDummyUserExists() throws DirectoryException {
        DirectoryService directories = Framework.getService(DirectoryService.class);
        try (Session userDir = directories.getDirectory("userDirectory").getSession()) {
            DocumentModel user = userDir.getEntry("dummy");
            return user != null;
        } catch (DirectoryException cause) {
            return false;
        }
    }

    protected LogEntry createDummyLogEntry() {
        Logs logs = Framework.getService(Logs.class);
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
        AuditReader reader = Framework.getService(AuditReader.class);
        List<LogEntry> entries = reader.getLogEntriesFor("dummy");
        return !entries.isEmpty();
    }

    protected DocumentModel createDummyDoc() {
        DocumentModel doc = documentManager.createDocumentModel("/", "dummy", "Document");
        doc = documentManager.createDocument(doc);
        documentManager.save();
        return doc;
    }

    /**
     * @since 5.9.5
     */
    public void clearDummyDoc() {
        PathRef ref = new PathRef("/dummy");
        if (documentManager.exists(ref)) {
            documentManager.removeDocument(ref);
        }
    }

    /**
     * @since 5.9.5
     */
    @Factory(scope = ScopeType.EVENT)
    public boolean isDummyDocExists() {
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
