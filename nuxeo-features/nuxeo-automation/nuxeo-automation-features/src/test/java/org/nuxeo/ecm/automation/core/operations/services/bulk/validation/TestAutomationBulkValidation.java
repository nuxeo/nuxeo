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

import static org.nuxeo.ecm.automation.core.operations.services.bulk.AutomationBulkAction.ACTION_NAME;
import static org.nuxeo.ecm.automation.core.operations.services.bulk.AutomationBulkAction.OPERATION_ID;
import static org.nuxeo.ecm.automation.core.operations.services.bulk.AutomationBulkAction.OPERATION_PARAMETERS;
import java.util.ArrayList;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.bulk.AbstractTestBulkActionValidation;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 10.10
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.automation.core")
public class TestAutomationBulkValidation extends AbstractTestBulkActionValidation<AutomationBulkValidation> {

    public TestAutomationBulkValidation() {
        super(AutomationBulkValidation.class);
    }

    @Test
    public void testAutomationActionInvalidParams() {
        String query = "SELECT * FROM Document";
        String repository = "test";
        String user = "test";
        BulkCommand command = createBuilder(ACTION_NAME, query, repository, user).param(OPERATION_ID, "fake")
                                                                                 .build();
        assertInvalidCommand(command, "Unknown operation id fake in command: " + command);

        command = createBuilder(ACTION_NAME, query, repository, user).param(OPERATION_ID, new ArrayList<>())
                                                                     .build();
        assertInvalidCommand(command, "Invalid " + OPERATION_ID + " in command: " + command);

        command = createBuilder(ACTION_NAME, query, repository, user).param(OPERATION_PARAMETERS, false)
                                                                     .build();
        assertInvalidCommand(command, "Invalid " + OPERATION_PARAMETERS + " in command: " + command);
    }

}
