/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.api;

import java.io.Serializable;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel.DocumentModelRefresh;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.types.Schema;

/**
 * A session to the Nuxeo Core.
 *
 * @see DocumentModel
 * @see DocumentRef
 * @author Bogdan Stefanescu
 * @author Florent Guillaume
 */
public interface CoreSession extends AutoCloseable {

    // used to pass properties to importDocument
    String IMPORT_VERSION_VERSIONABLE_ID = "ecm:versionableId";

    String IMPORT_VERSION_CREATED = "ecm:versionCreated";

    String IMPORT_VERSION_LABEL = "ecm:versionLabel";

    String IMPORT_VERSION_DESCRIPTION = "ecm:versionDescription";

    String IMPORT_VERSION_IS_LATEST = "ecm:isLatestVersion";

    String IMPORT_VERSION_IS_LATEST_MAJOR = "ecm:isLatestMajorVersion";

    String IMPORT_IS_VERSION = "ecm:isVersion";

    String IMPORT_VERSION_MAJOR = "ecm:majorVersion";

    String IMPORT_VERSION_MINOR = "ecm:minorVersion";

    String IMPORT_PROXY_TARGET_ID = "ecm:proxyTargetId";

    String IMPORT_PROXY_VERSIONABLE_ID = "ecm:proxyVersionableId";

    String IMPORT_LIFECYCLE_POLICY = "ecm:lifeCyclePolicy";

    String IMPORT_LIFECYCLE_STATE = "ecm:lifeCycleState";

    /** @since 5.4.2 */
    String IMPORT_LOCK_OWNER = "ecm:lockOwner";

    /**
     * Lock creation time as a Calendar object.
     *
     * @since 5.4.2
     */
    String IMPORT_LOCK_CREATED = "ecm:lockCreated";

    String IMPORT_CHECKED_IN = "ecm:isCheckedIn";

    String IMPORT_BASE_VERSION_ID = "ecm:baseVersionId";

    /** The document type to use to create a proxy by import. */
    String IMPORT_PROXY_TYPE = "ecm:proxy";

    /**
     * Allow version write, Boolean parameter passed in context data at saveDocument time.
     *
     * @since 5.9.2
     */
    String ALLOW_VERSION_WRITE = "allowVersionWrite";

    /**
     * Closes this session.
     *
     * @since 5.9.3
     */
    @Override
    void close();

    /**
     * Destroys any system resources held by this instance.
     * <p>
     * Called when the instance is no more needed.
     */
    void destroy();

    /**
     * Gets the document type object given its type name.
     *
     * @param type the document type name
     * @return the type the doc type object
     */
    DocumentType getDocumentType(String type);

    /**
     * Returns true if the session is currently connected to the repository.
     */
    boolean isLive(boolean onThread);

    /**
     * Cancels any pending change made through this session.
     */
    void cancel();

    /**
     * Saves any pending changes done until now through this session.
     */
    void save();

    /**
     * Gets the current session id.
     * <p>
     * If the client is not connected returns null.
     *
     * @return the session id or null if not connected
     */
    String getSessionId();

    /**
     * Returns {@code true} if all sessions in the current thread share the same state.
     *
     * @deprecated since 8.4 as it always returns true by design
     */
    @Deprecated
    boolean isStateSharedByAllThreadSessions();

    /**
     * Gets the principal that created the client session.
     *
     * @return the principal
     */
    Principal getPrincipal();

    /**
     * Checks if the principal that created the client session has the given privilege on the referred document.
     */
    boolean hasPermission(DocumentRef docRef, String permission);

    /**
     * Checks if a given principal has the given privilege on the referred document.
     */
    boolean hasPermission(Principal principal, DocumentRef docRef, String permission);

    /**
     * Gets the root document of this repository.
     *
     * @return the root document. cannot be null
     */
    DocumentModel getRootDocument();

    /**
     * Gets a document model given its reference.
     * <p>
     * The default schemas are used to populate the returned document model. Default schemas are configured via the
     * document type manager.
     * <p>
     * Any other data model not part of the default schemas will be lazily loaded as needed.
     *
     * @param docRef the document reference
     * @return the document
     * @throws DocumentNotFoundException if the document cannot be found
     */
    DocumentModel getDocument(DocumentRef docRef) throws DocumentNotFoundException;

    /**
     * Gets a list of documents given their references.
     * <p>
     * Documents that are not accessible are skipped.
     *
     * @throws DocumentNotFoundException if a document cannot be found
     */
    DocumentModelList getDocuments(DocumentRef[] docRefs) throws DocumentNotFoundException;

    /**
     * Gets a child document given its name and the parent reference.
     * <p>
     * Throws an exception if the document could not be found.
     * <p>
     * If the supplied id is null, returns the default child of the document if any, otherwise raises an exception.
     * <p>
     * If the parent is null or its path is null, then root is considered.
     *
     * @param parent the reference to the parent document
     * @param name the name of the child document to retrieve
     * @return the named child if exists
     * @throws DocumentNotFoundException if there is no child with the given name
     */
    DocumentModel getChild(DocumentRef parent, String name);

    /**
     * Tests if the document has a child with the given name.
     * <p>
     * This operation silently ignores non-folder documents: If the document is not a folder then returns false.
     *
     * @param parent the document
     * @param name the child name
     * @return {@code true} if the document has a child with the given name
     * @since 7.3
     */
    boolean hasChild(DocumentRef parent, String name);

