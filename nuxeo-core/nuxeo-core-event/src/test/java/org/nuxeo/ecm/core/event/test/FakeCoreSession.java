/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.core.event.test;

import java.io.Serializable;
import java.security.Principal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelIterator;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentModelsChunk;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.Filter;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.SerializableInputStream;
import org.nuxeo.ecm.core.api.Sorter;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.impl.DocsQueryProviderDef;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.operation.Operation;
import org.nuxeo.ecm.core.api.operation.ProgressMonitor;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecuritySummaryEntry;
import org.nuxeo.ecm.core.schema.DocumentType;

public class FakeCoreSession implements CoreSession {

    public void applyDefaultPermissions(String userOrGroupName)
            throws ClientException {
        // TODO Auto-generated method stub

    }

    public boolean canRemoveDocument(DocumentRef docRef) throws ClientException {
        // TODO Auto-generated method stub
        return false;
    }

    public void cancel() throws ClientException {
        // TODO Auto-generated method stub

    }

    public void checkIn(DocumentRef docRef, VersionModel version)
            throws ClientException {
        // TODO Auto-generated method stub

    }

    public void checkOut(DocumentRef docRef) throws ClientException {
        // TODO Auto-generated method stub

    }

    public String connect(String repositoryUri,
            Map<String, Serializable> context) throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public List<DocumentModel> copy(List<DocumentRef> src, DocumentRef dst)
            throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentModel copy(DocumentRef src, DocumentRef dst, String name)
            throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public List<DocumentModel> copyProxyAsDocument(List<DocumentRef> src,
            DocumentRef dst) throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentModel copyProxyAsDocument(DocumentRef src, DocumentRef dst,
            String name) throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentModel createDocument(DocumentModel model)
            throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentModel[] createDocument(DocumentModel[] docModels)
            throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public void importDocuments(List<DocumentModel> docModels)
            throws ClientException {
    }

    public DocumentModel createDocumentModel(String typeName)
            throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentModel createDocumentModel(String typeName,
            Map<String, Object> options) throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentModel createDocumentModel(String parentPath, String id,
            String typeName) throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentModel createProxy(DocumentRef parentRef, DocumentRef docRef,
            VersionModel version, boolean overwriteExistingProxy)
            throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public void destroy() {
        // TODO Auto-generated method stub

    }

    public void disconnect() throws ClientException {
        // TODO Auto-generated method stub

    }

