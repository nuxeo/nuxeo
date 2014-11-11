/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.api;

import java.io.Serializable;
import java.security.Principal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.api.impl.DocsQueryProviderDef;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.api.operation.Operation;
import org.nuxeo.ecm.core.api.operation.ProgressMonitor;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecuritySummaryEntry;
import org.nuxeo.ecm.core.schema.DocumentType;

/**
 * A session to the Nuxeo Core.
 * <p>
 * The session is opened and closed by a client and gives the
 * client the possibility to interact with the core.
 * <p>
 * The core a session connects to can be located in a separate (remote) JVM or
 * in the current one.
 * <p>
 * To create remote or local sessions, you need to
 * use a specific {@link CoreSessionFactory}
 * object. These objects are usually specified using extension points but
 * you can also use them programatically.
 *
 * @see DocumentModel
 * @see DocumentRef
 *
 * @author Bogdan Stefanescu
 * @author Florent Guillaume
 */
public interface CoreSession {

    // used to pass properties to importDocument
    String IMPORT_VERSION_VERSIONABLE_ID = "ecm:versionableId";

    String IMPORT_VERSION_CREATED = "ecm:versionCreated";

    String IMPORT_VERSION_LABEL = "ecm:versionLabel";

    String IMPORT_VERSION_DESCRIPTION = "ecm:versionDescription";

    String IMPORT_VERSION_MAJOR = "ecm:majorVersion";

    String IMPORT_VERSION_MINOR = "ecm:minorVersion";

    String IMPORT_PROXY_TARGET_ID = "ecm:proxyTargetId";

    String IMPORT_PROXY_VERSIONABLE_ID = "ecm:proxyVersionableId";

    String IMPORT_LIFECYCLE_POLICY = "ecm:lifeCyclePolicy";

    String IMPORT_LIFECYCLE_STATE = "ecm:lifeCycleState";

    String IMPORT_LOCK = "ecm:lock";

    String IMPORT_DIRTY = "ecm:dirty";

    String IMPORT_CHECKED_IN = "ecm:isCheckedIn";

    String IMPORT_BASE_VERSION_ID = "ecm:baseVersion";

    /** The document type to use to create a proxy by import. */
    String IMPORT_PROXY_TYPE = "ecm:proxy";

    /**
     * Gets the document type object given its type name.
     *
     * @param type the document type name
     * @return the type the doc type object
     */
    DocumentType getDocumentType(String type);

    /**
     * Utility method to generate VersionModel labels.
     *
     * @param docRef
     * @return the String representation of an auto-incremented counter that not
     *         used in any label of docRef
     * @throws ClientException
     */
    String generateVersionLabelFor(DocumentRef docRef) throws ClientException;

    /**
     * Connects to the repository given its URI. This opens a new session on the
     * specified repository.
     * <p>
     * This method <b>must</b> never be called by users. Is is indirectly
     * called from {@link CoreInstance#open(String, Map)} when creating the
     * client.
     *
     * @param repositoryUri the repository URI (unique in the platform)
     * @param context a map of properties used to initialize the session. Can be
     *            null if no context properties are specified.
     * @return the session ID if the connection succeed, null otherwise
     * @throws ClientException
     */
    String connect(String repositoryUri, Map<String, Serializable> context)
            throws ClientException;

    /**
     * Closes the current session and disconnects from the repository.
     * <p>
     * This method <b>must</b> never be called by users. Is is indirectly
     * called from {@link CoreInstance#close(CoreSession)} when closing the
     * client
     * <p>
     * All pending change made on the repository through this session are saved.
     *
     * @throws ClientException
     */
    void disconnect() throws ClientException;

    /**
     * Cancels any pending change made through this session.
     *
     * @throws ClientException
     */
    void cancel() throws ClientException;

    /**
     * Saves any pending changes done until now through this session.
     *
     * @throws ClientException
     */
    void save() throws ClientException;

    /**
     * Gets the current session id.
     * <p>
     * If the client is not connected returns null.
     *
     * @return the session id or null if not connected
     */
    String getSessionId();

    /**
     * Gets the principal that created the client session.
     *
     * @return the principal
     */
    Principal getPrincipal();

    /**
     * Checks if the principal that created the client session has the given
     * privilege on the referred document.
     *
     * @param docRef
     * @param permission
     * @return
     * @throws ClientException
     */
    boolean hasPermission(DocumentRef docRef, String permission)
            throws ClientException;

    /**
     * Gets the root document of this repository.
     *
     * @return the root document. cannot be null
     * @throws ClientException
     * @throws SecurityException
     */
    DocumentModel getRootDocument() throws ClientException;

    /**
     * Gets a document model given its reference.
     * <p>
     * The default schemas are used to populate the returned document model.
     * Default schemas are configured via the document type manager.
     * <p>
     * Any other data model not part of the default schemas will be lazily
     * loaded as needed.
     *
     * @param docRef the document reference
     * @return the document
     * @throws ClientException
     * @throws SecurityException
     */
    DocumentModel getDocument(DocumentRef docRef) throws ClientException;

    /**
     * Gets a document model given its reference and the initial set of schemas
     * to use.
     * <p>
     * Same as the previous method with the difference that the default schemas
     * are overwritten by the given schemas.
     *
     * @param docRef the document reference
     * @param schemas the initial schemas to use to populate the document model
     * @return
     * @throws ClientException
     * @throws SecurityException
     */
    DocumentModel getDocument(DocumentRef docRef, String[] schemas)
            throws ClientException;

