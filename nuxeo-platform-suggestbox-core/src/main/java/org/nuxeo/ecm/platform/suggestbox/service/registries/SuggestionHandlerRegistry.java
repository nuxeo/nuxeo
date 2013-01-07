/*
 * (C) Copyright 2010-2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Olivier Grisel
 */
package org.nuxeo.ecm.platform.suggestbox.service.registries;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.nuxeo.ecm.platform.suggestbox.service.descriptors.SuggestionHandlerDescriptor;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;

public class SuggestionHandlerRegistry extends
        ContributionFragmentRegistry<SuggestionHandlerDescriptor> {

    protected final Map<String, SuggestionHandlerDescriptor> suggestionHandlerDescriptors = new LinkedHashMap<String, SuggestionHandlerDescriptor>();

    public Collection<SuggestionHandlerDescriptor> getHandlers() {
        return suggestionHandlerDescriptors.values();
    }

    @Override
    public void contributionRemoved(String id,
            SuggestionHandlerDescriptor descriptor) {
        suggestionHandlerDescriptors.remove(id);
    }

    @Override
    public String getContributionId(SuggestionHandlerDescriptor descriptor) {
        return descriptor.getName();
    }

    @Override
    public void contributionUpdated(String id,
            SuggestionHandlerDescriptor contrib,
            SuggestionHandlerDescriptor newOrigContrib) {
        suggestionHandlerDescriptors.put(id, contrib);
    }

    @Override
    public SuggestionHandlerDescriptor clone(
            SuggestionHandlerDescriptor suggester) {
        try {
            return (SuggestionHandlerDescriptor) suggester.clone();
        } catch (CloneNotSupportedException e) {
            // this should never occur since clone implements Cloneable
            throw new RuntimeException(e);
        }
    }

    @Override
    public void merge(SuggestionHandlerDescriptor src,
            SuggestionHandlerDescriptor dst) {
    }
}
