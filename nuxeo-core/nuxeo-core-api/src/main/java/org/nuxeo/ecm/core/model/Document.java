/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Julien Anguenot
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.model;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.LifeCycleException;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.schema.DocumentType;

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
     * Gets the document's position in its containing folder (if ordered).
     *
     * @return the position
     * @since 6.0
     */
    Long getPos();

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
    Document getParent();

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
    String getPath();

    /**
     * Sets a simple property value.
     * <p>
     * For more generic properties described by an xpath, use {@link #setValue} instead.
     *
     * @param name the name of the property to set
     * @param value the value to set
     * @see #setValue
     */
    void setPropertyValue(String name, Serializable value);

    /**
     * Sets a property value.
     * <p>
     * The xpath may point to a partial path, in which case the value may be a complex {@link List} or {@link Map}.
     *
     * @param xpath the xpath of the property to set
     * @param value the value to set
     * @throws PropertyException if the property does not exist or the value is of the wrong type
     * @since 7.3
     */
    void setValue(String xpath, Object value) throws PropertyException;

    /**
     * Gets a simple property value.
     * <p>
     * For more generic properties described by an xpath, use {@link #getValue} instead.
     *
     * @param name the name of the property to get
     * @return the property value or {@code null} if the property is not set
     * @see #getValue
     */
    Serializable getPropertyValue(String name);

    /**
     * Gets a property value.
     * <p>
     * The xpath may point to a partial path, in which case the value may be a complex {@link List} or {@link Map}.
     *
     * @param xpath the xpath of the property to set
     * @return the property value or {@code null} if the property is not set
     * @throws PropertyException if the property does not exist
     */
    Object getValue(String xpath) throws PropertyException;

    /**
     * An accessor that can read or write a blob and know its xpath.
     *
     * @since 7.3
     */
    interface BlobAccessor {
        /** Gets the blob's xpath. */
        String getXPath();

        /** Gets the blob. */
        Blob getBlob();

        /** Sets the blob. */
        void setBlob(Blob blob);
    }

    /**
     * Visits all the blobs of this document and calls the passed blob visitor on each one.
     *
     * @since 7.3
     */
    void visitBlobs(Consumer<BlobAccessor> blobVisitor) throws PropertyException;

    /**
     * Visits the blobs of this document and, for those with a matching key, replace their key and digest with new ones.
     *
     * @param key the bob key to look for
     * @param newKey the new key
     * @param newDigest the new digest
     * @return the old digest if at least one replacement was done, {@code null} otherwise
     * @since 11.5
     */
    String replaceBlobDigest(String key, String newKey, String newDigest);

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
    void remove();

    /**
     * Turns the document into a record.
     * <p>
     * A record is a document with specific capabilities related to mandatory retention until a given date, and legal
     * holds. In addition, its main blob receives special treatment from the document blob manager to make sure it's
     * never shared with another blob at the storage level, and is deleted as soon as the record is deleted.
     * <p>
     * If the document is already a record, this method has no effect.
     * <p>
     * The permission {@value org.nuxeo.ecm.core.api.security.SecurityConstants#MAKE_RECORD} is required.
     *
     * @see #isRecord
     * @since 11.1
     */
    void makeRecord();

    /**
     * Checks if the document is a record.
     *
     * @return {@code true} if the document is a record, {@code false} otherwise
     * @see #makeRecord
     * @since 11.1
     */
    boolean isRecord();

    /**
     * Sets a retention date for the document (a record).
     * <p>
     * If no previous retention date was set, or if the previous retention date was
     * {@linkplain #RETAIN_UNTIL_INDETERMINATE indeterminate}, or if the previous retention date was <em>before</em>
     * the given value, then the retention date is set to the given value.
     * <p>
     * If the previous retention date was <em>after</em> the given value (that is, if trying to reduce the retention
     * time), an exception is thrown.
     * <p>
     * If the given value is {@code null} and the previous retention date is in the past (it has already expired), then
     * the retention date is set to {@code null}.
     * <p>
     * The permission {@value org.nuxeo.ecm.core.api.security.SecurityConstants#SET_RETENTION} is required.
     *
     * @param retainUntil the new retention date
     * @throws PropertyException if trying to reduce the retention time, or if the document is not a record
     * @see #getRetainUntil
     * @see #RETAIN_UNTIL_INDETERMINATE
     * @since 11.1
     */
    void setRetainUntil(Calendar retainUntil) throws PropertyException;

    /**
     * Gets the retention date for the document.
     *
     * @return the retention date, or {@value org.nuxeo.ecm.core.api.security.SecurityConstants#SET_RETENTION} for a
     *         retention in the indeterminate future, or {@code null} if there is no retention date
     * @see #setRetainUntil
     * @see #RETAIN_UNTIL_INDETERMINATE
     * @since 11.1
     */
    Calendar getRetainUntil();

    /**
     * Sets or removes a legal hold on the document (a record).
     * <p>
     * The permission {@value org.nuxeo.ecm.core.api.security.SecurityConstants#MANAGE_LEGAL_HOLD} is required.
     *
     * @param hold {@code true} to set a legal hold, {@code false} to remove it
     * @see #hasLegalHold
     * @throws PropertyException if the document is not a record
     * @since 11.1
     */
    void setLegalHold(boolean hold);

    /**
     * Checks if the document has a legal hold set.
     *
     * @return {@code true} if a legal hold has been set on the document, {@code false} otherwise
     * @see #setLegalHold
     * @since 11.1
     */
    boolean hasLegalHold();

    /**
     * Checks if the document has a retention date in the future or has a legal hold.
     *
     * @return {@code true} if the document has a retention date in the future or if it has a legal hold, {@code false}
     *         otherwise
     * @see #getRetainUntil
     * @see #hasLegalHold
     * @since 11.1
     */
    boolean isUnderRetentionOrLegalHold();

    /**
     * Checks whether this document is under active retention.
     *
     * @since 9.3
     * @deprecated since 11.1, unused, use {@link #hasLegalHold} instead
     */
    @Deprecated
    boolean isRetentionActive();

    /**
     * Sets or unsets this document as under active retention.
     *
     * @since 9.3
     * @deprecated since 11.1, unused, use {@link #setLegalHold} instead
     */
    @Deprecated
    void setRetentionActive(boolean retentionActive);

    /**
     * Gets the life cycle state of this document.
     *
     * @return the life cycle state
     */
    String getLifeCycleState();

    /**
     * Sets the life cycle state of this document.
     *
     * @param state the life cycle state
     */
    void setCurrentLifeCycleState(String state);

    /**
     * Gets the life cycle policy of this document.
     *
     * @return the life cycle policy
     */
    String getLifeCyclePolicy();

    /**
     * Sets the life cycle policy of this document.
     *
     * @param policy the life cycle policy
     */
    void setLifeCyclePolicy(String policy);

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
    Collection<String> getAllowedStateTransitions();

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
    void setSystemProp(String name, Serializable value);

    /**
     * Gets a system property.
     */
    <T extends Serializable> T getSystemProp(String name, Class<T> type);

    /**
     * Gets the current change token for this document.
     *
     * @return the change token
     * @since 9.1
     */
    String getChangeToken();

    /**
     * Validates that the passed user-visible change token is compatible with the one for this document.
     *
     * @return {@code false} if the change token is not valid
     * @since 9.2
     */
    boolean validateUserVisibleChangeToken(String changeToken);

    /**
     * Marks the document as being modified by a user change.
     * <p>
     * This causes an additional change token increment and check during save.
     *
     * @since 9.2
     */
    void markUserChange();

    /**
     * Loads a {@link DocumentPart} from storage.
     * <p>
     * Reading data is done by {@link DocumentPart} because of per-proxy schemas.
     */
    void readDocumentPart(DocumentPart dp) throws PropertyException;

    /**
     * Context passed to write operations to optionally record things to do at {@link #flush} time.
     *
     * @since 7.3
     */
    interface WriteContext {
        /**
         * Gets the recorded changed xpaths.
         */
        Set<String> getChanges();

        /**
         * Flushes recorded write operations.
         *
         * @param doc the base document being written
         */
        void flush(Document doc);
    }

    /**
     * Gets a write context for the current document.
     *
     * @since 7.3
     */
    WriteContext getWriteContext();

    /**
     * Writes a {@link DocumentPart} to storage.
     * <p>
     * Writing data is done by {@link DocumentPart} because of per-proxy schemas.
     *
     * @param dp the document part
     * @param writeContext the write context
     * @param create whether this is for a document creation
     * @return {@code true} if something changed
     * @since 11.1
     */
    boolean writeDocumentPart(DocumentPart dp, WriteContext writeContext, boolean create) throws PropertyException;

    /**
     * Writes a {@link DocumentPart} to storage.
     * <p>
     * Writing data is done by {@link DocumentPart} because of per-proxy schemas.
     *
     * @return {@code true} if something changed
     * @deprecated since 11.1, use the signature with {@code create} instead
     */
    @Deprecated
    default boolean writeDocumentPart(DocumentPart dp, WriteContext writeContext) throws PropertyException {
        return writeDocumentPart(dp, writeContext, false);
    }

    /**
     * Gets the facets available on this document (from the type and the instance facets).
     *
     * @return the facets
     * @since 5.4.2
     */
    Set<String> getAllFacets();

    /**
     * Gets the facets defined on this document instance. The type facets are not returned.
     *
     * @return the facets
     * @since 5.4.2
     */
    String[] getFacets();

    /**
     * Checks whether this document has a given facet, either from its type or added on the instance.
     *
     * @param facet the facet name
     * @return {@code true} if the document has the facet
     * @since 5.4.2
     */
    boolean hasFacet(String facet);

    /**
     * Adds a facet to this document.
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
     * Removes a facet from this document.
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
     * Sets a lock on this document.
     *
     * @param lock the lock to set
     * @return {@code null} if locking succeeded, or the existing lock if locking failed
     */
    Lock setLock(Lock lock);

    /**
     * Removes a lock from this document.
     *
     * @param the owner to check, or {@code null} for no check
     * @return {@code null} if there was no lock or if removal succeeded, or a lock if it blocks removal due to owner
     *         mismatch
     */
    Lock removeLock(String owner);

    /**
     * Gets the lock if one set on this document.
     *
     * @return the lock, or {@code null} if no lock is set
     */
    Lock getLock();

    /**
     * Gets a child document given its name.
     * <p>
     * Throws {@link DocumentNotFoundException} if the document could not be found.
     *
     * @param name the name of the child to retrieve
     * @return the child if exists
     * @throws DocumentNotFoundException if the child does not exist
     */
    Document getChild(String name);

    /**
     * Gets the children of the document.
     *
     * @return the children
     */
    List<Document> getChildren();

    /**
     * Gets a list of the children ids.
     *
     * @return a list of children ids.
     * @since 1.4.1
     */
    List<String> getChildrenIds();

    /**
     * Checks whether this document has a child of the given name.
     *
     * @param name the name of the child to check
     * @return {@code true} if the child exists, {@code false} otherwise
     */
    boolean hasChild(String name);

    /**
     * Tests if the document has any children.
     *
     * @return {@code true} if the document has children, {@code false} otherwise
     */
    boolean hasChildren();

    /**
     * Creates a new child document of the given type.
     *
     * @param name the name of the new child to create
     * @param typeName the type of the child to create
     * @return the newly created document
     */
    Document addChild(String name, String typeName);

    /**
     * Orders the given source child before the destination child.
     * <p>
     * Both source and destination must be names that point to child documents of this document. The source document
     * will be placed before the destination one. If destination is {@code null}, the source document will be appended
     * at the end of the children list.
     *
     * @param src the document to move
     * @param dest the document before which to place the source document
     */
    void orderBefore(String src, String dest);

    /**
     * Creates a new version.
     *
     * @param label the version label
     * @param checkinComment the checkin comment
     * @return the created version
     */
    Document checkIn(String label, String checkinComment);

    void checkOut();

    /**
     * Gets the list of version ids for this document.
     *
     * @return the list of version ids
     * @since 1.4.1
     */
    List<String> getVersionsIds();

    /**
     * Gets the versions for this document.
     *
     * @return the versions of the document, or an empty list if there are no versions
     */
    List<Document> getVersions();

    /**
     * Gets the last version of this document.
     * <p>
     * Returns {@code null} if there is no version at all.
     *
     * @return the last version, or {@code null} if there is no version
     */
    Document getLastVersion();

    /**
     * Gets the source for this document.
     * <p>
     * For a version, it's the working copy.
     * <p>
     * For a proxy, it's the version the proxy points to.
     *
     * @return the source document
     */
    Document getSourceDocument();

    /**
     * Replaces this document's content with the version specified.
     *
     * @param version the version to replace with
     */
    void restore(Document version);

    /**
     * Gets a version of this document, given its label.
     *
     * @param label the version label
     * @return the version
     */
    Document getVersion(String label);

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
    Document getBaseVersion();

    /**
     * Checks whether this document is checked out.
     *
     * @return {@code true} if the document is checked out, or {@code false} otherwise
     */
    boolean isCheckedOut();

    /**
     * Gets the version creation date of this document if it's a version or a proxy.
     *
     * @return the version creation date, or {@code null} if it's not a version or a proxy
     */
    Calendar getVersionCreationDate();

    /**
     * Gets the version check in comment of this document if it's a version or a proxy.
     *
     * @return the check in comment, or {@code null} if it's not a version or a proxy
     */
    String getCheckinComment();

    /**
     * Gets the version series id.
     *
     * @return the version series id
     */
    String getVersionSeriesId();

    /**
     * Gets the version label.
     *
     * @return the version label
     */
    String getVersionLabel();

    /**
     * Checks whether this document is the latest version.
     *
     * @return {@code true} if this is the latest version, or {@code false} otherwise
     */
    boolean isLatestVersion();

    /**
     * Checks whether this document is a major version.
     *
     * @return {@code true} if this is a major version, or {@code false} otherwise
     */
    boolean isMajorVersion();

    /**
     * Checks whether this document is the latest major version.
     *
     * @return {@code true} if this is the latest major version, or {@code false} otherwise
     */
    boolean isLatestMajorVersion();

    /**
     * Checks if there is a checked out working copy for the version series of this document.
     *
     * @return {@code true} if there is a checked out working copy
     */
    boolean isVersionSeriesCheckedOut();

    /**
     * Gets the working copy for this document.
     *
     * @return the working copy
     */
    Document getWorkingCopy();

    /**
     * Gets the document (version or live document) to which this proxy points.
     */
    Document getTargetDocument();

    /**
     * Sets the document (version or live document) to which this proxy points.
     */
    void setTargetDocument(Document target);

}