    /**
     * Gets the children of the given parent.
     *
     * @param parent the parent reference
     * @return the children if any, an empty list if no children or null if the specified parent document is not a
     *         folder
     */
    DocumentModelList getChildren(DocumentRef parent);

    /**
     * Gets an iterator to the children of the given parent.
     *
     * @param parent the parent reference
     * @return iterator over the children collection or null if the specified parent document is not a folder
     */
    DocumentModelIterator getChildrenIterator(DocumentRef parent);

    /**
     * Gets the children of the given parent filtered according to the given document type.
     *
     * @param parent the parent reference
     * @param type the wanted document type
     * @return the documents if any, an empty list if none were found or null if the parent document is not a folder
     */
    DocumentModelList getChildren(DocumentRef parent, String type);

    /**
     * Gets an iterator to the children of the given parent filtered according to the given document type.
     */
    DocumentModelIterator getChildrenIterator(DocumentRef parent, String type);

    /**
     * Gets the children of the given parent filtered according to the given document type and permission.
     *
     * @param parent the parent reference
     * @param type the wanted document type
     * @param perm the permission the user must have
     * @return the documents if any, an empty list if none were found or null if the parent document is not a folder
     */
    DocumentModelList getChildren(DocumentRef parent, String type, String perm);

    /**
     * Same as {@link #getChildren(DocumentRef, String, String)} but the result is filtered and then sorted using the
     * specified filter and sorter.
     *
     * @param parent the parent reference
     * @param type the wanted type
     * @param perm permission to check for. If null, defaults to READ
     * @param filter the filter to use if any, null otherwise
     * @param sorter the sorter to use if any, null otherwise
     * @return the list of the children or an empty list if no children were found or null if the given parent is not a
     *         folder
     */
    DocumentModelList getChildren(DocumentRef parent, String type, String perm, Filter filter, Sorter sorter);

    /**
     * Gets the references of the children. No permission is checked if perm is null.
     *
     * @param parentRef the parent reference
     * @param perm the permission to check on the children (usually READ); if null, <b>no permission is checked</b>
     * @return a list of children references
     * @since 1.4.1
     */
    List<DocumentRef> getChildrenRefs(DocumentRef parentRef, String perm);

    /**
     * Gets the children of the given parent filtered according to the given document type and permission. Long result
     * sets are loaded frame by frame transparently by the DocumentModelIterator.
     */
    DocumentModelIterator getChildrenIterator(DocumentRef parent, String type, String perm, Filter filter);

    /**
     * Same as {@link #getChildren(DocumentRef, String, String, Filter, Sorter)} without specific permission filtering.
     *
     * @param parent the parent reference
     * @param type the wanted type
     * @param filter the filter to use if any, null otherwise
     * @param sorter the sorter to use if any, null otherwise
     * @return the list of the children or an empty list if no children were found or null if the given parent is not a
     *         folder
     */
    DocumentModelList getChildren(DocumentRef parent, String type, Filter filter, Sorter sorter);

    /**
     * Same as {@link CoreSession#getChildren(DocumentRef)} but returns only folder documents.
     *
     * @param parent the parent ref
     * @return a list of children if any, an empty one if none or null if the given parent is not a folder
     */
    DocumentModelList getFolders(DocumentRef parent);

    /**
     * Same as {@link CoreSession#getFolders(DocumentRef)} but uses an optional filter and sorter on the result.
     *
     * @param parent the parent reference
     * @param filter the filter to use or null if none
     * @param sorter the sorter to use or null if none
     * @return a list of children if any, an empty one if none or null if the given parent is not a folder
     */
    DocumentModelList getFolders(DocumentRef parent, Filter filter, Sorter sorter);

    /**
     * Same as {@link CoreSession#getChildren(DocumentRef)} but returns only non-folder documents.
     *
     * @param parent the parent reference
     * @return a list of children if any, an empty one if none or null if the given parent is not a folder
     */
    DocumentModelList getFiles(DocumentRef parent);

    /**
     * Same as {@link #getFiles} but uses an optional filter and sorter on the result.
     *
     * @param parent the parent reference
     * @param filter the filter to use or null if none
     * @param sorter the sorter to use or null if none
     * @return a list of children if any, an empty one if none or null if the given parent is not a folder
     */
    DocumentModelList getFiles(DocumentRef parent, Filter filter, Sorter sorter);

    /**
     * Returns the parent ref of the document referenced by {@code docRef} or {@code null} if this is the root document.
     * <p>
     * This method does not check the permissions on the parent document of this {@code CoreSession}'s {@code Principal}
     * .
     *
     * @since 5.4.2
     */
    DocumentRef getParentDocumentRef(DocumentRef docRef);

    /**
     * Gets the parent document or null if this is the root document.
     *
     * @return the parent document or null if this is the root document
     */
    DocumentModel getParentDocument(DocumentRef docRef);

    /**
     * Gets the parent documents in path from the root to the given document or empty list if this is the root document.
     * <p>
     * Documents the principal is is not allowed to browse are filtered out the parents list.
     *
     * @return the list with parent documents or empty list if this is the root document
     */
    List<DocumentModel> getParentDocuments(DocumentRef docRef);

    /**
     * Tests if the document pointed by the given reference exists and is accessible.
     * <p>
     * This operation makes no difference between non-existence and permission problems.
     * <p>
     * If the parent is null or its path is null, then root is considered.
     *
     * @param docRef the reference to the document to test for existence
     * @return true if the referenced document exists, false otherwise
     */
    boolean exists(DocumentRef docRef);

