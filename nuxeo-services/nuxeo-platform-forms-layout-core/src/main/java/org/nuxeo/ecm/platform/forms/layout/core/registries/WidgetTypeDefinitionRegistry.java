/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.platform.forms.layout.core.registries;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.platform.forms.layout.api.WidgetTypeDefinition;
import org.nuxeo.runtime.model.SimpleContributionRegistry;

/**
 * Registry holding widget type definitions for a given category.
 *
 * @since 5.5
 */
public class WidgetTypeDefinitionRegistry extends
        SimpleContributionRegistry<WidgetTypeDefinition> {

    protected final String category;

    public WidgetTypeDefinitionRegistry(String category) {
        super();
        this.category = category;
    }

    public String getCategory() {
        return category;
    }

    @Override
    public String getContributionId(WidgetTypeDefinition contrib) {
        return contrib.getName();
    }

    public List<WidgetTypeDefinition> getDefinitions() {
        List<WidgetTypeDefinition> res = new ArrayList<WidgetTypeDefinition>();
        for (WidgetTypeDefinition item : currentContribs.values()) {
            if (item != null) {
                res.add(item);
            }
        }
        return res;
    }

    public WidgetTypeDefinition getDefinition(String id) {
        return getCurrentContribution(id);
    }

    @Override
    // overridden to handle aliases
    public synchronized void addContribution(WidgetTypeDefinition contrib) {
        super.addContribution(contrib);
        List<String> aliases = contrib.getAliases();
        if (aliases != null) {
            for (String alias : aliases) {
                FragmentList<WidgetTypeDefinition> head = addFragment(alias,
                        contrib);
                contributionUpdated(alias, head.merge(this), contrib);
            }
        }
    }

    @Override
    // overridden to handle aliases
    public synchronized void removeContribution(WidgetTypeDefinition contrib,
            boolean useEqualsMethod) {
        super.removeContribution(contrib, useEqualsMethod);
        List<String> aliases = contrib.getAliases();
        if (aliases != null) {
            for (String alias : aliases) {
                FragmentList<WidgetTypeDefinition> head = removeFragment(alias,
                        contrib, useEqualsMethod);
                if (head != null) {
                    WidgetTypeDefinition result = head.merge(this);
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
