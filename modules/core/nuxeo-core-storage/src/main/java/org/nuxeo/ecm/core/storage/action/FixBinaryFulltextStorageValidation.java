/*
 * (C) Copyright 2025 Nuxeo (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.ecm.core.storage.action;

import java.util.List;

import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.repository.FulltextConfiguration;
import org.nuxeo.ecm.core.bulk.AbstractBulkActionValidation;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 2023.27
 */
public class FixBinaryFulltextStorageValidation extends AbstractBulkActionValidation {

    @Override
    protected List<String> getParametersToValidate() {
        return List.of();
    }

    @Override
    protected void validateCommand(BulkCommand command) throws IllegalArgumentException {
        // mare sure repository is configured with fulltext "stored in blob"
        var session = CoreInstance.getCoreSession(command.getRepository());
        RepositoryService repositoryService = Framework.getService(RepositoryService.class);
        Repository repository = repositoryService.getRepository(session.getRepositoryName());
        FulltextConfiguration fulltextConfiguration = repository.getFulltextConfiguration();
        if (!fulltextConfiguration.isFulltextStoredInBlob()) {
            throw new IllegalArgumentException("The repository: " + command.getRepository()
                    + " is not configured with nuxeo.fulltext.storedInBlob=true");
        }
    }
}
