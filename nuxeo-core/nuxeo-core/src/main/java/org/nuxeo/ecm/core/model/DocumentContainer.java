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

import java.util.Iterator;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentException;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface DocumentContainer {

    /**
     * Resolves a document given its relative path to the current document.
     * <p>
     * If the path is absolute then it will be transformed in a relative path
     * (i.e. the leading '/' removed).
     *
     * @param relPath the path relative to this document
     * @return the resolved document
     * @throws DocumentException
     */
    Document resolvePath(String relPath) throws DocumentException;

    /**
     * Gets a child document given its name.
     * <p>
     * Throws an exception if the document could not be found.
     * <p>
     * If the supplied name is null, returns the default child of the document
     * if any, otherwise raises an exception.
     *
     * @param name the name of the child to retrieve
     * @return the named child if exists, raises a NoSuchDocumentException
     *         otherwise
     * @throws UnsupportedOperationException if this is not a folder document
     * @throws DocumentException
     */
    Document getChild(String name) throws DocumentException;

    /**
     * Gets an iterator over the children of the document.
     * <p>
     * This operation silently ignores non-folder documents: if the document is
     * not a folder then it returns an empty iterator.
     *
     * @return the children iterator
     * @throws DocumentException
     */
    Iterator<Document> getChildren() throws DocumentException;

    DocumentIterator getChildren(int start) throws DocumentException;

    /**
     * Gets a list of the children ids.
     *
     * @return a list of children ids.
     * @since 1.4.1
     */
    List<String> getChildrenIds() throws DocumentException;

    /**
     * Tests if the document has the named child.
     * <p>
     * This operation silently ignores non-folder documents:
     * If the document is not a folder then return false.
     *
     * @param name the name of the child to test
     * @return true if the named child exists, false otherwise
     * @throws DocumentException
     */
    boolean hasChild(String name) throws DocumentException;

    /**
     * Tests if the document has any children.
     * <p>
     * This operation silently ignores non-folder documents:
     * If the document is not a folder then returns false.
     *
     * @return true if document has children, false otherwise
     * @throws DocumentException
     */
    boolean hasChildren() throws DocumentException;

    /**
     * Creates a new child document given its typename.
     * <p>
     * This operation throws an error if the current document is not a folder.
     *
     * @param name the name of the new child to create
     * @param typeName the type of the child to create
     * @return the newly created document
     * @throws DocumentException
     */
    Document addChild(String name, String typeName) throws DocumentException;


    /**
     * Remove the child having the given name
     * <p>
     * If this is not a folder does nothing.
     * <p>
     * If this is a folder and no child exists with the given name then throws an exception
     *
     * @param name the child name
     * @throws DocumentException
     */
    void removeChild(String name) throws DocumentException;

    /**
     * Order the given source child before the destination child.
     * Both source and destinatin must be names that point to child documents
     * of the current document. The source document will be placed before the destination one.
     * If destination is null the source document will be appended at the end of the children list
     * @param src the document to move
     * @param dest the document before which to place the source document
     * @throws DocumentException if this document is not an orderable folder or any other exception occurs
     */
    void orderBefore(String src, String dest) throws DocumentException;

}
