/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.targetplatforms.core.service;

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

    public abstract void run(DirectoryService service, Session session);

    public void run() {
        Session session = null;
        try {
            // check if entry already exists
            DirectoryService service = Framework.getService(DirectoryService.class);
            session = service.open(dirName);
            run(service, session);
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

}
