/*
 * (C) Copyright 2015-2019 Nuxeo (http://nuxeo.com/) and others.
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
import java.util.List;

import javax.inject.Inject;

import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 7.10
 */
@RunWith(FeaturesRunner.class)
public class TestPermissionsPurge extends AbstractPermissionsPurge {

    @Inject
    protected WorkManager workManager;

    @Override
    public void scheduleWork(List<String> usernames) {
        DocumentModel searchDocument = session.createDocumentModel("PermissionsSearch");
        searchDocument.setPropertyValue("rs:ace_username", (Serializable) usernames);

        workManager.schedule(new PermissionsPurgeWork(searchDocument));
    }
}
