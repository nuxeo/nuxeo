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
 * $Id: LifeCycleServiceImpl.java 28980 2008-01-11 22:55:02Z sfermigier $
 */

package org.nuxeo.ecm.core.lifecycle.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.lifecycle.LifeCycle;
import org.nuxeo.ecm.core.lifecycle.LifeCycleException;
import org.nuxeo.ecm.core.lifecycle.LifeCycleManager;
import org.nuxeo.ecm.core.lifecycle.LifeCycleService;
import org.nuxeo.ecm.core.lifecycle.extensions.LifeCycleDescriptor;
import org.nuxeo.ecm.core.lifecycle.extensions.LifeCycleManagerDescriptor;
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
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class LifeCycleServiceImpl extends DefaultComponent implements
        LifeCycleService {

    public static final ComponentName NAME = new ComponentName(
            "org.nuxeo.ecm.core.lifecycle.LifeCycleService");

    private static final Log log = LogFactory.getLog(LifeCycleServiceImpl.class);

    /** Lifecycle manager name -> life cycle manager instance. */
    private Map<String, LifeCycleManager> lifeCycleManagers;

    /** Lifecycle name -> life cycle descriptor instance. */
    private Map<String, LifeCycle> lifeCycles;

    /** Type name -> life cycle name. */
    private Map<String, String> typesMapping;

    public LifeCycleServiceImpl() {
        lifeCycleManagers = new HashMap<String, LifeCycleManager>();
        lifeCycles = new HashMap<String, LifeCycle>();
        typesMapping = new HashMap<String, String>();
    }

    public LifeCycle getLifeCycleByName(String name) {
        return lifeCycles.get(name);
    }

    public LifeCycle getLifeCycleFor(Document doc) {
        String lifeCycleName = getLifeCycleNameFor(doc.getType().getName());
        return getLifeCycleByName(lifeCycleName);
    }

    public LifeCycleManager getLifeCycleManagerByName(String name) {
        return lifeCycleManagers.get(name);
    }

    public LifeCycleManager getLifeCycleManagerFor(Document doc) {

        LifeCycleManager lifeCycleManager;

        // Try if a life cycle is associated to this given document.
        LifeCycle lifeCycle = getLifeCycleFor(doc);
        if (lifeCycle != null) {
            lifeCycleManager = getLifeCycleManagerByName(lifeCycle
                    .getLifeCycleManagerName());
        } else {
            lifeCycleManager = null;
            log.info("No life cycle associate to document type : "
                    + doc.getType());
        }

        return lifeCycleManager;
    }

    public Collection<LifeCycleManager> getLifeCycleManagers() {
        return lifeCycleManagers.values();
    }

    public String getLifeCycleNameFor(String typeName) {
        return typesMapping.get(typeName);
    }

    public Collection<LifeCycle> getLifeCycles() {
        return lifeCycles.values();
    }

    public String getCurrentLifeCycleState(Document doc)
            throws LifeCycleException {

        String currentLifeCycleState;
        LifeCycleManager lifeCycleManager = getLifeCycleManagerFor(doc);

        if (lifeCycleManager != null) {
            currentLifeCycleState = lifeCycleManager.getState(doc);
        } else {
            currentLifeCycleState = null;
            log.info("No life cycle manager found for document type = "
                    + doc.getType() + " check out your configuration.");
        }

        return currentLifeCycleState;
    }

    public Collection<String> getTypesFor(String lifeCycleName) {
        Collection<String> types = new ArrayList<String>();
        for (String typeName : typesMapping.keySet()) {
            if (typesMapping.get(typeName).equals(lifeCycleName)) {
                types.add(typeName);
            }
        }
        return types;
    }

    public Map<String, String> getTypesMapping() {
        return typesMapping;
    }

    public void setTypesMapping(Map<String, String> typesMapping) {
        this.typesMapping = typesMapping;
    }

    public void setLifeCycles(Map<String, LifeCycle> lifeCycles) {
        this.lifeCycles = lifeCycles;
    }

    public void setLifeCycleManagers(Map<String, LifeCycleManager> lifeCycleManagers) {
        this.lifeCycleManagers = lifeCycleManagers;
    }

    public void initialize(Document doc) throws LifeCycleException {
        LifeCycle documentLifeCycle = getLifeCycleFor(doc);
        if (documentLifeCycle == null) {
            log.info("Not life cycle policy for this document. Nothing to do !");
            return;
        }

        // Set current life cycle state
        String initialStateName = documentLifeCycle.getInitialStateName();
        try {
            getLifeCycleManagerFor(doc).setState(doc, initialStateName);
        } catch (Exception e) {
            throw new LifeCycleException("Failed to set initial state", e);
        }

        // Set the life cycle policy
        String policy = documentLifeCycle.getName();
        try {
            getLifeCycleManagerFor(doc).setPolicy(doc, policy);
        } catch (Exception e) {
            throw new LifeCycleException("Failed to set life cycle policy", e);
        }
    }

    public void followTransition(Document doc, String transitionName)
            throws LifeCycleException {
        String currentStateName = getCurrentLifeCycleState(doc);
        LifeCycle lifeCycle = getLifeCycleFor(doc);
        if (lifeCycle.getAllowedStateTransitionsFrom(currentStateName)
                .contains(transitionName)) {
            String destinationStateName = lifeCycle.getTransitionByName(
                    transitionName).getDestinationStateName();
            setCurrentLifeCycleState(doc, destinationStateName);
        } else {
            throw new LifeCycleException("Not allowed to follow transition <"
                    + transitionName + "> from state <" + currentStateName + '>');
        }
    }

    public void reinitLifeCycle(Document doc) throws LifeCycleException {
        LifeCycle documentLifeCycle = getLifeCycleFor(doc);
        if (documentLifeCycle == null) {
            log.debug("No life cycle policy for this document. Nothing to do !");
            return;
        }

        // Set current life cycle state
        String initialStateName = documentLifeCycle.getInitialStateName();
        try {
            getLifeCycleManagerFor(doc).setState(doc, initialStateName);
        } catch (Exception e) {
            throw new LifeCycleException("Failed to set initial state", e);
        }
    }

    /**
     * Register extensions.
     *
     */
    @Override
    public void registerExtension(Extension extension) throws Exception {
        Object[] contributions = extension.getContributions();
        if (contributions != null) {
            if (extension.getExtensionPoint().equals("lifecycle")) {
                for (Object lifeCycle : contributions) {
                    LifeCycleDescriptor lifeCycleDescriptor = (LifeCycleDescriptor) lifeCycle;
                    log.info("Trying to register a life cycle with name="
                            + lifeCycleDescriptor.getName());
                    lifeCycles.put(lifeCycleDescriptor.getName(),
                            lifeCycleDescriptor.getLifeCycle());
                }
            }
            if (extension.getExtensionPoint().equals("lifecyclemanager")) {
                for (Object lifeCycleManager : contributions) {
                    LifeCycleManagerDescriptor desc = (LifeCycleManagerDescriptor) lifeCycleManager;
                    LifeCycleManager manager = (LifeCycleManager) extension.getContext().loadClass(
                                    desc.getClassName()).newInstance();
                    manager.setName(desc.getName());
                    log.info("Trying to register a lifecycle manager implementation with name="
                            + manager.getName());
                    lifeCycleManagers.put(manager.getName(), manager);
                }
            }
            if (extension.getExtensionPoint().equals("types")) {
                for (Object mapping : contributions) {
                    LifeCycleTypesDescriptor desc = (LifeCycleTypesDescriptor) mapping;
                    log.info("Trying to register a life cycle types mapping for life cycle :");
                    typesMapping.putAll(desc.getTypesMapping());
                }
            }
        }
    }

    public void setCurrentLifeCycleState(Document doc, String stateName)
            throws LifeCycleException {

        LifeCycleManager lifeCycleManager = getLifeCycleManagerFor(doc);

        if (lifeCycleManager != null) {
            lifeCycleManager.setState(doc, stateName);
        } else {
            throw new LifeCycleException(
                    "Not life cycle manager found for document type = "
                    + doc.getType() + "Check out your configuration");
        }
    }

    public String getLifeCyclePolicy(Document doc) throws LifeCycleException {
        String lifeCyclePolicy;

        LifeCycleManager lifeCycleManager = getLifeCycleManagerFor(doc);
        if (lifeCycleManager != null) {
            lifeCyclePolicy = lifeCycleManager.getPolicy(doc);
        } else {
            lifeCyclePolicy = null;
            log.info("No life cycle manager found for document type = "
                    + doc.getType() + " check out your configuration.");
        }
        return lifeCyclePolicy;
    }

    public void setLifeCycelPolicy(Document doc, String policy)
            throws LifeCycleException {

        LifeCycleManager lifeCycleManager = getLifeCycleManagerFor(doc);

        if (lifeCycleManager != null) {
            lifeCycleManager.setPolicy(doc, policy);
        } else {
            throw new LifeCycleException(
                    "Not life cycle manager found for document type = "
                    + doc.getType() + "Check out your configuration");
        }
    }

    /**
     * Unregisters an extension.
     *
     */
    @Override
    public void unregisterExtension(Extension extension) throws Exception {
        super.unregisterExtension(extension);
        Object[] contributions = extension.getContributions();
        if (contributions != null) {
            if (extension.getExtensionPoint().equals("lifecycle")) {
                for (Object lifeCycle : contributions) {
                    LifeCycleDescriptor lifeCycleDescriptor = (LifeCycleDescriptor) lifeCycle;
                    log.info("Trying to unregister a life cycle with name="
                            + lifeCycleDescriptor.getName());
                    lifeCycles.remove(lifeCycleDescriptor.getName());
                }
            }
            if (extension.getExtensionPoint().equals("lifecyclemanager")) {
                for (Object lifeCycleManager : contributions) {
                    LifeCycleManagerDescriptor desc = (LifeCycleManagerDescriptor) lifeCycleManager;
                    log.info("Trying to unregister a life cycle manager with name="
                                    + desc.getName());
                    lifeCycleManagers.remove(desc.getName());
                }

            }
            if (extension.getExtensionPoint().equals("types")) {
                for (Object contrib : contributions) {
                    LifeCycleTypesDescriptor desc = (LifeCycleTypesDescriptor) contrib;
                    for (String typeName : desc.getTypesMapping().keySet()) {
                        typesMapping.remove(typeName);
                    }
                }

            }
        }
    }

}
