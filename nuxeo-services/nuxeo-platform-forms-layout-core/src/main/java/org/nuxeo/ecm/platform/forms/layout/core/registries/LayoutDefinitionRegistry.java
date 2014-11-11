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

import org.nuxeo.ecm.platform.forms.layout.api.LayoutDefinition;
import org.nuxeo.runtime.model.SimpleContributionRegistry;

/**
 * Registry holding layout definitions for a given category
 *
 * @since 5.5
 */
public class LayoutDefinitionRegistry extends
        SimpleContributionRegistry<LayoutDefinition> {

    protected final String category;

    public LayoutDefinitionRegistry(String category) {
        super();
        this.category = category;
    }

    public String getCategory() {
        return category;
    }

    public List<String> getLayoutNames() {
        List<String> res = new ArrayList<String>();
        res.addAll(currentContribs.keySet());
        return res;
    }

    @Override
    public String getContributionId(LayoutDefinition contrib) {
        return contrib.getName();
    }

    public LayoutDefinition getLayoutDefinition(String id) {
        return getCurrentContribution(id);
    }

    @Override
    // overridden to handle aliases
    public synchronized void addContribution(LayoutDefinition contrib) {
        super.addContribution(contrib);
        List<String> aliases = contrib.getAliases();
        if (aliases != null) {
            for (String alias : aliases) {
                FragmentList<LayoutDefinition> head = addFragment(alias,
                        contrib);
                contributionUpdated(alias, head.merge(this), contrib);
            }
        }
    }

    @Override
    // overridden to handle aliases
    public synchronized void removeContribution(LayoutDefinition contrib,
            boolean useEqualsMethod) {
        super.removeContribution(contrib, useEqualsMethod);
        List<String> aliases = contrib.getAliases();
        if (aliases != null) {
            for (String alias : aliases) {
                FragmentList<LayoutDefinition> head = removeFragment(alias,
                        contrib, useEqualsMethod);
                if (head != null) {
                    LayoutDefinition result = head.merge(this);
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
