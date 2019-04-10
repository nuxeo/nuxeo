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
 *     Anahide Tchertchian
 */
package org.nuxeo.targetplatforms.core.service;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;

/**
 * Updater in charge of override directory changes.
 *
 * @since 5.7.1
 */
public abstract class DirectoryUpdater {

    public static final String DEFAULT_DIR = "targetPlatformOverrides";

    public static final String SCHEMA = "target_platform_override";

    public static final String DEPRECATED_PROP = "deprecated";

    public static final String ENABLED_PROP = "enabled";

    public static final String RESTRICTED_PROP = "restricted";

    public static final String TRIAL_PROP = "trial";

    public static final String DEFAULT_PROP = "default";

    protected String dirName;

    public DirectoryUpdater(String dirName) {
        super();
        this.dirName = dirName;
    }

    public abstract void run(DirectoryService service, Session session) throws ClientException;

    public void run() throws ClientException {
        Session session = null;
        try {
            // check if entry already exists
            DirectoryService service = Framework.getLocalService(DirectoryService.class);
            session = service.open(dirName);
            run(service, session);
        } catch (DirectoryException e) {
            throw new ClientException(e);
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

}
