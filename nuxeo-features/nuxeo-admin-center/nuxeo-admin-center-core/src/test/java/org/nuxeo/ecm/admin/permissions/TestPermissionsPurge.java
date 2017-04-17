/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.admin.permissions;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @since 7.10
 */
@RunWith(FeaturesRunner.class)
public class TestPermissionsPurge extends AbstractPermissionsPurge {

    @Override
    public void scheduleWork(List<String> usernames) {
        DocumentModel searchDocument = session.createDocumentModel("PermissionsSearch");
        searchDocument.setPropertyValue("rs:ace_username", (Serializable) usernames);

        TransactionHelper.commitOrRollbackTransaction();

        PermissionsPurgeWork work = new PermissionsPurgeWork(searchDocument);
        workManager.schedule(work, WorkManager.Scheduling.IF_NOT_RUNNING_OR_SCHEDULED);
    }
}
