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

package org.nuxeo.ecm.core.model;

import java.io.InputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

import javax.transaction.xa.XAResource;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.query.Query;
import org.nuxeo.ecm.core.query.QueryException;
import org.nuxeo.ecm.core.query.QueryFilter;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.security.SecurityManager;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface Session {

    // parameters for the session contexts
    String USER_NAME = "username";

    /**
     * Gets the session id.
     * <p>
     * This is an unique identifier of this session relative to the current JVM.
     *
     * @return the session id
     */
    long getSessionId();

    /**
     * The unique ID of the user session on this repository.
     * <p>
     * The ID is unique in the application (possible deployed on multiple
     * machines).
     * <p>
     * This ID is normally passed by the session creator through the session
     * context.
     *
     * @return the user session id
     */
    String getUserSessionId();

    /**
     * Gets the repository that created this session.
     *
     * @return the repository
     */
    Repository getRepository();

    /**
     * Gets the session context used to create this session.
     *
     * @return the session context
     */
    Map<String, Serializable> getSessionContext();

    /**
     * Creates a query object given a SQL like query string.
     *
     * @param query the SQL like query
     * @return the query
     */
    Query createQuery(String query, Query.Type qType, String... params)
            throws QueryException;

    /**
     *
     * @throws QueryException
     */
    IterableQueryResult queryAndFetch(String query, String queryType,
            QueryFilter queryFilter, Object... params) throws QueryException;

    /**
     * Gets the type manager used by the repository.
     *
     * @return the type manager
     */
    SchemaManager getTypeManager();

    /**
     * Gets the security manager used by the repository.
     *
     * @return the security manager
     */
    SecurityManager getSecurityManager();

    /**
     * The XAResource corresponding to this session.
     *
     * @return the XAResource
     */
    XAResource getXAResource();

    /**
     * Saves this session.
     *
     * @throws DocumentException if any error occurs
     */
    void save() throws DocumentException;

    /**
     * Cancels changes done in this session.
     *
     * @throws DocumentException if any error occurs
     */
    void cancel() throws DocumentException;

    /**
     * Checks whether the session is alive.
     *
     * @return true if the session is closed, false otherwise
     */
    boolean isLive();

    /**
     * Closes this session and saves changes on the workspace, if any.
     *
     * @throws DocumentException if any error occurs
     */
    void close() throws DocumentException;

    /**
     * Closes the session and cancel changes on workspace, if any.
     */
    void dispose();

    /**
     * Gets the document at the given path, if any.
     *
     * @param path
     * @return
     * @throws DocumentException if any error occurs
     */
    Document resolvePath(String path) throws DocumentException;

    /**
     * Gets a document given its ID.
     *
     * @param uuid the document id
     * @return the document
     * @throws DocumentException if any error occurs
     */
    Document getDocumentByUUID(String uuid) throws DocumentException;

    /**
     * Gets the root document in this repository.
     *
     * @return the root document
     * @throws DocumentException if any error occurs
     */
    Document getRootDocument() throws DocumentException;

    /**
     * Copies the source document to the given folder.
     * <p>
     * If the destination document is not a folder, an exception is thrown.
     *
     * @param src
     * @param dst
     * @param name
     * @throws DocumentException if any error occurs
     */
    Document copy(Document src, Document dst, String name)
            throws DocumentException;

    /**
     * Moves the source document to the given folder.
     * <p>
     * If the destination document is not a folder an exception is thrown.
     *
     * @param src the source document to move
     * @param dst the destination folder
     * @param name the new name of the document or null if the original name
     *            should be preserved
     * @throws DocumentException if any error occurs
     */
    Document move(Document src, Document dst, String name)
            throws DocumentException;

    /**
     * Gets a blob stream using the given key.
     * <p>
     * The implementation may use anything as the key. It may use the property
     * path or a unique ID of the property.
     * <p>
     * This method can be used to lazily fetch blob content.
     *
     * @param key the key of the blob data
     * @return the blob stream
     * @throws DocumentException if any error occurs
     */
    InputStream getDataStream(String key) throws DocumentException;

    /**
     * Creates a generic proxy to the given document inside the given folder.
     *
     * @param doc the document
     * @param folder the folder
     * @return the proxy
     * @throws DocumentException if any error occurs
     */
    Document createProxy(Document doc, Document folder)
            throws DocumentException;

    /**
     * Creates a proxy to the given version of the given document inside the
     * given parent.
     *
     * @param parent the parent
     * @param doc the document
     * @param versionLabel the version
     * @return the proxy
     * @throws DocumentException if any error occurs
     */
    Document createProxyForVersion(Document parent, Document doc,
            String versionLabel) throws DocumentException;

    /**
     * Finds the proxies for a document. If the folder is not null, the search
     * will be limited to its children.
     * <p>
     * If the document is a version, then only proxies to that version will be
     * looked up.
     *
     * @param doc the document or version
     * @param folder the folder, or null
     * @return the list of proxies if any is found otherwise an empty list
     * @throws DocumentException if any error occurs
     * @since 1.4.1 for the case where doc is a proxy
     */
    Collection<Document> getProxies(Document doc, Document folder)
            throws DocumentException;

    /**
     * Imports a document with a given id and parent.
     * <p>
     * The document can then be filled with the normal imported properties.
     *
     * @param uuid the document uuid
     * @param parent the document parent, or {@code null} for a version
     * @param name the document name in its parent
     * @param typeName the document type, or {@code ecm:proxy} for a proxy
     * @param properties system properties of the document, which will vary
     *            depending whether it's a live document, a version or a proxy
     *            (see the various {@code IMPORT_*} constants of
     *            {@link CoreSession})
     * @return a writable {@link Document}, even for proxies and versions
     * @throws DocumentException
     */
    Document importDocument(String uuid, Document parent, String name,
            String typeName, Map<String, Serializable> properties)
            throws DocumentException;

    /**
     * Gets a version of a document, given its versionable id and label.
     * <p>
     * The version model contains the label of the version to look for. On
     * return, it is filled with the version's description and creation date.
     *
     * @param versionableId the versionable id
     * @param versionModel the version model
     * @return the version, or {@code null} if not found
     * @throws DocumentException
     */
    Document getVersion(String versionableId, VersionModel versionModel)
            throws DocumentException;

}
