/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.ecm.core.scroll;

import org.nuxeo.ecm.core.api.scroll.ScrollService;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @since 11.1
 */
public class ScrollComponent extends DefaultComponent {

    public static final String XP_SCROLL = "scroll";

    protected ScrollService scrollService;

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(ScrollService.class)) {
            return (T) scrollService;
        }
        return null;
    }

    @Override
    public void start(ComponentContext context) {
        super.start(context);
        scrollService = new ScrollServiceImpl(getRegistryContributions(XP_SCROLL));
    }

}