    /**
     * Tests if the document has any children.
     * <p>
     * This operation silently ignores non-folder documents: If the document is not a folder then returns false.
     * <p>
     * If the parent is null or its path is null, then root is considered.
     *
     * @param docRef the reference to the document to test
     * @return true if document has children, false otherwise
     */
    boolean hasChildren(DocumentRef docRef);

    /**
     * Creates a document model using type name.
     * <p>
     * Used to fetch initial datamodels from the type definition.
     * <p>
     * DocumentModel creation notifies a {@link DocumentEventTypes#EMPTY_DOCUMENTMODEL_CREATED} so that core event
     * listener can initialize its content with computed properties.
     *
     * @return the initial document model
     */
    DocumentModel createDocumentModel(String typeName);

    /**
     * Creates a document model using required information.
     * <p>
     * Used to fetch initial datamodels from the type definition.
     * <p>
     * DocumentModel creation notifies a {@link DocumentEventTypes#EMPTY_DOCUMENTMODEL_CREATED} so that core event
     * listener can initialize its content with computed properties.
     *
     * @param parentPath the parent path
     * @param name The destination name
     * @param typeName the type name
     * @return the initial document model
     */
    DocumentModel createDocumentModel(String parentPath, String name, String typeName);

    /**
     * Creates a document model using required information.
     * <p>
     * Used to fetch initial datamodels from the type definition.
     * <p>
     * DocumentModel creation notifies a {@link DocumentEventTypes#EMPTY_DOCUMENTMODEL_CREATED} so that core event
     * listener can initialize its content with computed properties.
     *
     * @param typeName the type name
     * @param options additional contextual data provided to core event listeners
     * @return the initial document model
     */
    DocumentModel createDocumentModel(String typeName, Map<String, Object> options);

    /**
     * Creates a document using given document model for initialization.
     * <p>
     * The model contains path of the new document, its type and optionally the initial data models of the document.
     * <p>
     *
     * @param model the document model to use for initialization
     * @return the created document
     */
    DocumentModel createDocument(DocumentModel model);

    /**
     * Bulk creation of documents.
     *
     * @param docModels the document models to use for intialization
     * @return the created documents
     */
    DocumentModel[] createDocument(DocumentModel[] docModels);

    /**
     * Low-level import of documents, reserved for the administrator.
     * <p>
     * This method is used to import documents with given ids, or directly import versions and proxies.
     * <p>
     * The id, parent, name and typeName must be present in each docModel.
     * <p>
     * The context data needs to be filled with values depending on the type of the document:
     * <p>
     * For a proxy (type = {@code "ecm:proxyType"}): {@link #IMPORT_PROXY_TARGET_ID} and
     * {@link #IMPORT_PROXY_VERSIONABLE_ID}.
     * <p>
     * For a version (no parent): {@link #IMPORT_VERSION_VERSIONABLE_ID}, {@link #IMPORT_VERSION_CREATED},
     * {@link #IMPORT_VERSION_LABEL} and {@link #IMPORT_VERSION_DESCRIPTION}.
     * <p>
     * For a live document: {@link #IMPORT_BASE_VERSION_ID} and {@link #IMPORT_CHECKED_IN} (Boolean).
     * <p>
     * For a live document or a version: {@link #IMPORT_LIFECYCLE_POLICY} , {@link #IMPORT_LIFECYCLE_STATE},
     * {@link #IMPORT_VERSION_MAJOR} (Long) and {@link #IMPORT_VERSION_MINOR} (Long).
     *
     * @param docModels the documents to create
     */
    void importDocuments(List<DocumentModel> docModels);

    /**
     * Saves changes done on the given document model.
     *
     * @param docModel the document model that needs modified
     */
    DocumentModel saveDocument(DocumentModel docModel);

    /**
     * Bulk document saving.
     *
     * @param docModels the document models that needs to be saved
     */
    void saveDocuments(DocumentModel[] docModels);

    /**
     * Check if a document can be removed. This needs the REMOVE permission on the document and the REMOVE_CHILDREN
     * permission on the parent.
     * <p>
     * For an archived version to be removeable, it must not be referenced from any proxy or be the base of a working
     * document, and the REMOVE permission must be available on the working document (or the user must be an
     * administrator if no working document exists).
     *
     * @param docRef the document
     * @return true if the document can be removed
     */
    boolean canRemoveDocument(DocumentRef docRef);

    /**
     * Removes this document and all its children, if any.
     *
     * @param docRef the reference to the document to remove
     */
    void removeDocument(DocumentRef docRef);

    /**
     * Bulk method to remove documents.
     * <p>
     * This method is safe with respect to orderings: it doesn't fail if an ancestor of a document occurs before the
     * document.
     * </p>
     *
     * @param docRefs the refs to the document to remove
     */
    void removeDocuments(DocumentRef[] docRefs);

    /**
     * Removes all children from the given document.
     *
     * @param docRef the reference to the document to remove
     */
    void removeChildren(DocumentRef docRef);

    /**
     * Copies the source document to the destination folder under the given name. If the name is null the original name
     * is preserved.
     * <p>
     * If the destination document is not a folder or it doesn't exists then throws an exception.
     * <p>
     * If the source is a proxy the destination will be a copy of the proxy.
     *
     * @param src the source document reference
     * @param dst the destination folder reference
     * @param name the new name of the file or null if the original name must be preserved
     * @param copyOptions the options for copy
     */
    DocumentModel copy(DocumentRef src, DocumentRef dst, String name, CopyOption... copyOptions);

