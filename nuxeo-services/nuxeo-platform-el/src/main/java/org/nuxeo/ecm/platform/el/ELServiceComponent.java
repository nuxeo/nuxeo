/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.el;

import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

import java.util.ArrayList;
import java.util.List;

import javax.el.ELContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Implementation for the service providing access to EL-related functions.
 *
 * @since 8.3
 */
public class ELServiceComponent extends DefaultComponent implements ELService {

    private static final Log log = LogFactory.getLog(ELServiceComponent.class);

    private static final String XP_EL_CONTEXT_FACTORY = "elContextFactory";

    protected static final ELContextFactory DEFAULT_EL_CONTEXT_FACTORY = new DefaultELContextFactory();

    protected List<ELContextFactoryDescriptor> elContextFactoryDescriptors;

    protected ELContextFactory elContextFactory = DEFAULT_EL_CONTEXT_FACTORY;

    @Override
    public void activate(ComponentContext context) {
        elContextFactoryDescriptors = new ArrayList<>(1);
    }

    @Override
    public void deactivate(ComponentContext context) {
        elContextFactoryDescriptors.clear();
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (XP_EL_CONTEXT_FACTORY.equals(extensionPoint)) {
            ELContextFactoryDescriptor desc = (ELContextFactoryDescriptor) contribution;
            log.info("Registered ELContextFactory: " + desc.klass.getName());
            registerELContextFactoryDescriptor(desc);
        } else {
            throw new NuxeoException("Unknown extension point: " + extensionPoint);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (XP_EL_CONTEXT_FACTORY.equals(extensionPoint)) {
            ELContextFactoryDescriptor desc = (ELContextFactoryDescriptor) contribution;
            log.info("Unregistered ELContextFactory: " + desc.klass.getName());
            unregisterELContextFactoryDescriptor(desc);
        } else {
            throw new NuxeoException("Unknown extension point: " + extensionPoint);
        }
    }

    public void registerELContextFactoryDescriptor(ELContextFactoryDescriptor desc) {
        elContextFactoryDescriptors.add(desc);
        elContextFactory = desc.newInstance();
    }

    public void unregisterELContextFactoryDescriptor(ELContextFactoryDescriptor desc) {
        elContextFactoryDescriptors.remove(desc);
        if (elContextFactoryDescriptors.isEmpty()) {
            elContextFactory = DEFAULT_EL_CONTEXT_FACTORY;
        } else {
            desc = elContextFactoryDescriptors.get(elContextFactoryDescriptors.size() - 1);
            elContextFactory = desc.newInstance();
        }
    }

    @Override
    public ELContext createELContext() {
        return elContextFactory.get();
    }

}
