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

package org.nuxeo.ecm.core.bulk;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 10.10
 */
public abstract class AbstractBulkActionValidation implements BulkActionValidation {

    @Override
    public void validate(BulkCommand command) throws IllegalArgumentException {

        // Check for unknown parameter
        Set<String> params = command.getParams().keySet();
        List<String> actionParams = getParametersToValidate();
        for (String param : params) {
            if (!actionParams.contains(param)) {
                throw new IllegalArgumentException(unknownParameterMessage(param, command));
            }
        }
        validateCommand(command);
    }

    protected String invalidParameterMessage(String parameter, BulkCommand command) {
        return "Invalid " + parameter + " in command: " + command;
    }

    protected String unknownParameterMessage(String parameter, BulkCommand command) {
        return "Unknown parameter " + parameter + " in command: " + command;
    }

    protected void validateBoolean(String param, BulkCommand command) {
        Serializable value = command.getParam(param);
        if (value != null && !Boolean.TRUE.equals(value) && !Boolean.FALSE.equals(value)) {
            throw new IllegalArgumentException(invalidParameterMessage(param, command));
        }
    }

    protected void validateString(String param, BulkCommand command) {
        Serializable value = command.getParam(param);
        validateStringValue(param, value, command);
    }

    protected void validateStringValue(String param, Serializable value, BulkCommand command) {
        if (value != null && !(value instanceof String)) {
            throw new IllegalArgumentException(invalidParameterMessage(param, command));
        }
    }

    protected void validateMap(String param, BulkCommand command) {
        Serializable value = command.getParam(param);
        if (value != null && !(value instanceof Map)) {
            throw new IllegalArgumentException(invalidParameterMessage(param, command));
        }
    }

    protected void validateList(String param, BulkCommand command) {
        Serializable value = command.getParam(param);
        if (value != null && !(value instanceof List<?>)) {
            throw new IllegalArgumentException(invalidParameterMessage(param, command));
        }
    }

    protected void validateSchema(String param, Serializable value, BulkCommand command) {
        validateStringValue(param, value, command);
        String schema = (String) value;
        if (Framework.getService(SchemaManager.class).getSchema(schema) == null) {
            throw new IllegalArgumentException("Unknown schema " + schema + " in command: " + command);
        }
    }

    protected void validateXpath(String param, Serializable value, BulkCommand command) {
        validateStringValue(param, value, command);
        String xpath = (String) value;
        if (Framework.getService(SchemaManager.class).getField(xpath) == null) {
            throw new IllegalArgumentException("Unknown xpath " + xpath + " in command: " + command);
        }
    }

    /**
     * Returns the list of parameters to validate.
     */
    protected abstract List<String> getParametersToValidate();

    /**
     * Validates the command.
     */
    protected abstract void validateCommand(BulkCommand command) throws IllegalArgumentException;

}
