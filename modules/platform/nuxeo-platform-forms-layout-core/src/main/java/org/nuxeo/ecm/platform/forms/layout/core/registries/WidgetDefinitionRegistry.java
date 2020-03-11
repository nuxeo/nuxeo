/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.util.List;

import org.nuxeo.ecm.platform.forms.layout.api.WidgetDefinition;
import org.nuxeo.runtime.model.SimpleContributionRegistry;

/**
 * Registry holding widget definitions (global widgets) for a given category
 *
 * @since 5.5
 */
public class WidgetDefinitionRegistry extends SimpleContributionRegistry<WidgetDefinition> {

    protected final String category;

    public WidgetDefinitionRegistry(String category) {
        super();
        this.category = category;
    }

    public String getCategory() {
        return category;
    }

    @Override
    public String getContributionId(WidgetDefinition contrib) {
        return contrib.getName();
    }

    public WidgetDefinition getWidgetDefinition(String id) {
        return getCurrentContribution(id);
    }

    @Override
    // overridden to handle aliases
    public synchronized void addContribution(WidgetDefinition contrib) {
        super.addContribution(contrib);
        List<String> aliases = contrib.getAliases();
        if (aliases != null) {
            for (String alias : aliases) {
                FragmentList<WidgetDefinition> head = addFragment(alias, contrib);
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
    public synchronized void removeContribution(WidgetDefinition contrib) {
        removeContribution(contrib, true);
    }

    @Override
    // overridden to handle aliases
    public synchronized void removeContribution(WidgetDefinition contrib, boolean useEqualsMethod) {
        super.removeContribution(contrib, useEqualsMethod);
        List<String> aliases = contrib.getAliases();
        if (aliases != null) {
            for (String alias : aliases) {
                FragmentList<WidgetDefinition> head = removeFragment(alias, contrib, useEqualsMethod);
                if (head != null) {
                    WidgetDefinition result = head.merge(this);
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
