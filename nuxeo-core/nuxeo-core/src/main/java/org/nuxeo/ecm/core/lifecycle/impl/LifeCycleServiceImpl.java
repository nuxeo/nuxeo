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

package org.nuxeo.ecm.core.lifecycle.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.lifecycle.LifeCycle;
import org.nuxeo.ecm.core.lifecycle.LifeCycleException;
import org.nuxeo.ecm.core.lifecycle.LifeCycleService;
import org.nuxeo.ecm.core.lifecycle.LifeCycleState;
import org.nuxeo.ecm.core.lifecycle.extensions.LifeCycleDescriptor;
import org.nuxeo.ecm.core.lifecycle.extensions.LifeCycleTypesDescriptor;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Extension;

/**
 * Life cycle service implementation.
 *
 * @see org.nuxeo.ecm.core.lifecycle.LifeCycleService
 *
 * @author Julien Anguenot
 * @author Florent Guillaume
 */
public class LifeCycleServiceImpl extends DefaultComponent implements
        LifeCycleService {

    public static final ComponentName NAME = new ComponentName(
            "org.nuxeo.ecm.core.lifecycle.LifeCycleService");

    private static final Log log = LogFactory.getLog(LifeCycleServiceImpl.class);

    /** Lifecycle name -> life cycle descriptor instance. */
    private final Map<String, LifeCycle> lifeCycles;

    /** Type name -> life cycle name. */
    private final Map<String, String> typesMapping;

    /**
     * a Mapping from doc type => list of transition that should no recurse.
     */
    protected final Map<String, List<String>> docTypeToNonRecursiveTransition = new HashMap<String, List<String>>();

    public LifeCycleServiceImpl() {
        lifeCycles = new HashMap<String, LifeCycle>();
        typesMapping = new HashMap<String, String>();
    }

    @Override
    public LifeCycle getLifeCycleByName(String name) {
        return lifeCycles.get(name);
    }

    @Override
    public LifeCycle getLifeCycleFor(Document doc) {
        String lifeCycleName = getLifeCycleNameFor(doc.getType().getName());
        return getLifeCycleByName(lifeCycleName);
    }

    @Override
    public String getLifeCycleNameFor(String typeName) {
        return typesMapping.get(typeName);
    }

    @Override
    public Collection<LifeCycle> getLifeCycles() {
        return lifeCycles.values();
    }

    @Override
    public Collection<String> getTypesFor(String lifeCycleName) {
        Collection<String> types = new ArrayList<String>();
        for (String typeName : typesMapping.keySet()) {
            if (typesMapping.get(typeName).equals(lifeCycleName)) {
                types.add(typeName);
            }
        }
        return types;
    }

    @Override
    public Map<String, String> getTypesMapping() {
        return typesMapping;
    }

    @Override
    public void initialize(Document doc) throws LifeCycleException {
        initialize(doc, null);
    }

    @Override
    public void initialize(Document doc, String initialStateName)
            throws LifeCycleException {
        String lifeCycleName;
        LifeCycle documentLifeCycle = getLifeCycleFor(doc);
        if (documentLifeCycle == null) {
            lifeCycleName = "undefined";
            if (initialStateName == null) {
                initialStateName = "undefined";
            }
        } else {
            lifeCycleName = documentLifeCycle.getName();
            // set initial life cycle state
            if (initialStateName == null) {
                initialStateName = documentLifeCycle.getDefaultInitialStateName();
            } else {
                // check it's a valid state
                LifeCycleState state = documentLifeCycle.getStateByName(initialStateName);
                if (state == null) {
                    throw new LifeCycleException(String.format(
                            "State '%s' is not a valid state "
                                    + "for lifecycle %s", initialStateName,
                            lifeCycleName));
                } else if (!documentLifeCycle.getInitialStateNames().contains(
                        initialStateName)) {
                    log.warn(String.format(
                            "State '%s' is not a valid initial state "
                                    + "for lifecycle %s", initialStateName,
                            lifeCycleName));
                }
            }
        }
        doc.setCurrentLifeCycleState(initialStateName);
        doc.setLifeCyclePolicy(lifeCycleName);
    }

    @Override
    public void followTransition(Document doc, String transitionName)
            throws LifeCycleException {
        String lifeCycleState = doc.getLifeCycleState();
        LifeCycle lifeCycle = getLifeCycleFor(doc);
        if (lifeCycle.getAllowedStateTransitionsFrom(lifeCycleState).contains(
                transitionName)) {
            String destinationStateName = lifeCycle.getTransitionByName(
                    transitionName).getDestinationStateName();
            doc.setCurrentLifeCycleState(destinationStateName);
        } else {
            throw new LifeCycleException("Not allowed to follow transition <"
                    + transitionName + "> from state <" + lifeCycleState
                    + '>');
        }
    }

    @Override
    public void reinitLifeCycle(Document doc) throws LifeCycleException {
        LifeCycle documentLifeCycle = getLifeCycleFor(doc);
        if (documentLifeCycle == null) {
            log.debug("No lifecycle policy for this document. Nothing to do !");
            return;
        }
        doc.setCurrentLifeCycleState(documentLifeCycle.getDefaultInitialStateName());
    }

    /**
     * Register extensions.
     */
    @Override
    public void registerExtension(Extension extension) throws Exception {
        Object[] contributions = extension.getContributions();
        if (contributions != null) {
            String point = extension.getExtensionPoint();
            if (point.equals("lifecycle")) {
                for (Object contribution : contributions) {
                    LifeCycleDescriptor desc = (LifeCycleDescriptor) contribution;
                    log.info("Registering lifecycle: " + desc.getName());
                    lifeCycles.put(desc.getName(), desc.getLifeCycle());
                }
            } else if (point.equals("lifecyclemanager")) {
                log.warn("Ignoring deprecated lifecyclemanager extension point");
            } else if (point.equals("types")) {
                for (Object mapping : contributions) {
                    LifeCycleTypesDescriptor desc = (LifeCycleTypesDescriptor) mapping;
                    log.info("Registering lifecycle types mapping: "
                            + desc.getDocumentType() + "-"
                            + desc.getLifeCycleName());
                    typesMapping.put(desc.getDocumentType(),
                            desc.getLifeCycleName());
                    String transitionArray = desc.getNoRecursionForTransitions();
                    List<String> transitions = new ArrayList<String>();
                    if (transitionArray != null && !transitionArray.isEmpty()) {
                        transitions = Arrays.asList(desc.getNoRecursionForTransitions().split(
                                ","));
                    }
                    docTypeToNonRecursiveTransition.put(desc.getDocumentType(),
                            transitions);
                }
            }
        }
    }

    /**
     * Unregisters an extension.
     */
    @Override
    public void unregisterExtension(Extension extension) throws Exception {
        super.unregisterExtension(extension);
        Object[] contributions = extension.getContributions();
        if (contributions != null) {
            String point = extension.getExtensionPoint();
            if (point.equals("lifecycle")) {
                for (Object lifeCycle : contributions) {
                    LifeCycleDescriptor lifeCycleDescriptor = (LifeCycleDescriptor) lifeCycle;
                    log.debug("Unregistering lifecycle: "
                            + lifeCycleDescriptor.getName());
                    lifeCycles.remove(lifeCycleDescriptor.getName());
                }
            } else if (point.equals("types")) {
                for (Object contrib : contributions) {
                    LifeCycleTypesDescriptor desc = (LifeCycleTypesDescriptor) contrib;
                    typesMapping.remove(desc.getDocumentType());
                }

            }
        }
    }

    @Override
    public List<String> getNonRecursiveTransitionForDocType(String docTypeName) {
        return docTypeToNonRecursiveTransition.get(docTypeName);
    }

}
