/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Julien Anguenot
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.lifecycle;

import java.util.Collection;
import java.util.Map;

import org.nuxeo.ecm.core.model.Document;

/**
 * Life cycle service.
 *
 * @see org.nuxeo.ecm.core.lifecycle.impl.LifeCycleServiceImpl
 *
 * @author Julien Anguenot
 * @author Florent Guillaume
 */
public interface LifeCycleService {

    /**
     * Initializes the life cycle for the given document.
     *
     * @param doc the document instance
     * @throws LifeCycleException
     */
    void initialize(Document doc) throws LifeCycleException;

    /**
     * Follows a given transition.
     *
     * @param doc the Document instance
     * @param transitionName the transition name
     * @throws LifeCycleException
     */
    void followTransition(Document doc, String transitionName)
            throws LifeCycleException;

    /**
     * Returns the state of a given document.
     *
     * @param doc a document instance
     * @return a string holding the life cycle state of the
     * @throws LifeCycleException
     */
    String getCurrentLifeCycleState(Document doc) throws LifeCycleException;

    /**
     * Sets the state of the given document.
     *
     * @param doc a document instance
     * @param stateName the name of state as a string
     * @throws LifeCycleException
     */
    void setCurrentLifeCycleState(Document doc, String stateName)
            throws LifeCycleException;

    /**
     * Returns the life cycle policy for the given document.
     *
     * @param doc the document instance
     * @return the life cycle policy name
     * @throws LifeCycleException TODO
     */
    String getLifeCyclePolicy(Document doc) throws LifeCycleException;

    /**
     * Sets the life cycle policy for this document.
     *
     * @param doc
     *            the document instance
     * @param policy
     *            the life cycle policy name
     * @throws LifeCycleException
     */
    void setLifeCycelPolicy(Document doc, String policy)
            throws LifeCycleException;

    /**
     * Returns the life cycle manager.
     *
     * @return a LifeCycleManager
     */
    LifeCycleManager getLifeCycleManager();

    /**
     * Returns a life cycle given its name.
     *
     * @param name the life cycle's name
     * @return a life cycle descriptor instance or null if not found.
     */
    LifeCycle getLifeCycleByName(String name);

    /**
     * Returns all the registered life cycles.
     *
     * @return a collection of lifecycle descriptors
     */
    Collection<LifeCycle> getLifeCycles();

    /**
     * Returns the types which follow a given life cycle.
     *
     * @param lifeCycleName a string holding the name of the life cycle
     * @return a collection of type names as strings
     */
    Collection<String> getTypesFor(String lifeCycleName);

    /**
     * Returns the lifecycle name that the given type follows.
     *
     * @param typeName the type's name
     * @return the life cycle name
     */
    String getLifeCycleNameFor(String typeName);

    /**
     * Returns the mapping from types to life cycle names.
     *
     * @return a mapping from types to life cycle names
     */
    Map<String, String> getTypesMapping();

    /**
     * Sets the mapping for types to life cycle names.
     *
     * @param typesMapping a mapping from types to life cycle names
     */
    void setTypesMapping(Map<String, String> typesMapping);

    /**
     * Sets the mapping from life cycle names to life cycle instances.
     *
     * @param lifeCycles
     *            a mapping from life cycle names to life cycle instances
     */
    void setLifeCycles(Map<String, LifeCycle> lifeCycles);

    /**
     * Returns the life cycle a given document follows.
     *
     * @param doc the document instance
     * @return the life cycle instance
     */
    LifeCycle getLifeCycleFor(Document doc);

    /**
     * Returns the life cycle manager instance in use for a given document.
     *
     * @param doc the document instance
     * @return the life cycle manager instance
     */
    LifeCycleManager getLifeCycleManagerFor(Document doc);

    /**
     * Sets the current state to the initial state as defined by the associated lifecycle.
     *
     * @param doc
     * @throws LifeCycleException
     */
    void reinitLifeCycle(Document doc) throws LifeCycleException;

}
