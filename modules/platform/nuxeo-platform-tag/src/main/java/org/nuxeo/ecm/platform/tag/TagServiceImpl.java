/*
 * (C) Copyright 2009-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Radu Darlea
 *     Catalin Baican
 *     Florent Guillaume
 */

package org.nuxeo.ecm.platform.tag;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.Component;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * the tag component.
 */
public class TagServiceImpl extends DefaultComponent {

    public static final ComponentName NAME = new ComponentName("org.nuxeo.ecm.platform.tag.TagService");

    protected TagService tagService = new FacetedTagService();

    @Override
    public int getApplicationStartedOrder() {
        // should deploy before repository service because the tag service is indirectly used (through a listener) by
        // the repository init handlers
        Component component = (Component) Framework.getRuntime()
                                                   .getComponentInstance(
                                                           "org.nuxeo.ecm.core.repository.RepositoryServiceComponent")
                                                   .getInstance();
        return component.getApplicationStartedOrder() - 1;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == TagServiceImpl.class) {
            return adapter.cast(this);
        }
        return (T) tagService;
    }
}
