/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 */

package org.nuxeo.ecm.core.api.adapter;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class DocumentAdapterService extends DefaultComponent {

    public static final ComponentName NAME = new ComponentName(ComponentName.DEFAULT_TYPE,
            "org.nuxeo.ecm.core.api.DocumentAdapterService");

    protected static final String XP = "adapters";

    /**
     * Document adapters
     */
    protected Map<Class<?>, DocumentAdapterDescriptor> adapters;

    @Override
    public void start(ComponentContext context) {
        adapters = this.<DocumentAdapterDescriptor> getRegistryContributions(XP)
                       .stream()
                       .collect(Collectors.toConcurrentMap(DocumentAdapterDescriptor::getInterface,
                               Function.identity()));
    }

    @Override
    public void stop(ComponentContext context) throws InterruptedException {
        adapters = null;
    }

    public DocumentAdapterDescriptor getAdapterDescriptor(Class<?> itf) {
        return adapters.get(itf);
    }

    /**
     * @since 5.7
     */
    public DocumentAdapterDescriptor[] getAdapterDescriptors() {
        Collection<DocumentAdapterDescriptor> values = adapters.values();
        return values.toArray(new DocumentAdapterDescriptor[values.size()]);
    }

}
