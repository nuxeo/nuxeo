/*
 * (C) Copyright 2002 - 2006 Nuxeo SARL <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 *
 * $Id: AbstractPlugin.java 4105 2006-10-15 12:29:25Z sfermigier $
 */

package org.nuxeo.ecm.platform.filemanager.service.extension;

import static org.nuxeo.ecm.core.api.security.SecurityConstants.ADD_CHILDREN;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.READ_PROPERTIES;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.pathsegment.PathSegmentService;
import org.nuxeo.ecm.core.versioning.VersioningService;
import org.nuxeo.ecm.platform.filemanager.service.FileManagerService;
import org.nuxeo.ecm.platform.filemanager.utils.FileManagerUtils;
import org.nuxeo.ecm.platform.types.Type;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.runtime.api.Framework;

/**
 * File importer abstract class.
 * <p>
 * Default file importer behavior.
 *
 * @see FileImporter
 *
 * @author <a href="mailto:akalogeropoulos@nuxeo.com">Andreas Kalogeropolos</a>
 */
public abstract class AbstractFileImporter implements FileImporter {

    private static final long serialVersionUID = 1L;

    protected String name = "";

    protected String docType;

    protected List<String> filters = new ArrayList<String>();

    protected List<Pattern> patterns;

    protected boolean enabled = true;

    protected Integer order = 0;

    // to be used by plugin implementation to gain access to standard file
    // creation utility methods without having to lookup the service
    protected FileManagerService fileManagerService;

    public List<String> getFilters() {
        return filters;
    }

    public void setFilters(List<String> filters) {
        this.filters = filters;
        patterns = new ArrayList<Pattern>();
        for (String filter : filters) {
            patterns.add(Pattern.compile(filter));
        }
    }

