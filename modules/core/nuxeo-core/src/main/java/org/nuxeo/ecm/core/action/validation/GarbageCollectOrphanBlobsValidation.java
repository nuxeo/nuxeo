/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard
 */
package org.nuxeo.ecm.core.action.validation;

import static org.nuxeo.ecm.core.action.GarbageCollectOrphanBlobsAction.DRY_RUN_PARAM;

import java.util.List;

import org.nuxeo.ecm.core.blob.DocumentBlobManager;
import org.nuxeo.ecm.core.bulk.AbstractBulkActionValidation;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 2023
 */
public class GarbageCollectOrphanBlobsValidation extends AbstractBulkActionValidation {

    @Override
    protected List<String> getParametersToValidate() {
        return List.of(DRY_RUN_PARAM);
    }

    @Override
    protected void validateCommand(BulkCommand command) throws IllegalArgumentException {
        validateBoolean(DRY_RUN_PARAM, command);
        Framework.getService(DocumentBlobManager.class).checkCanDeleteBlob(command.getRepository());
    }

}