    /**
     * Gets a list of documents given their references.
     * <p>
     * Documents that are not accessible are skipped.
     */
    DocumentModelList getDocuments(DocumentRef[] docRefs)
            throws ClientException;

    /**
     * Gets a child document given its name and the parent reference.
     * <p>
     * Throws an exception if the document could not be found.
     * <p>
     * If the supplied id is null, returns the default child of the document if
     * any, otherwise raises an exception.
     * <p>
     * If the parent is null or its path is null, then root is considered.
     *
     * @param parent the reference to the parent document
     * @param name the name of the child document to retrieve
     * @return the named child if exists, raises a ClientException otherwise
     * @throws ClientException if there is no child with the given name
     */
    DocumentModel getChild(DocumentRef parent, String name)
            throws ClientException;

    /**
     * Gets the children of the given parent.
     *
     * @param parent the parent reference
     * @return the children if any, an empty list if no children or null if the
     *         specified parent document is not a folder
     * @throws ClientException
     */
    DocumentModelList getChildren(DocumentRef parent) throws ClientException;

    /**
     * Gets an iterator to the children of the given parent.
     *
     * @param parent the parent reference
     * @return iterator over the children collection or null if the specified
     *         parent document is not a folder
     * @throws ClientException
     */
    DocumentModelIterator getChildrenIterator(DocumentRef parent)
            throws ClientException;

    /**
     * Gets the children of the given parent filtered according to the given
     * document type.
     *
     * @param parent the parent reference
     * @param type the wanted document type
     * @return the documents if any, an empty list if none were found or null if
     *         the parent document is not a folder
     * @throws ClientException
     */
    DocumentModelList getChildren(DocumentRef parent, String type)
            throws ClientException;

    /**
     * Gets an iterator to the children of the given parent filtered according to the given
     * document type.
     */
    DocumentModelIterator getChildrenIterator(DocumentRef parent, String type)
            throws ClientException;

    /**
     * Gets the children of the given parent filtered according to the given
     * document type and permission.
     *
     * @param parent the parent reference
     * @param type the wanted document type
     * @param type the permission the user must have
     * @return the documents if any, an empty list if none were found or null if
     *         the parent document is not a folder
     * @throws ClientException
     */
    DocumentModelList getChildren(DocumentRef parent, String type, String perm)
            throws ClientException;

    /**
     * Same as {@link #getChildren(DocumentRef, String, String)} but the result
     * is filtered and then sorted using the specified filter and sorter.
     *
     * @param parent the parent reference
     * @param type the wanted type
     * @param perm permission to check for. If null, defaults to READ
     * @param filter the filter to use if any, null otherwise
     * @param sorter the sorter to use if any, null otherwise
     * @return the list of the children or an empty list if no children were
     *         found or null if the given parent is not a folder
     * @throws ClientException
     */
    DocumentModelList getChildren(DocumentRef parent, String type, String perm,
            Filter filter, Sorter sorter) throws ClientException;

    /**
     * Gets the references of the children. No permission is checked if perm is
     * null.
     *
     * @param parentRef the parent reference
     * @param perm the permission to check on the children (usually READ); if
     *            null, <b>no permission is checked</b>
     * @return a list of children references
     * @throws ClientException
     * @since 1.4.1
     */
    List<DocumentRef> getChildrenRefs(DocumentRef parentRef, String perm)
            throws ClientException;

    /**
     * Method used internally to retrieve frames of a long result.
     *
     * @param def
     * @param type
     * @param perm
     * @param filter
     * @param start
     * @param count
     * @return
     * @throws ClientException
     */
    DocumentModelsChunk getDocsResultChunk(DocsQueryProviderDef def,
            String type, String perm, Filter filter, int start, int count)
            throws ClientException;

    /**
     * Gets the children of the given parent filtered according to the given
     * document type and permission. Long result sets are loaded frame by frame
     * transparently by the DocumentModelIterator.
     *
     * @param parent
     * @param type
     * @param perm
     * @param filter
     * @return
     * @throws ClientException
     */
    DocumentModelIterator getChildrenIterator(DocumentRef parent, String type,
            String perm, Filter filter) throws ClientException;

    /**
     * Same as {@link #getChildren(DocumentRef, String, String, Filter, Sorter)}
     * without specific permission filtering.
     *
     * @param parent the parent reference
     * @param type the wanted type
     * @param filter the filter to use if any, null otherwise
     * @param sorter the sorter to use if any, null otherwise
     * @return the list of the children or an empty list if no children were
     *         found or null if the given parent is not a folder
     * @throws ClientException
     */
    DocumentModelList getChildren(DocumentRef parent, String type,
            Filter filter, Sorter sorter) throws ClientException;

    /**
     * Same as {@link CoreSession#getChildren(DocumentRef)} but returns only
     * folder documents.
     *
     * @param parent the parent ref
     * @return a list of children if any, an empty one if none or null if the
     *         given parent is not a folder
     * @throws ClientException
     */
    DocumentModelList getFolders(DocumentRef parent) throws ClientException;

    /**
     * Same as {@link CoreSession#getFolders(DocumentRef)} but returns a lazy
     * loading iterator over the list of children.
     *
     * @param parent the parent reference
     * @return a list of children if any, an empty one if none or null if the
     *         given parent is not a folder
     * @throws ClientException
     */
    DocumentModelIterator getFoldersIterator(DocumentRef parent)
            throws ClientException;

