/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 */

package org.nuxeo.ecm.core.management.test;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.management.storage.DocumentStoreHandler;

public class FakeDocumentStoreHandler implements DocumentStoreHandler {

    public String repositoryName;

    public static FakeDocumentStoreHandler testInstance;

    public FakeDocumentStoreHandler() {
        testInstance = this;
    }

    @Override
    public void onStorageInitialization(CoreSession session, DocumentRef rootletRef) {
        repositoryName = session.getRepositoryName();
    }

}
