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

package org.nuxeo.ecm.automation.core.operations.services.bulk.validation;

import static org.nuxeo.ecm.automation.core.operations.services.bulk.AbstractAutomationBulkAction.AutomationComputation.DOCS_INPUT_TYPE;
import static org.nuxeo.ecm.automation.core.operations.services.bulk.AbstractAutomationBulkAction.AutomationComputation.DOC_INPUT_TYPE;
import static org.nuxeo.ecm.automation.core.operations.services.bulk.AutomationBulkAction.OPERATION_ID;
import static org.nuxeo.ecm.automation.core.operations.services.bulk.AutomationBulkAction.OPERATION_PARAMETERS;

import java.util.Arrays;
import java.util.List;

import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationNotFoundException;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.core.bulk.AbstractBulkActionValidation;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 10.10
 */
public class AutomationBulkValidation extends AbstractBulkActionValidation {

    @Override
    protected List<String> getParametersToValidate() {
        return Arrays.asList(OPERATION_ID, OPERATION_PARAMETERS);
    }

    @Override
    protected void validateCommand(BulkCommand command) throws IllegalArgumentException {

        // Check for operationParameters
        validateMap(OPERATION_PARAMETERS, command);

        // Check for operation id if it exists, and if its input type is compatible
        validateString(OPERATION_ID, command);
        String operationId = command.getParam(OPERATION_ID);
        try {
            OperationType op = Framework.getService(AutomationService.class).getOperation(operationId);
            String inputType = op.getInputType();
            if (inputType != null && !DOC_INPUT_TYPE.equals(inputType) && !DOCS_INPUT_TYPE.equals(inputType)) {
                throw new IllegalArgumentException(
                        "Unsupported operation input type : " + inputType + " in command: " + command);
            }
        } catch (OperationNotFoundException e) {
            throw new IllegalArgumentException("Unknown operation id " + operationId + " in command: " + command);
        }

    }

}
