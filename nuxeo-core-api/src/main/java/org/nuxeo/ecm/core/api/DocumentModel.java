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
 * $Id$
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
 * <pre><code>
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
 * </code></pre>
 *
 * @see CoreSession
 * @see DataModel
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface DocumentModel extends Serializable {

    int REFRESH_LOCK = 1;
    int REFRESH_LIFE_CYCLE = 2;
    int REFRESH_PREFETCH = 4;
    int REFRESH_ACP_IF_LOADED = 8; // refresh now only if already loaded
    int REFRESH_ACP_LAZY = 16; // refresh later in lazy mode
    int REFRESH_ACP = 32; // refresh now
    int REFRESH_CONTENT_IF_LOADED = 64; // refresh now only if already loaded
    int REFRESH_CONTENT_LAZY = 128; // refresh later in lazy mode
    int REFRESH_CONTENT = 256; // refresh now
    int REFRESH_STATE = REFRESH_LIFE_CYCLE | REFRESH_LOCK;
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
     * Gets the schemas defined by this document type.
     *
     * @return the defined schemas
     */
    String[] getDeclaredSchemas();

    /**
     * Gets the facets defined by this document type.
     *
     * @return the defined facets
     */
    Set<String> getDeclaredFacets();

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
     * This uses the cached lock information and doesn't connect to the server.
     * <p>
     * To get fresh information from the server, use
     * {@link CoreSession#getLock(DocumentRef)}.
     *
     * @return the lock key if the document is locked or null otherwise
     */
    String getLock();

    /**
     * Tests if the document is locked.
     * <p>
     * This is using the cached lock information and doesn't connect to the
     * server
     * <p>
     * To get fresh information from the server use
     * {@link CoreSession#getLock(DocumentRef)}.
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
     */
    void setLock(String key) throws ClientException;

    /**
     * Unlocks the given document.
     * <p>
     * This is a wrapper for {@link CoreSession#unlock(DocumentRef)}
     *
     * @throws ClientException if the document is already locked or other error
     *             occurs
     */
    void unlock() throws ClientException;

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
    void setProperty(String schemaName, String name, Object value) throws ClientException;

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
    void setProperties(String schemaName, Map<String, Object> data) throws ClientException;

    /**
     * Checks whether this document model has the given schema.
     *
     * @param schema the schema name to check
     * @return true if the document has this schema, false otherwise
     */
    boolean hasSchema(String schema);

    /**
     * Checks if this document has the given facet.
     *
     * @param facet the facet to check
     * @return true if the document has this facet, false otherwise
     */
    boolean hasFacet(String facet);

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
     * Returns the version label if the document model is a version.
     *
     * @return the version label or null if not a version.
     */
    String getVersionLabel();

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
    <T extends Serializable> T getSystemProp(String systemProperty, Class<T> type)
            throws ClientException, DocumentException;


    /**
     * Get a document part given its schema name
     * @param schema the schema
     * @return the document aprt or null if none exists for that schema
     * @throws ClientException
     */
    // TODO throw an exception if schema is not impl  by the doc?
    DocumentPart getPart(String schema) throws ClientException;

    /**
     * Gets this document's parts.
     */
    DocumentPart[] getParts() throws ClientException;

    /**
     * Gets a property given a xpath.
     */
    Property getProperty(String xpath) throws PropertyException, ClientException;

    /**
     * Gets a property value given a xpath.
     */
    Serializable getPropertyValue(String xpath) throws PropertyException, ClientException;

    /**
     * Sets a property value given a xpath.
     */
    void setPropertyValue(String xpath, Serializable value) throws PropertyException, ClientException;

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
     * The data models will be removed and all prefetch and system data will be refreshed from the server
     * <p>
     * The refreshed data contains:
     * <ul>
     * <li> document life cycle
     * <li> document lock state, acp if required
     * <li> document prefetch map
     * <li> acp if required - otherwise acp info will be cleared so that it will be refetched in lazy way
     * <li> document parts if required - otherwise parts data will be removed to be refreshed lazy
     * </ul>
     * The refresh flags are:
     * <ul>
     * <li> {@link DocumentModel#REFRESH_LIFE_CYCLE}
     * <li> {@link DocumentModel#REFRESH_LOCK}
     *
     * <li> {@link DocumentModel#REFRESH_PREFETCH}
     * <li> {@link DocumentModel#REFRESH_ACP_IF_LOADED}
     * <li> {@link DocumentModel#REFRESH_ACP_LAZY}
     * <li> {@link DocumentModel#REFRESH_ACP}
     * <li> {@link DocumentModel#REFRESH_CONTENT_IF_LOADED}
     * <li> {@link DocumentModel#REFRESH_CONTENT_LAZY}
     * <li> {@link DocumentModel#REFRESH_CONTENT}
     * <li> {@link DocumentModel#REFRESH_STATE} same as REFRESH_LIFE_CYCLE | REFRESH_LOCK
     * <li> {@link DocumentModel#REFRESH_DEFAULT} same as REFRESH_STATE | REFRESH_DEFAULT | REFRESH_ACP_IF_LOADED | REFRESH_CONTENT_IF_LOADED
     * <li> {@link DocumentModel#REFRESH_ALL} same as REFRESH_STATE | REFRESH_PREFTECH | REFRESH_ACP | REFRESH_CONTENT
     * </ul>
     * If XX_IF_LOADED is used then XX will be refreshed only if already loaded in the document - otherwise a lazy refresh will be done
     *
     * @param refreshFlags the refresh flags
     * @param schemas the document parts (schemas) that should be refreshed now
     */
    void refresh(int refreshFlags, String[] schemas) throws ClientException;

    /**
     * Same as {@code DocumentModel.refresh(REFRESH_DEFAULT)}.
     */
    void refresh() throws ClientException;

    /**
     * Clone operation. Must be made public instead of just protected as in Object.
     */
    DocumentModel clone() throws CloneNotSupportedException;

}
