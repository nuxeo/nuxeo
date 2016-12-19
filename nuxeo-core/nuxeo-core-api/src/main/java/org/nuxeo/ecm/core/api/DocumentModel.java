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
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.nuxeo.common.collections.ScopeType;
import org.nuxeo.common.collections.ScopedMap;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyVisitor;
import org.nuxeo.ecm.core.api.model.resolver.PropertyObjectResolver;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.Prefetch;

/**
 * The document model is a serializable representation of a core document.
 * <p>
 * The document model is made from several data models, each data model is bound to a schema. All the information about
 * a document (like security) is expressed using schemas (and implicitly data models).
 * <p>
 * Data models are lazily loaded as they are needed. At document model creation only data models corresponding to the
 * default schemas are loaded. The default schemas are configured in the type manager through extension points.
 *
 * @see CoreSession
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

    int REFRESH_IF_LOADED = REFRESH_STATE | REFRESH_PREFETCH | REFRESH_ACP_IF_LOADED | REFRESH_CONTENT_IF_LOADED;

    int REFRESH_LAZY = REFRESH_STATE | REFRESH_PREFETCH | REFRESH_ACP_LAZY | REFRESH_CONTENT_LAZY;

    int REFRESH_ALL = REFRESH_STATE | REFRESH_PREFETCH | REFRESH_ACP | REFRESH_CONTENT;

    int REFRESH_DEFAULT = REFRESH_STATE | REFRESH_PREFETCH | REFRESH_ACP_IF_LOADED | REFRESH_CONTENT_LAZY;

    /**
     * Gets the document type object.
     *
     * @return the document type object
     */
    DocumentType getDocumentType();

    /**
     * Retrieves the session id corresponding to this object.
     * <p>
     * This method should rarely be used, use {@link #getCoreSession} directly instead.
     * <p>
     * Using the session id you can retrieve the core session that created the object.
     * <p>
     * Document models created by the user on the client side are not bound to any session. They are simple DTO used to
     * transport data.
     *
     * @return the session id the session ID for server side created doc models or null for client side models (used for
     *         data transportation)
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
     * Detaches the documentImpl from its existing session, so that it can survive beyond the session's closing.
     *
     * @param loadAll if {@code true}, load all data and ACP from the session before detaching
     * @since 5.6
     */
    void detach(boolean loadAll);

    /**
     * Reattaches a document impl to an existing session.
     *
     * @param sid the session id
     * @since 5.6
     */
    void attach(String sid);

    /**
     * Gets a reference to the core document that can be used either remotely or locally (opens the core JVM).
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
     * Gets the document's position in its containing folder (if ordered).
     *
     * @return the position, or {@code null} if the containing folder is not ordered
     * @since 6.0
     */
    Long getPos();

    /**
     * Get a text suitable to be shown in a UI for this document.
     *
     * @return the title or the internal name if no title could be found
     */
    String getTitle();

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
     * Gets the schemas available on this document (from the type and the facets).
     *
     * @return the schemas
     * @since 5.4.2
     */
    String[] getSchemas();

    /**
     * Gets the schemas available on this document (from the type and the facets).
     *
     * @deprecated use {@link #getSchemas} instead, or call {@link #getDocumentType} and look up the type schemas
     * @return the schemas
     */
    @Deprecated
    String[] getDeclaredSchemas();

    /**
     * Checks if the document has the given schema, either from its type or added on the instance through a facet.
     *
     * @param schema the schema name
     * @return {@code true} if the document has the schema
     */
    boolean hasSchema(String schema);

    /**
     * Gets the facets available on this document (from the type and the instance facets).
     *
     * @return the facets
     * @since 5.4.2
     */
    Set<String> getFacets();

    /**
     * Gets the facets available on this document (from the type and the instance facets).
     *
     * @deprecated use {@link #getFacets} instead, or call {@link #getDocumentType} and look up the type facets
     * @return the facets
     */
    @Deprecated
    Set<String> getDeclaredFacets();

    /**
     * Checks if the document has a facet, either from its type or added on the instance.
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
     * @return {@code true} if the facet was added, or {@code false} if it is already present
     * @throws IllegalArgumentException if the facet does not exist
     * @since 5.4.2
     */
    boolean addFacet(String facet);

    /**
     * Removes a facet from the document instance.
     * <p>
     * It's not possible to remove a facet coming from the document type.
     *
     * @param facet the facet name
     * @return {@code true} if the facet was removed, or {@code false} if it isn't present or is present on the type or
     *         does not exit
     * @since 5.4.2
     */
    boolean removeFacet(String facet);

    /**
     * INTERNAL, not for public use.
     * <p>
     * Gets a list with the currently fetched data models.
     *
     * @return the data models that are already fetched as a collection
     * @deprecated since 8.4, internal method
     * @see #getSchemas
     * @see #getProperties
     * @see #getPropertyObject
     * @see #getPropertyObjects
     */
    @Deprecated
    Collection<DataModel> getDataModelsCollection();

    /**
     * Gets the data models.
     *
     * @return the data models that are already fetched.
     * @deprecated since 8.4, use direct {@link Property} getters instead
     * @see #getSchemas
     * @see #getProperties
     * @see #getPropertyObject
     * @see #getPropertyObjects
     */
    @Deprecated
    Map<String, DataModel> getDataModels();

    /**
     * Gets the data model corresponding to the given schema.
     * <p>
     * Null is returned if the document type has no such schema.
     *
     * @param schema the schema name
     * @return the data model or null if no such schema is supported
     * @deprecated since 8.4, use direct {@link Property} getters instead
     * @see #getSchemas
     * @see #getProperties
     * @see #getPropertyObject
     * @see #getPropertyObjects
     */
    @Deprecated
    DataModel getDataModel(String schema);

    /**
     * Sets path info.
     * <p>
     * path and ref attributes will be set according to info
     */
    void setPathInfo(String parentPath, String name);

    /**
     * Tests if the document is locked.
     * <p>
     * Lock info is cached on the document for performance. Use {@link CoreSession#getLockInfo} to get the non-cached
     * status.
     *
     * @return the lock key if the document is locked or null otherwise
     */
    boolean isLocked();

    /**
     * Sets a lock on the document.
     *
     * @return the lock info that was set
     * @throws LockException if the document is already locked
     * @since 5.4.2
     */
    Lock setLock() throws LockException;

    /**
     * Gets the lock info on the document.
     * <p>
     * Lock info is cached on the document for performance. Use {@link CoreSession#getLockInfo} to get the non-cached
     * status.
     *
     * @return the lock info if the document is locked, or {@code null} otherwise
     * @since 5.4.2
     */
    Lock getLockInfo();

    /**
     * Removes the lock on the document.
     * <p>
     * The caller principal should be the same as the one who set the lock or to belongs to the administrator group,
     * otherwise an exception will be throw.
     * <p>
     * If the document was not locked, does nothing.
     * <p>
     * Returns the previous lock info.
     *
     * @return the removed lock info, or {@code null} if there was no lock
     * @throws LockException if the document is locked by someone else
     * @since 5.4.2
     */
    Lock removeLock() throws LockException;

    /**
     * Tests if the document is checked out.
     * <p>
     * A checked out document can be modified normally. A checked in document is identical to the last version that it
     * created, and not modifiable.
     * <p>
     * Only applicable to documents that are live (not versions and not proxies).
     *
     * @return {@code true} if the document is checked out, {@code false} if it is checked in
     * @since 5.4
     */
    boolean isCheckedOut();

    /**
     * Checks out a document.
     * <p>
     * A checked out document can be modified normally.
     * <p>
     * Only applicable to documents that are live (not versions and not proxies).
     *
     * @since 5.4
     */
    void checkOut();

    /**
     * Checks in a document and returns the created version.
     * <p>
     * A checked in document is identical to the last version that it created, and not modifiable.
     * <p>
     * Only applicable to documents that are live (not versions and not proxies).
     *
     * @param option whether to do create a new {@link VersioningOption#MINOR} or {@link VersioningOption#MAJOR} version
     *            during check in
     * @param checkinComment the checkin comment
     * @return the version just created
     * @since 5.4
     */
    DocumentRef checkIn(VersioningOption option, String checkinComment);

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
    String getCheckinComment();

    /**
     * Gets the version series id for this document.
     * <p>
     * All documents and versions derived by a check in or checkout from the same original document share the same
     * version series id.
     *
     * @return the version series id
     * @since 5.4
     */
    String getVersionSeriesId();

    /**
     * Checks if a document is the latest version in the version series.
     *
     * @since 5.4
     */
    boolean isLatestVersion();

    /**
     * Checks if a document is a major version.
     *
     * @since 5.4
     */
    boolean isMajorVersion();

    /**
     * Checks if a document is the latest major version in the version series.
     *
     * @since 5.4
     */
    boolean isLatestMajorVersion();

    /**
     * Checks if there is a checked out working copy for the version series of this document.
     *
     * @since 5.4
     */
    boolean isVersionSeriesCheckedOut();

    /**
     * Gets the access control policy (ACP) for this document.
     * <p>
     * Returns null if no security was defined on this document.
     * <p>
     * The ACP can be used to introspect or to evaluate user privileges on this document.
     * <p>
     * This is a wrapper for {@link CoreSession#getACP(DocumentRef)} but it is recommended since it caches the ACP for
     * later usage.
     *
     * @return the security data model or null if none
     */
    ACP getACP();

    /**
     * Sets the ACP for this document model.
     * <p>
     * This is a wrapper for {@link CoreSession#setACP(DocumentRef, ACP, boolean)}
     *
     * @see {@link CoreSession#setACP(DocumentRef, ACP, boolean)}
     * @param acp the ACP to set
     * @param overwrite whether to overwrite the old ACP or not
     */
    void setACP(ACP acp, boolean overwrite);

    /**
     * Gets a property from the given schema.
     * <p>
     * The data model owning the property will be fetched from the server if not already fetched.
     *
     * @param schemaName the schema name
     * @param name the property name
     * @return the property value or null if no such property exists
     */
    Object getProperty(String schemaName, String name);

    /**
     * Gets a property object from the given schema.
     *
     * @param schema the schema name
     * @param name the property name
     * @return the property, or {@code null} if no such property exists
     * @since 8.4
     */
    Property getPropertyObject(String schema, String name);

    /**
     * Sets the property value from the given schema.
     * <p>
     * This operation will not fetch the data model if not already fetched
     *
     * @param schemaName the schema name
     * @param name the property name
     * @param value the property value
     */
    void setProperty(String schemaName, String name, Object value);

    /**
     * Gets the values from the given data model as a map.
     * <p>
     * The operation will fetch the data model from the server if not already fetched.
     *
     * @param schemaName the data model schema name
     * @return the values map
     */
    Map<String, Object> getProperties(String schemaName);

    /**
     * Sets values for the given data model.
     * <p>
     * This will not fetch the data model if not already fetched.
     *
     * @param schemaName the schema name
     * @param data the values to set
     */
    void setProperties(String schemaName, Map<String, Object> data);

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
     */
    boolean isDownloadable();

    /**
     * Checks if this document is a version.
     *
     * @return true if the document is an older version of another document, false otherwise
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
     * @return {@code true} if the document is a version or a proxy to a version, {@code false} otherwise
     * @since 1.6.1 (5.3.1)
     */
    boolean isImmutable();

    /**
     * Checks if the document has actual data to write (dirty parts).
     *
     * @since 5.5
     */
    boolean isDirty();

    /**
     * Method that implement the visitor pattern.
     * <p>
     * The visitor must return null to stop visiting children otherwise a context object that will be passed as the arg
     * argument to children
     *
     * @param visitor the visitor to accept
     * @param arg an argument passed to the visitor. This should be used by the visitor to carry on the visiting
     *            context.
     * @since 5.5
     */
    void accept(PropertyVisitor visitor, Object arg);

    /**
     * Adapts the document to the given interface.
     * <p>
     * Attention, the first computation will cache the adaptation result for later calls.
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
     * @return the life cycle as a string
     */
    String getCurrentLifeCycleState();

    /**
     * Returns the life cycle policy of the document.
     *
     * @see org.nuxeo.ecm.core.lifecycle
     * @return the life cycle policy
     */
    String getLifeCyclePolicy();

    /**
     * Follows a given life cycle transition.
     * <p>
     * This will update the current life cycle of the document.
     *
     * @param transition the name of the transition to follow
     * @return a boolean representing the status if the operation
     */
    boolean followTransition(String transition);

    /**
     * Gets the allowed state transitions for this document.
     *
     * @return a collection of state transitions as string
     */
    Collection<String> getAllowedStateTransitions();

    /**
     * Gets the context data associated to this document.
     *
     * @return serializable map of context data.
     */
    ScopedMap getContextData();

    /**
     * Gets the context data associated to this document for given scope and given key.
     */
    Serializable getContextData(ScopeType scope, String key);

    /**
     * Adds mapping to the context data for given scope.
     * <p>
     * Context data is like a request map set on the document model to pass additional information to components
     * interacting with the document model (events processing for instance).
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
    void copyContent(DocumentModel sourceDoc);

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
     * <p>
     * Since 5.6, the timestamp does not hold milliseconds anymore as some databases do not store them, which could
     * interfere with cache key comparisons.
     *
     * @return the cache key as a string
     */
    String getCacheKey();

    /**
     * Returns the source document identifier.
     * <p>
     * This is useful when not interested about the repository UUID itself. Technically, this is the current version
     * UUID.
     *
     * @return the source id as a string.
     */
    String getSourceId();

    /**
     * Checks if a property is prefetched.
     *
     * @param xpath the property xpath
     * @return {@code true} if it is prefetched
     * @since 5.5
     */
    boolean isPrefetched(String xpath);

    /**
     * Checks if a property is prefetched.
     *
     * @param schemaName the schema name
     * @param name the property name
     * @return {@code true} if it is prefetched
     * @since 5.5
     */
    boolean isPrefetched(String schemaName, String name);

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
     * Gets system property of the specified type. This is not a lazy loaded property, thus the request is made directly
     * to the server. This is needed as some critical system properties might be changed directly in the core.
     */
    <T extends Serializable> T getSystemProp(String systemProperty, Class<T> type);

    /**
     * Get a document part given its schema name
     *
     * @param schema the schema
     * @return the document aprt or null if none exists for that schema
     * @deprecated since 8.4, use direct {@link Property} getters instead
     * @see #getPropertyObject
     * @see #getPropertyObjects
     */
    @Deprecated
    DocumentPart getPart(String schema);

    /**
     * Gets this document's parts.
     *
     * @deprecated since 8.4, use direct {@link Property} getters instead
     * @see #getSchemas
     * @see #getPropertyObject
     * @see #getPropertyObjects
     */
    @Deprecated
    DocumentPart[] getParts();

    /**
     * Gets the {@link Property} objects for the given schema.
     * <p>
     * An empty list is returned if the document doesn't have the schema.
     *
     * @param schema the schema
     * @return the properties
     * @since 8.4
     */
    Collection<Property> getPropertyObjects(String schema);

    /**
     * Gets a property given a xpath.
     * <p>
     * Note that what's called xpath in this context is not an actual XPath as specified by the w3c. Main differences
     * are that in our xpath:
     * <ul>
     * <li>Indexes start at 0 instead of 1</li>
     * <li>You can express {@code foo/bar[i]/baz} as {@code foo/i/baz}</li>
     * </ul>
     * The latter is possible because in Nuxeo lists of complex elements are homogenous, so the name of the second-level
     * element is implied.
     */
    Property getProperty(String xpath) throws PropertyException;

    /**
     * Gets a property value given a xpath.
     * <p>
     * Note that what's called xpath in this context is not an actual XPath as specified by the w3c. Main differences
     * are that in our xpath:
     * <ul>
     * <li>Indexes start at 0 instead of 1</li>
     * <li>You can express {@code foo/bar[i]/baz} as {@code foo/i/baz}</li>
     * </ul>
     * The latter is possible because in Nuxeo lists of complex elements are homogenous, so the name of the second-level
     * element is implied.
     */
    Serializable getPropertyValue(String xpath) throws PropertyException;

    /**
     * Sets a property value given a xpath.
     */
    void setPropertyValue(String xpath, Serializable value) throws PropertyException;

    /**
     * Clears any prefetched or cached document data.
     * <p>
     * This will force the document to lazily update its data when required.
     */
    void reset();

    /**
     * Refresh document data from server.
     * <p>
     * The data models will be removed and all prefetch and system data will be refreshed from the server
     * <p>
     * The refreshed data contains:
     * <ul>
     * <li>document life cycle
     * <li>document lock state, acp if required
     * <li>document prefetch map
     * <li>acp if required - otherwise acp info will be cleared so that it will be refetched in lazy way
     * <li>document parts if required - otherwise parts data will be removed to be refreshed lazy
     * </ul>
     * The refresh flags are:
     * <ul>
     * <li>{@link DocumentModel#REFRESH_STATE}
     * <li>{@link DocumentModel#REFRESH_PREFETCH}
     * <li>{@link DocumentModel#REFRESH_ACP_IF_LOADED}
     * <li>{@link DocumentModel#REFRESH_ACP_LAZY}
     * <li>{@link DocumentModel#REFRESH_ACP}
     * <li>{@link DocumentModel#REFRESH_CONTENT_IF_LOADED}
     * <li>{@link DocumentModel#REFRESH_CONTENT_LAZY}
     * <li>{@link DocumentModel#REFRESH_CONTENT}
     * <li>{@link DocumentModel#REFRESH_DEFAULT} same as REFRESH_STATE | REFRESH_DEFAULT | REFRESH_ACP_IF_LOADED |
     * REFRESH_CONTENT_IF_LOADED
     * <li>{@link DocumentModel#REFRESH_ALL} same as REFRESH_STATE | REFRESH_PREFTECH | REFRESH_ACP | REFRESH_CONTENT
     * </ul>
     * If XX_IF_LOADED is used then XX will be refreshed only if already loaded in the document - otherwise a lazy
     * refresh will be done
     *
     * @param refreshFlags the refresh flags
     * @param schemas the document parts (schemas) that should be refreshed now
     */
    void refresh(int refreshFlags, String[] schemas);

    /** Info fetched internally during a refresh. */
    class DocumentModelRefresh {

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

        public Prefetch prefetch;

        public Set<String> instanceFacets;

        public DocumentPart[] documentParts;
    }

    /**
     * Same as {@code DocumentModel.refresh(REFRESH_DEFAULT)}.
     */
    void refresh();

    /**
     * Clone operation. Must be made public instead of just protected as in Object.
     */
    DocumentModel clone() throws CloneNotSupportedException;

    /**
     * Opaque string that represents the last update state of the DocumentModel.
     * <p>
     * This token can be used for optimistic locking and avoid dirty updates. See CMIS spec :
     * http://docs.oasis-open.org/cmis/CMIS/v1.0/os/cmis-spec-v1.0.html#_Toc243905432
     *
     * @since 5.5
     * @return the ChangeToken string that can be null for some Document types
     */
    String getChangeToken();

    /**
     * Gets the fulltext extracted from the binary fields.
     *
     * @since 5.9.3
     */
    Map<String, String> getBinaryFulltext();

    /**
     * @param xpath the property xpath
     * @return A {@link PropertyObjectResolver} to manage the property reference to external entities, null if this
     *         property's type has no resolver.
     * @since 7.1
     */
    PropertyObjectResolver getObjectResolver(String xpath);

}