    /**
     * Same as {@link CoreSession#getFolders(DocumentRef)} but uses an optional filter
     * and sorter on the result.
     *
     * @param parent the parent reference
     * @param filter the filter to use or null if none
     * @param sorter the sorter to use or null if none
     * @return a list of children if any, an empty one if none or null if the
     *         given parent is not a folder
     * @throws ClientException
     */
    DocumentModelList getFolders(DocumentRef parent, Filter filter,
            Sorter sorter) throws ClientException;

    /**
     * Same as {@link CoreSession#getChildren(DocumentRef)} but returns only
     * non-folder documents.
     *
     * @param parent the parent reference
     * @return a list of children if any, an empty one if none or null if the
     *         given parent is not a folder
     * @throws ClientException
     */
    DocumentModelList getFiles(DocumentRef parent) throws ClientException;

    /**
     * Same as {@link CoreSession#getFiles(DocumentRef)} but returns an iterator.
     *
     * @param parent
     * @return
     * @throws ClientException
     */
    DocumentModelIterator getFilesIterator(DocumentRef parent)
            throws ClientException;

    /**
     * Same as {@link #getFiles} but uses an optional filter and sorter on the
     * result.
     *
     * @param parent the parent reference
     * @param filter the filter to use or null if none
     * @param sorter the sorter to use or null if none
     * @return a list of children if any, an empty one if none or null if the
     *         given parent is not a folder
     * @throws ClientException
     */
    DocumentModelList getFiles(DocumentRef parent, Filter filter, Sorter sorter)
            throws ClientException;

    /**
     * Gets the parent document or null if this is the root document.
     *
     * @return the parent document or null if this is the root document
     * @throws ClientException
     */
    DocumentModel getParentDocument(DocumentRef docRef) throws ClientException;

    /**
     * Gets the parent documents in path from the root to the given document or
     * empty list if this is the root document.
     * <p>
     * Documents the principal is is not allowed to browse are filtered out the
     * parents list.
     *
     * @return the list with parent documents or empty list if this is the root
     *         document
     * @throws ClientException
     */
    List<DocumentModel> getParentDocuments(DocumentRef docRef)
            throws ClientException;

    /**
     * Tests if the document pointed by the given reference exists and is
     * accessible.
     * <p>
     * This operation makes no difference between non-existence and permission
     * problems.
     * <p>
     * If the parent is null or its path is null, then root is considered.
     *
     * @param docRef the reference to the document to test for existence
     * @return true if the referenced document exists, false otherwise
     * @throws ClientException
     */
    boolean exists(DocumentRef docRef) throws ClientException;

    /**
     * Tests if the document has any children.
     * <p>
     * This operation silently ignores non-folder documents: If the document is
     * not a folder then returns false.
     * <p>
     * If the parent is null or its path is null, then root is considered.
     *
     * @param docRef the reference to the document to test
     * @return true if document has children, false otherwise
     * @throws ClientException
     */
    boolean hasChildren(DocumentRef docRef) throws ClientException;

    /**
     * Creates a document model using type name.
     * <p>
     * Used to fetch initial datamodels from the type definition.
     * <p>
     * DocumentModel creation notifies a
     * {@link DocumentEventTypes.EMPTY_DOCUMENTMODEL_CREATED} so that core event
     * listener can initialize its content with computed properties.
     *
     * @param typeName
     * @return the initial document model
     * @throws ClientException
     */
    DocumentModel createDocumentModel(String typeName) throws ClientException;

    /**
     * Creates a document model using required information.
     * <p>
     * Used to fetch initial datamodels from the type definition.
     * <p>
     * DocumentModel creation notifies a
     * {@link DocumentEventTypes.EMPTY_DOCUMENTMODEL_CREATED} so that core event
     * listener can initialize its content with computed properties.
     *
     * @param parentPath
     * @param id
     * @param typeName
     * @return the initial document model
     * @throws ClientException
     */
    DocumentModel createDocumentModel(String parentPath, String id,
            String typeName) throws ClientException;

    /**
     * Creates a document model using required information.
     * <p>
     * Used to fetch initial datamodels from the type definition.
     * <p>
     * DocumentModel creation notifies a
     * {@link DocumentEventTypes.EMPTY_DOCUMENTMODEL_CREATED} so that core event
     * listener can initialize its content with computed properties.
     *
     * @param typeName
     * @param options additional contextual data provided to core event
     *            listeners
     * @return the initial document model
     * @throws ClientException
     */
    DocumentModel createDocumentModel(String typeName,
            Map<String, Object> options) throws ClientException;

    /**
     * Creates a document using given document model for initialization.
     * <p>
     * The model contains path of the new document, its type and optionally the
     * initial data models of the document.
     * <p>
     *
     * @param model the document model to use for initialization
     * @return the created document
     * @throws ClientException
     */
    DocumentModel createDocument(DocumentModel model) throws ClientException;

    /**
     * Bulk creation of documents.
     *
     * @param docModels the document models to use for intialization
     * @return the created documents
     * @throws ClientException
     */
    DocumentModel[] createDocument(DocumentModel[] docModels)
            throws ClientException;

