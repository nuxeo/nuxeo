/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.service.impl;

import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.adapter.impl.DefaultTopLevelFolderItem;
import org.nuxeo.drive.service.TopLevelFolderItemFactory;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

/**
 * Default implementation of a {@link TopLevelFolderItemFactory}.
 *
 * @author Antoine Taillefer
 */
public class DefaultTopLevelFolderItemFactory extends AbstractVirtualFolderItemFactory implements
        TopLevelFolderItemFactory {

    /*---------------------- VirtualFolderItemFactory ---------------*/
    @Override
    public FolderItem getVirtualFolderItem(NuxeoPrincipal principal) {
        return getTopLevelFolderItem(principal);
    }

    /*----------------------- TopLevelFolderItemFactory ---------------------*/
    @Override
    public FolderItem getTopLevelFolderItem(NuxeoPrincipal principal) {
        return new DefaultTopLevelFolderItem(getName(), principal, getFolderName());
    }

}
