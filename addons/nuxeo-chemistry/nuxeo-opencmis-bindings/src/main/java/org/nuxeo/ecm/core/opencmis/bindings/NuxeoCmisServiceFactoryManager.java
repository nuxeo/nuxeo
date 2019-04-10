/*
 * (C) Copyright 2014-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *
 */
package org.nuxeo.ecm.core.opencmis.bindings;

import java.util.Map;

import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.SimpleContributionRegistry;

/**
 * Service holding the definition
 */
public class NuxeoCmisServiceFactoryManager extends DefaultComponent {

    private static final String XP_FACTORY = "factory";

    protected NuxeoCmisServiceFactoryDescriptorRegistry registry = new NuxeoCmisServiceFactoryDescriptorRegistry();

    protected static class NuxeoCmisServiceFactoryDescriptorRegistry extends
            SimpleContributionRegistry<NuxeoCmisServiceFactoryDescriptor> {

        @Override
        public String getContributionId(NuxeoCmisServiceFactoryDescriptor contrib) {
            return XP_FACTORY;
        }

        @Override
        public NuxeoCmisServiceFactoryDescriptor clone(NuxeoCmisServiceFactoryDescriptor orig) {
            return new NuxeoCmisServiceFactoryDescriptor(orig);
        }

        @Override
        public void merge(NuxeoCmisServiceFactoryDescriptor src, NuxeoCmisServiceFactoryDescriptor dst) {
            dst.merge(src);
        }

        @Override
        public boolean isSupportingMerge() {
            return true;
        }

        public void clear() {
            currentContribs.clear();
        }

        public NuxeoCmisServiceFactoryDescriptor getNuxeoCmisServiceFactoryDescriptor() {
            return getCurrentContribution(XP_FACTORY);
        }
    }

    @Override
    public void activate(ComponentContext context) {
        registry.clear();
    }

    @Override
    public void deactivate(ComponentContext context) {
        registry.clear();
    }

    @Override
    public void registerContribution(Object contrib, String xpoint, ComponentInstance contributor) {
        if (XP_FACTORY.equals(xpoint)) {
            addContribution((NuxeoCmisServiceFactoryDescriptor) contrib);
        } else {
            throw new RuntimeException("Unknown extension point: " + xpoint);
        }
    }

    @Override
    public void unregisterContribution(Object contrib, String xpoint, ComponentInstance contributor) {
        if (XP_FACTORY.equals(xpoint)) {
            removeContribution((NuxeoCmisServiceFactoryDescriptor) contrib);
        } else {
            throw new RuntimeException("Unknown extension point: " + xpoint);
        }
    }

    protected void addContribution(NuxeoCmisServiceFactoryDescriptor descriptor) {
        registry.addContribution(descriptor);
    }

    protected void removeContribution(NuxeoCmisServiceFactoryDescriptor descriptor) {
        registry.removeContribution(descriptor);
    }

    /**
     * Gets the {@link NuxeoCmisServiceFactory} based on contributed {@link NuxeoCmisServiceFactoryDescriptor}s.
     */
    public NuxeoCmisServiceFactory getNuxeoCmisServiceFactory() {
        NuxeoCmisServiceFactoryDescriptor descriptor = registry.getNuxeoCmisServiceFactoryDescriptor();

        Class<? extends NuxeoCmisServiceFactory> factoryClass = descriptor.getFactoryClass();
        Map<String, String> factoryParameters = descriptor.factoryParameters;
        NuxeoCmisServiceFactory nuxeoCmisServiceFactory;
        try {
            nuxeoCmisServiceFactory = factoryClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Cannot instantiate nuxeoCmisServiceFactory: " + factoryClass.getName(), e);
        }

        nuxeoCmisServiceFactory.init(factoryParameters);
        return nuxeoCmisServiceFactory;
    }

}