    /**
     * Low-level import of documents, reserved for the administrator.
     * <p>
     * This method is used to import documents with given ids, or directly
     * import versions and proxies.
     * <p>
     * The id, parent, name and typeName must be present in each docModel.
     * <p>
     * The context data needs to be filled with values depending on the type of
     * the document:
     * <p>
     * For a proxy (type = {@code "ecm:proxyType"}):
     * {@link #IMPORT_PROXY_TARGET_ID} and {@link #IMPORT_PROXY_VERSIONABLE_ID}.
     * <p>
     * For a version (no parent): {@link #IMPORT_VERSION_VERSIONABLE_ID},
     * {@link #IMPORT_VERSION_CREATED}, {@link #IMPORT_VERSION_LABEL} and
     * {@link #IMPORT_VERSION_DESCRIPTION}.
     * <p>
     * For a live document: {@link #IMPORT_BASE_VERSION_ID} and
     * {@link #IMPORT_CHECKED_IN} (Boolean).
     * <p>
     * For a live document or a version: {@link #IMPORT_LIFECYCLE_POLICY} ,
     * {@link #IMPORT_LIFECYCLE_STATE}, {@link #IMPORT_DIRTY} (Boolean),
     * {@link #IMPORT_VERSION_MAJOR} (Long) and {@link #IMPORT_VERSION_MINOR}
     * (Long).
     *
     * @param docModels the documents to create
     * @throws ClientException
     */
    void importDocuments(List<DocumentModel> docModels) throws ClientException;

    /**
     * Saves changes done on the given document model.
     *
     * @param docModel the document model that needs modified
     * @throws ClientException
     */
    DocumentModel saveDocument(DocumentModel docModel) throws ClientException;

    /**
     * Bulk document saving.
     *
     * @param docModels the document models that needs to be saved
     * @throws ClientException
     */
    void saveDocuments(DocumentModel[] docModels) throws ClientException;

    /**
     * Check if a document can be removed. This needs the REMOVE permission on
     * the document and the REMOVE_CHILDREN permission on the parent.
     * <p>
     * For an archived version to be removeable, it must not be referenced from
     * any proxy or be the base of a working document, and the REMOVE permission
     * must be available on the working document (or the user must be an
     * administrator if no working document exists).
     *
     * @param docRef the document
     * @return true if the document can be removed
     */
    boolean canRemoveDocument(DocumentRef docRef) throws ClientException;

    /**
     * Removes this document and all its children, if any.
     *
     * @param docRef the reference to the document to remove
     * @throws ClientException
     */
    void removeDocument(DocumentRef docRef) throws ClientException;

    /**
     * Bulk method to remove documents.
     * <p>
     * This method is safe with respect to orderings: it doesn't fail if an
     * ancestor of a document occurs before the document.
     * </p>
     *
     * @param docRefs the refs to the document to remove
     * @throws ClientException
     */
    void removeDocuments(DocumentRef[] docRefs) throws ClientException;

    /**
     * Removes all children from the given document.
     *
     * @param docRef the reference to the document to remove
     * @throws ClientException
     */
    void removeChildren(DocumentRef docRef) throws ClientException;

    /**
     * Copies the source document to the destination folder under the given
     * name. If the name is null the original name is preserved.
     * <p>
     * If the destination document is not a folder or it doesn't exists then
     * throws an exception.
     * <p>
     * If the source is a proxy the destination will be a copy of the proxy.
     *
     * @param src the source document reference
     * @param dst the destination folder reference
     * @param name the new name of the file or null if the original name must be
     *            preserved
     * @throws ClientException
     * @throws SecurityException
     */
    DocumentModel copy(DocumentRef src, DocumentRef dst, String name)
            throws ClientException;

    /**
     * Bulk copy. Destination must be a folder document.
     *
     * @param src the documents to copy
     * @param dst the destination folder
     * @throws ClientException
     * @throws SecurityException
     */
    List<DocumentModel> copy(List<DocumentRef> src, DocumentRef dst)
            throws ClientException;

    /**
     * Work like copy but in the case of a source proxy the destination will be
     * a new document instead of a proxy.
     *
     * @see CoreSession#copy(DocumentRef, DocumentRef, String)
     *
     * @param src the source document reference
     * @param dst the destination folder reference
     * @param name the new name of the file or null if the original name must be
     *            preserved
     * @throws ClientException
     * @throws SecurityException
     */
    DocumentModel copyProxyAsDocument(DocumentRef src, DocumentRef dst,
            String name) throws ClientException;

    /**
     * Bulk copyProxyAsDocument. Destination must be a folder document.
     *
     * @param src the documents to copy
     * @param dst the destination folder
     * @throws ClientException
     * @throws SecurityException
     */
    List<DocumentModel> copyProxyAsDocument(List<DocumentRef> src,
            DocumentRef dst) throws ClientException;

    /**
     * Moves the source document to the destination folder under the given name.
     * If the name is {@code null} or if there is a collision, a suitable new
     * name is found.
     * <p>
     * If the destination document is not a folder or it doesn't exists then
     * throws an exception.
     *
     * @param src the source document reference
     * @param dst the destination folder reference
     * @param name the new name of the file, or {@code null}
     * @throws ClientException
     * @throws SecurityException
     */
    DocumentModel move(DocumentRef src, DocumentRef dst, String name)
            throws ClientException;

    /**
     * Bulk move. Destination must be a folder document.
     *
     * @param src the documents to move
     * @param dst the destination folder
     * @throws ClientException
     * @throws SecurityException
     */
    void move(List<DocumentRef> src, DocumentRef dst) throws ClientException;

    /**
     * Gets the document access control policy.
     * <p>
     * The returned ACP is the ACP defined on that document if any + the
     * inherited ACL if any. If neither a local ACP nor inherited ACL exists
     * null is returned.
     * <p>
     * Note that modifying the returned ACP will not affect in any way the
     * stored document ACP. To modify the ACP you must explicitely set it by
     * calling {@link CoreSession#setACP(DocumentRef, ACP, boolean)}
     * <p>
     * This method will always fetch a fresh ACP from the storage. The
     * recommended way to get the ACP is to use {@link DocumentModel#getACP()}
     * this way the ACP will be cached at the document model level and so you
     * can use it for multiple permission checks without fetching it each time.
     *
     * @param docRef the doc ref to retrieve ACP or null if none
     * @return the ACP
     * @throws ClientException
     */
    ACP getACP(DocumentRef docRef) throws ClientException;

