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

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.nuxeo.ecm.admin.permissions.PermissionsPurgeWork;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelListCollector;
import org.nuxeo.ecm.core.api.CoreSession;
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

    @Param(name = "usernames", description = "Coma separate list of username")
    protected String usernames = "";

    @Context
    protected CoreSession session;

    @OperationMethod
    public void purgeDocs(List<DocumentModel> docs) {
        List<String> ancestorIds = docs.stream().map(DocumentModel::getId).collect(Collectors.toList());
        List<String> usernames = Arrays.asList(this.usernames.split(",\\s*"));

        DocumentModel searchDocument = session.createDocumentModel("PermissionsSearch");
        searchDocument.setPropertyValue("rs:ace_username", (Serializable) usernames);
        searchDocument.setPropertyValue("rs:ecm_ancestorIds", (Serializable) ancestorIds);

        new PermissionsPurgeWork(searchDocument).launch();
    }

    @OperationMethod
    public void purgeDoc(DocumentModel doc) {
        purgeDocs(Collections.singletonList(doc));
    }

    @OperationMethod
    public void purge() {
        purgeDocs(Collections.emptyList());
    }
}
