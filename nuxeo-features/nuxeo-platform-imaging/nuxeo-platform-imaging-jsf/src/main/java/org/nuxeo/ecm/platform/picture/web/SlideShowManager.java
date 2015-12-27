/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.picture.web;

import javax.faces.event.ActionEvent;

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
     */
    void resetIndex();

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
