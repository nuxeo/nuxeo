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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.faces.context.FacesContext;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.groups.audit.service.acl.excel.AclNameShortner;
import org.nuxeo.ecm.platform.groups.audit.service.acl.job.RunnableAclAudit;
import org.nuxeo.ecm.platform.groups.audit.service.acl.job.Work;
import org.nuxeo.ecm.platform.groups.audit.service.acl.job.publish.IResultPublisher;
import org.nuxeo.ecm.platform.groups.audit.service.acl.job.publish.PublishAsDocument;
import org.nuxeo.ecm.platform.groups.audit.service.acl.job.publish.PublishByMail;
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

    protected static String OUTPUT_FILE_NAME = "permissions";
    protected static String OUTPUT_FILE_EXT = ".xls";
    private static final String WORK_NAME = "Permission Audit for ";


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
        buildAndSendByMail(tmpFile);
        //buildAndSaveAsChildDocument(tmpFile);
    }

    /** Execute ACL audit and download the result in a XLS file. */
    protected void buildAndDownload(final File tmpFile) throws ClientException {
        final FacesContext context = FacesContext.getCurrentInstance();
        final DocumentModel auditRoot = navigationContext.getCurrentDocument();

        Runnable todo = new RunnableAclAudit(documentManager, auditRoot,
                tmpFile) {
            @Override
            public void onAuditDone() {
                ComponentUtils.downloadFile(context, OUTPUT_FILE_NAME + OUTPUT_FILE_EXT, tmpFile);
            }
        };
        todo.run();
    }

    /**
     * Execute ACL audit asynchronously and save the result XLS file as child of the document
     * we started the analysis from.
     */
    protected void buildAndSaveAsChildDocument(final File tmpFile) throws ClientException {
        final DocumentModel auditRoot = navigationContext.getCurrentDocument();
        final String repository = documentManager.getRepositoryName();
        final String workName = WORK_NAME + auditRoot.getPathAsString();

        // Work to do and publishing
        final Work work = new Work(workName);
        new RunnableAclAudit(documentManager, auditRoot, work, tmpFile){
            @Override
            public void onAuditDone() {
                IResultPublisher publisher = new PublishAsDocument(getOutputFile(), workName, repository, auditRoot);
                try {
                    publisher.publish();
                } catch (ClientException e) {
                    facesMessages.add(StatusMessage.Severity.ERROR, e.getMessage());
                    log.error(e,e);
                }
            }
        };
        WorkManager wm = Framework.getLocalService(WorkManager.class);
        wm.schedule(work);

        // Shows information about work, and output
        String message = resourcesAccessor.getMessages().get("message.audit.acl.started");
        facesMessages.add(StatusMessage.Severity.INFO, message);
    }

    /**
     * Execute ACL audit asynchronously and send the result to current user.
     */
    protected void buildAndSendByMail(final File tmpFile) throws ClientException {
        final DocumentModel auditRoot = navigationContext.getCurrentDocument();
        final String repository = documentManager.getRepositoryName();
        final String to = currentNuxeoPrincipal.getEmail();
        final String defaultFrom = "noreply@nuxeo.com";
        final String workName = WORK_NAME + auditRoot.getPathAsString();

        if(StringUtils.isBlank(to)){
            facesMessages.add(StatusMessage.Severity.ERROR, "Your email is missing from your profile.");
            return;
        }

        // Work to do and publishing
        final Work work = new Work(workName);
        new RunnableAclAudit(documentManager, auditRoot, work, tmpFile){
            @Override
            public void onAuditDone() {
                // content to send
                FileBlob fb = new FileBlob(getOutputFile(), "application/xls");
                fb.setFilename(OUTPUT_FILE_NAME + OUTPUT_FILE_EXT);

                // do publish
                IResultPublisher publisher = new PublishByMail(fb, to, defaultFrom, repository);
                try {
                    publisher.publish();
                } catch (ClientException e) {
                    log.error(e,e);
                    facesMessages.add(StatusMessage.Severity.ERROR, e.getMessage());
                }
            }
        };

        WorkManager wm = Framework.getLocalService(WorkManager.class);
        wm.schedule(work);

        // Shows information about work, and output
        String message = resourcesAccessor.getMessages().get("message.acl.audit.started");
        facesMessages.add(StatusMessage.Severity.INFO, message);
    }

    /* */

    protected Set<String> existingPermissions = new HashSet<String>();

    {
        AclNameShortner names = new AclNameShortner();
        existingPermissions.addAll(names.getFullNames());
    }

    public Set<String> getExistingPermissions(){
        return existingPermissions;
    }

}
