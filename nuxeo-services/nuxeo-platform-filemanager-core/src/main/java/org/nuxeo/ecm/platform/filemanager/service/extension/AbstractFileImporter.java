/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.platform.filemanager.service.extension;

import static org.nuxeo.ecm.core.api.security.SecurityConstants.ADD_CHILDREN;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.READ_PROPERTIES;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.pathsegment.PathSegmentService;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.ecm.platform.filemanager.api.FileImporterContext;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
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
 * @author <a href="mailto:akalogeropoulos@nuxeo.com">Andreas Kalogeropolos</a>
 */
public abstract class AbstractFileImporter implements FileImporter {

    private static final long serialVersionUID = 1L;

    protected String name = "";

    protected String docType;

    protected transient List<String> filters = new ArrayList<>();

    protected transient List<Pattern> patterns;

    protected boolean enabled = true;

    protected Integer order = 0;

    public static final String SKIP_UPDATE_AUDIT_LOGGING = "org.nuxeo.filemanager.skip.audit.logging.forupdates";

    // duplicated from Audit module to avoid circular dependency
    public static final String DISABLE_AUDIT_LOGGER = "disableAuditLogger";

    // to be used by plugin implementation to gain access to standard file
    // creation utility methods without having to lookup the service
    /**
     * @deprecated since 10.3, use {@link Framework#getService(Class)} instead if needed
     */
    @Deprecated(since = "10.3")
    protected transient FileManagerService fileManagerService;

    protected AbstractFileImporter() {
        this.fileManagerService = (FileManagerService) Framework.getService(FileManager.class);
    }

    @Override
    public List<String> getFilters() {
        return filters;
    }

    @Override
    public void setFilters(List<String> filters) {
        this.filters = filters;
        patterns = new ArrayList<>();
        for (String filter : filters) {
            patterns.add(Pattern.compile(filter));
        }
    }

