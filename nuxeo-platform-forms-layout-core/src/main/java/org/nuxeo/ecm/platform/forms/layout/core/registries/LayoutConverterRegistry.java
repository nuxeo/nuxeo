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

import org.nuxeo.ecm.platform.forms.layout.descriptors.LayoutConverterDescriptor;
import org.nuxeo.runtime.model.SimpleContributionRegistry;

/**
 * @since 5.5
 */
public class LayoutConverterRegistry extends
        SimpleContributionRegistry<LayoutConverterDescriptor> {

    protected final String category;

    public LayoutConverterRegistry(String category) {
        super();
        this.category = category;
    }

    @Override
    public String getContributionId(LayoutConverterDescriptor contrib) {
        return contrib.getName();
    }

    public String getCategory() {
        return category;
    }

    public List<String> getLayoutNames() {
        List<String> res = new ArrayList<String>();
        res.addAll(currentContribs.keySet());
        return res;
    }

    public List<LayoutConverterDescriptor> getConverters() {
        List<LayoutConverterDescriptor> res = new ArrayList<LayoutConverterDescriptor>();
        for (LayoutConverterDescriptor item : currentContribs.values()) {
            if (item != null) {
                res.add(item);
            }
        }
        return res;
    }

    public LayoutConverterDescriptor getConverter(String id) {
        return getCurrentContribution(id);
    }

}