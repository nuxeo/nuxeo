/*
 * (C) Copyright 2011-2021 Nuxeo SA (http://nuxeo.com/) and others.
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

import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.XAnnotatedObject;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetTypeDefinition;
import org.nuxeo.ecm.platform.forms.layout.descriptors.WidgetTypeDescriptor;
import org.w3c.dom.Element;

/**
 * Registry holding widget type definitions for a given category.
 *
 * @since 5.5
 */
public class WidgetTypeDefinitionRegistry extends AbstractCategoryMapRegistry {

    @Override
    protected List<String> getCategories(Context ctx, XAnnotatedObject xObject, Element element) {
        var contrib = (WidgetTypeDescriptor) xObject.newInstance(ctx, element);
        return List.of(contrib.getCategories());
    }

    @Override
    protected <T> List<String> getContributionAliases(T contribution) {
        return ((WidgetTypeDefinition) contribution).getAliases();
    }

    @Override
    protected <T> Object getConvertedContribution(T contribution) {
        var desc = (WidgetTypeDescriptor) contribution;
        return desc.getWidgetTypeDefinition();
    }

}
