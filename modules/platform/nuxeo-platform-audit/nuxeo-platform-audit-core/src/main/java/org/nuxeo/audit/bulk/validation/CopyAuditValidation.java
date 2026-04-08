/*
 * (C) Copyright 2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.audit.bulk.validation;

import static org.nuxeo.audit.bulk.CopyAuditAction.PARAMETER_BACKEND_NAME;

import java.util.List;
import java.util.Objects;

import org.nuxeo.audit.service.AuditService;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.bulk.AbstractBulkActionValidation;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.query.QueryParseException;
import org.nuxeo.ecm.core.query.sql.SQLQueryParser;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 2025.19
 */
public class CopyAuditValidation extends AbstractBulkActionValidation {

    @Override
    protected List<String> getParametersToValidate() {
        return List.of(PARAMETER_BACKEND_NAME);
    }

    @Override
    protected void validateCommand(BulkCommand command) throws IllegalArgumentException {
        // Check target backend exists
        String targetBackendName = requireStringNonBlank(PARAMETER_BACKEND_NAME, command);
        checkBackendExists(targetBackendName);

        // Check only one source backend is submitted, and exists
        String sourceBackendName;
        try {
            var query = SQLQueryParser.parse(command.getQuery());
            if (query.getFromClause().count() != 1) {
                throw new IllegalArgumentException("Invalid query with multiple FROM: " + command.getQuery());
            }
            sourceBackendName = query.getFromClause().get(0);
        } catch (QueryParseException e) {
            throw new IllegalArgumentException("Invalid query: " + command.getQuery(), e);
        }
        checkBackendExists(sourceBackendName);

        // Check source and target backends are not the same
        if (Objects.equals(sourceBackendName, targetBackendName)) {
            throw new IllegalArgumentException(
                    "Source and target backends must be different, backend name: " + sourceBackendName);
        }
    }

    protected static void checkBackendExists(String backendName) {
        try {
            Framework.getService(AuditService.class).getAuditBackend(backendName);
        } catch (NuxeoException e) {
            throw new IllegalArgumentException("Unsupported backend name: " + backendName, e);
        }
    }
}
