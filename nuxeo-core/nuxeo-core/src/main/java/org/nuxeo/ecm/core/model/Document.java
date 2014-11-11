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
 *     Julien Anguenot
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.model;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.lifecycle.LifeCycleException;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.types.ComplexType;

/**
 * A low-level document from a {@link Session}.
 */
public interface Document {

    /**
     * Gets the session that owns this document.
     *
     * @return the session
     */
    Session getSession();

    /**
     * Gets the name of this document.
     *
     * @return the document name
     */
    String getName();

    /**
     * Gets this document's UUID.
     *
     * @return the document UUID
     */
    String getUUID();

    /**
     * Gets the parent document, or {@code null} if this is the root document.
     *
     * @return the parent document, or {@code null}
     */
    Document getParent() throws DocumentException;

    /**
     * Gets the type of this document.
     *
     * @return the document type
     */
    DocumentType getType();

    /**
     * Gets the path of this document.
     *
     * @return the path
     */
    String getPath() throws DocumentException;

    /**
     * Sets a property value.
     *
     * @param name the name of the property to set
     * @param value the value to set
     */
    void setPropertyValue(String name, Serializable value)
            throws DocumentException;

    /**
     * Gets a property value.
     *
     * @param name the name of the property to get
     * @return the property value or {@code null} if the property is not set
     */
    Serializable getPropertyValue(String name) throws DocumentException;

    /**
     * Checks whether this document is a folder.
     *
     * @return {@code true} if the document is a folder, {@code false} otherwise
     */
    boolean isFolder();

    /**
     * Sets this document as readonly or not.
     *
     * @since 5.9.2
     */
    void setReadOnly(boolean readonly);

    /**
     * Checks whether this document is readonly or not.
     *
     * @since 5.9.2
     */
    boolean isReadOnly();

    /**
     * Removes this document and all its children, if any.
     */
    void remove() throws DocumentException;

    /**
     * Gets the life cycle state of this document.
     *
     * @return the life cycle state
     */
    String getLifeCycleState() throws LifeCycleException;

    /**
     * Sets the life cycle state of this document.
     *
     * @param state the life cycle state
     */
    void setCurrentLifeCycleState(String state) throws LifeCycleException;

    /**
     * Gets the life cycle policy of this document.
     *
     * @return the life cycle policy
     */
    String getLifeCyclePolicy() throws LifeCycleException;

    /**
     * Sets the life cycle policy of this document.
     *
     * @param policy the life cycle policy
     */
    void setLifeCyclePolicy(String policy) throws LifeCycleException;

    /**
     * Follows a given life cycle transition.
     * <p>
     * This will update the life cycle state of the document.
     *
     * @param transition the name of the transition to follow
     */
    void followTransition(String transition) throws LifeCycleException;

    /**
     * Returns the allowed state transitions for this document.
     *
     * @return a collection of state transitions
     */
    Collection<String> getAllowedStateTransitions() throws LifeCycleException;

    /**
     * Checks whether or not this document is a proxy.
     *
     * @return {@code true} if this document is a proxy, {@code false} otherwise
     */
    boolean isProxy();

    /**
     * Gets the repository in which the document lives.
     *
     * @return the repository name.
     */
    String getRepositoryName();

    /**
     * Sets a system property.
     */
    void setSystemProp(String name, Serializable value)
            throws DocumentException;

    /**
     * Gets a system property.
     */
    <T extends Serializable> T getSystemProp(String name, Class<T> type)
            throws DocumentException;

    /**
     * Loads a {@link DocumentPart} from storage.
     * <p>
     * Reading data is done by {@link DocumentPart} because of per-proxy
     * schemas.
     */
    void readDocumentPart(DocumentPart dp) throws PropertyException;

    /**
     * Reads a set of prefetched fields.
     * <p>
     * Reading data is done by {@link ComplexType} because of per-proxy schemas.
     *
     * @since 5.9.4
     */
    Map<String, Serializable> readPrefetch(ComplexType complexType,
            Set<String> xpaths) throws PropertyException;

