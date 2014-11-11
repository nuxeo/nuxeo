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

import org.nuxeo.ecm.platform.forms.layout.descriptors.WidgetConverterDescriptor;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 * @since 5.5
 */
public class WidgetConverterRegistry extends
        ContributionFragmentRegistry<WidgetConverterDescriptor> {

    protected final String category;

    protected final Map<String, WidgetConverterDescriptor> converters;

    public WidgetConverterRegistry(String category) {
        super();
        this.category = category;
        this.converters = new HashMap<String, WidgetConverterDescriptor>();
    }

    public String getCategory() {
        return category;
    }

    public List<String> getLayoutNames() {
        List<String> res = new ArrayList<String>();
        res.addAll(converters.keySet());
        return res;
    }

    @Override
    public String getContributionId(WidgetConverterDescriptor contrib) {
        return contrib.getName();
    }

    @Override
    public void contributionUpdated(String id,
            WidgetConverterDescriptor contrib,
            WidgetConverterDescriptor newOrigContrib) {
        converters.put(id, contrib);
    }

    @Override
    public void contributionRemoved(String id,
            WidgetConverterDescriptor origContrib) {
        converters.remove(id);
    }

    @Override
    public boolean isSupportingMerge() {
        return false;
    }

    @Override
    public WidgetConverterDescriptor clone(WidgetConverterDescriptor orig) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void merge(WidgetConverterDescriptor src,
            WidgetConverterDescriptor dst) {
        throw new UnsupportedOperationException();
    }

    public List<WidgetConverterDescriptor> getConverters() {
        List<WidgetConverterDescriptor> res = new ArrayList<WidgetConverterDescriptor>();
        for (WidgetConverterDescriptor item : converters.values()) {
            if (item != null) {
                res.add(item);
            }
        }
        return res;
    }

    public WidgetConverterDescriptor getConverter(String id) {
        return converters.get(id);
    }

}
