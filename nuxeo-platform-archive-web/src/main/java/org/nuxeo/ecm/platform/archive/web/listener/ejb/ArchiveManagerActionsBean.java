/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id:ArchiveManagerActionsBean.java 4487 2006-10-19 22:27:14Z janguenot $
 */

package org.nuxeo.ecm.platform.archive.web.listener.ejb;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Begin;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.datamodel.DataModel;
import org.jboss.seam.annotations.datamodel.DataModelSelection;
import org.jboss.seam.core.FacesMessages;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.event.CoreEvent;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.api.event.DocumentEventCategories;
import org.nuxeo.ecm.core.api.event.impl.CoreEventImpl;
import org.nuxeo.ecm.core.api.facet.VersioningDocument;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.platform.archive.api.ArchiveManager;
import org.nuxeo.ecm.platform.archive.api.ArchiveRecord;
import org.nuxeo.ecm.platform.archive.service.NXArchiveFactoryService;
import org.nuxeo.ecm.platform.archive.web.listener.ArchiveManagerActions;
import org.nuxeo.ecm.platform.events.api.DocumentMessage;
import org.nuxeo.ecm.platform.events.api.DocumentMessageProducer;
import org.nuxeo.ecm.platform.events.api.delegate.DocumentMessageProducerBusinessDelegate;
import org.nuxeo.ecm.platform.events.api.impl.DocumentMessageImpl;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;
import org.nuxeo.runtime.api.Framework;

/**
 * Archive manager actions bean.
 * <p>
 *
 * @author <a href="mailto:bt@nuxeo.com">Bogdan Tatar</a>
 */
@Name("archiveManagerActions")
@Scope(CONVERSATION)
public class ArchiveManagerActionsBean implements ArchiveManagerActions {

    private static final long serialVersionUID = -6110545879809627627L;

    private static final Log log = LogFactory.getLog(ArchiveManagerActionsBean.class);

    private boolean showCreateForm = false;

    private boolean required = true;

    private String commandName1;

    private String commandName2;

    private boolean editable = true;

    private String style;

    @DataModel
    private List<ArchiveRecord> archiveRecords;

    @DataModelSelection
    private ArchiveRecord sel;

    @In(value = "selectedArchiveRecord", required = false)
    @Out(value = "selectedArchiveRecord", required = false)
    private ArchiveRecord selectedArchiveRecord;

    @In(required = true)
    private NavigationContext navigationContext;

    @In(create = true, required = false)
    private transient FacesMessages facesMessages;

    @In(create = true)
    private ResourcesAccessor resourcesAccessor;

    private ArchiveManager archiveManager;

    private NXArchiveFactoryService archiveService;

    private transient DocumentMessageProducer docMsgProducer;

    @Create
    public void initialize() {
        try {
            archiveManager = Framework.getService(ArchiveManager.class);
            archiveService = (NXArchiveFactoryService) Framework.getRuntime().getComponent(
                    NXArchiveFactoryService.NAME);
        } catch (Exception e) {
            log.error("Could not get archive manager service", e);
        }
    }

    @Destroy
    public void destroy() {
        archiveManager = null;
        archiveService = null;
        log.debug("Removing Archive Seam component...");
    }

    @Observer(value = EventNames.DOCUMENT_SELECTION_CHANGED, create = false, inject=false)
    public void invalidateArchiveRecords() throws Exception {
        log.debug("Invalidate archive records.................");
        archiveRecords = null;
        selectedArchiveRecord = null;
        showCreateForm = false;
        required = true;
    }

    @Begin(join = true)
    @Factory("archiveRecords")
    public void computeArchiveRecords() throws Exception {
        String docUID = archiveService.getArchiveRecordFactory().generateArchiveRecordFrom(
                navigationContext.getCurrentDocument()).getDocUID();
        archiveRecords = archiveManager.getArchiveRecordsByDocUID(docUID);
        log.debug("archiveRecords computed .................!");
    }

