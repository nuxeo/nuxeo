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

import org.nuxeo.ecm.platform.forms.layout.api.LayoutDefinition;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 * Registry holding layout definitions for a given category
 *
 * @since 5.5
 */
public class LayoutDefinitionRegistry extends
        ContributionFragmentRegistry<LayoutDefinition> {

    protected final String category;

    protected final Map<String, LayoutDefinition> layoutDefs;

    public LayoutDefinitionRegistry(String category) {
        super();
        this.category = category;
        this.layoutDefs = new HashMap<String, LayoutDefinition>();
    }

    public String getCategory() {
        return category;
    }

    public List<String> getLayoutNames() {
        List<String> res = new ArrayList<String>();
        res.addAll(layoutDefs.keySet());
        return res;
    }

    @Override
    public String getContributionId(LayoutDefinition contrib) {
        return contrib.getName();
    }

    @Override
    public void contributionUpdated(String id, LayoutDefinition contrib,
            LayoutDefinition newOrigContrib) {
        layoutDefs.put(id, contrib);
    }

    @Override
    public void contributionRemoved(String id, LayoutDefinition origContrib) {
        layoutDefs.remove(id);
    }

    @Override
    public boolean isSupportingMerge() {
        return false;
    }

    @Override
    public LayoutDefinition clone(LayoutDefinition orig) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void merge(LayoutDefinition src, LayoutDefinition dst) {
        throw new UnsupportedOperationException();
    }

    public LayoutDefinition getLayoutDefinition(String id) {
        return layoutDefs.get(id);
    }

}
