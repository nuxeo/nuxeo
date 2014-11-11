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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.platform.forms.layout.api.WidgetTypeDefinition;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 * Registry holding widget type definitions for a given category.
 *
 * @since 5.5
 */
public class WidgetTypeDefinitionRegistry extends
        ContributionFragmentRegistry<WidgetTypeDefinition> {

    protected final String category;

    protected final Map<String, WidgetTypeDefinition> widgetTypeDefs;

    public WidgetTypeDefinitionRegistry(String category) {
        super();
        this.category = category;
        this.widgetTypeDefs = new HashMap<String, WidgetTypeDefinition>();
    }

    public String getCategory() {
        return category;
    }

    @Override
    public String getContributionId(WidgetTypeDefinition contrib) {
        return contrib.getName();
    }

    @Override
    public void contributionUpdated(String id, WidgetTypeDefinition contrib,
            WidgetTypeDefinition newOrigContrib) {
        widgetTypeDefs.put(id, contrib);
    }

    @Override
    public void contributionRemoved(String id, WidgetTypeDefinition origContrib) {
        widgetTypeDefs.remove(id);
    }

    @Override
    public boolean isSupportingMerge() {
        return false;
    }

    @Override
    public WidgetTypeDefinition clone(WidgetTypeDefinition orig) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void merge(WidgetTypeDefinition src, WidgetTypeDefinition dst) {
        throw new UnsupportedOperationException();
    }

    public List<WidgetTypeDefinition> getDefinitions() {
        List<WidgetTypeDefinition> res = new ArrayList<WidgetTypeDefinition>();
        for (WidgetTypeDefinition item : widgetTypeDefs.values()) {
            if (item != null) {
                res.add(item);
            }
        }
        return res;
    }

    public WidgetTypeDefinition getDefinition(String id) {
        return widgetTypeDefs.get(id);
    }

}
