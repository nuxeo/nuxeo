/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 * Registry for lifecycle &lt;-&gt; types association
 *
 * @since 5.6
 */
public class LifeCycleTypeRegistry extends ContributionFragmentRegistry<LifeCycleTypesDescriptor> {

    private static final Log log = LogFactory.getLog(LifeCycleTypeRegistry.class);

    /** Type name -&gt; life cycle name. */
    protected Map<String, String> typesMapping = new HashMap<>();

    /**
     * a mapping from doc type -&gt; list of transitions that should not recurse.
     */
    protected Map<String, List<String>> docTypeToNonRecursiveTransition = new HashMap<>();

    @Override
    public String getContributionId(LifeCycleTypesDescriptor contrib) {
        return contrib.getDocumentType();
    }

    @Override
    public void contributionUpdated(String id, LifeCycleTypesDescriptor contrib, LifeCycleTypesDescriptor newOrigContrib) {
        log.info("Registering lifecycle types mapping: " + contrib.getDocumentType() + "-" + contrib.getLifeCycleName());
        typesMapping.put(contrib.getDocumentType(), contrib.getLifeCycleName());
        String transitionArray = contrib.getNoRecursionForTransitions();
        List<String> transitions = new ArrayList<>();
        if (transitionArray != null && !transitionArray.isEmpty()) {
            transitions = Arrays.asList(contrib.getNoRecursionForTransitions().split(","));
        }
        docTypeToNonRecursiveTransition.put(contrib.getDocumentType(), transitions);
    }

    @Override
    public void contributionRemoved(String id, LifeCycleTypesDescriptor origContrib) {
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
        Collection<String> types = new ArrayList<>();
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