    /**
     * Sets the ACP for this document.
     * <p>
     * If the ACP contains an <code>INHERITED</code> ACL it will be discarded.
     * Only ACLs relative to the current document may be changed.
     * <p>
     * If the <code>overwrite</code> argument is false, the ACP is merged with
     * the existing one if any. The merge is done as follow:
     * <ul>
     * <li> If any ACL is that already exists on the document ACp is redefined
     * by the new ACO then it will be replaced by the new one. So if you want to
     * remove an ACl in this mode you need to specify an empty ACL.
     * <li> If the new ACP contains an ACl that is not defined by the old one
     * the it will be added to the merged ACP.
     * <li> If the <code>owners</code> are specified then they will replace
     * the existing ones if any. Otherwise the old owners are preserved if any.
     * As for the ACL if you want to remove existing owners you need to specify
     * an empty owner array (and not a null one)
     * </ul>
     * If the <code>overwrite</code> argument is true, the old ACP will be
     * replaced by the new one.
     * <p>
     * This way if you can remove the existing ACP by specifying a null ACP and
     * <code>overwrite</code> argument set to true.
     * <p>
     * Setting a null ACP when <code>overwrite</code> is false will do
     * nothing.
     *
     * @param docRef
     * @param acp
     * @param overwrite
     * @throws ClientException
     * @throws SecurityException
     */
    void setACP(DocumentRef docRef, ACP acp, boolean overwrite)
            throws ClientException;

    /*
     * Support for lazy loading
     */

    /**
     * Retrieves the data model given its schema and the reference of the owner
     * document.
     *
     * @param docRef the document owner reference
     * @param schema the schema
     * @return the data model
     * @throws ClientException
     */
    DataModel getDataModel(DocumentRef docRef, String schema)
            throws ClientException;

    /**
     * Retrieves the given field value from the given schema for the given
     * document.
     *
     * @param docRef the document reference
     * @param schema the schema
     * @param field the field name
     * @return the field value
     * @throws ClientException
     */
    Object getDataModelField(DocumentRef docRef, String schema, String field)
            throws ClientException;

    /**
     * The bulk version of the above method.
     * <p>
     * Retrieves the fields values from the given schema in the given document.
     *
     * @param docRef the document reference
     * @param schema the schema
     * @param fields the field names
     * @return the field values
     * @throws ClientException
     */
    Object[] getDataModelFields(DocumentRef docRef, String schema,
            String[] fields) throws ClientException;

    /**
     * Gets the data input stream given its key.
     * <p>
     * The key is implementation-dependent - this can be a property path an ID,
     * etc.
     * <p>
     * This method is used to lazily fetch blob streams.
     *
     * @param key
     * @return
     * @throws ClientException
     * @throws SecurityException
     */
    SerializableInputStream getContentData(String key) throws ClientException;

    /**
     * Returns an URI identifying the stream given the blob property id. This
     * method should be used by a client to download the data of a blob
     * property.
     * <p>
     * The blob is fetched from the repository and the blob stream is registered
     * against the streaming service so the stream will be available remotely
     * through stream service API.
     * <p>
     * After the client has called this method, it will be able to download the
     * stream using streaming server API.
     *
     * @return an URI identifying the remote stream
     * @throws ClientException
     */
    String getStreamURI(String blobPropertyId) throws ClientException;

    // -------- Versioning API ---------------

    /**
     * Gets the last version of a document.
     *
     * @param docRef the reference to the document
     * @return the version
     * @throws ClientException if any error occurs
     */
    VersionModel getLastVersion(DocumentRef docRef) throws ClientException;

    /**
     * Gets the document corresponding to the last version for the given
     * document.
     *
     * @param docRef the reference to the document
     * @return the document model corresponding to the version
     * @throws ClientException
     */
    DocumentModel getLastDocumentVersion(DocumentRef docRef)
            throws ClientException;

    /**
     * Gets the head (live) document for this document.
     *
     * @param docRef the reference to the document
     * @return the version
     * @throws ClientException if any error occurs
     */
    DocumentModel getSourceDocument(DocumentRef docRef) throws ClientException;

    /**
     * Gets the references of the versions of the document.
     *
     * @param docRef the reference to the document
     * @return a list of version references
     * @throws ClientException
     * @since 1.4.1
     */
    List<DocumentRef> getVersionsRefs(DocumentRef docRef)
            throws ClientException;

    /**
     * Retrieves all the versions for a specified document.
     *
     * @param docRef the reference to the document
     * @return the list of {@see DocumentModel} representing versions, empty
     *         list if none is found.
     * @throws ClientException
     */
    List<DocumentModel> getVersions(DocumentRef docRef) throws ClientException;

    /**
     * Retrieves all the versions for a specified document.
     *
     * @param docRef the reference to the document
     * @return the list of {@see VersionModel} representing versions, empty list
     *         if none is found.
     */
    List<VersionModel> getVersionsForDocument(DocumentRef docRef)
            throws ClientException;

    /**
     * Gets a document version, given the versionable id and label.
     * <p>
     * The version model contains the label of the version to look for. On
     * return, it is filled with the version's description and creation date.
     * <p>
     * Restricted to administrators.
     *
     * @param versionableId the versionable id
     * @param versionModel the version model holding the label
     * @return the version, or {@code null} if not found
     * @throws ClientException
     */
    DocumentModel getVersion(String versionableId, VersionModel versionModel)
            throws ClientException;

