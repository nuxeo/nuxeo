/*
 * (C) Copyright 2013-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer
 */
package org.nuxeo.drive.service.impl;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.IdRef;

/**
 * Helper to handle synchronization root definitions.
 *
 * @author Antoine Taillefer
 */
public final class RootDefinitionsHelper {

    private RootDefinitionsHelper() {
        // Utility class
    }

    /**
     * Parses the given synchronization root definitions string.
     */
    public static Map<String, Set<IdRef>> parseRootDefinitions(String rootDefinitions) {
        Map<String, Set<IdRef>> lastActiveRootRefs = new LinkedHashMap<>();
        if (rootDefinitions != null) {
            String[] rootDefinitionComponents = StringUtils.split(rootDefinitions, ",");
            for (String rootDefinition : rootDefinitionComponents) {
                String[] rootComponents = StringUtils.split(rootDefinition, ":");
                String repoName = rootComponents[0].trim();
                lastActiveRootRefs.computeIfAbsent(repoName, k -> new HashSet<>())
                                  .add(new IdRef(rootComponents[1].trim()));
            }
        }
        return lastActiveRootRefs;
    }

}
