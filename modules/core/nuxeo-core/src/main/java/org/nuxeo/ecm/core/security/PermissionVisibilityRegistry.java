/*
 * (C) Copyright 2006-2021 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *     Bogdan Stefanescu
 *     Olivier Grisel
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.core.security;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.XAnnotatedObject;
import org.nuxeo.common.xmap.registry.MapRegistry;
import org.w3c.dom.Element;

/**
 * Registry for custom merge of {@link PermissionVisibilityDescriptor} contributions.
 *
 * @since 11.5
 */
public class PermissionVisibilityRegistry extends MapRegistry {

    public static final String DEFAULT_ID = "";

    @Override
    public void initialize() {
        super.initialize();
        var defaultDesc = (PermissionVisibilityDescriptor) contributions.get(DEFAULT_ID);
        if (defaultDesc == null) {
            // make sure there is a default desc
            contributions.put(DEFAULT_ID, new PermissionVisibilityDescriptor());
        } else {
            // rebuild contributions by merging them with default visibility
            this.<PermissionVisibilityDescriptor> getContributionValues().forEach(pvd -> {
                if (StringUtils.isNotEmpty(pvd.getTypeName())) {
                    var copy = new PermissionVisibilityDescriptor(defaultDesc);
                    copy.merge(pvd);
                    contributions.put(pvd.getTypeName(), copy);
                }
            });
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T> T getMergedInstance(Context ctx, XAnnotatedObject xObject, Element element, Object existing) {
        PermissionVisibilityDescriptor contrib = getInstance(ctx, xObject, element);
        if (existing != null) {
            ((PermissionVisibilityDescriptor) existing).merge(contrib);
            return (T) existing;
        } else {
            return (T) contrib;
        }
    }

}
