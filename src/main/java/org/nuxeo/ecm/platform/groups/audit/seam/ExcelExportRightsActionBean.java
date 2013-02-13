/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Martin Pernollet
 */

package org.nuxeo.ecm.platform.groups.audit.seam;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.groups.audit.service.acl.job.RunnableAclAudit;
import org.nuxeo.ecm.platform.groups.audit.service.acl.job.Work;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;
import org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;
import org.nuxeo.runtime.api.Framework;

@Name("excelExportRightsAction")
@Scope(ScopeType.EVENT)
public class ExcelExportRightsActionBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(ExcelExportRightsActionBean.class);

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected NavigationContext navigationContext;

    @In(create = true)
    protected DocumentsListsManager documentsListsManager;

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In(create = true)
    protected transient ResourcesAccessor resourcesAccessor;

    @In(create = true, required = false)
    protected NuxeoPrincipal currentNuxeoPrincipal;

    protected static String OUTPUT_FILE = "rights.xls";

    public String doGet() {
        try {
            buildAndDownload();
        } catch (Exception e) {
            log.error(e, e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    "doGet error: " + e.getMessage());
        }
        return null;
    }

    public boolean accept() {
        return true;
    }

    protected List<DocumentModel> getCurrentlySelectedDocuments() {
        if (navigationContext.getCurrentDocument().isFolder()) {
            return documentsListsManager.getWorkingList(DocumentsListsManager.CURRENT_DOCUMENT_SELECTION);
        } else {
            return null;
        }
    }

    /* XLS REPORT */

    public void buildAndDownload() throws ClientException, IOException {
        File tmpFile = File.createTempFile("rights", ".xls");
        tmpFile.deleteOnExit();
        buildAndDownloadAsync(tmpFile);
    }

    /** Execute ACL audit and download the result in a XLS file. */
    protected void buildAndDownload(final File tmpFile) throws ClientException {
        final FacesContext context = FacesContext.getCurrentInstance();
        final DocumentModel auditRoot = navigationContext.getCurrentDocument();

        Runnable todo = new RunnableAclAudit(documentManager, auditRoot,
                tmpFile) {
            public void onAuditDone() {
                ComponentUtils.downloadFile(context, OUTPUT_FILE, tmpFile);
            }
        };
        todo.run();
    }

    /**
     * Execute ACL audit and save the result XLS file as child of the document
     * we started the analysis from.
     */
    protected void buildAndDownloadAsync(final File tmpFile) throws ClientException {
        final DocumentModel auditRoot = navigationContext.getCurrentDocument();
        final String repository = documentManager.getRepositoryName();

        final String workName = "ACL Audit for " + auditRoot.getPathAsString();
        final Work work = new Work(workName);
        new RunnableAclAudit(documentManager, auditRoot, work, tmpFile){
            public void onAuditDone() {
                log.debug("about to save audit");
                Blob b = new FileBlob(getOutputFile());
                b.setFilename(OUTPUT_FILE);

                try {
                    reconnectAndCreateDocument(repository, auditRoot, workName, b);
                } catch (ClientException e) {
                    log.error(e,e);
                }
            }
        };

        WorkManager wm = Framework.getLocalService(WorkManager.class);
        wm.schedule(work);

        String message = resourcesAccessor.getMessages().get("message.audit.acl.started");
        facesMessages.add(StatusMessage.Severity.INFO, message);
    }

    protected void reconnectAndCreateDocument(String repository,
            final DocumentModel parent, final String name, final Blob doc)
            throws ClientException {
        new UnrestrictedSessionRunner(repository) {
            @Override
            public void run() throws ClientException {
                createOrUpdateDocument(session, parent, name, doc);
                log.debug("audit saved");
            }
        }.runUnrestricted();
    }

    protected DocumentModel createOrUpdateDocument(CoreSession session,
            DocumentModel parent, String name, Blob doc) throws ClientException {
        DocumentRef dr = new PathRef(parent.getPath().append(name).toString());
        String filenamePlusExt = doc.getFilename();
        if (session.exists(dr)) {
            DocumentModel document = session.getDocument(dr);
            document.setPropertyValue("file:content", (Serializable) doc);
            document.setPropertyValue("file:filename", filenamePlusExt);
            document.setPropertyValue("dublincore:title", name);
            return session.saveDocument(document);
        } else {
            DocumentModel document = session.createDocumentModel(
                    parent.getPathAsString(),
                    IdUtils.generatePathSegment(name), "File");
            document.setPropertyValue("file:content", (Serializable) doc);
            document.setPropertyValue("file:filename", filenamePlusExt);
            document.setPropertyValue("dublincore:title", name);
            DocumentModel d = session.createDocument(document);
            return session.saveDocument(d);
        }
    }
}
