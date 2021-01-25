/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     "Stephane Lacoin (aka matic) <slacoin@nuxeo.org>"
 */
package org.nuxeo.ecm.core.persistence;

import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author Stephane Lacoin [aka matic]
 */
public class PersistenceComponent extends DefaultComponent
        implements HibernateConfigurator, PersistenceProviderFactory {

    protected static final String XP = "hibernate";

    @Override
    public int getApplicationStartedOrder() {
        return 50; // even before repository init
    }

    @Override
    public void start(ComponentContext context) {
        /*
         * Initialize all the persistence units synchronously at startup, otherwise init may end up being called during
         * the first asynchronous event, which means hibernate init may happen in parallel with the main Nuxeo startup
         * thread which may be doing the hibernate init for someone else (JBPM for instance).
         */
        this.<HibernateConfiguration> getRegistryContributions(XP).forEach(desc -> {
            PersistenceProvider pp = new PersistenceProvider(desc);
            pp.openPersistenceUnit(); // creates tables etc.
            pp.closePersistenceUnit();
        });
    }

    @Override
    public PersistenceProvider newProvider(String name) {
        return new PersistenceProvider(getHibernateConfiguration(name));
    }

    @Override
    public HibernateConfiguration getHibernateConfiguration(String name) {
        return this.<HibernateConfiguration> getRegistryContribution(XP, name)
                   .orElseThrow(() -> new PersistenceError(
                           String.format("No hibernate configuration identified by '%s' is available", name)));
    }

}