    /**
     * Returns a document that represents the specified version of the document.
     *
     * @param docRef the reference to the document
     * @param version the version for which we want the corresponding document
     * @return
     * @throws ClientException
     */
    DocumentModel getDocumentWithVersion(DocumentRef docRef,
            VersionModel version) throws ClientException;

    /**
     * Restores the given document to the specified version permitting to skip
     * the creation of the snapshot for current document.
     *
     * @param docRef the reference to the document
     * @param version the version to which the document should be restored to -
     *            only the label is used for the moment
     * @param skipSnapshotCreation indicates if skipping snapshot creation
     */
    DocumentModel restoreToVersion(DocumentRef docRef, VersionModel version,
            boolean skipSnapshotCreation) throws ClientException;

    /**
     * Restores the given document to the specified version.
     *
     * @param docRef the reference to the document
     * @param version the version to which the document should be restored to -
     *            only the label is used for the moment
     */
    DocumentModel restoreToVersion(DocumentRef docRef, VersionModel version)
            throws ClientException;

    /**
     * Checks out a versioned document.
     *
     * @param docRef the reference to the document
     * @throws ClientException
     */
    void checkOut(DocumentRef docRef) throws ClientException;

    /**
     * Checks in a modified document, creating a new version.
     *
     * @param docRef the reference to the document
     * @param version the version descriptor
     * @throws ClientException
     */
    void checkIn(DocumentRef docRef, VersionModel version)
            throws ClientException;

    /**
     * Returns whether the current document is checked-out or not.
     *
     * @param docRef the reference to the document
     * @return
     * @throws ClientException
     */
    boolean isCheckedOut(DocumentRef docRef) throws ClientException;

    /**
     * Creates a proxy for the given version of the given document.
     *
     * @param docRef the reference to the document
     * @param version the version
     * @return the proxy
     * @throws ClientException if any error occurs
     */
    DocumentModel createProxy(DocumentRef parentRef, DocumentRef docRef,
            VersionModel version, boolean overwriteExistingProxy)
            throws ClientException;

    /** -------------------------- Query API --------------------------- * */

    /**
     * Executes the given NXQL query an returns the result.
     *
     * @param query the query to execute
     * @return the query result
     * @throws ClientException
     */
    DocumentModelList query(String query) throws ClientException;

    /**
     * Executes the given NXQL query an returns the result.
     *
     * @param query the query to execute
     * @param max number of document to retrieve
     * @return the query result
     * @throws ClientException
     */
    DocumentModelList query(String query, int max) throws ClientException;

    /**
     * Executes the given NXQL query and returns the result that matches the
     * filter.
     *
     * @param query the query to execute
     * @param filter the filter to apply to result
     * @return the query result
     * @throws ClientException
     */
    DocumentModelList query(String query, Filter filter) throws ClientException;

    /**
     * Executes the given NXQL query and returns the result that matches the
     * filter.
     *
     * @param query the query to execute
     * @param filter the filter to apply to result
     * @param max number of document to retrieve
     * @return the query result
     * @throws ClientException
     */
    DocumentModelList query(String query, Filter filter, int max)
            throws ClientException;

    /**
     * Executes the given NXQL query and returns the result that matches the
     * filter.
     *
     * @param query the query to execute
     * @param filter the filter to apply to result
     * @param limit the maximum number of documents to retrieve, or 0 for all of
     *            them
     * @param offset the offset (starting at 0) into the list of documents
     * @param countTotal if {@code true}, return a {@link DocumentModelList}
     *            that includes a total size of the underlying list (size if
     *            there was no limit or offset)
     * @return the query result
     * @throws ClientException
     */
    DocumentModelList query(String query, Filter filter, long limit,
            long offset, boolean countTotal) throws ClientException;

    /**
     *
     * @throws ClientException
     */
    IterableQueryResult queryAndFetch(String query,
            String queryType, Object... params) throws ClientException;

    /**
     * Executes the given NXQL query and returns an iterators of results.
     *
     * @param query the query to execute
     * @param filter the filter to apply to result
     * @param max number of document to retrieve
     * @return the query result iterator
     * @throws ClientException
     */
    DocumentModelIterator queryIt(String query, Filter filter, int max)
            throws ClientException;

    /**
     * Executes a specific FULLTEXT enabled query for the given keywords.
     *
     * @param keywords
     * @return
     * @throws ClientException
     *
     * @deprecated use SearchService instead. See
     *             {@url http://doc.nuxeo.org/reference/html/search-service.html}
     */
    @Deprecated
    DocumentModelList querySimpleFts(String keywords) throws ClientException;

    /**
     * Executes a specific FULLTEXT enabled query for the given keywords,
     * returning only results that match the specified filter.
     *
     * @param keywords
     * @param filter
     * @return
     * @throws ClientException
     *
     * @deprecated use SearchService instead. See
     *             {@url http://doc.nuxeo.org/reference/html/search-service.html}
     */
    @Deprecated
    DocumentModelList querySimpleFts(String keywords, Filter filter)
            throws ClientException;

    /**
     * @deprecated use SearchService instead. See
     *             {@url http://doc.nuxeo.org/reference/html/search-service.html}
     */
    @Deprecated
    DocumentModelIterator querySimpleFtsIt(String query, Filter filter,
            int pageSize) throws ClientException;