    /**
     * Writes a {@link DocumentPart} to storage.
     * <p>
     * Writing data is done by {@link DocumentPart} because of per-proxy
     * schemas.
     */
    void writeDocumentPart(DocumentPart dp) throws PropertyException;

    /**
     * Gets the facets available on this document (from the type and the
     * instance facets).
     *
     * @return the facets
     * @since 5.4.2
     */
    Set<String> getAllFacets();

    /**
     * Gets the facets defined on this document instance. The type facets are
     * not returned.
     *
     * @return the facets
     * @since 5.4.2
     */
    String[] getFacets();

    /**
     * Checks whether this document has a given facet, either from its type or
     * added on the instance.
     *
     * @param facet the facet name
     * @return {@code true} if the document has the facet
     *
     * @since 5.4.2
     */
    boolean hasFacet(String facet);

    /**
     * Adds a facet to this document.
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
    boolean addFacet(String facet) throws DocumentException;

    /**
     * Removes a facet from this document.
     * <p>
     * It's not possible to remove a facet coming from the document type.
     *
     * @param facet the facet name
     * @return {@code true} if the facet was removed, or {@code false} if it
     *         isn't present or is present on the type or does not exit
     *
     * @since 5.4.2
     */
    boolean removeFacet(String facet) throws DocumentException;

    /**
     * Sets a lock on this document.
     *
     * @param lock the lock to set
     * @return {@code null} if locking succeeded, or the existing lock if
     *         locking failed
     */
    Lock setLock(Lock lock) throws DocumentException;

    /**
     * Removes a lock from this document.
     *
     * @param the owner to check, or {@code null} for no check
     * @return {@code null} if there was no lock or if removal succeeded, or a
     *         lock if it blocks removal due to owner mismatch
     */
    Lock removeLock(String owner) throws DocumentException;

    /**
     * Gets the lock if one set on this document.
     *
     * @return the lock, or {@code null} if no lock is set
     */
    Lock getLock() throws DocumentException;

    /**
     * Gets a child document given its name.
     * <p>
     * Throws {@link NoSuchDocumentException} if the document could not be
     * found.
     *
     * @param name the name of the child to retrieve
     * @return the child if exists
     * @throws NoSuchDocumentException if the child does not exist
     */
    Document getChild(String name) throws DocumentException;

    /**
     * Gets an iterator over the children of the document.
     * <p>
     * Returns an empty iterator for non-folder documents.
     *
     * @return the children iterator
     */
    Iterator<Document> getChildren() throws DocumentException;

    /**
     * Gets a list of the children ids.
     * <p>
     * Returns an empty list for non-folder documents.
     *
     * @return a list of children ids.
     * @since 1.4.1
     */
    List<String> getChildrenIds() throws DocumentException;

    /**
     * Checks whether this document has a child of the given name.
     * <p>
     * Returns {@code false} for non-folder documents.
     *
     * @param name the name of the child to check
     * @return {@code true} if the child exists, {@code false} otherwise
     */
    boolean hasChild(String name) throws DocumentException;

    /**
     * Tests if the document has any children.
     * <p>
     * Returns {@code false} for non-folder documents.
     *
     * @return {@code true} if the document has children, {@code false}
     *         otherwise
     */
    boolean hasChildren() throws DocumentException;

    /**
     * Creates a new child document of the given type.
     * <p>
     * Throws an error if this document is not a folder.
     *
     * @param name the name of the new child to create
     * @param typeName the type of the child to create
     * @return the newly created document
     */
    Document addChild(String name, String typeName) throws DocumentException;

    /**
     * Orders the given source child before the destination child.
     * <p>
     * Both source and destination must be names that point to child documents
     * of this document. The source document will be placed before the
     * destination one. If destination is {@code null}, the source document will
     * be appended at the end of the children list.
     *
     * @param src the document to move
     * @param dest the document before which to place the source document
     * @throws DocumentException if this document is not an orderable folder
     */
    void orderBefore(String src, String dest) throws DocumentException;

