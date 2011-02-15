/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Leroy Merlin (http://www.leroymerlin.fr/) - initial implementation
 */

package org.nuxeo.opensocial.services;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.shindig.social.opensocial.spi.ActivityService;
import org.apache.shindig.social.opensocial.spi.AppDataService;
import org.apache.shindig.social.opensocial.spi.PersonService;
import org.nuxeo.runtime.api.Framework;

import com.google.inject.AbstractModule;

public class NuxeoServiceModule extends AbstractModule {

    private static final Log LOG = LogFactory.getLog(NuxeoServiceModule.class);

    @Override
    protected final void configure() {

        try {
            bind(PersonService.class).toInstance(
                    Framework.getService(PersonService.class));
            bind(ActivityService.class).toInstance(
                    Framework.getService(ActivityService.class));
            bind(AppDataService.class).toInstance(
                    Framework.getService(AppDataService.class));

        } catch (Exception e) {
            LOG.error("Unable to bind Shindig services to Nuxeo components");
            LOG.error(e.getMessage());
        }

    }

}
