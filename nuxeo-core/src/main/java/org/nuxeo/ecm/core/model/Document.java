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

import java.io.Serializable;
import java.util.Calendar;
import java.util.Collection;

import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.lifecycle.LifeCycleException;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.versioning.VersionableDocument;

/**
 * A document object.
 * <p>
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
// TODO: how to handle child operations on non folder documents? throwing an
// {@link UnsupportedOperationException} exception or returning null or treating
// the document as an empty folder? as is the document
public interface Document extends DocumentContainer, PropertyContainer,
        VersionableDocument, Lockable {

    /**
     * Gets the session that owns this document.
     *
     * @return the session
     */
    Session getSession();

    /**
     * Gets the name of this document.
     *
     * @return the document name. Cannot be null
     * @throws DocumentException
     *             if any exception occurred
     */
    String getName() throws DocumentException;

    /**
     * Gets this document's UUID.
     *
     * @return the document UUID (cannot be null)
     * @throws DocumentException
     */
    String getUUID() throws DocumentException;

    /**
     * Gets the parent document or null if this is the root document.
     *
     * @return the parent document or null if this is the root document
     * @throws DocumentException
     */
    Document getParent() throws DocumentException;

    /**
     * Gets this document's type.
     *
     * @return the document type (cannot be null)
     */
    DocumentType getType();

    /**
     * Gets the path of this document.
     *
     * @return the path
     * @throws DocumentException
     */
    String getPath() throws DocumentException;

    /**
     * Gets the last modification time on this document.
     *
     * @return last modification time
     * @throws DocumentException
     */
    Calendar getLastModified() throws DocumentException;

    /**
     * Sets the dirty flag on this document.
     *
     * @param value true to set the dirty flag, false otherwise
     * @throws DocumentException
     */
    void setDirty(boolean value) throws DocumentException;


    /**
     * Checks if the dirty flag is set on this document.
     *
     * @return true if the dirty flag is set, false otherwise
     * @throws DocumentException
     */
    boolean isDirty() throws DocumentException;


    /**
     * Tests whether this document represent a folder or a leaf document.
     *
     * @return true if the document is a folder document, false otherwise
     */
    boolean isFolder();

    /**
     * Removes this document and all its children, if any.
     *
     * @throws DocumentException
     *             if an error occurs
     */
    void remove() throws DocumentException;

    /**
     * Saves any modification done on this document or its children.
     * <p>
     * For some implementations this may do nothing if they are commiting
     * modifications as they are done by calling the corresponding method.
     *
     * @throws DocumentException
     *             if an error occurs
     */
    void save() throws DocumentException;

    /**
     * Returns the life cycle of the document.
     *
     * @see org.nuxeo.ecm.core.lifecycle
     *
     * @return the life cycle as a string
     * @throws LifeCycleException
     */
    String getCurrentLifeCycleState() throws LifeCycleException;

    /**
     * Sets the lifecycle state of the document.
     *
     * @param state the state
     * @throws LifeCycleException
     */
    void setCurrentLifeCycleState(String state) throws LifeCycleException;

    /**
     * Returns the life cycle policy of this document.
     *
     * @return the life cycle policy name of this document as a string
     * @throws LifeCycleException
     */
    String getLifeCyclePolicy() throws LifeCycleException;

    /**
     * Sets the life cycle policy of this document.
     *
     * @param policy the policy
     * @throws LifeCycleException
     */
    void setLifeCyclePolicy(String policy) throws LifeCycleException;

    /**
     * Follows a given life cycle transition.
     * <p>
     * This will update the current life cycle of the document.
     *
     * @param transition the name of the transition to follow
     * @return a boolean representing the status if the operation
     * @throws LifeCycleException
     */
    boolean followTransition(String transition) throws LifeCycleException;

    /**
     * Returns the allowed state transitions for this document.
     *
     * @return a collection of state transitions as string
     */
    Collection<String> getAllowedStateTransitions() throws LifeCycleException;

    /**
     * Checks whether or not this doc is a proxy document.
     *
     * @return true if it's a proxy false otherwise
     */
    boolean isProxy();

    /**
     * Returns the repository in which the document lives.
     *
     * @return a Repository instance.
     */
    Repository getRepository();

    /**
     * Set a system property which is a property of by the built-in node type
     * ECM_SYSTEM_ANY.
     *
     * @param <T>
     * @param name
     * @param value
     * @throws DocumentException
     */
    <T extends Serializable> void setSystemProp(String name, T value)
            throws DocumentException;

    /**
     * Get system property of the specified type.
     *
     * @param <T>
     * @param name
     * @param type
     * @return
     * @throws DocumentException
     */
    <T extends Serializable> T getSystemProp(String name, Class<T> type)
            throws DocumentException;


    /**
     * Load document part properties from storage and fill them inside the given document part
     * @param dp
     * @throws Exception
     */
    void readDocumentPart(DocumentPart dp) throws Exception;

    /**
     * Read modifications in the given document part and write them on storage
     * @param dp
     * @throws Exception
     */
    void writeDocumentPart(DocumentPart dp) throws Exception;

}