    /**
     * @deprecated use SearchService instead. See
     *             {@url http://doc.nuxeo.org/reference/html/search-service.html}
     */
    @Deprecated
    DocumentModelIterator querySimpleFtsIt(String query, String startingPath,
            Filter filter, int pageSize) throws ClientException;

    /** -------------------------- Security API --------------------------- * */

    /**
     * Retrieves the available security permissions existing in the system.
     * <p>
     *
     * @return a raw list of permission names, either basic or group names
     * @throws ClientException
     */
    // TODO: (Hardcoded at the moment. In the future wil get data from
    // LDAP/database.)
    List<String> getAvailableSecurityPermissions() throws ClientException;

    /**
     * Returns the life cycle of the document.
     *
     * @see org.nuxeo.ecm.core.lifecycle
     *
     * @param docRef the document reference
     * @return the life cycle as a string
     * @throws ClientException
     */
    String getCurrentLifeCycleState(DocumentRef docRef) throws ClientException;

    /**
     * Returns the life cycle policy of the document.
     *
     * @see org.nuxeo.ecm.core.lifecycle
     *
     * @param docRef the document reference
     * @return the life cycle policy
     * @throws ClientException
     */
    String getLifeCyclePolicy(DocumentRef docRef) throws ClientException;

    /**
     * Follows a given life cycle transition.
     * <p>
     * This will update the current life cycle of the document.
     *
     * @param docRef the document reference
     * @param transition the name of the transition to follow
     * @return a boolean representing the status if the operation
     * @throws ClientException
     */
    boolean followTransition(DocumentRef docRef, String transition)
            throws ClientException;

    /**
     * Gets the allowed state transitions for this document.
     *
     * @param docRef the document reference
     * @return a collection of state transitions as string
     */
    Collection<String> getAllowedStateTransitions(DocumentRef docRef)
            throws ClientException;

    /**
     * Retrieves the given field value from the given schema for all the given
     * documents.
     *
     * @param docRefs the document references
     * @param schema the schema
     * @param field the field name
     * @return the field values in the same order as the given docRefs
     * @throws ClientException
     * @throws ClientException
     */
    Object[] getDataModelsField(DocumentRef[] docRefs, String schema,
            String field) throws ClientException;

    /**
     * Creates an array with all parent refs starting from the given document up
     * to the root. So the return value will have [0] = parent ref; [1] = parent
     * parent ref... etc.
     *
     * @param docRef
     * @return an array with ancestor documents ref
     * @throws ClientException
     */
    DocumentRef[] getParentDocumentRefs(DocumentRef docRef)
            throws ClientException;

    /**
     * Retrieves the given field value from the given schema for the given
     * document along with all its parent documents.
     *
     * @param docRef the document reference
     * @param schema the schema
     * @param field the field name
     * @return an array with field values of all documents on the path from the
     *         given document to the root
     * @throws ClientException
     */
    Object[] getDataModelsFieldUp(DocumentRef docRef, String schema,
            String field) throws ClientException;

    /**
     * Gets the lock key on the given document if a lock exists or null
     * otherwise.
     *
     * @param doc the document
     * @return the lock key if the document is locked, null otherwise
     * @throws ClientException
     */
    String getLock(DocumentRef doc) throws ClientException;

    /**
     * Sets a lock on the given document using the given key.
     *
     * @param doc the document reference
     * @param key the lock key
     * @throws ClientException if a lock is already set or other exception
     *             occurred
     */
    void setLock(DocumentRef doc, String key) throws ClientException;

    /**
     * Removes the lock if one exists.
     * <p>
     * The caller principal should be the same as the one who set the lock or to
     * belongs to the administrator group, otherwise an exception will be throw.
     * <p>
     * If the document was not locked do nothing
     *
     * @param docRef the document to unlock
     * @throws ClientException
     * @return the lock key that was removed
     */
    String unlock(DocumentRef docRef) throws ClientException;

    /**
     * Applies default Read permissions on root JCR Document for the given user
     * or group name. It can only be called by Administrators.
     * <p>
     * Usage: As an administrator, you may want to add new users or groups. This
     * method needs to be called to grand default reading permissions on the
     * root document of the repository for the newly created users/groups.
     *
     * @param userOrGroupName
     * @throws ClientException
     */
    void applyDefaultPermissions(String userOrGroupName) throws ClientException;

    /**
     * Destroys any system resources held by this instance.
     * <p>
     * Called when the instance is no more needed.
     */
    void destroy();

    /**
     * Checks if the given document is dirty.
     *
     * @param doc the doc reference
     * @return true if dirty false otherwise
     * @throws ClientException
     */
    boolean isDirty(DocumentRef doc) throws ClientException;

    /**
     * Publishes the document in a section overwriting any existing proxy to the
     * same document. This is simmilar to publishDocument(docToPublish, section,
     * true);
     *
     * @param docToPublish
     * @param section
     * @return The proxy document that was created
     * @throws ClientException
     * @since 1.4.1 for the case where docToPublish is a proxy
     */
    DocumentModel publishDocument(DocumentModel docToPublish,
            DocumentModel section) throws ClientException;

    /**
     * Publishes the document in a section.
     *
     * @param docToPublish
     * @param section
     * @param overwriteExistingProxy
     * @return The proxy document that was created
     * @throws ClientException
     */
    DocumentModel publishDocument(DocumentModel docToPublish,
            DocumentModel section, boolean overwriteExistingProxy)
            throws ClientException;

    /**
     * Checks if the document is already published in the section and retrieves
     * it's version.
     *
     * @param document
     * @param section
     * @return
     */
    VersionModel isPublished(DocumentModel document, DocumentModel section);