    /**
     * Copies the source document to the destination folder under the given name. If the name is null the original name
     * is preserved.
     * <p>
     * If the destination document is not a folder or it doesn't exists then throws an exception.
     * <p>
     * If the source is a proxy the destination will be a copy of the proxy.
     *
     * @param src the source document reference
     * @param dst the destination folder reference
     * @param name the new name of the file or null if the original name must be preserved
     * @param resetLifeCycle the property that flagged whether reset destination document lifecycle or not
     * @since 5.7
     * @deprecated Since 8.2. Use {@link #copy(DocumentRef, DocumentRef, String, CopyOption...)} instead
     */
    @Deprecated
    DocumentModel copy(DocumentRef src, DocumentRef dst, String name, boolean resetLifeCycle);

    /**
     * Bulk copy. Destination must be a folder document.
     *
     * @param src the documents to copy
     * @param dst the destination folder
     * @param copyOptions the options for copy
     * @since 8.2
     */
    List<DocumentModel> copy(List<DocumentRef> src, DocumentRef dst, CopyOption... copyOptions);

    /**
     * Bulk copy. Destination must be a folder document.
     *
     * @param src the documents to copy
     * @param dst the destination folder
     * @param resetLifeCycle the property that flagged whether reset destination document lifecycle or not
     * @since 5.7
     * @deprecated Since 8.2. Use {@link #copy(List, DocumentRef, CopyOption...)} instead
     */
    @Deprecated
    List<DocumentModel> copy(List<DocumentRef> src, DocumentRef dst, boolean resetLifeCycle);

    /**
     * Work like copy but in the case of a source proxy the destination will be a new document instead of a proxy.
     *
     * @see CoreSession#copy(DocumentRef, DocumentRef, String, CopyOption...)
     * @param src the source document reference
     * @param dst the destination folder reference
     * @param name the new name of the file or null if the original name must be preserved
     * @param copyOptions the options for copy
     * @since 8.2
     */
    DocumentModel copyProxyAsDocument(DocumentRef src, DocumentRef dst, String name, CopyOption... copyOptions);

    /**
     * Work like copy but in the case of a source proxy the destination will be a new document instead of a proxy.
     *
     * @param src the source document reference
     * @param dst the destination folder reference
     * @param name the new name of the file or null if the original name must be preserved
     * @param resetLifeCycle the property that flagged whether reset destination document lifecycle or not
     * @since 5.7
     * @deprecated Since 8.2. Use {@link #copyProxyAsDocument(DocumentRef, DocumentRef, String, CopyOption...)} instead
     */
    @Deprecated
    DocumentModel copyProxyAsDocument(DocumentRef src, DocumentRef dst, String name, boolean resetLifeCycle);

    /**
     * Bulk copyProxyAsDocument. Destination must be a folder document.
     *
     * @param src the documents to copy
     * @param dst the destination folder
     * @param copyOptions the options of copy
     * @since 8.2
     */
    List<DocumentModel> copyProxyAsDocument(List<DocumentRef> src, DocumentRef dst, CopyOption... copyOptions);

    /**
     * Bulk copyProxyAsDocument. Destination must be a folder document.
     *
     * @param src the documents to copy
     * @param dst the destination folder
     * @param resetLifeCycle the property that flagged whether reset destination document lifecycle or not
     * @since 5.7
     * @deprecated Since 8.2. Use {@link #copyProxyAsDocument(List, DocumentRef, CopyOption...)} instead
     */
    @Deprecated
    List<DocumentModel> copyProxyAsDocument(List<DocumentRef> src, DocumentRef dst, boolean resetLifeCycle);

    /**
     * Moves the source document to the destination folder under the given name. If the name is {@code null} or if there
     * is a collision, a suitable new name is found.
     * <p>
     * If the destination document is not a folder or it doesn't exists then throws an exception.
     *
     * @param src the source document reference
     * @param dst the destination folder reference
     * @param name the new name of the file, or {@code null}
     */
    DocumentModel move(DocumentRef src, DocumentRef dst, String name);

    /**
     * Bulk move. Destination must be a folder document.
     *
     * @param src the documents to move
     * @param dst the destination folder
     */
    void move(List<DocumentRef> src, DocumentRef dst);

    /**
     * Gets the document access control policy.
     * <p>
     * The returned ACP is the ACP defined on that document if any + the inherited ACL if any. If neither a local ACP
     * nor inherited ACL exists null is returned.
     * <p>
     * Note that modifying the returned ACP will not affect in any way the stored document ACP. To modify the ACP you
     * must explicitely set it by calling {@link CoreSession#setACP(DocumentRef, ACP, boolean)}
     * <p>
     * This method will always fetch a fresh ACP from the storage. The recommended way to get the ACP is to use
     * {@link DocumentModel#getACP()} this way the ACP will be cached at the document model level and so you can use it
     * for multiple permission checks without fetching it each time.
     *
     * @param docRef the doc ref to retrieve ACP or null if none
     * @return the ACP
     */
    ACP getACP(DocumentRef docRef);