    public boolean matches(String mimeType) {
        for (Pattern pattern : patterns) {
            if (pattern.matcher(mimeType).matches()) {
                return true;
            }
        }
        return false;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDocType() {
        return docType;
    }

    public void setDocType(String docType) {
        this.docType = docType;
    }

    /**
     * Gets the doc type to use in the given container.
     */
    public String getDocType(DocumentModel container) {
        return getDocType(); // use XML configuration
    }

    /**
     * Default document type to use when the plugin XML configuration does not
     * specify one.
     * <p>
     * To implement when the default {@link #create} method is used.
     */
    public String getDefaultDocType() {
        throw new UnsupportedOperationException();
    }

    /**
     * Whether document overwrite is detected by checking title or filename.
     * <p>
     * To implement when the default {@link #create} method is used.
     */
    public boolean isOverwriteByTitle() {
        throw new UnsupportedOperationException();
    }

    /**
     * Creates the document (sets its properties). {@link #updateDocument} will
     * be called after this.
     * <p>
     * Default implementation sets the title.
     */
    public void createDocument(DocumentModel doc, Blob content, String title)
            throws ClientException {
        doc.setPropertyValue("dc:title", title);
    }

    /**
     * Updates the document (sets its properties).
     * <p>
     * Default implementation sets the content.
     */
    public void updateDocument(DocumentModel doc, Blob content)
            throws ClientException {
        try {
            content = content.persist();
        } catch (IOException e) {
            throw new ClientException(e);
        }
        doc.getAdapter(BlobHolder.class).setBlob(content);
    }

    @Override
    public DocumentModel create(CoreSession session, Blob content, String path,
            boolean overwrite, String fullname, TypeManager typeService)
            throws ClientException, IOException {
        path = getNearestContainerPath(session, path);
        DocumentModel container = session.getDocument(new PathRef(path));
        String docType = getDocType(container); // from override or descriptor
        if (docType == null) {
            docType = getDefaultDocType();
        }
        doSecurityCheck(session, path, docType, typeService);
        String filename = FileManagerUtils.fetchFileName(fullname);
        String title = FileManagerUtils.fetchTitle(filename);
        content.setFilename(filename);
        // look for an existing document with same title or filename
        DocumentModel doc;
        if (isOverwriteByTitle()) {
            doc = FileManagerUtils.getExistingDocByTitle(session, path, title);
        } else {
            doc = FileManagerUtils.getExistingDocByFileName(session, path,
                    filename);
        }
        if (overwrite && doc != null) {
            // make sure we save any existing data
            checkIn(doc);
            // update data
            updateDocument(doc, content);
            // save
            doc = doc.getCoreSession().saveDocument(doc);
        } else {
            // create document model
            doc = session.createDocumentModel(docType);
            createDocument(doc, content, title);
            // set path
            PathSegmentService pss = Framework.getLocalService(PathSegmentService.class);
            doc.setPathInfo(path, pss.generatePathSegment(doc));
            // update data
            updateDocument(doc, content);
            // create
            doc = session.createDocument(doc);
        }
        // check in if requested
        checkInAfterAdd(doc);
        session.save();
        return doc;
    }

    public FileManagerService getFileManagerService() {
        return fileManagerService;
    }

    public void setFileManagerService(FileManagerService fileManagerService) {
        this.fileManagerService = fileManagerService;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public int compareTo(FileImporter other) {
        Integer otherOrder = other.getOrder();
        if (order == null && otherOrder == null) {
            return 0;
        } else if (order == null) {
            return 1;
        } else if (otherOrder == null) {
            return -1;
        }
        return order.compareTo(otherOrder);
    }

    // TODO: remove
    protected TypeManager getTypeService() throws ClientException {
        try {
            return Framework.getService(TypeManager.class);
        } catch (Exception e) {
            throw new ClientException(e);
        }
    }

    /**
     * Returns nearest container path
     * <p>
     * If given path points to a folderish document, return it. Else, return
     * parent path.
     */
    protected String getNearestContainerPath(CoreSession documentManager,
            String path) throws ClientException {
        DocumentModel currentDocument = documentManager.getDocument(new PathRef(
                path));
        if (!currentDocument.isFolder()) {
            path = path.substring(0, path.lastIndexOf('/'));
        }
        return path;
    }

    protected void checkIn(DocumentModel doc) throws ClientException {
        VersioningOption option = fileManagerService.getVersioningOption();
        if (option != null && option != VersioningOption.NONE) {
            if (doc.isCheckedOut()) {
                doc.checkIn(option, null);
            }
        }
    }

    protected void checkInAfterAdd(DocumentModel doc) throws ClientException {
        if (fileManagerService.doVersioningAfterAdd()) {
            checkIn(doc);
        }
    }

    /**
     * @deprecated use {@link #checkIn} instead, noting that it does not save
     *             the document
     */
    @Deprecated
    protected DocumentModel overwriteAndIncrementversion(
            CoreSession documentManager, DocumentModel doc)
            throws ClientException {
        doc.putContextData(VersioningService.VERSIONING_OPTION,
                fileManagerService.getVersioningOption());
        return documentManager.saveDocument(doc);
    }

    protected void doSecurityCheck(CoreSession documentManager, String path,
            String typeName, TypeManager typeService)
            throws DocumentSecurityException, ClientException {
        // perform the security checks
        PathRef containerRef = new PathRef(path);
        if (!documentManager.hasPermission(containerRef, READ_PROPERTIES)
                || !documentManager.hasPermission(containerRef, ADD_CHILDREN)) {
            throw new DocumentSecurityException(
                    "Not enough rights to create folder");
        }
        DocumentModel container = documentManager.getDocument(containerRef);

        Type containerType = typeService.getType(container.getType());
        if (containerType == null) {
            return;
        }

        if (!typeService.isAllowedSubType(typeName, container.getType(),
                container)) {
            throw new ClientException(
                    String.format(
                            "Cannot create document of type %s in container with type %s",
                            typeName, containerType.getId()));
        }
    }

}
