/*
 * (C) Copyright 2014-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.directory.core;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.repository.RepositoryInitializationHandler;
import org.nuxeo.ecm.directory.DefaultDirectoryFactory;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;

/**
 * Factory implementation for directory on repository
 *
 * @since 8.2
 */
public class CoreDirectoryFactory extends DefaultDirectoryFactory {

    protected RepositoryInitializationHandler initializationHandler = new CoreDirectoryInitializationHandler();

    @Override
    public void activate(ComponentContext context) {
        initializationHandler.install();
    }

    @Override
    public void deactivate(ComponentContext context) {
        initializationHandler.uninstall();
    }

    /**
     * Finalizes Core Directories initialization as soon as the repositories are available.
     *
     * @since 10.3
     */
    public static class CoreDirectoryInitializationHandler extends RepositoryInitializationHandler {

        @Override
        public void doInitializeRepository(CoreSession coreSession) {
            DirectoryService directoryService = Framework.getService(DirectoryService.class);
            for (Directory directory : directoryService.getDirectories()) {
                if (!(directory instanceof CoreDirectory)) {
                    continue;
                }
                CoreDirectory coreDirectory = (CoreDirectory) directory;
                if (!coreDirectory.repositoryName.equals(coreSession.getRepositoryName())) {
                    continue;
                }
                coreDirectory.initializeCoreSession(coreSession);
            }
        }

    }

}
