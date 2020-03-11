/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.directory;

import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Default implementation for a directory factory component, that simply delegates registration of descriptors to the
 * {@link DirectoryService}.
 *
 * @since 8.2
 */
public class DefaultDirectoryFactory extends DefaultComponent {

    public static final String DIRECTORIES_XP = "directories";

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (!DIRECTORIES_XP.equals(extensionPoint)) {
            throw new NuxeoException("Unknown extension point: " + extensionPoint);
        }
        BaseDirectoryDescriptor descriptor = (BaseDirectoryDescriptor) contribution;
        Framework.getService(DirectoryService.class).registerDirectoryDescriptor(descriptor);
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (!DIRECTORIES_XP.equals(extensionPoint)) {
            return;
        }
        BaseDirectoryDescriptor descriptor = (BaseDirectoryDescriptor) contribution;
        Framework.getService(DirectoryService.class).unregisterDirectoryDescriptor(descriptor);
    }

}