    public List<SelectItem> getDocumentVersions() throws Exception {
        List<SelectItem> documentVersions = new ArrayList<SelectItem>();
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        CoreSession currentSession = getCurrentSession();

        VersioningDocument docVer = currentDocument.getAdapter(VersioningDocument.class);
        StringBuffer ver = new StringBuffer();
        ver.append(docVer.getMajorVersion()).append('.').append(
                docVer.getMinorVersion());
        documentVersions.add(new SelectItem(ver.toString(), ver.toString()));

        if (!currentDocument.isProxy()) {
            for (VersionModel version : currentSession.getVersionsForDocument(currentDocument.getRef())) {
                DocumentModel tempDoc = currentSession.getDocumentWithVersion(
                        currentDocument.getRef(), version);
                if (tempDoc != null) {
                    ver = new StringBuffer();
                    docVer = tempDoc.getAdapter(VersioningDocument.class);
                    ver.append(docVer.getMajorVersion()).append('.').append(
                            docVer.getMinorVersion());
                    documentVersions.add(new SelectItem(ver.toString(),
                            ver.toString()));
                }
            }
        }
        return documentVersions;
    }

    public String addArchiveRecord() throws Exception {
        String message;
        String eventId;
        if (selectedArchiveRecord.getId() == 0) {
            archiveManager.addArchiveRecord(selectedArchiveRecord);
            message = "label.archive.record.added";
            eventId = ArchiveRecordEventTypes.ARCHIVE_RECORD_CREATED;
            log.debug("archive record added ...");
        } else {
            archiveManager.editArchiveRecord(selectedArchiveRecord);
            message = "label.archive.record.edited";
            eventId = ArchiveRecordEventTypes.ARCHIVE_RECORD_EDITED;
            log.debug("archive record edited ...");
        }
        archiveRecords.remove(selectedArchiveRecord);
        selectedArchiveRecord = null;
        computeArchiveRecords();

        facesMessages.add(FacesMessage.SEVERITY_INFO,
                resourcesAccessor.getMessages().get(message));

        showCreateForm = false;
        notifyEvent(eventId, navigationContext.getCurrentDocument(), null,
                null, null, true);
        return "document_archive";
    }

    public String deleteArchiveRecord() throws Exception {
        selectedArchiveRecord = sel;
        String message = null;
        if (selectedArchiveRecord != null
                && archiveRecords.contains(selectedArchiveRecord)) {
            Boolean deleted = archiveManager.deleteArchiveRecord(selectedArchiveRecord.getId());
            if (deleted) {
                archiveRecords.remove(selectedArchiveRecord);
                notifyEvent(ArchiveRecordEventTypes.ARCHIVE_RECORD_DELETED,
                        navigationContext.getCurrentDocument(), null, null,
                        null, true);
                message = "label.archive.record.deleted";
                log.debug("archive record deleted ...");
            } else {
                message = "label.archive.record.not.deleted";
                log.debug("there was a problem while trying to delete this archive record !");
            }
        }
        facesMessages.add(FacesMessage.SEVERITY_INFO,
                resourcesAccessor.getMessages().get(message));
        selectedArchiveRecord = null;
        required = true;
        showCreateForm = false;
        return "document_archive";
    }

    public String editArchiveRecord() throws Exception {
        selectedArchiveRecord = sel;
        showCreateForm = true;
        required = true;
        commandName1 = "command.edit";
        commandName2 = "command.cancel";
        editable = true;
        style = "none";
        return "document_archive";
    }

    public String viewArchiveRecord() throws Exception {
        selectedArchiveRecord = sel;
        showCreateForm = true;
        required = true;
        commandName2 = "command.ok";
        editable = false;
        style = "notEditable";
        return "document_archive";
    }

    public boolean getShowCreateForm() {
        return showCreateForm;
    }

    public void setShowCreateForm(boolean showCreateForm) {
        this.showCreateForm = showCreateForm;
    }

