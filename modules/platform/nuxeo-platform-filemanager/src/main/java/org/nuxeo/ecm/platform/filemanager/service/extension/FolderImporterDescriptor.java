/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.filemanager.service.extension;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XRegistry;
import org.nuxeo.common.xmap.registry.XRegistryId;
import org.nuxeo.ecm.core.api.NuxeoException;

/**
 * Descriptor for folder import.
 */
@XObject("folderImporter")
@XRegistry
public class FolderImporterDescriptor {

    @XNode("@name")
    @XRegistryId
    protected String name;

    /**
     * @deprecated since 11.1.
     */
    @Deprecated(since = "11.1")
    @XNode("@class")
    protected String className;

    @XNode("@class")
    protected Class<? extends FolderImporter> klass;

    public String getName() {
        return name;
    }

    /**
     * @deprecated since 11.1.
     */
    @Deprecated(since = "11.1")
    public String getClassName() {
        return className;
    }

    /**
     * @since 11.1
     */
    public FolderImporter newInstance() {
        try {
            FolderImporter folderImporter = klass.getDeclaredConstructor().newInstance();
            folderImporter.setName(name);
            return folderImporter;
        } catch (ReflectiveOperationException e) {
            throw new NuxeoException(e);
        }
    }

}
