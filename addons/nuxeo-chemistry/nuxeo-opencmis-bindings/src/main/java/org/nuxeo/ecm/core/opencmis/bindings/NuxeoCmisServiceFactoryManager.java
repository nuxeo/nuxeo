/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
        public String getContributionId(
                NuxeoCmisServiceFactoryDescriptor contrib) {
            return XP_FACTORY;
        }

        @Override
        public NuxeoCmisServiceFactoryDescriptor clone(
                NuxeoCmisServiceFactoryDescriptor orig) {
            return new NuxeoCmisServiceFactoryDescriptor(orig);
        }

        @Override
        public void merge(NuxeoCmisServiceFactoryDescriptor src,
                NuxeoCmisServiceFactoryDescriptor dst) {
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
    public void activate(ComponentContext context) throws Exception {
        registry.clear();
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        registry.clear();
    }

    @Override
    public void registerContribution(Object contrib, String xpoint,
            ComponentInstance contributor) {
        if (XP_FACTORY.equals(xpoint)) {
            addContribution((NuxeoCmisServiceFactoryDescriptor) contrib);
        } else {
            throw new RuntimeException("Unknown extension point: " + xpoint);
        }
    }

    @Override
    public void unregisterContribution(Object contrib, String xpoint,
            ComponentInstance contributor) throws Exception {
        if (XP_FACTORY.equals(xpoint)) {
            removeContribution((NuxeoCmisServiceFactoryDescriptor) contrib);
        } else {
            throw new RuntimeException("Unknown extension point: " + xpoint);
        }
    }

    protected void addContribution(NuxeoCmisServiceFactoryDescriptor descriptor) {
        registry.addContribution(descriptor);
    }

    protected void removeContribution(
            NuxeoCmisServiceFactoryDescriptor descriptor) {
        registry.removeContribution(descriptor);
    }

    /**
     * Gets the {@link NuxeoCmisServiceFactory} based on contributed
     * {@link NuxeoCmisServiceFactoryDescriptor}s.
     */
    public NuxeoCmisServiceFactory getNuxeoCmisServiceFactory() {
        NuxeoCmisServiceFactoryDescriptor descriptor = registry.getNuxeoCmisServiceFactoryDescriptor();

        Class<? extends NuxeoCmisServiceFactory> factoryClass = null;
        Map<String, String> factoryParameters = null;
        if (descriptor != null) {
            factoryClass = descriptor.getFactoryClass();
            factoryParameters = descriptor.factoryParameters;
        }
        NuxeoCmisServiceFactory nuxeoCmisServiceFactory;
        try {
            nuxeoCmisServiceFactory = factoryClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(
                    "Cannot instantiate nuxeoCmisServiceFactory: "
                            + factoryClass.getName(), e);
        }

        nuxeoCmisServiceFactory.init(factoryParameters);
        return nuxeoCmisServiceFactory;
    }

}
