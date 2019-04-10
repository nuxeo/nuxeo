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
        Map<String, Set<IdRef>> lastActiveRootRefs = new LinkedHashMap<String, Set<IdRef>>();
        if (rootDefinitions != null) {
            String[] rootDefinitionComponents = StringUtils.split(rootDefinitions, ",");
            for (String rootDefinition : rootDefinitionComponents) {
                String[] rootComponents = StringUtils.split(rootDefinition, ":");
                String repoName = rootComponents[0].trim();
                Set<IdRef> refs = lastActiveRootRefs.get(repoName);
                if (refs == null) {
                    refs = new HashSet<IdRef>();
                    lastActiveRootRefs.put(repoName, refs);
                }
                refs.add(new IdRef(rootComponents[1].trim()));
            }
        }
        return lastActiveRootRefs;
    }

}