    /**
     * Sets the ACP for this document.
     * <p>
     * If the ACP contains an <code>INHERITED</code> ACL it will be discarded. Only ACLs relative to the current
     * document may be changed.
     * <p>
     * If the <code>overwrite</code> argument is false, the ACP is merged with the existing one if any. The merge is
     * done as follow:
     * <ul>
     * <li>If any ACL is that already exists on the document ACp is redefined by the new ACO then it will be replaced by
     * the new one. So if you want to remove an ACl in this mode you need to specify an empty ACL.
     * <li>If the new ACP contains an ACl that is not defined by the old one the it will be added to the merged ACP.
     * <li>If the <code>owners</code> are specified then they will replace the existing ones if any. Otherwise the old
     * owners are preserved if any. As for the ACL if you want to remove existing owners you need to specify an empty
     * owner array (and not a null one)
     * </ul>
     * If the <code>overwrite</code> argument is true, the old ACP will be replaced by the new one.
     * <p>
     * This way if you can remove the existing ACP by specifying a null ACP and <code>overwrite</code> argument set to
     * true.
     * <p>
     * Setting a null ACP when <code>overwrite</code> is false will do nothing.
     */
    void setACP(DocumentRef docRef, ACP acp, boolean overwrite);

    /**
     * Replace the {@code oldACE} with the {@code newACE} on the given {@code aclName}.
     * <p>
     *
     * @since 7.4
     */
    void replaceACE(DocumentRef docRef, String aclName, ACE oldACE, ACE newACE);

    /**
     * Returns {@code true} if negative ACLs are allowed.
     * <p>
     * Negative ACLs are ACLs that include an ACE with a deny (isGranted=false). This does not include the full-blocking
     * ACE for Everyone/Everything, which is always allowed.
     *
     * @return {@code true} if negative ACLs are allowed
     * @since 6.0
     */
    boolean isNegativeAclAllowed();

    /*
     * Support for lazy loading
     */

    /**
     * Retrieves a data model given a document reference and a schema.
     * <p>
     * For INTERNAL use by the core.
     *
     * @since 5.4.2
     */
    DataModel getDataModel(DocumentRef docRef, Schema schema);

    // -------- Versioning API ---------------

    /**
     * Gets the document corresponding to the last version for the given document.
     *
     * @param docRef the reference to the document
     * @return the document model corresponding to the version
     */
    DocumentModel getLastDocumentVersion(DocumentRef docRef);

    /**
     * Gets the document reference corresponding to the last version for the given document.
     *
     * @param docRef the reference to the document
     * @return the document reference corresponding to the last version
     */
    DocumentRef getLastDocumentVersionRef(DocumentRef docRef);

    /**
     * Gets the head (live) document for this document.
     *
     * @param docRef the reference to the document
     * @return the version
     */
    DocumentModel getSourceDocument(DocumentRef docRef);

    /**
     * Gets the references of the versions of the document.
     *
     * @param docRef the reference to the document
     * @return a list of version references
     * @since 1.4.1
     */
    List<DocumentRef> getVersionsRefs(DocumentRef docRef);

    /**
     * Retrieves all the versions for a specified document.
     *
     * @param docRef the reference to the document
     * @return the list of {@link DocumentModel} representing versions, empty list if none is found.
     */
    List<DocumentModel> getVersions(DocumentRef docRef);

    /**
     * Retrieves all the versions for a specified document.
     *
     * @param docRef the reference to the document
     * @return the list of {@link VersionModel} representing versions, empty list if none is found.
     */
    List<VersionModel> getVersionsForDocument(DocumentRef docRef);

    /**
     * Gets a document version, given the versionable id and label.
     * <p>
     * The version model contains the label of the version to look for. On return, it is filled with the version's
     * description and creation date.
     *
     * @param versionableId the versionable id
     * @param versionModel the version model holding the label
     * @return the version, or {@code null} if not found
     */
    DocumentModel getVersion(String versionableId, VersionModel versionModel);

    /**
     * Gets the version label for a document, according to the versioning service.
     *
     * @param docModel the document
     * @return the version label
     */
    String getVersionLabel(DocumentModel docModel);

    /**
     * Returns a document that represents the specified version of the document.
     *
     * @param docRef the reference to the document
     * @param version the version for which we want the corresponding document
     */
    DocumentModel getDocumentWithVersion(DocumentRef docRef, VersionModel version);

    /**
     * Restores the given document to the specified version.
     *
     * @param docRef the reference to the document
     * @param versionRef the reference to the version
     * @param skipSnapshotCreation {@code true} if the document should not be snapshotted before being restored
     * @param skipCheckout {@code true} if the restored document should be kept in a checked-in state
     * @since 5.4
     */
    DocumentModel restoreToVersion(DocumentRef docRef, DocumentRef versionRef, boolean skipSnapshotCreation,
            boolean skipCheckout);

    /**
     * Restores the given document to the specified version.
     *
     * @param docRef the reference to the document
     * @param versionRef the reference to the version
     * @since 5.4
     */
    DocumentModel restoreToVersion(DocumentRef docRef, DocumentRef versionRef);

    /**
     * Gets the version to which a checked in document is linked.
     * <p>
     * Returns {@code null} for a checked out document or a version or a proxy.
     *
     * @return the version, or {@code null}
     */
    DocumentRef getBaseVersion(DocumentRef docRef);

    /**
     * Checks out a versioned document.
     *
     * @param docRef the reference to the document
     */
    void checkOut(DocumentRef docRef);

