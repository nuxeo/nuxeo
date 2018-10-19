/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.core.lifecycle.impl;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.LifeCycleException;
import org.nuxeo.ecm.core.lifecycle.LifeCycle;
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
 * @author Julien Anguenot
 * @author Florent Guillaume
 */
public class LifeCycleServiceImpl extends DefaultComponent implements LifeCycleService {

    public static final ComponentName NAME = new ComponentName("org.nuxeo.ecm.core.lifecycle.LifeCycleService");

    private static final Log log = LogFactory.getLog(LifeCycleServiceImpl.class);

    protected LifeCycleRegistry lifeCycles = new LifeCycleRegistry();

    protected LifeCycleTypeRegistry lifeCycleTypes = new LifeCycleTypeRegistry();

    public LifeCycleServiceImpl() {
    }

    @Override
    public LifeCycle getLifeCycleByName(String name) {
        return lifeCycles.getLifeCycle(name);
    }

    @Override
    public LifeCycle getLifeCycleFor(Document doc) {
        String lifeCycleName = getLifeCycleNameFor(doc.getType().getName());
        return getLifeCycleByName(lifeCycleName);
    }

    @Override
    public String getLifeCycleNameFor(String typeName) {
        return lifeCycleTypes.getLifeCycleNameForType(typeName);
    }

    @Override
    public Collection<LifeCycle> getLifeCycles() {
        return lifeCycles.getLifeCycles();
    }

    @Override
    public Collection<String> getTypesFor(String lifeCycleName) {
        return lifeCycleTypes.getTypesFor(lifeCycleName);
    }

    @Override
    public Map<String, String> getTypesMapping() {
        return lifeCycleTypes.getTypesMapping();
    }

    @Override
    public void initialize(Document doc) throws LifeCycleException {
        initialize(doc, null);
    }

    @Override
    public void initialize(Document doc, String initialStateName) throws LifeCycleException {
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
                    throw new LifeCycleException(String.format("State '%s' is not a valid state " + "for lifecycle %s",
                            initialStateName, lifeCycleName));
                }
            }
        }
        doc.setCurrentLifeCycleState(initialStateName);
        doc.setLifeCyclePolicy(lifeCycleName);
    }

    @Override
    public void followTransition(Document doc, String transitionName) throws LifeCycleException {
        String lifeCycleState = doc.getLifeCycleState();
        LifeCycle lifeCycle = getLifeCycleFor(doc);
        if (lifeCycle != null && lifeCycle.getAllowedStateTransitionsFrom(lifeCycleState).contains(transitionName)) {
            String destinationStateName = lifeCycle.getTransitionByName(transitionName).getDestinationStateName();
            doc.setCurrentLifeCycleState(destinationStateName);
        } else {
            throw new LifeCycleException(
                    "Not allowed to follow transition <" + transitionName + "> from state <" + lifeCycleState + '>');
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
    public void registerExtension(Extension extension) {
        Object[] contributions = extension.getContributions();
        if (contributions != null) {
            String point = extension.getExtensionPoint();
            if (point.equals("lifecycle")) {
                for (Object contribution : contributions) {
                    LifeCycleDescriptor desc = (LifeCycleDescriptor) contribution;
                    lifeCycles.addContribution(desc);
                }
            } else if (point.equals("lifecyclemanager")) {
                log.warn("Ignoring deprecated lifecyclemanager extension point");
            } else if (point.equals("types")) {
                for (Object mapping : contributions) {
                    LifeCycleTypesDescriptor desc = (LifeCycleTypesDescriptor) mapping;
                    lifeCycleTypes.addContribution(desc);
                }
            }
        }
    }

    /**
     * Unregisters an extension.
     */
    @Override
    public void unregisterExtension(Extension extension) {
        super.unregisterExtension(extension);
        Object[] contributions = extension.getContributions();
        if (contributions != null) {
            String point = extension.getExtensionPoint();
            if (point.equals("lifecycle")) {
                for (Object lifeCycle : contributions) {
                    LifeCycleDescriptor lifeCycleDescriptor = (LifeCycleDescriptor) lifeCycle;
                    lifeCycles.removeContribution(lifeCycleDescriptor);
                }
            } else if (point.equals("types")) {
                for (Object contrib : contributions) {
                    LifeCycleTypesDescriptor desc = (LifeCycleTypesDescriptor) contrib;
                    lifeCycleTypes.removeContribution(desc);
                }

            }
        }
    }

    @Override
    public List<String> getNonRecursiveTransitionForDocType(String docTypeName) {
        return lifeCycleTypes.getNonRecursiveTransitionForDocType(docTypeName);
    }

}
