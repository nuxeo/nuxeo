/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.rendition.service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.XAnnotatedObject;
import org.nuxeo.common.xmap.registry.AbstractRegistry;
import org.w3c.dom.Element;

/**
 * Registry without merge, removal or enablement.
 *
 * @since 11.5
 */
public class DefaultRenditionRegistry extends AbstractRegistry {

    protected List<DefaultRenditionDescriptor> contributions = new CopyOnWriteArrayList<>();

    @Override
    public void initialize() {
        contributions.clear();
        super.initialize();
        // handle values in reverse order to apply the more recent contributions first
        Collections.reverse(contributions);
    }

    public List<DefaultRenditionDescriptor> getContributionValues() {
        checkInitialized();
        return Collections.unmodifiableList(contributions);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T> T doRegister(Context ctx, XAnnotatedObject xObject, Element element, String extensionId) {
        DefaultRenditionDescriptor contrib = getInstance(ctx, xObject, element);
        if (contrib != null) {
            contributions.add(contrib);
        }
        return (T) contrib;
    }

}