    /**
     * Checks in a modified document, creating a new version.
     *
     * @param docRef the reference to the document
     * @param option whether to do create a new {@link VersioningOption#MINOR} or {@link VersioningOption#MAJOR} version
     *            during check in
     * @param checkinComment the checkin comment
     * @return the version just created
     * @since 5.4
     */
    DocumentRef checkIn(DocumentRef docRef, VersioningOption option, String checkinComment);

    /**
     * Returns whether the current document is checked-out or not.
     *
     * @param docRef the reference to the document
     */
    boolean isCheckedOut(DocumentRef docRef);

    /**
     * Gets the version series id for a document.
     * <p>
     * All documents and versions derived by a check in or checkout from the same original document share the same
     * version series id.
     *
     * @param docRef the document reference
     * @return the version series id
     * @since 5.4
     */
    String getVersionSeriesId(DocumentRef docRef);

    /**
     * Gets the working copy (live document) for a proxy or a version.
     *
     * @param docRef the document reference
     * @return the working copy, or {@code null} if not found
     * @since 5.4
     */
    DocumentModel getWorkingCopy(DocumentRef docRef);

    /**
     * Creates a generic proxy to the given document inside the given folder.
     * <p>
     * The document may be a version, or a working copy (live document) in which case the proxy will be a "shortcut".
     *
     * @since 1.6.1 (5.3.1)
     */
    DocumentModel createProxy(DocumentRef docRef, DocumentRef folderRef);

    /** -------------------------- Query API --------------------------- * */

    /**
     * Executes the given NXQL query an returns the result.
     *
     * @param query the query to execute
     * @return the query result
     */
    DocumentModelList query(String query);

    /**
     * Executes the given NXQL query an returns the result.
     *
     * @param query the query to execute
     * @param max number of document to retrieve
     * @return the query result
     */
    DocumentModelList query(String query, int max);

    /**
     * Executes the given NXQL query and returns the result that matches the filter.
     *
     * @param query the query to execute
     * @param filter the filter to apply to result
     * @return the query result
     */
    DocumentModelList query(String query, Filter filter);

    /**
     * Executes the given NXQL query and returns the result that matches the filter.
     *
     * @param query the query to execute
     * @param filter the filter to apply to result
     * @param max number of document to retrieve
     * @return the query result
     */
    DocumentModelList query(String query, Filter filter, int max);

    /**
     * Executes the given NXQL query and returns the result that matches the filter.
     *
     * @param query the query to execute
     * @param filter the filter to apply to result
     * @param limit the maximum number of documents to retrieve, or 0 for all of them
     * @param offset the offset (starting at 0) into the list of documents
     * @param countTotal if {@code true}, return a {@link DocumentModelList} that includes a total size of the
     *            underlying list (size if there was no limit or offset)
     * @return the query result
     */
    DocumentModelList query(String query, Filter filter, long limit, long offset, boolean countTotal);

    /**
     * Executes the given NXQL query and returns the result that matches the filter.
     *
     * @param query the query to execute
     * @param filter the filter to apply to result
     * @param limit the maximum number of documents to retrieve, or 0 for all of them
     * @param offset the offset (starting at 0) into the list of documents
     * @param countUpTo if {@code -1}, count the total size without offset/limit.<br>
     *            If {@code 0}, don't count the total size.<br>
     *            If {@code n}, count the total number if there are less than n documents otherwise set the size to
     *            {@code -1}.
     * @return the query result
     * @since 5.6
     */
    DocumentModelList query(String query, Filter filter, long limit, long offset, long countUpTo);

    /**
     * Executes the given query and returns the result that matches the filter.
     *
     * @param query the query to execute
     * @param queryType the query type, like "NXQL"
     * @param filter the filter to apply to result
     * @param limit the maximum number of documents to retrieve, or 0 for all of them
     * @param offset the offset (starting at 0) into the list of documents
     * @param countTotal if {@code true}, return a {@link DocumentModelList} that includes a total size of the
     *            underlying list (size if there was no limit or offset)
     * @return the query result
     * @since 5.5
     */
    DocumentModelList query(String query, String queryType, Filter filter, long limit, long offset, boolean countTotal);

    /**
     * Executes the given query and returns the result that matches the filter.
     *
     * @param query the query to execute
     * @param queryType the query type, like "NXQL"
     * @param filter the filter to apply to result
     * @param limit the maximum number of documents to retrieve, or 0 for all of them
     * @param offset the offset (starting at 0) into the list of documents
     * @param countUpTo if {@code -1}, return a {@link DocumentModelList} that includes a total size of the underlying
     *            list (size if there was no limit or offset). <br>
     *            If {@code 0}, don't return the total size of the underlying list. <br>
     *            If {@code n}, return the total size of the underlying list when the size is smaller than {@code n}
     *            else return a total size of {@code -1}.
     * @return the query result
     * @since 5.6
     */
    DocumentModelList query(String query, String queryType, Filter filter, long limit, long offset, long countUpTo);

    /**
     * Executes the given query and returns an iterable of maps containing the requested properties (which must be
     * closed when done).
     *
     * @param query the query to execute
     * @param queryType the query type, usually "NXQL"
     * @param params optional query-type-dependent parameters
     * @return an {@link IterableQueryResult}, which <b>must</b> be closed after use
     */
    IterableQueryResult queryAndFetch(String query, String queryType, Object... params);

