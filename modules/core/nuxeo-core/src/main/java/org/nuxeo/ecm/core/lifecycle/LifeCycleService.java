/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Julien Anguenot
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.lifecycle;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.LifeCycleException;
import org.nuxeo.ecm.core.lifecycle.event.BulkLifeCycleChangeListener;
import org.nuxeo.ecm.core.model.Document;

/**
 * Life cycle service.
 *
 * @see org.nuxeo.ecm.core.lifecycle.impl.LifeCycleServiceImpl
 * @author Julien Anguenot
 * @author Florent Guillaume
 */
public interface LifeCycleService {

    /**
     * Initializes the life cycle for the given document.
     * <p>
     * Document state will be set to the life cycle initial state.
     *
     * @param doc the document instance
     */
    void initialize(Document doc) throws LifeCycleException;

    /**
     * Initializes the life cycle for the given document.
     * <p>
     * Tries to set given state on document, if it's a valid initial state.
     *
     * @param doc the document instance
     * @param initialStateName the initial state name
     */
    void initialize(Document doc, String initialStateName) throws LifeCycleException;

    /**
     * Follows a given transition.
     *
     * @param doc the Document instance
     * @param transitionName the transition name
     */
    void followTransition(Document doc, String transitionName) throws LifeCycleException;

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
     * Returns a list of transition for which, when a it is followed, it should no recurse in its children. The
     * {@link BulkLifeCycleChangeListener} will listen to the transition taken event and call a follow transition on the
     * children of the document if the document is folderish. It check this list of transition to find out if it should
     * recurse.
     *
     * @see BulkLifeCycleChangeListener
     * @param docTypeName The doc type
     * @return a list of transition name
     */
    List<String> getNonRecursiveTransitionForDocType(String docTypeName);

    /**
     * Returns the mapping from types to life cycle names.
     *
     * @return a mapping from types to life cycle names
     */
    Map<String, String> getTypesMapping();

    /**
     * Returns the life cycle a given document follows.
     *
     * @param doc the document instance
     * @return the life cycle instance
     */
    LifeCycle getLifeCycleFor(Document doc);

    /**
     * Sets the current state to the initial state as defined by the associated lifecycle.
     */
    void reinitLifeCycle(Document doc) throws LifeCycleException;

}
