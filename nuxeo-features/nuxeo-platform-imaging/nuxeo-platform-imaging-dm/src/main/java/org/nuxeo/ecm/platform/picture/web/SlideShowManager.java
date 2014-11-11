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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.picture.web;

import javax.faces.event.ActionEvent;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Provide SlideShow related actions.
 *
 * @author <a href="mailto:ldoguin@nuxeo.com">Laurent Doguin</a>
 * @deprecated since 6.0. See NXP-15370.
 */
@Deprecated
public interface SlideShowManager {

    /**
     * Sets the index.
     *
     * @param idx an Integer holding the current document's index.
     */
    void setIndex(Integer idx);

    /**
     * Gets the index.
     *
     * @return an Integer holding the current document's index.
     */
    Integer getIndex();

    void inputValidation(ActionEvent arg0);

    /**
     * Reinitializes the values at every changes.
     *
     * @throws ClientException
     */
    void resetIndex() throws ClientException;

    /**
     * Increments the index.
     */
    void incIndex();

    /**
     * Decrements the index.
     */
    void decIndex();

    /**
     * Sets the index to 1.
     */
    void firstPic();

    /**
     * Sets the index to the last picture available.
     */
    void lastPic();

    /**
     * Gets the ChildrenSize. The amount of children from the current document.
     *
     * @return an Integer holding childrenSize
     */
    Integer getChildrenSize();

    /**
     * Gets the DocumentModel of a child from the index.
     *
     * @return a DocumentModel holding the child
     */
    DocumentModel getChild();

    /**
     * Sets the DocumentModel of a child from the index.
     *
     * @param child a DocumentModel holding the child
     */
    void setChild(DocumentModel child);

}