    /**
     * Executes the given query and returns an iterable of maps containing the requested properties (which must be
     * closed when done).
     * <p>
     * It's possible to specify {@code distinctDocuments = true} to get a maximum of one row of results per document,
     * this will behave differently only when the {@code WHERE} clause contains wildcards.
     *
     * @param query the query to execute
     * @param queryType the query type, usually "NXQL"
     * @param distinctDocuments if {@code true} then a maximum of one row per document will be returned
     * @param params optional query-type-dependent parameters
     * @return an {@link IterableQueryResult}, which <b>must</b> be closed after use
     * @since 7.10-HF04, 8.2
     */
    IterableQueryResult queryAndFetch(String query, String queryType, boolean distinctDocuments, Object... params);

    /**
     * Executes the given query and returns the first batch of results, next batch must be requested within the
     * {@code keepAliveSeconds} delay.
     *
     * @param query The NXQL query to execute
     * @param batchSize The expected result batch size, note that more results can be returned when the backend don't
     *            implement properly this feature
     * @param keepAliveSeconds The scroll context lifetime in seconds
     * @return A {@link ScrollResult} including the search results and a scroll id, to be passed to the subsequent calls
     *         to {@link #scroll(String)}
     * @since 8.4
     */
    ScrollResult scroll(String query, int batchSize, int keepAliveSeconds);

    /**
     * Get the next batch of result, the {@code scrollId} is part of the previous {@link ScrollResult} response.
     *
     * @throws NuxeoException when the {@code scrollId} is unknown or when the scroll operation has timed out
     * @since 8.4
     */
    ScrollResult scroll(String scrollId);

    /** -------------------------- Security API --------------------------- * */

    /**
     * Retrieves the available security permissions existing in the system.
     * <p>
     *
     * @return a raw list of permission names, either basic or group names
     */
    // TODO: (Hardcoded at the moment. In the future wil get data from
    // LDAP/database.)
    List<String> getAvailableSecurityPermissions();

    /**
     * Returns the life cycle of the document.
     *
     * @see org.nuxeo.ecm.core.lifecycle
     * @param docRef the document reference
     * @return the life cycle as a string
     */
    String getCurrentLifeCycleState(DocumentRef docRef);

    /**
     * Returns the life cycle policy of the document.
     *
     * @see org.nuxeo.ecm.core.lifecycle
     * @param docRef the document reference
     * @return the life cycle policy
     */
    String getLifeCyclePolicy(DocumentRef docRef);

    /**
     * Follows a given life cycle transition.
     * <p>
     * This will update the current life cycle of the document.
     *
     * @param docRef the document reference
     * @param transition the name of the transition to follow
     * @return a boolean representing the status if the operation
     * @throws LifeCycleException if the transition cannot be followed
     */
    boolean followTransition(DocumentRef docRef, String transition) throws LifeCycleException;

    /**
     * Follows a given life cycle transition.
     * <p>
     * This will update the current life cycle of the document.
     *
     * @param doc the document model
     * @param transition the name of the transition to follow
     * @return a boolean representing the status if the operation
     * @throws LifeCycleException if the transition cannot be followed
     */
    boolean followTransition(DocumentModel doc, String transition) throws LifeCycleException;

    /**
     * Gets the allowed state transitions for this document.
     *
     * @param docRef the document reference
     * @return a collection of state transitions as string
     */
    Collection<String> getAllowedStateTransitions(DocumentRef docRef);

    /**
     * Reinitializes the life cycle state of the document to its default state.
     *
     * @param docRef the document
     * @since 5.4.2
     */
    void reinitLifeCycleState(DocumentRef docRef);

    /**
     * Retrieves the given field value from the given schema for all the given documents.
     *
     * @param docRefs the document references
     * @param schema the schema
     * @param field the field name
     * @return the field values in the same order as the given docRefs
     */
    Object[] getDataModelsField(DocumentRef[] docRefs, String schema, String field);

    /**
     * Creates an array with all parent refs starting from the given document up to the root. So the return value will
     * have [0] = parent ref; [1] = parent parent ref... etc.
     *
     * @return an array with ancestor documents ref
     */
    DocumentRef[] getParentDocumentRefs(DocumentRef docRef);

    /**
     * Retrieves the given field value from the given schema for the given document along with all its parent documents.
     *
     * @param docRef the document reference
     * @param schema the schema
     * @param field the field name
     * @return an array with field values of all documents on the path from the given document to the root
     */
    Object[] getDataModelsFieldUp(DocumentRef docRef, String schema, String field);

    /**
     * Sets a lock on the given document.
     *
     * @param docRef the document reference
     * @return the lock info that was set
     * @throws LockException if the document is already locked
     * @since 5.4.2
     */
    Lock setLock(DocumentRef docRef) throws LockException;

    /**
     * Gets the lock info on the given document.
     * <p>
     * Lock info is never cached, and needs to use a separate transaction in a separate thread, so care should be taken
     * to not call this method needlessly.
     *
     * @param docRef the document reference
     * @return the lock info if the document is locked, or {@code null} otherwise
     * @since 5.4.2
     */
    Lock getLockInfo(DocumentRef docRef);

    /**
     * Removes the lock on the given document.
     * <p>
     * The caller principal should be the same as the one who set the lock or to belongs to the administrator group,
     * otherwise an exception will be throw.
     * <p>
     * If the document was not locked, does nothing.
     * <p>
     * Returns the previous lock info.
     *
     * @param docRef the document to unlock
     * @return the removed lock info, or {@code null} if there was no lock
     * @since 5.4.2
     * @throws LockException if the document is locked by someone else
     */
    Lock removeLock(DocumentRef docRef) throws LockException;

