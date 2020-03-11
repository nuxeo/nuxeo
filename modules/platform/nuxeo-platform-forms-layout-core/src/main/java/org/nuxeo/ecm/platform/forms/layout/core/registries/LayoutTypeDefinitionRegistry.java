/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.forms.layout.core.registries;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.platform.forms.layout.api.LayoutTypeDefinition;
import org.nuxeo.runtime.model.SimpleContributionRegistry;

/**
 * Regiustry for layoyut type definitions.
 *
 * @since 6.0
 */
public class LayoutTypeDefinitionRegistry extends SimpleContributionRegistry<LayoutTypeDefinition> {

    protected final String category;

    public LayoutTypeDefinitionRegistry(String category) {
        super();
        this.category = category;
    }

    public String getCategory() {
        return category;
    }

    @Override
    public String getContributionId(LayoutTypeDefinition contrib) {
        return contrib.getName();
    }

    public List<LayoutTypeDefinition> getDefinitions() {
        List<LayoutTypeDefinition> res = new ArrayList<>();
        for (LayoutTypeDefinition item : currentContribs.values()) {
            if (item != null) {
                res.add(item);
            }
        }
        return res;
    }

    public LayoutTypeDefinition getDefinition(String id) {
        return getCurrentContribution(id);
    }

    @Override
    // overridden to handle aliases
    public synchronized void addContribution(LayoutTypeDefinition contrib) {
        super.addContribution(contrib);
        List<String> aliases = contrib.getAliases();
        if (aliases != null) {
            for (String alias : aliases) {
                FragmentList<LayoutTypeDefinition> head = addFragment(alias, contrib);
                contributionUpdated(alias, head.merge(this), contrib);
            }
        }
    }

    /**
     * Overridden to use equals method when removing elements.
     *
     * @since 7.2
     */
    @Override
    public synchronized void removeContribution(LayoutTypeDefinition contrib) {
        removeContribution(contrib, true);
    }

    @Override
    // overridden to handle aliases
    public synchronized void removeContribution(LayoutTypeDefinition contrib, boolean useEqualsMethod) {
        super.removeContribution(contrib, useEqualsMethod);
        List<String> aliases = contrib.getAliases();
        if (aliases != null) {
            for (String alias : aliases) {
                FragmentList<LayoutTypeDefinition> head = removeFragment(alias, contrib, useEqualsMethod);
                if (head != null) {
                    LayoutTypeDefinition result = head.merge(this);
                    if (result != null) {
                        contributionUpdated(alias, result, contrib);
                    } else {
                        contributionRemoved(alias, contrib);
                    }
                }
            }
        }
    }

}
