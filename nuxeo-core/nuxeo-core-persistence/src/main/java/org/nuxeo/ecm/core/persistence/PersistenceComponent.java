/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     "Stephane Lacoin (aka matic) <slacoin@nuxeo.org>"
 */
package org.nuxeo.ecm.core.persistence;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author "Stephane Lacoin (aka matic) <slacoin@nuxeo.org>"
 *
 */
public class PersistenceComponent extends DefaultComponent  implements HibernateConfigurator, PersistenceProviderFactory  {

    protected final Map<String,HibernateConfiguration> registry =
        new HashMap<String,HibernateConfiguration>();


    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) throws Exception {
        if ("hibernate".equals(extensionPoint)) {
            registerHibernateContribution((HibernateConfiguration)contribution, contributor.getName());
        }
    }

    protected void registerHibernateContribution(HibernateConfiguration contribution, ComponentName contributorName) {
        if (contribution.name == null) {
            throw new PersistenceError(contributorName + " should set the 'name' attribute of hibernate configurations");
        }
        if (!registry.containsKey(contribution.name)) {
            registry.put(contribution.name, contribution);
        } else {
            registry.get(contribution.name).merge(contribution);
        }
    }

    public PersistenceProvider newProvider(String name) {
        EntityManagerFactoryProvider emfProvider = registry.get(name);
        if (emfProvider == null) {
            throw new PersistenceError("no hibernate configuration identified by '" + name + "' is available");
        }
        return new PersistenceProvider(emfProvider);
    }

    public HibernateConfiguration getHibernateConfiguration(String name) {
        HibernateConfiguration config = registry.get(name);
        if (config == null) {
            throw new PersistenceError("no hibernate configuration identified by '" + name + "' is available");
        }
        return config;
    }

}