    /**
     * Applies default Read permissions on root JCR Document for the given user or group name. It can only be called by
     * Administrators.
     * <p>
     * Usage: As an administrator, you may want to add new users or groups. This method needs to be called to grand
     * default reading permissions on the root document of the repository for the newly created users/groups.
     */
    void applyDefaultPermissions(String userOrGroupName);

    /**
     * Publishes the document in a section overwriting any existing proxy to the same document. This is simmilar to
     * publishDocument(docToPublish, section, true);
     *
     * @return The proxy document that was created
     * @since 1.4.1 for the case where docToPublish is a proxy
     */
    DocumentModel publishDocument(DocumentModel docToPublish, DocumentModel section);

    /**
     * Publishes the document in a section.
     *
     * @return The proxy document that was created
     */
    DocumentModel publishDocument(DocumentModel docToPublish, DocumentModel section, boolean overwriteExistingProxy);

    /**
     * Finds the proxies for a document. If the parent is not null, the search will be limited to its direct children.
     * <p>
     * If the document is a version, then only proxies to that version will be looked up.
     * <p>
     * If the document is a proxy, then all similar proxies (pointing to any version of the same versionable) are
     * retrieved.
     *
     * @param docRef the target document for the proxies
     * @param folderRef the folder where proxies are located or {@code null}
     * @return the list of the proxies. An empty list is returned if no proxy are found
     * @since 1.4.1 for the case where docRef is a proxy
     */
    DocumentModelList getProxies(DocumentRef docRef, DocumentRef folderRef);

    /**
     * Returns the type of his parent SuperSpace (workspace, section, etc.). SuperSpace is qualified by the SuperSpace
     * facet.
     */
    String getSuperParentType(DocumentModel doc);

    /**
     * Returns the parent SuperSpace (workspace, section, etc.). SuperSpace is qualified by the SuperSpace facet.
     *
     * @return DocumentModel of SuperSpace
     */
    DocumentModel getSuperSpace(DocumentModel doc);

    /**
     * Returns the repository name against which this core session is bound.
     *
     * @return the repository name used currently used as an identifier
     */
    String getRepositoryName();

    /**
     * Gets system property of the specified type for the document ref.
     */
    <T extends Serializable> T getDocumentSystemProp(DocumentRef ref, String systemProperty, Class<T> type);

    /**
     * Sets given value as a system property.
     */
    <T extends Serializable> void setDocumentSystemProp(DocumentRef ref, String systemProperty, T value);

    /**
     * Given a parent document, order the source child before the destination child. The source and destination must be
     * name of child documents of the given parent document. (a document name can be retrieved using
     * <code>docModel.getName()</code>) To place the source document at the end of the children list use a null
     * destination node.
     *
     * @param parent the parent document
     * @param src the document to be moved (ordered)
     * @param dest the document before which the reordered document will be placed If null the source document will be
     *            placed at the end of the children list
     */
    void orderBefore(DocumentRef parent, String src, String dest);

    /**
     * Internal method - it is used internally by {@link DocumentModel#refresh()}
     * <p>
     * Get fresh data from a document given a description of what kind of data should be refetched.
     * <p>
     * The refresh information is specified using a bit mask. See {@link DocumentModel} for all accepted flags.
     * <p>
     * When the flag {@link DocumentModel#REFRESH_CONTENT_IF_LOADED} is specified a third argument must be passed
     * representing the schema names for document parts to refresh. This argument is ignored if the flag is not
     * specified or no schema names are provided
     *
     * @param ref the document reference
     * @param refreshFlags refresh flags as defined in {@link DocumentModel}
     * @param schemas the schema names if a partial content refresh is required
     * @return a DocumentModelRefresh object
     */
    DocumentModelRefresh refreshDocument(DocumentRef ref, int refreshFlags, String[] schemas);

    /**
     * Provides the full list of all permissions or groups of permissions that contain the given one (inclusive). It
     * makes the method {@link org.nuxeo.ecm.core.security.SecurityService#getPermissionsToCheck} available remote.
     *
     * @return the list, as an array of strings.
     */
    String[] getPermissionsToCheck(String permission);

    /**
     * Find the first parent with the given {@code facet} and adapt it on the {@code adapterClass}.
     * <p>
     * This method does not check the permissions on the document to be adapted of this {@code CoreSession}'s
     * {@code Principal}, and so the adapter must not need other schemas from the {@code DocumentModel} except the
     * schemas related to the given facet.
     *
     * @return the first parent with the given {@code facet} adapted, or {@code null} if no parent found or the document
     *         does not support the given {@code adapterClass}.
     * @since 5.4.2
     */
    <T extends DetachedAdapter> T adaptFirstMatchingDocumentWithFacet(DocumentRef docRef, String facet,
            Class<T> adapterClass);

    /**
     * Gets the fulltext extracted from the binary fields.
     *
     * @param ref the document reference
     * @return the fulltext map or {@code null} if not supported.
     * @since 5.9.3
     */
    Map<String, String> getBinaryFulltext(DocumentRef ref);

    /** @since 8.2 */
    enum CopyOption {

        RESET_LIFE_CYCLE,

        RESET_CREATOR;

        public static boolean isResetLifeCycle(CopyOption... options) {
            return Arrays.asList(options).contains(RESET_LIFE_CYCLE);
        }

        public static boolean isResetCreator(CopyOption... options) {
            return Arrays.asList(options).contains(RESET_CREATOR);
        }

    }

}
