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

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.platform.forms.layout.api.WidgetDefinition;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 * Registry holding widget definitions (global widgets) for a given category
 *
 * @since 5.5
 */
public class WidgetDefinitionRegistry extends
        ContributionFragmentRegistry<WidgetDefinition> {

    protected final String category;

    protected final Map<String, WidgetDefinition> widgetDefs;

    public WidgetDefinitionRegistry(String category) {
        super();
        this.category = category;
        this.widgetDefs = new HashMap<String, WidgetDefinition>();
    }

    public String getCategory() {
        return category;
    }

    @Override
    public String getContributionId(WidgetDefinition contrib) {
        return contrib.getName();
    }

    @Override
    public void contributionUpdated(String id, WidgetDefinition contrib,
            WidgetDefinition newOrigContrib) {
        widgetDefs.put(id, contrib);
    }

    @Override
    public void contributionRemoved(String id, WidgetDefinition origContrib) {
        widgetDefs.remove(id);
    }

    @Override
    public boolean isSupportingMerge() {
        return false;
    }

    @Override
    public WidgetDefinition clone(WidgetDefinition orig) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void merge(WidgetDefinition src, WidgetDefinition dst) {
        throw new UnsupportedOperationException();
    }

    public WidgetDefinition getWidgetDefinition(String id) {
        return widgetDefs.get(id);
    }

}