    public boolean exists(DocumentRef docRef) throws ClientException {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean followTransition(DocumentRef docRef, String transition)
            throws ClientException {
        // TODO Auto-generated method stub
        return false;
    }

    public String generateVersionLabelFor(DocumentRef docRef)
            throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public ACP getACP(DocumentRef docRef) throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public Collection<String> getAllowedStateTransitions(DocumentRef docRef)
            throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public List<String> getAvailableSecurityPermissions()
            throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentModel getChild(DocumentRef parent, String name)
            throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentModelList getChildren(DocumentRef parent)
            throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentModelList getChildren(DocumentRef parent, String type)
            throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentModelList getChildren(DocumentRef parent, String type,
            String perm) throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentModelList getChildren(DocumentRef parent, String type,
            Filter filter, Sorter sorter) throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentModelList getChildren(DocumentRef parent, String type,
            String perm, Filter filter, Sorter sorter) throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentModelIterator getChildrenIterator(DocumentRef parent)
            throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentModelIterator getChildrenIterator(DocumentRef parent,
            String type) throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentModelIterator getChildrenIterator(DocumentRef parent,
            String type, String perm, Filter filter) throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public List<DocumentRef> getChildrenRefs(DocumentRef parentRef, String perm)
            throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public SerializableInputStream getContentData(String key)
            throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public String getCurrentLifeCycleState(DocumentRef docRef)
            throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public DataModel getDataModel(DocumentRef docRef, String schema)
            throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public Object getDataModelField(DocumentRef docRef, String schema,
            String field) throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public Object[] getDataModelFields(DocumentRef docRef, String schema,
            String[] fields) throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public Object[] getDataModelsField(DocumentRef[] docRefs, String schema,
            String field) throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public Object[] getDataModelsFieldUp(DocumentRef docRef, String schema,
            String field) throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentModelsChunk getDocsResultChunk(DocsQueryProviderDef def,
            String type, String perm, Filter filter, int start, int count)
            throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentModel getDocument(DocumentRef docRef) throws ClientException {
        DocumentRef parentRef = new IdRef("01");
        String[] schemas = {"file","dublincore"};
        DocumentModel srcDoc = new DocumentModelImpl("sid0", "File", "02", new Path("/"), docRef, parentRef, schemas, null);
        return srcDoc;
    }

    public DocumentModel getDocument(DocumentRef docRef, String[] schemas)
            throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public <T extends Serializable> T getDocumentSystemProp(DocumentRef ref,
            String systemProperty, Class<T> type) throws ClientException,
            DocumentException {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentType getDocumentType(String type) {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentModel getVersion(String versionableId,
            VersionModel versionModel) throws ClientException {
        return null;
    }

    public DocumentModel getDocumentWithVersion(DocumentRef docRef,
            VersionModel version) throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentModelList getDocuments(DocumentRef[] docRefs)
            throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentModelList getFiles(DocumentRef parent)
            throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentModelList getFiles(DocumentRef parent, Filter filter,
            Sorter sorter) throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentModelIterator getFilesIterator(DocumentRef parent)
            throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentModelList getFolders(DocumentRef parent)
            throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentModelList getFolders(DocumentRef parent, Filter filter,
            Sorter sorter) throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentModelIterator getFoldersIterator(DocumentRef parent)
            throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentModel getLastDocumentVersion(DocumentRef docRef)
            throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public VersionModel getLastVersion(DocumentRef docRef)
            throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public String getLifeCyclePolicy(DocumentRef docRef) throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public String getLock(DocumentRef doc) throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentModel getParentDocument(DocumentRef docRef)
            throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentRef[] getParentDocumentRefs(DocumentRef docRef)
            throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public List<DocumentModel> getParentDocuments(DocumentRef docRef)
            throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public Principal getPrincipal() {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentModelList getProxies(DocumentRef docRef,
            DocumentRef folderRef) throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public String[] getProxyVersions(DocumentRef docRef, DocumentRef folderRef)
            throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public String getRepositoryName() {
        return "default";
    }

    public DocumentModel getRootDocument() throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public List<SecuritySummaryEntry> getSecuritySummary(
            DocumentModel docModel, Boolean includeParents)
            throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public String getSessionId() {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentModel getSourceDocument(DocumentRef docRef)
            throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public String getStreamURI(String blobPropertyId) throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public String getSuperParentType(DocumentModel doc) throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentModel getSuperSpace(DocumentModel doc)
            throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public List<DocumentModel> getVersions(DocumentRef docRef)
            throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public List<VersionModel> getVersionsForDocument(DocumentRef docRef)
            throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public List<DocumentRef> getVersionsRefs(DocumentRef docRef)
            throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean hasChildren(DocumentRef docRef) throws ClientException {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean hasPermission(DocumentRef docRef, String permission)
            throws ClientException {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isCheckedOut(DocumentRef docRef) throws ClientException {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isDirty(DocumentRef doc) throws ClientException {
        // TODO Auto-generated method stub
        return false;
    }

    public VersionModel isPublished(DocumentModel document,
            DocumentModel section) {
        // TODO Auto-generated method stub
        return null;
    }

    public void move(List<DocumentRef> src, DocumentRef dst)
            throws ClientException {
        // TODO Auto-generated method stub

    }

    public DocumentModel move(DocumentRef src, DocumentRef dst, String name)
            throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public void orderBefore(DocumentRef parent, String src, String dest)
            throws ClientException {
        // TODO Auto-generated method stub

    }

    public DocumentModel publishDocument(DocumentModel docToPublish,
            DocumentModel section) throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentModel publishDocument(DocumentModel docToPublish,
            DocumentModel section, boolean overwriteExistingProxy)
            throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentModelList query(String query) throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentModelList query(String query, int max)
            throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentModelList query(String query, Filter filter)
            throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentModelList query(String query, Filter filter, int max)
            throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentModelIterator queryIt(String query, Filter filter, int max)
            throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentModelList querySimpleFts(String keywords)
            throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentModelList querySimpleFts(String keywords, Filter filter)
            throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentModelIterator querySimpleFtsIt(String query, Filter filter,
            int pageSize) throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentModelIterator querySimpleFtsIt(String query,
            String startingPath, Filter filter, int pageSize)
            throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public Object[] refreshDocument(DocumentRef ref, int refreshFlags,
            String[] schemas) throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public void removeChildren(DocumentRef docRef) throws ClientException {
        // TODO Auto-generated method stub

    }

    public void removeDocument(DocumentRef docRef) throws ClientException {
        // TODO Auto-generated method stub

    }

    public void removeDocuments(DocumentRef[] docRefs) throws ClientException {
        // TODO Auto-generated method stub

    }

    public DocumentModel restoreToVersion(DocumentRef docRef,
            VersionModel version) throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public <T> T run(Operation<T> cmd) throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public <T> T run(Operation<T> op, ProgressMonitor monitor)
            throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public void save() throws ClientException {
        // TODO Auto-generated method stub

    }

    public DocumentModel saveDocument(DocumentModel docModel)
            throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public void saveDocuments(DocumentModel[] docModels) throws ClientException {
        // TODO Auto-generated method stub

    }

    public void setACP(DocumentRef docRef, ACP acp, boolean overwrite)
            throws ClientException {
        // TODO Auto-generated method stub

    }

    public <T extends Serializable> void setDocumentSystemProp(DocumentRef ref,
            String systemProperty, T value) throws ClientException,
            DocumentException {
        // TODO Auto-generated method stub

    }

    public void setLock(DocumentRef doc, String key) throws ClientException {
        // TODO Auto-generated method stub

    }

    public String unlock(DocumentRef docRef) throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentModelList query(String query, Filter filter, long limit,
            long offset, boolean countTotal) throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public String[] getPermissionsToCheck(String permission) {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentModel restoreToVersion(DocumentRef docRef,
            VersionModel version, boolean skipSnapshotCreation)
            throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

}
