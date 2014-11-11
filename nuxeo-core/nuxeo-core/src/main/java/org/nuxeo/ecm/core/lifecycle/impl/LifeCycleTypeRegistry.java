/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
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
import org.nuxeo.ecm.core.lifecycle.extensions.LifeCycleTypesDescriptor;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 * Registry for lifecycle <-> types association
 *
 * @since 5.6
 */
public class LifeCycleTypeRegistry extends
        ContributionFragmentRegistry<LifeCycleTypesDescriptor> {

    private static final Log log = LogFactory.getLog(LifeCycleTypeRegistry.class);

    /** Type name -> life cycle name. */
    protected Map<String, String> typesMapping = new HashMap<String, String>();

    /**
     * a mapping from doc type -> list of transitions that should not recurse.
     */
    protected Map<String, List<String>> docTypeToNonRecursiveTransition = new HashMap<String, List<String>>();

    @Override
    public String getContributionId(LifeCycleTypesDescriptor contrib) {
        return contrib.getDocumentType();
    }

    @Override
    public void contributionUpdated(String id,
            LifeCycleTypesDescriptor contrib,
            LifeCycleTypesDescriptor newOrigContrib) {
        log.info("Registering lifecycle types mapping: "
                + contrib.getDocumentType() + "-" + contrib.getLifeCycleName());
        typesMapping.put(contrib.getDocumentType(), contrib.getLifeCycleName());
        String transitionArray = contrib.getNoRecursionForTransitions();
        List<String> transitions = new ArrayList<String>();
        if (transitionArray != null && !transitionArray.isEmpty()) {
            transitions = Arrays.asList(contrib.getNoRecursionForTransitions().split(
                    ","));
        }
        docTypeToNonRecursiveTransition.put(contrib.getDocumentType(),
                transitions);
    }

    @Override
    public void contributionRemoved(String id,
            LifeCycleTypesDescriptor origContrib) {
        typesMapping.remove(id);
        docTypeToNonRecursiveTransition.remove(id);
    }

    @Override
    public boolean isSupportingMerge() {
        return false;
    }

    @Override
    public LifeCycleTypesDescriptor clone(LifeCycleTypesDescriptor orig) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void merge(LifeCycleTypesDescriptor src, LifeCycleTypesDescriptor dst) {
        throw new UnsupportedOperationException();
    }

    // API

    public String getLifeCycleNameForType(String docType) {
        return typesMapping.get(docType);
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

    public List<String> getNonRecursiveTransitionForDocType(String docTypeName) {
        return docTypeToNonRecursiveTransition.get(docTypeName);
    }

}
