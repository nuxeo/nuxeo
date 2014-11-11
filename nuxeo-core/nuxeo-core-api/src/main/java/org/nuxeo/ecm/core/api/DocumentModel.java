/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.api;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.nuxeo.common.collections.ScopeType;
import org.nuxeo.common.collections.ScopedMap;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.schema.DocumentType;

/**
 * The document model is a serializable representation of a core document.
 * <p>
 * The document model is made from several data models, each data model is bound
 * to a schema. All the information about a document (like security) is
 * expressed using schemas (and implicitly data models).
 * <p>
 * Data models are lazily loaded as they are needed. At document model creation
 * only data models corresponding to the default schemas are loaded. The default
 * schemas are configured in the type manager through extension points.
 * <p>
 * The user may overwrite the default schemas by passing the schemas to be used
 * at model creation via {@link CoreSession#getDocument(DocumentRef, String[])}
 * <p>
 * How a lazy data model is loaded depends on the implementation.
 * <p>
 * Anyway the API already provides a mechanism to handle this as follow:
 *
 * <pre>
 * <code>
 * public DataModel getDataModel(String schema) {
 *     DataModel dataModel = dataModels.get(schema);
 *     if (dataModel != null &amp;&amp; !dataModel.isLoaded()) {
 *         CoreSession client = CoreInstance.getInstance().getClient(
 *                 getSessionId());
 *         if (client != null) {
 *             dataModel = client.getDataModel(getRef(), schema);
 *             dataModels.put(schema, dataModel);
 *         }
 *     }
 *     return dataModel;
 * }
 * </code>
 * </pre>
 *
 * @see CoreSession
 * @see DataModel
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface DocumentModel extends Serializable {

    int REFRESH_STATE = 1; // "small" state (life cycle, lock, versioning)

    int REFRESH_PREFETCH = 4;

    int REFRESH_ACP_IF_LOADED = 8; // refresh now only if already loaded

    int REFRESH_ACP_LAZY = 16; // refresh later in lazy mode

    int REFRESH_ACP = 32; // refresh now

    int REFRESH_CONTENT_IF_LOADED = 64; // refresh now only if already loaded

    int REFRESH_CONTENT_LAZY = 128; // refresh later in lazy mode

    int REFRESH_CONTENT = 256; // refresh now

    int REFRESH_IF_LOADED = REFRESH_STATE | REFRESH_PREFETCH
            | REFRESH_ACP_IF_LOADED | REFRESH_CONTENT_IF_LOADED;

    int REFRESH_LAZY = REFRESH_STATE | REFRESH_PREFETCH | REFRESH_ACP_LAZY
            | REFRESH_CONTENT_LAZY;

    int REFRESH_ALL = REFRESH_STATE | REFRESH_PREFETCH | REFRESH_ACP
            | REFRESH_CONTENT;

    int REFRESH_DEFAULT = REFRESH_STATE | REFRESH_PREFETCH
            | REFRESH_ACP_IF_LOADED | REFRESH_CONTENT_LAZY;

    /**
     * Gets the document type object.
     *
     * @return the document type object
     */
    DocumentType getDocumentType();

    /**
     * Retrieves the session id corresponding to this object.
     * <p>
     * This method should rarely be used, use {@link #getCoreSession} directly
     * instead.
     * <p>
     * Using the session id you can retrieve the core session that created the
     * object.
     * <p>
     * Document models created by the user on the client side are not bound to
     * any session. They are simple DTO used to transport data.
     *
     * @return the session id the session ID for server side created doc models
     *         or null for client side models (used for data transportation)
     */
    String getSessionId();

    /**
     * Gets the core session to which this document is tied.
     * <p>
     * This may be null if the document has been detached from a session.
     *
     * @return the core session
     * @since 5.2.GA
     */
    CoreSession getCoreSession();

    /**
     * Gets a reference to the core document that can be used either remotely or
     * locally (opens the core JVM).
     *
     * @return the document reference
     */
    DocumentRef getRef();

    /**
     * Retrieves the parent reference of the current document.
     *
     * @return the parent reference or null if no parent
     */
    DocumentRef getParentRef();

    /**
     * Gets the document UUID.
     *
     * @return the document UUID
     */
    String getId();

    /**
     * Gets the document name.
     *
     * @return the document name
     */
    String getName();

    /**
     * Get a text suitable to be shown in a UI for this document.
     *
     * @return the title or the internal name if no title could be found
     * @throws ClientException
     */
    String getTitle() throws ClientException;

    /**
     * Gets the document path as a string.
     *
     * @return the document path as string
     */
    String getPathAsString();

    /**
     * Gets the document path.
     *
     * @return the document path as string
     */
    Path getPath();

    /**
     * Gets the document type name.
     *
     * @return the document type name
     */
    String getType();

    /**
     * Gets the schemas available on this document (from the type and the
     * facets).
     *
     * @return the schemas
     *
     * @since 5.4.2
     */
    String[] getSchemas();

    /**
     * Gets the schemas available on this document (from the type and the
     * facets).
     *
     * @deprecated use {@link #getSchemas} instead, or call
     *             {@link #getDocumentType} and look up the type schemas
     *
     * @return the schemas
     */
    @Deprecated
    String[] getDeclaredSchemas();

    /**
     * Checks if the document has the given schema, either from its type or
     * added on the instance through a facet.
     *
     * @param schema the schema name
     * @return {@code true} if the document has the schema
     */
    boolean hasSchema(String schema);

    /**
     * Gets the facets available on this document (from the type and the
     * instance facets).
     *
     * @return the facets
     *
     * @since 5.4.2
     */
    Set<String> getFacets();

    /**
     * Gets the facets available on this document (from the type and the
     * instance facets).
     *
     * @deprecated use {@link #getFacets} instead, or call
     *             {@link #getDocumentType} and look up the type facets
     *
     * @return the facets
     */
    @Deprecated
    Set<String> getDeclaredFacets();

    /**
     * Checks if the document has a facet, either from its type or added on the
     * instance.
     *
     * @param facet the facet name
     * @return {@code true} if the document has the facet
     */
    boolean hasFacet(String facet);

    /**
     * Adds a facet to the document instance.
     * <p>
     * Does nothing if the facet was already present on the document.
     *
     * @param facet the facet name
     * @return {@code true} if the facet was added, or {@code false} if it is
     *         already present
     * @throws DocumentException if the facet does not exist
     *
     * @since 5.4.2
     */
    boolean addFacet(String facet);

    /**
     * Removes a facet from the document instance.
     * <p>
     * It's not possible to remove a facet coming from the document type.
     *
     * @param facet the facet name
     * @return {@code true} if the facet was removed, or {@code false} if it
     *         isn't present or is present on the type or does not exit
     *
     * @since 5.4.2
     */
    boolean removeFacet(String facet);

    /**
     * Gets a list with the currently fetched data models.
     *
     * @return the data models that are already fetched as a collection
     */
    Collection<DataModel> getDataModelsCollection();

    /**
     * Gets the data models.
     *
     * @return the data models that are already fetched.
     */
    DataModelMap getDataModels();

    /**
     * Gets the data model corresponding to the given schema.
     * <p>
     * Null is returned if the document type has no such schema.
     *
     * @param schema the schema name
     * @return the data model or null if no such schema is supported
     * @throws ClientException
     */
    DataModel getDataModel(String schema) throws ClientException;

    /**
     * Sets path info.
     * <p>
     * path and ref attributes will be set according to info
     *
     * @param parentPath
     * @param name
     */
    void setPathInfo(String parentPath, String name);

    /**
     * Gets the lock key if the document is locked.
     * <p>
     * Lock info is cached on the document for performance. Use
     * {@link CoreSession#getLockInfo} to get the non-cached status.
     *
     * @return the lock key if the document is locked or null otherwise
     *
     * @deprecated since 5.4.2, use {@link #getLockInfo} instead
     */
    @Deprecated
    String getLock();

    /**
     * Tests if the document is locked.
     * <p>
     * Lock info is cached on the document for performance. Use
     * {@link CoreSession#getLockInfo} to get the non-cached status.
     *
     * @return the lock key if the document is locked or null otherwise
     */
    boolean isLocked();

    /**
     * Locks this document using the given key.
     * <p>
     * This is a wrapper for {@link CoreSession#setLock(DocumentRef, String)}.
     *
     * @param key the key to use when locking
     * @throws ClientException if the document is already locked or other error
     *             occurs
     *
     * @deprecated since 5.4.2, use {@link #setLock} instead
     */
    @Deprecated
    void setLock(String key) throws ClientException;

    /**
     * Unlocks the given document.
     *
     * @throws ClientException if the document is already locked or other error
     *             occurs
     *
     * @deprecated since 5.4.2, use {@link #removeLock} instead
     */
    @Deprecated
    void unlock() throws ClientException;

    /**
     * Sets a lock on the document.
     *
     * @return the lock info that was set
     * @throws ClientException if a lock was already set
     *
     * @since 5.4.2
     */
    Lock setLock() throws ClientException;

    /**
     * Gets the lock info on the document.
     * <p>
     * Lock info is cached on the document for performance. Use
     * {@link CoreSession#getLockInfo} to get the non-cached status.
     *
     * @return the lock info if the document is locked, or {@code null}
     *         otherwise
     *
     * @since 5.4.2
     */
    Lock getLockInfo() throws ClientException;

    /**
     * Removes the lock on the document.
     * <p>
     * The caller principal should be the same as the one who set the lock or to
     * belongs to the administrator group, otherwise an exception will be throw.
     * <p>
     * If the document was not locked, does nothing.
     * <p>
     * Returns the previous lock info.
     *
     * @return the removed lock info, or {@code null} if there was no lock
     *
     * @since 5.4.2
     */
    Lock removeLock() throws ClientException;

    /**
     * Tests if the document is checked out.
     * <p>
     * A checked out document can be modified normally. A checked in document is
     * identical to the last version that it created, and not modifiable.
     * <p>
     * Only applicable to documents that are live (not versions and not
     * proxies).
     *
     * @return {@code true} if the document is checked out, {@code false} if it
     *         is checked in
     * @since 5.4
     */
    boolean isCheckedOut() throws ClientException;

    /**
     * Checks out a document.
     * <p>
     * A checked out document can be modified normally.
     * <p>
     * Only applicable to documents that are live (not versions and not
     * proxies).
     *
     * @since 5.4
     */
    void checkOut() throws ClientException;

    /**
     * Checks in a document and returns the created version.
     * <p>
     * A checked in document is identical to the last version that it created,
     * and not modifiable.
     * <p>
     * Only applicable to documents that are live (not versions and not
     * proxies).
     *
     * @param option whether to do create a new {@link VersioningOption#MINOR}
     *            or {@link VersioningOption#MAJOR} version during check in
     * @param checkinComment the checkin comment
     * @return the version just created
     * @since 5.4
     */
    DocumentRef checkIn(VersioningOption option, String checkinComment)
            throws ClientException;

    /**
     * Returns the version label.
     * <p>
     * The label returned is computed by the VersioningService.
     *
     * @return the version label, or {@code null}
     */
    String getVersionLabel();

    /**
     * Returns the checkin comment if the document model is a version.
     *
     * @return the checkin comment, or {@code null}
     * @since 5.4
     */
    String getCheckinComment() throws ClientException;

    /**
     * Gets the version series id for this document.
     * <p>
     * All documents and versions derived by a check in or checkout from the
     * same original document share the same version series id.
     *
     * @return the version series id
     * @since 5.4
     */
    String getVersionSeriesId() throws ClientException;

    /**
     * Checks if a document is the latest version in the version series.
     * @since 5.4
     */
    boolean isLatestVersion() throws ClientException;

    /**
     * Checks if a document is a major version.
     * @since 5.4
     */
    boolean isMajorVersion() throws ClientException;

    /**
     * Checks if a document is the latest major version in the version series.
     * @since 5.4
     */
    boolean isLatestMajorVersion() throws ClientException;

    /**
     * Checks if there is a checked out working copy for the version series of
     * this document.
     * @since 5.4
     */
    boolean isVersionSeriesCheckedOut() throws ClientException;

    /**
     * Gets the access control policy (ACP) for this document.
     * <p>
     * Returns null if no security was defined on this document.
     * <p>
     * The ACP can be used to introspect or to evaluate user privileges on this
     * document.
     * <p>
     * This is a wrapper for {@link CoreSession#getACP(DocumentRef)} but it is
     * recommended since it caches the ACP for later usage.
     *
     * @return the security data model or null if none
     * @throws ClientException
     */
    ACP getACP() throws ClientException;

    /**
     * Sets the ACP for this document model.
     * <p>
     * This is a wrapper for
     * {@link CoreSession#setACP(DocumentRef, ACP, boolean)}
     *
     * @see {@link CoreSession#setACP(DocumentRef, ACP, boolean)}
     * @param acp the ACP to set
     * @param overwrite whether to overwrite the old ACP or not
     * @throws ClientException
     */
    void setACP(ACP acp, boolean overwrite) throws ClientException;

    /**
     * Gets a property from the given schema.
     * <p>
     * The data model owning the property will be fetched from the server if not
     * already fetched.
     *
     * @param schemaName the schema name
     * @param name the property name
     * @return the property value or null if no such property exists
     * @throws ClientException
     */
    Object getProperty(String schemaName, String name) throws ClientException;

    /**
     * Sets the property value from the given schema.
     * <p>
     * This operation will not fetch the data model if not already fetched
     *
     * @param schemaName the schema name
     * @param name the property name
     * @param value the property value
     * @throws ClientException
     */
    void setProperty(String schemaName, String name, Object value)
            throws ClientException;

    /**
     * Gets the values from the given data model as a map.
     * <p>
     * The operation will fetch the data model from the server if not already
     * fetched.
     *
     * @param schemaName the data model schema name
     * @return the values map
     * @throws ClientException
     */
    Map<String, Object> getProperties(String schemaName) throws ClientException;

    /**
     * Sets values for the given data model.
     * <p>
     * This will not fetch the data model if not already fetched.
     *
     * @param schemaName the schema name
     * @param data the values to set
     * @throws ClientException
     */
    void setProperties(String schemaName, Map<String, Object> data)
            throws ClientException;


    /**
     * Checks if this document is a folder.
     *
     * @return true if the document is a folder, false otherwise
     */
    boolean isFolder();

    /**
     * Checks if this document can have versions.
     *
     * @return true if the document can have versions, false otherwise
     */
    boolean isVersionable();

    /**
     * Checks if this document can be downloaded.
     *
     * @return true if the document has downloadable content, false otherwise
     * @throws ClientException
     */
    boolean isDownloadable() throws ClientException;

    /**
     * Checks if this document is a version.
     *
     * @return true if the document is an older version of another document,
     *         false otherwise
     */
    boolean isVersion();

    /**
     * Checks if this document is a proxy.
     *
     * @return true if the document is a proxy false otherwise
     */
    boolean isProxy();

    /**
     * Checks if this document is immutable.
     *
     * @return {@code true} if the document is a version or a proxy to a
     *         version, {@code false} otherwise
     * @since 1.6.1 (5.3.1)
     */
    boolean isImmutable();

    /**
     * Adapts the document to the given interface.
     *
     * <p>
     * Attention, the first computation will cache the adaptation result for
     * later calls.
     * </p>
     *
     * @param <T> the interface type to adapt to
     * @param itf the interface class
     * @return the adapted document
     */
    <T> T getAdapter(Class<T> itf);

    /**
     * Adapts the document to the given interface.
     *
     * @param <T> the interface type to adapt to
     * @param itf the interface class
     * @param refreshCache : readapt and stores in cache if already exists.
     * @return the adapted document
     */
    <T> T getAdapter(Class<T> itf, boolean refreshCache);

    /**
     * Returns the life cycle of the document.
     *
     * @see org.nuxeo.ecm.core.lifecycle
     *
     * @return the life cycle as a string
     */
    String getCurrentLifeCycleState() throws ClientException;

    /**
     * Returns the life cycle policy of the document.
     *
     * @see org.nuxeo.ecm.core.lifecycle
     *
     * @return the life cycle policy
     */
    String getLifeCyclePolicy() throws ClientException;

    /**
     * Follows a given life cycle transition.
     * <p>
     * This will update the current life cycle of the document.
     *
     * @param transition the name of the transition to follow
     * @return a boolean representing the status if the operation
     */
    boolean followTransition(String transition) throws ClientException;

    /**
     * Gets the allowed state transitions for this document.
     *
     * @return a collection of state transitions as string
     */
    Collection<String> getAllowedStateTransitions() throws ClientException;

    /**
     * Gets the context data associated to this document.
     *
     * @return serializable map of context data.
     */
    ScopedMap getContextData();

    /**
     * Gets the context data associated to this document for given scope and
     * given key.
     */
    Serializable getContextData(ScopeType scope, String key);

    /**
     * Adds mapping to the context data for given scope.
     * <p>
     * Context data is like a request map set on the document model to pass
     * additional information to components interacting with the document model
     * (events processing for instance).
     */
    void putContextData(ScopeType scope, String key, Serializable value);

    /**
     * Gets the context data using the default scope.
     *
     * @param key the context data key
     * @return the value
     */
    Serializable getContextData(String key);

    /**
     * Sets a context data in the default scope.
     *
     * @param key the context data key
     * @param value the value
     */
    void putContextData(String key, Serializable value);

    /**
     * Copies the context data from given document to this document.
     */
    void copyContextData(DocumentModel otherDocument);

    /**
     * Copies all the data from a source document.
     */
    void copyContent(DocumentModel sourceDoc) throws ClientException;

    /**
     * Returns the name of the repository in which the document is stored.
     *
     * @return the repository name as a string.
     */
    String getRepositoryName();

    /**
     * Returns a cache key.
     * <p>
     * Cache key will be computed like this : <code>
     *     docUUID + "-" + sessionId + "-" + timestamp
     *   </code>
     * <p>
     * We will use the last modification time if present for the timestamp.
     *
     * @return the cache key as a string
     * @throws ClientException
     */
    String getCacheKey() throws ClientException;

    /**
     * Returns the source document identifier.
     * <p>
     * This is useful when not interested about the repository UUID itself.
     * Technically, this is the current version UUID.
     *
     * @return the source id as a string.
     */
    String getSourceId();

    /**
     * Returns the map of prefetched values.
     *
     * @return the map of prefetched values.
     */
    Map<String, Serializable> getPrefetch();

    /**
     * Store a value in the prefetched inner map.
     */
    void prefetchProperty(String id, Object value);

    /**
     * Used to set lifecycle state along with prefetching other properties.
     */
    void prefetchCurrentLifecycleState(String lifecycle);

    /**
     * Used to set lifecycle policy along with prefetching other properties.
     */
    void prefetchLifeCyclePolicy(String lifeCyclePolicy);

    boolean isLifeCycleLoaded();

    /**
     * Gets system property of the specified type. This is not a lazy loaded
     * property, thus the request is made directly to the server. This is needed
     * as some critical system properties might be changed directly in the core.
     */
    <T extends Serializable> T getSystemProp(String systemProperty,
            Class<T> type) throws ClientException, DocumentException;

    /**
     * Get a document part given its schema name
     *
     * @param schema the schema
     * @return the document aprt or null if none exists for that schema
     * @throws ClientException
     */
    // TODO throw an exception if schema is not impl by the doc?
    DocumentPart getPart(String schema) throws ClientException;

    /**
     * Gets this document's parts.
     */
    DocumentPart[] getParts() throws ClientException;

    /**
     * Gets a property given a xpath.
     */
    Property getProperty(String xpath) throws PropertyException,
            ClientException;

    /**
     * Gets a property value given a xpath.
     */
    Serializable getPropertyValue(String xpath) throws PropertyException,
            ClientException;

    /**
     * Sets a property value given a xpath.
     */
    void setPropertyValue(String xpath, Serializable value)
            throws PropertyException, ClientException;

    /**
     * Returns the flags set on the document model.
     */
    long getFlags();

    /**
     * Clears any prefetched or cached document data.
     * <p>
     * This will force the document to lazily update its data when required.
     */
    void reset();

    /**
     * Refresh document data from server.
     * <p>
     * The data models will be removed and all prefetch and system data will be
     * refreshed from the server
     * <p>
     * The refreshed data contains:
     * <ul>
     * <li>document life cycle
     * <li>document lock state, acp if required
     * <li>document prefetch map
     * <li>acp if required - otherwise acp info will be cleared so that it will
     * be refetched in lazy way
     * <li>document parts if required - otherwise parts data will be removed to
     * be refreshed lazy
     * </ul>
     * The refresh flags are:
     * <ul>
     * <li> {@link DocumentModel#REFRESH_STATE}
     * <li> {@link DocumentModel#REFRESH_PREFETCH}
     * <li> {@link DocumentModel#REFRESH_ACP_IF_LOADED}
     * <li> {@link DocumentModel#REFRESH_ACP_LAZY}
     * <li> {@link DocumentModel#REFRESH_ACP}
     * <li> {@link DocumentModel#REFRESH_CONTENT_IF_LOADED}
     * <li> {@link DocumentModel#REFRESH_CONTENT_LAZY}
     * <li> {@link DocumentModel#REFRESH_CONTENT}
     * <li> {@link DocumentModel#REFRESH_DEFAULT} same as REFRESH_STATE |
     * REFRESH_DEFAULT | REFRESH_ACP_IF_LOADED | REFRESH_CONTENT_IF_LOADED
     * <li> {@link DocumentModel#REFRESH_ALL} same as REFRESH_STATE |
     * REFRESH_PREFTECH | REFRESH_ACP | REFRESH_CONTENT
     * </ul>
     * If XX_IF_LOADED is used then XX will be refreshed only if already loaded
     * in the document - otherwise a lazy refresh will be done
     *
     * @param refreshFlags the refresh flags
     * @param schemas the document parts (schemas) that should be refreshed now
     */
    void refresh(int refreshFlags, String[] schemas) throws ClientException;

    /** Info fetched internally during a refresh. */
    public static class DocumentModelRefresh {

        public String lifeCycleState;

        public String lifeCyclePolicy;

        public boolean isCheckedOut;

        public boolean isLatestVersion;

        public boolean isMajorVersion;

        public boolean isLatestMajorVersion;

        public boolean isVersionSeriesCheckedOut;

        public String versionSeriesId;

        public String checkinComment;

        public ACP acp;

        public Map<String, Serializable> prefetch;

        public DocumentPart[] documentParts;
    }

    /**
     * Same as {@code DocumentModel.refresh(REFRESH_DEFAULT)}.
     */
    void refresh() throws ClientException;

    /**
     * Clone operation. Must be made public instead of just protected as in
     * Object.
     */
    DocumentModel clone() throws CloneNotSupportedException;

}
