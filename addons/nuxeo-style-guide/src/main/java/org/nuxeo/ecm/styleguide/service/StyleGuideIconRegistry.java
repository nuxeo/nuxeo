/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.styleguide.service;

import org.nuxeo.ecm.styleguide.service.descriptors.IconDescriptor;
import org.nuxeo.runtime.model.SimpleContributionRegistry;

/**
 * @since 5.7
 */
public class StyleGuideIconRegistry extends SimpleContributionRegistry<IconDescriptor> {

    @Override
    public String getContributionId(IconDescriptor contrib) {
        return contrib.getPath();
    }

    public IconDescriptor getIcon(String path) {
        return currentContribs.get(path);
    }

}