    /**
     * Creates a new version.
     *
     * @param label the version label
     * @param checkinComment the checkin comment
     * @return the created version
     */
    Document checkIn(String label, String checkinComment)
            throws DocumentException;

    void checkOut() throws DocumentException;

    /**
     * Gets the list of version ids for this document.
     *
     * @return the list of version ids
     * @since 1.4.1
     */
    List<String> getVersionsIds() throws DocumentException;

    /**
     * Gets the versions for this document.
     *
     * @return the versions of the document, or an empty list if there are no
     *         versions
     */
    List<Document> getVersions() throws DocumentException;

    /**
     * Gets the last version of this document.
     * <p>
     * Returns {@code null} if there is no version at all.
     *
     * @return the last version, or {@code null} if there is no version
     */
    Document getLastVersion() throws DocumentException;

    /**
     * Gets the source for this document.
     * <p>
     * For a version, it's the working copy.
     * <p>
     * For a proxy, it's the version the proxy points to.
     *
     * @return the source document
     */
    Document getSourceDocument() throws DocumentException;

    /**
     * Replaces this document's content with the version specified.
     *
     * @param version the version to replace with
     */
    void restore(Document version) throws DocumentException;

    /**
     * Gets a version of this document, given its label.
     *
     * @param label the version label
     * @return the version
     */
    Document getVersion(String label) throws DocumentException;

    /**
     * Checks whether this document is a version document.
     *
     * @return {@code true} if it's a version, {@code false} otherwise
     */
    boolean isVersion();

    /**
     * Gets the version to which a checked in document is linked.
     * <p>
     * Returns {@code null} for a checked out document or a version or a proxy.
     *
     * @return the version, or {@code null}
     */
    Document getBaseVersion() throws DocumentException;

    /**
     * Checks whether this document is checked out.
     *
     * @return {@code true} if the document is checked out, or {@code false}
     *         otherwise
     */
    boolean isCheckedOut() throws DocumentException;

    /**
     * Gets the version creation date of this document if it's a version or a
     * proxy.
     *
     * @return the version creation date, or {@code null} if it's not a version
     *         or a proxy
     */
    Calendar getVersionCreationDate() throws DocumentException;

    /**
     * Gets the version check in comment of this document if it's a version or a
     * proxy.
     *
     * @return the check in comment, or {@code null} if it's not a version or a
     *         proxy
     */
    String getCheckinComment() throws DocumentException;

    /**
     * Gets the version series id.
     *
     * @return the version series id
     */
    String getVersionSeriesId() throws DocumentException;

    /**
     * Gets the version label.
     *
     * @return the version label
     */
    String getVersionLabel() throws DocumentException;

    /**
     * Checks whether this document is the latest version.
     *
     * @return {@code true} if this is the latest version, or {@code false}
     *         otherwise
     */
    boolean isLatestVersion() throws DocumentException;

    /**
     * Checks whether this document is a major version.
     *
     * @return {@code true} if this is a major version, or {@code false}
     *         otherwise
     */
    boolean isMajorVersion() throws DocumentException;

    /**
     * Checks whether this document is the latest major version.
     *
     * @return {@code true} if this is the latest major version, or
     *         {@code false} otherwise
     */
    boolean isLatestMajorVersion() throws DocumentException;

    /**
     * Checks if there is a checked out working copy for the version series of
     * this document.
     *
     * @return {@code true} if there is a checked out working copy
     */
    boolean isVersionSeriesCheckedOut() throws DocumentException;

    /**
     * Gets the working copy for this document.
     *
     * @return the working copy
     */
    Document getWorkingCopy() throws DocumentException;

    /**
     * Gets the document (version or live document) to which this proxy points.
     */
    Document getTargetDocument();

    /**
     * Sets the document (version or live document) to which this proxy points.
     */
    void setTargetDocument(Document target) throws DocumentException;

}