    public void toggleCreateForm(ActionEvent event) {
        if (showCreateForm) {
            showCreateForm = false;
        } else {
            showCreateForm = true;
            required = true;
            try {
                selectedArchiveRecord = NXArchiveFactoryService.getArchiveRecordFactory().generateArchiveRecordFrom(
                        navigationContext.getCurrentDocument());
            } catch (Exception e) {
                log.debug(
                        "there was a problem while populating the new archive record with data from the current document",
                        e);
            }
            commandName1 = "command.add";
            commandName2 = "command.cancel";
            style = "none";
            editable = true;
        }
    }

    public void initializeArchiveManager() throws Exception {
        log.debug("Initializing ...");
    }

    public ArchiveRecord getSelectedArchiveRecord() {
        return selectedArchiveRecord;
    }

    public void setSelectedArchiveRecord(ArchiveRecord selectedArchiveRecord) {
        this.selectedArchiveRecord = selectedArchiveRecord;
    }

    public String cancel() throws Exception {
        log.debug("cancel adding/editing archive record ...");
        selectedArchiveRecord = null;
        showCreateForm = false;
        return "document_archive";
    }

    public boolean getRequired() {
        return required;
    }

    public void setRequired(ActionEvent event) {
        required = false;
    }

    public boolean getEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public String getCommandName1() {
        return commandName1;
    }

    public void setCommandName1(String commandName) {
        commandName1 = commandName;
    }

    public String getCommandName2() {
        return commandName2;
    }

    public void setCommandName2(String commandName) {
        commandName2 = commandName;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    private void notifyEvent(String eventId, DocumentModel source,
            Map<String, Object> options, String category, String comment,
            boolean withLifeCycle) throws Exception {

        CoreSession currentSession = getCurrentSession();
        String repositoryName = currentSession.getRepositoryName();

        // Default category
        if (category == null) {
            category = DocumentEventCategories.EVENT_DOCUMENT_CATEGORY;
        }

        if (options == null) {
            options = new HashMap<String, Object>();
        }

        // Name of the current repository
        options.put(CoreEventConstants.REPOSITORY_NAME, repositoryName);

        // Document life cycle
        if (source != null && withLifeCycle) {
            String currentLifeCycleState = null;
            try {
                currentLifeCycleState = source.getCurrentLifeCycleState();
            } catch (ClientException err) {
                // FIXME no lifecycle -- this shouldn't generated an
                // exception (and ClientException logs the spurious error)
            }
            options.put(CoreEventConstants.DOC_LIFE_CYCLE,
                    currentLifeCycleState);
        }

        // Add the session ID
        options.put(CoreEventConstants.SESSION_ID,
                currentSession.getSessionId());

        CoreEvent event = new CoreEventImpl(eventId, source, options,
                currentSession.getPrincipal(), category, comment);

        DocumentMessage message = new DocumentMessageImpl(source, event);

//        EventMessage message = new EventMessageImpl();
//        message.feed(event);
        getDocumentMessageProducer().produce(message);
    }

    private CoreSession getCurrentSession() throws Exception {

        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        CoreSession currentSession = CoreInstance.getInstance().getSession(
                currentDocument.getSessionId());
        if (currentSession == null) {
            String repositoryName = currentDocument.getRepositoryName();
            if (repositoryName == null) {
                log.debug(String.format(
                        "document '%s' has null repositoryName ",
                        currentDocument.getTitle()));
                return null;
            }
            RepositoryManager repositoryManager = Framework.getService(RepositoryManager.class);
            currentSession = repositoryManager.getRepository(repositoryName).open();
            if (currentSession == null) {
                log.debug(String.format("document '%s' has null session ",
                        currentDocument.getTitle()));
                return null;
            }
        }
        return currentSession;
    }

    private DocumentMessageProducer getDocumentMessageProducer()
            throws Exception {
        if (docMsgProducer == null) {
            docMsgProducer = DocumentMessageProducerBusinessDelegate.getRemoteDocumentMessageProducer();
        }
        return docMsgProducer;
    }

}