    @Override
    public boolean matches(String mimeType) {
        for (Pattern pattern : patterns) {
            if (pattern.matcher(mimeType).matches()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDocType() {
        return docType;
    }

    @Override
    public void setDocType(String docType) {
        this.docType = docType;
    }

    /**
     * Gets the doc type to use in the given container.
     */
    protected String getDocType(DocumentModel container) { // NOSONAR
        return getDocType(); // use XML configuration
    }

    /**
     * Default document type to use when the plugin XML configuration does not specify one.
     * <p>
     * To implement when the default {@link #createOrUpdate(FileImporterContext)} method is used.
     */
    protected String getDefaultDocType() {
        throw new UnsupportedOperationException();
    }

    /**
     * Whether document overwrite is detected by checking title or filename.
     * <p>
     * To implement when the default {@link #createOrUpdate(FileImporterContext)} method is used.
     */
    protected boolean isOverwriteByTitle() {
        throw new UnsupportedOperationException();
    }

    /**
     * Creates the document (sets its properties). {@link #updateDocument} will be called after this.
     * <p>
     * Default implementation sets the title.
     */
    protected void createDocument(DocumentModel doc, String title) {
        doc.setPropertyValue("dc:title", title);
    }

    /**
     * Tries to update the document <code>doc</code> with the blob <code>content</code>.
     * <p>
     * Returns <code>true</code> if the document is really updated.
     *
     * @since 7.1
     */
    protected boolean updateDocumentIfPossible(DocumentModel doc, Blob content) {
        updateDocument(doc, content);
        return true;
    }

    /**
     * Updates the document (sets its properties).
     * <p>
     * Default implementation sets the content.
     */
    protected void updateDocument(DocumentModel doc, Blob content) {
        doc.getAdapter(BlobHolder.class).setBlob(content);
    }

    protected Blob getBlob(DocumentModel doc) {
        return doc.getAdapter(BlobHolder.class).getBlob();
    }

    @Override
    public boolean isOneToMany() {
        return false;
    }

    @Override
    public DocumentModel create(CoreSession session, Blob content, String path, boolean overwrite, String fullname,
            TypeManager typeService) throws IOException {
        FileImporterContext context = FileImporterContext.builder(session, content, path)
                                                         .overwrite(overwrite)
                                                         .fileName(fullname)
                                                         .build();
        return createOrUpdate(context);
    }

    @Override
    public DocumentModel createOrUpdate(FileImporterContext context) throws IOException {
        CoreSession session = context.getSession();
        String path = getNearestContainerPath(session, context.getParentPath());
        DocumentModel container = session.getDocument(new PathRef(path));
        String targetDocType = getDocType(container); // from override or descriptor
        if (targetDocType == null) {
            targetDocType = getDefaultDocType();
        }
        doSecurityCheck(session, path, targetDocType);

        Blob blob = context.getBlob();
        String filename = FileManagerUtils.fetchFileName(context.getFileName());
        String title = FileManagerUtils.fetchTitle(filename);
        blob.setFilename(filename);
        // look for an existing document with same title or filename
        DocumentModel doc;
        if (isOverwriteByTitle()) {
            doc = FileManagerUtils.getExistingDocByTitle(session, path, title);
        } else {
            doc = FileManagerUtils.getExistingDocByFileName(session, path, filename);
        }
        if (context.isOverwrite() && doc != null) {
            Blob previousBlob = getBlob(doc);
            // check that previous blob allows overwrite
            if (previousBlob != null) {
                BlobProvider blobProvider = Framework.getService(BlobManager.class).getBlobProvider(previousBlob);
                if (blobProvider != null && !blobProvider.supportsUserUpdate()) {
                    throw new DocumentSecurityException("Cannot overwrite blob");
                }
            }
            // update data
            boolean isDocumentUpdated = updateDocumentIfPossible(doc, blob);
            if (!isDocumentUpdated) {
                return null;
            }
            if (Framework.isBooleanPropertyTrue(SKIP_UPDATE_AUDIT_LOGGING)) {
                // skip the update event if configured to do so
                doc.putContextData(DISABLE_AUDIT_LOGGER, true);
            }
            if (context.isPersistDocument()) {
                // save
                doc.putContextData(CoreSession.SOURCE, "fileimporter-" + getName());
                doc = doc.getCoreSession().saveDocument(doc);
                session.save();
            }
        } else {
            // create document model
            doc = session.createDocumentModel(targetDocType);
            createDocument(doc, title);
            // set path
            PathSegmentService pss = Framework.getService(PathSegmentService.class);
            doc.setPathInfo(path, pss.generatePathSegment(doc));
            // update data
            updateDocument(doc, blob);
            if (context.isPersistDocument()) {
                // create
                doc.putContextData(CoreSession.SOURCE, "fileimporter-" + getName());
                doc = session.createDocument(doc);
                session.save();
            }
        }
        return doc;
    }

    /**
     * Avoid checkin for a 0-length blob. Microsoft-WebDAV-MiniRedir first creates a 0-length file and then locks it
     * before putting the real file. But we don't want this first placeholder to cause a versioning event.
     *
     * @deprecated since 9.1 automatic versioning is now handled at versioning service level, remove versioning
     *             behaviors from importers
     */
    @Deprecated(since = "9.1")
    protected boolean skipCheckInForBlob(Blob blob) {
        return blob == null || blob.getLength() == 0;
    }

    /**
     * @deprecated since 10.3, use {@link Framework#getService(Class)} instead if needed
     */
    @Deprecated(since = "10.3")
    public FileManagerService getFileManagerService() {
        return fileManagerService;
    }

    /**
     * @deprecated since 10.3, use {@link Framework#getService(Class)} instead if needed
     */
    @Deprecated(since = "10.3")
    @Override
    public void setFileManagerService(FileManagerService fileManagerService) {
        this.fileManagerService = fileManagerService;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public Integer getOrder() {
        return order;
    }

    @Override
    public void setOrder(Integer order) {
        this.order = order;
    }

    @Override
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

    /**
     * Returns nearest container path
     * <p>
     * If given path points to a folderish document, return it. Else, return parent path.
     */
    protected String getNearestContainerPath(CoreSession documentManager, String path) {
        DocumentModel currentDocument = documentManager.getDocument(new PathRef(path));
        if (!currentDocument.isFolder()) {
            path = path.substring(0, path.lastIndexOf('/'));
        }
        return path;
    }

    /**
     * @deprecated since 9.1 automatic versioning is now handled at versioning service level, remove versioning
     *             behaviors from importers
     */
    @Deprecated(since = "9.1")
    protected void checkIn(DocumentModel doc) {
        VersioningOption option = fileManagerService.getVersioningOption();
        if (option != null && option != VersioningOption.NONE && doc.isCheckedOut()) {
            doc.checkIn(option, null);
        }
    }

    /**
     * @deprecated since 9.1 automatic versioning is now handled at versioning service level, remove versioning
     *             behaviors from importers
     */
    @Deprecated(since = "9.1")
    protected void checkInAfterAdd(DocumentModel doc) {
        if (fileManagerService.doVersioningAfterAdd()) {
            checkIn(doc);
        }
    }

    /**
     * @since 10.10
     */
    protected void doSecurityCheck(CoreSession documentManager, String path, String typeName) {
        doSecurityCheck(documentManager, path, typeName, Framework.getService(TypeManager.class));
    }

    protected void doSecurityCheck(CoreSession documentManager, String path, String typeName, TypeManager typeService) {
        // perform the security checks
        PathRef containerRef = new PathRef(path);
        if (!documentManager.hasPermission(containerRef, READ_PROPERTIES)
                || !documentManager.hasPermission(containerRef, ADD_CHILDREN)) {
            throw new DocumentSecurityException("Not enough rights to create folder");
        }
        DocumentModel container = documentManager.getDocument(containerRef);

        Type containerType = typeService.getType(container.getType());
        if (containerType == null) {
            return;
        }

        if (!typeService.isAllowedSubType(typeName, container.getType(), container)) {
            throw new NuxeoException(String.format("Cannot create document of type %s in container with type %s",
                    typeName, containerType.getId()));
        }
    }

}