    /**
     * Gets all proxies to document docRef inside folder folderRef.
     *
     * @param docRef the target document for the proxies
     * @param folderRef the folder where proxies are located
     * @return the list of the proxies. An empty list is returned if no proxy
     *         are found
     * @throws ClientException if any error occurs
     * @since 1.4.1 for the case where docRef is a proxy
     */
    DocumentModelList getProxies(DocumentRef docRef, DocumentRef folderRef)
            throws ClientException;

    /**
     * Gets all proxy versions to document docRef inside folder folderRef.
     * <p>
     * Intended to be used by UI clients to display information about proxies in
     * sections.
     *
     * @param docRef the target document for the proxies
     * @param folderRef the folder where proxies are located
     * @return an array of the proxy versions. Null is returned if no proxies
     *         are found the specified folder
     * @throws ClientException if any error occurs
     */
    String[] getProxyVersions(DocumentRef docRef, DocumentRef folderRef)
            throws ClientException;

    /**
     * Returns the type of his parent SuperSpace (workspace, section, etc.).
     * SuperSpace is qualified by the SuperSpace facet.
     *
     * @param doc
     * @return
     */
    String getSuperParentType(DocumentModel doc) throws ClientException;

    /**
     * Returns the parent SuperSpace (workspace, section, etc.). SuperSpace is
     * qualified by the SuperSpace facet.
     *
     * @param doc
     * @return DocumentModel of SuperSpace
     * @throws ClientException
     */
    DocumentModel getSuperSpace(DocumentModel doc) throws ClientException;

    /**
     * Returns security descriptors of doc and all it's children that hold
     * explicit security.
     *
     * @param docModel the document node from where the security export is done
     * @param includeParents flag is the parent nodes holding security
     *            information should be added at the top of the returned list
     * @return a list of SecuritySummaryEntry
     * @throws ClientException
     */
    List<SecuritySummaryEntry> getSecuritySummary(DocumentModel docModel,
            Boolean includeParents) throws ClientException;

    /**
     * Returns the repository name against which this core session is bound.
     *
     * @return the repository name used currently used as an identifier
     */
    String getRepositoryName();

    /**
     * Gets system property of the specified type for the document ref.
     *
     * @param <T>
     * @param ref
     * @param systemProperty
     * @param type
     * @return
     * @throws ClientException
     * @throws DocumentException
     */
    <T extends Serializable> T getDocumentSystemProp(DocumentRef ref,
            String systemProperty, Class<T> type) throws ClientException,
            DocumentException;

    /**
     * Sets given value as a system property.
     *
     * @param <T>
     * @param ref
     * @param systemProperty
     * @param value
     * @throws ClientException
     * @throws DocumentException
     */
    <T extends Serializable> void setDocumentSystemProp(DocumentRef ref,
            String systemProperty, T value) throws ClientException,
            DocumentException;

    /**
     * Given a parent document, order the source child before the destination
     * child. The source and destination must be name of child documents of the
     * given parent document. (a document name can be retrieved using
     * <code>docModel.getName()</code>) To place the source document at the
     * end of the children list use a null destination node.
     *
     * @param parent the parent document
     * @param src the document to be moved (ordered)
     * @param dest the document before which the reordered document will be
     *            placed If null the source document will be placed at the end
     *            of the children list
     * @throws ClientException if the parent document is not an orderable folder
     *             or other error occurs
     */
    void orderBefore(DocumentRef parent, String src, String dest)
            throws ClientException;

    /**
     * Run a command
     *
     * @param <T> command result type
     * @param cmd the command to run
     * @return the command result
     * @throws ClientException if any error occurs
     */
    <T> T run(Operation<T> cmd) throws ClientException;

    /**
     * Run a command and notify the given monitor about the execution progress
     *
     * @param <T>
     * @param op
     * @param monitor
     * @return
     * @throws ClientException
     */
    <T> T run(Operation<T> op, ProgressMonitor monitor) throws ClientException;

    /**
     * Internal method - it is used internally by
     * {@link DocumentModel#refresh()}
     * <p>
     * Get fresh data from a document given a description of what kind of data
     * should be refetched.
     * <p>
     * The refresh information is specified using a bit mask. See
     * {@link DocumentModel} for all accepted flags.
     * <p>
     * When the flag {@link DocumentModel#REFRESH_CONTENT_IF_LOADED} is
     * specified a third argument must be passed representing the schema names
     * for document parts to refresh. This argument is ignored if the flag is
     * not specified or no schema names are provided
     * <p>
     * The result is an array defined as follows:
     * <ul>
     * <li> on index 0 - the prefetch data
     * <li> on index 1 - the lock state info
     * <li> on index 2 - the life cycle state info
     * <li> on index 3 - the life cycle policy
     * <li> on index 4 - the ACP
     * <li> on index 5 - an array of {@link DocumentPart} objects
     * </ul>
     *
     * @param ref the document reference
     * @param refreshFlags refresh flags as defined in {@link DocumentModel}
     * @param schemas the schema names if a partial content refresh is required
     * @return an array containing the refreshed data - this array will always
     *         have 5 elements.
     *
     * @throws ClientException
     */
    Object[] refreshDocument(DocumentRef ref, int refreshFlags, String[] schemas)
            throws ClientException;

    /**
     * Provides the full list of all permissions or groups of permissions that
     * contain the given one (inclusive).
     * It makes the method {@link SecurityService#getPermissionsToCheck()}
     * available remote.
     * @param permission
     * @return the list, as an array of strings.
     */
    String[] getPermissionsToCheck(String permission);

}
