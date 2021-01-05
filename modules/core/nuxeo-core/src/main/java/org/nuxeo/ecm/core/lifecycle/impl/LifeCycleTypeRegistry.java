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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.nuxeo.common.xmap.registry.MapRegistry;
import org.nuxeo.ecm.core.lifecycle.extensions.LifeCycleTypesDescriptor;

/**
 * Registry for lifecycle &lt;-&gt; types association
 *
 * @since 5.6
 */
public class LifeCycleTypeRegistry extends MapRegistry {

    /** Type name -&gt; life cycle name. */
    protected Map<String, String> typesMapping = new ConcurrentHashMap<>();

    /**
     * a mapping from doc type -&gt; list of transitions that should not recurse.
     */
    protected Map<String, List<String>> docTypeToNonRecursiveTransition = new ConcurrentHashMap<>();

    @Override
    public void initialize() {
        typesMapping.clear();
        docTypeToNonRecursiveTransition.clear();
        super.initialize();
        this.<LifeCycleTypesDescriptor> getContributionValues().forEach(contrib -> {
            typesMapping.put(contrib.getDocumentType(), contrib.getLifeCycleName());
            String transitionArray = contrib.getNoRecursionForTransitions();
            List<String> transitions = new ArrayList<>();
            if (transitionArray != null && !transitionArray.isEmpty()) {
                transitions = Arrays.asList(contrib.getNoRecursionForTransitions().split(","));
            }
            docTypeToNonRecursiveTransition.put(contrib.getDocumentType(), transitions);
        });
    }

    // API

    public String getLifeCycleNameForType(String docType) {
        checkInitialized();
        return typesMapping.get(docType);
    }

    public Collection<String> getTypesFor(String lifeCycleName) {
        checkInitialized();
        Collection<String> types = new ArrayList<>();
        for (String typeName : typesMapping.keySet()) {
            if (typesMapping.get(typeName).equals(lifeCycleName)) {
                types.add(typeName);
            }
        }
        return types;
    }

    public Map<String, String> getTypesMapping() {
        checkInitialized();
        return Collections.unmodifiableMap(typesMapping);
    }

    public List<String> getNonRecursiveTransitionForDocType(String docTypeName) {
        checkInitialized();
        return docTypeToNonRecursiveTransition.get(docTypeName);
    }

}
