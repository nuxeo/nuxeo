/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
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

import org.nuxeo.ecm.platform.forms.layout.api.LayoutTypeDefinition;
import org.nuxeo.runtime.model.SimpleContributionRegistry;

/**
 * Regiustry for layoyut type definitions.
 *
 * @since 5.9.6
 */
public class LayoutTypeDefinitionRegistry extends
        SimpleContributionRegistry<LayoutTypeDefinition> {

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
        List<LayoutTypeDefinition> res = new ArrayList<LayoutTypeDefinition>();
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
                FragmentList<LayoutTypeDefinition> head = addFragment(alias,
                        contrib);
                contributionUpdated(alias, head.merge(this), contrib);
            }
        }
    }

    @Override
    // overridden to handle aliases
    public synchronized void removeContribution(LayoutTypeDefinition contrib,
            boolean useEqualsMethod) {
        super.removeContribution(contrib, useEqualsMethod);
        List<String> aliases = contrib.getAliases();
        if (aliases != null) {
            for (String alias : aliases) {
                FragmentList<LayoutTypeDefinition> head = removeFragment(alias,
                        contrib, useEqualsMethod);
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
