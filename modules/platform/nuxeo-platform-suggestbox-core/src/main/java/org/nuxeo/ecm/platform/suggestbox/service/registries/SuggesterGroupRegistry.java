/*
 * (C) Copyright 2010-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Olivier Grisel
 */
package org.nuxeo.ecm.platform.suggestbox.service.registries;

import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.XAnnotatedObject;
import org.nuxeo.common.xmap.registry.MapRegistry;
import org.nuxeo.ecm.platform.suggestbox.service.descriptors.SuggesterGroupDescriptor;
import org.w3c.dom.Element;

/**
 * Handles custom merge for {@link SuggesterGroupDescriptor} contributions.
 */
public class SuggesterGroupRegistry extends MapRegistry {

    @Override
    @SuppressWarnings("unchecked")
    protected <T> T getMergedInstance(Context ctx, XAnnotatedObject xObject, Element element, Object existing) {
        SuggesterGroupDescriptor contrib = getInstance(ctx, xObject, element);
        if (existing != null) {
            ((SuggesterGroupDescriptor) existing).mergeFrom(contrib);
            return (T) existing;
        } else {
            return (T) contrib;
        }
    }

    public SuggesterGroupDescriptor getSuggesterGroupDescriptor(String name) {
        return this.<SuggesterGroupDescriptor> getContribution(name).orElse(null);
    }

}
