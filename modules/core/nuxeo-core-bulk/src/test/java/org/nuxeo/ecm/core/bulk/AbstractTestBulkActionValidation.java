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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;

/**
 * @since 10.10
 */
public abstract class AbstractTestBulkActionValidation<T extends BulkActionValidation> {

    protected Class<T> actionValidationClass;

    public AbstractTestBulkActionValidation(Class<T> actionValidationClass) {
        this.actionValidationClass = actionValidationClass;
    }

    protected void assertInvalidCommand(BulkCommand command, String errorMessage) {
        try {
            T actionValidation = actionValidationClass.getDeclaredConstructor().newInstance();
            actionValidation.validate(command);
            fail("command " + command + " should have been invalid");
        } catch (ReflectiveOperationException e) {
            throw new NuxeoException("Cannot create validation class of type " + actionValidationClass.getName(), e);
        } catch (IllegalArgumentException e) {
            assertEquals(errorMessage, e.getMessage());
        }
    }

    protected BulkCommand.Builder createBuilder(String actionName, String query, String repository, String user) {
        return new BulkCommand.Builder(actionName, query, user).repository(repository);
    }
}
