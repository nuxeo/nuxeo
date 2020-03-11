/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 */

package org.nuxeo.ecm.core.bulk.validation;

import static org.nuxeo.ecm.core.bulk.action.SetPropertiesAction.PARAM_DISABLE_AUDIT;
import static org.nuxeo.ecm.core.bulk.action.SetPropertiesAction.PARAM_VERSIONING_OPTION;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.bulk.AbstractBulkActionValidation;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;

/**
 * @since 10.10
 */
public class SetPropertiesValidation extends AbstractBulkActionValidation {

    @Override
    public void validate(BulkCommand command) throws IllegalArgumentException {

        Set<String> params = new HashSet<>(command.getParams().keySet());
        params.removeAll(getParametersToValidate());
        // Check for unknown xpath
        for (String param : params) {
            validateXpath(null, param, command);
        }
        validateCommand(command);

    }

    @Override
    protected List<String> getParametersToValidate() {
        return Arrays.asList(PARAM_DISABLE_AUDIT, PARAM_VERSIONING_OPTION);
    }

    @Override
    protected void validateCommand(BulkCommand command) throws IllegalArgumentException {

        // Check for disableAudit parameter
        validateBoolean(PARAM_DISABLE_AUDIT, command);

        // Check for versioningOption parameter
        validateString(PARAM_VERSIONING_OPTION, command);
        String versioningOption = command.getParam(PARAM_VERSIONING_OPTION);
        if (versioningOption != null
                && Stream.of(VersioningOption.values()).noneMatch(op -> versioningOption.equals(op.name()))) {
            throw new IllegalArgumentException(invalidParameterMessage(PARAM_VERSIONING_OPTION, command));
        }
    }
}
