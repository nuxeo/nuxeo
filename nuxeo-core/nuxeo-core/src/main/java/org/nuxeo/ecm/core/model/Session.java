/*
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.model;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.PartialList;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.query.QueryFilter;

/**
 * Internal Session accessing the low-level storage.
 */
public interface Session {

    // parameters for the session contexts
    String USER_NAME = "username";

    /**
     * Gets the repository that created this session.
     *
     * @return the repository
     */
    String getRepositoryName();

    /**
     * Does a query.
     *
     * @since 5.9.4
     */
    PartialList<Document> query(String query, String queryType, QueryFilter queryFilter, long countUpTo);

    /**
     * Does a query and fetch the individual results as maps.
     */
    IterableQueryResult queryAndFetch(String query, String queryType, QueryFilter queryFilter,
            boolean distinctDocuments, Object[] params);

    /**
     * Does a query and fetch the individual results as maps.
     *
     * @since 7.10-HF25, 8.10-HF06, 9.2
     */
    PartialList<Map<String, Serializable>> queryProjection(String query, String queryType, QueryFilter queryFilter,
            boolean distinctDocuments, long countUpTo, Object[] params);

    /**
     * Gets the lock manager for this session.
     *
     * @return the lock manager
     * @since 7.4
     */
    LockManager getLockManager();

    /**
     * Saves this session.
     */
    void save();

    /**
     * Checks whether the session is alive.
     *
     * @return true if the session is closed, false otherwise
     */
    boolean isLive();

    /**
     * Returns {@code true} if all sessions in the current thread share the same state.
     */
    boolean isStateSharedByAllThreadSessions();

    /**
     * Closes this session. Does not save.
     */
    void close();

    /**
     * Gets the document at the given path, if any.
     *
     * @param path
     * @return
     * @throws DocumentNotFoundException if the document doesn't exist
     */
    Document resolvePath(String path) throws DocumentNotFoundException;

    /**
     * Gets a document given its ID.
     *
     * @param uuid the document id
     * @return the document
     * @throws DocumentNotFoundException if the document doesn't exist
     */
    Document getDocumentByUUID(String uuid) throws DocumentNotFoundException;

    /**
     * Gets the root document in this repository.
     *
     * @return the root document
     */
    Document getRootDocument();

    /**
     * Gets the null document, to be used as a fake parent to add placeless children.
     *
     * @return the null document
     */
    Document getNullDocument();

    /**
     * Copies the source document to the given folder.
     * <p>
     * If the destination document is not a folder, an exception is thrown.
     *
     * @param src
     * @param dst
     * @param name
     */
    Document copy(Document src, Document dst, String name);

    /**
     * Moves the source document to the given folder.
     * <p>
     * If the destination document is not a folder an exception is thrown.
     *
     * @param src the source document to move
     * @param dst the destination folder
     * @param name the new name of the document or null if the original name should be preserved
     */
    Document move(Document src, Document dst, String name);

    /**
     * Creates a generic proxy to the given document inside the given folder.
     *
     * @param doc the document
     * @param folder the folder
     * @return the proxy
     */
    Document createProxy(Document doc, Document folder);

    /**
     * Finds the proxies for a document. If the folder is not null, the search will be limited to its children.
     * <p>
     * If the document is a version, then only proxies to that version will be looked up.
     *
     * @param doc the document or version
     * @param folder the folder, or null
     * @return the list of proxies if any is found otherwise an empty list
     * @since 1.4.1 for the case where doc is a proxy
     */
    List<Document> getProxies(Document doc, Document folder);

    /**
     * Sets a proxies' target.
     * <p>
     * The target must have the same version series as the proxy.
     *
     * @param proxy the proxy
     * @param target the new target
     * @since 5.5
     */
    void setProxyTarget(Document proxy, Document target);

    /**
     * Imports a document with a given id and parent.
     * <p>
     * The document can then be filled with the normal imported properties.
     *
     * @param uuid the document uuid
     * @param parent the document parent, or {@code null} for a version
     * @param name the document name in its parent
     * @param typeName the document type, or {@code ecm:proxy} for a proxy
     * @param properties system properties of the document, which will vary depending whether it's a live document, a
     *            version or a proxy (see the various {@code IMPORT_*} constants of {@link CoreSession})
     * @return a writable {@link Document}, even for proxies and versions
     */
    Document importDocument(String uuid, Document parent, String name, String typeName,
            Map<String, Serializable> properties);

    /**
     * Gets a version of a document, given its versionable id and label.
     * <p>
     * The version model contains the label of the version to look for. On return, it is filled with the version's
     * description and creation date.
     *
     * @param versionableId the versionable id
     * @param versionModel the version model
     * @return the version, or {@code null} if not found
     */
    Document getVersion(String versionableId, VersionModel versionModel);

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

    ACP getMergedACP(Document doc);

    void setACP(Document doc, ACP acp, boolean overwrite);

    /**
     * Gets the fulltext extracted from the binary fields.
     *
     * @since 5.9.3
     */
    Map<String, String> getBinaryFulltext(String id);

}
