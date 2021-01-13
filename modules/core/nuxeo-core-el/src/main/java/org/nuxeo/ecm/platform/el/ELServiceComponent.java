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

import javax.el.ELContext;

import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Implementation for the service providing access to EL-related functions.
 *
 * @since 8.3
 */
public class ELServiceComponent extends DefaultComponent implements ELService {

    private static final String XP_EL_CONTEXT_FACTORY = "elContextFactory";

    protected static final ELContextFactory DEFAULT_EL_CONTEXT_FACTORY = new DefaultELContextFactory();

    protected ELContextFactory elContextFactory;

    @Override
    public void start(ComponentContext context) {
        elContextFactory = this.<ELContextFactoryDescriptor> getRegistryContribution(XP_EL_CONTEXT_FACTORY)
                               .map(ELContextFactoryDescriptor::newInstance)
                               .orElse(DEFAULT_EL_CONTEXT_FACTORY);
    }

    @Override
    public void stop(ComponentContext context) throws InterruptedException {
        elContextFactory = null;
    }

    @Override
    public ELContext createELContext() {
        return elContextFactory.get();
    }

}
