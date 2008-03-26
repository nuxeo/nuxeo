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
 * $Id: LifeCycleManager.java 19491 2007-05-27 13:51:18Z sfermigier $
 */

package org.nuxeo.ecm.core.lifecycle;

import org.nuxeo.ecm.core.model.Document;

/**
 * Life cycle manager.
 * <p>
 * Responsible of the life cycle properties storage abstraction.
 *
 * @see org.nuxeo.ecm.core.repository.jcr.JCRLifeCycleManager
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public interface LifeCycleManager {

    /**
     * Returns the name of the manager.
     *
     * @return the name of the manager as a string.
     */
    String getName();

    /**
     * Sets the name of the life cycle manager.
     *
     * @param name
     *            of the life cycle manager
     */
    void setName(String name);

    /**
     * Returns the state of a given document.
     *
     * @param doc
     *            a document instance
     * @return string holding the life cycle state of the document
     */
    String getState(Document doc) throws LifeCycleException;

    /**
     * Sets the state of the given document.
     *
     * @param doc
     *            a document instance
     * @param stateName
     *            the name of state as a string
     * @throws LifeCycleException
     */
    void setState(Document doc, String stateName)
            throws LifeCycleException;

    /**
     * Returns the life cycle policy.
     * <p>
     * The life cycle policy is the life cycle name itself.
     *
     * @param doc
     *            a document instance
     * @return the life cycle policy
     * @throws LifeCycleException
     *             TODO
     */
    String getPolicy(Document doc) throws LifeCycleException;

    /**
     * Sets the life cycle policy.
     *
     * <p/>
     * The life cycle policy is the life cycle name itself.
     *
     * @param doc the document instance
     * @param policy
     *            the life cycle policy
     * @throws LifeCycleException
     */
    void setPolicy(Document doc, String policy) throws LifeCycleException;

}
