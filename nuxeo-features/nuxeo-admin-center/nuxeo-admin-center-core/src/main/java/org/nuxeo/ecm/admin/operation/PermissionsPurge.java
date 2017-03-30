/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Arnaud Kervern
 */

package org.nuxeo.ecm.admin.operation;

import org.nuxeo.ecm.admin.permissions.PermissionsPurgeWork;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Schedule a {@link PermissionsPurgeWork} to archive ACEs based on permissions_purge page provider from the input
 * document.
 *
 * @since 9.1
 */
@Operation(id = PermissionsPurge.ID, category = Constants.CAT_SERVICES, label = "Archiving ACEs", description = "Schedule a work to archive ACEs based on permissions_purge page provider from the input document.")
public class PermissionsPurge {
    static final String ID = "PermissionsPurge";

    @OperationMethod
    public void purge(DocumentModel doc) {
        new PermissionsPurgeWork(doc).launch();
    }
}
