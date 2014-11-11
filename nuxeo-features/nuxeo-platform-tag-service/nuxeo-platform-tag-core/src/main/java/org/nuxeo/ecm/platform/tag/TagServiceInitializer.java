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
 *     "Stephane Lacoin (aka matic) <slacoin@nuxeo.com>"
 */
package org.nuxeo.ecm.platform.tag;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.persistence.HibernateConfiguration;
import org.nuxeo.ecm.core.persistence.HibernateConfigurator;
import org.nuxeo.ecm.core.repository.RepositoryInitializationHandler;
import org.nuxeo.ecm.platform.tag.persistence.TagSchemaUpdater;
import org.nuxeo.runtime.api.Framework;

/**
 * @author "Stephane Lacoin (aka matic) <slacoin@nuxeo.com>"
 *
 */
public class TagServiceInitializer extends RepositoryInitializationHandler {

    @Override
    public void doInitializeRepository(CoreSession session) throws ClientException {
        HibernateConfigurator configurator = Framework.getLocalService(HibernateConfigurator.class);
        HibernateConfiguration configuration = configurator.getHibernateConfiguration("nxtags");
        TagSchemaUpdater updater = new TagSchemaUpdater(configuration.hibernateProperties);
        updater.update();
    }

}
