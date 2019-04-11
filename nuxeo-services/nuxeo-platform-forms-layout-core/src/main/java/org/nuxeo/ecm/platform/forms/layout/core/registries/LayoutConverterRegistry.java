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

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.platform.forms.layout.descriptors.LayoutConverterDescriptor;
import org.nuxeo.runtime.model.SimpleContributionRegistry;

/**
 * @since 5.5
 */
public class LayoutConverterRegistry extends SimpleContributionRegistry<LayoutConverterDescriptor> {

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
        List<String> res = new ArrayList<>();
        res.addAll(currentContribs.keySet());
        return res;
    }

    public List<LayoutConverterDescriptor> getConverters() {
        List<LayoutConverterDescriptor> res = new ArrayList<>();
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
