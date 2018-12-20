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

package org.nuxeo.ecm.core.validation;

import static org.nuxeo.ecm.core.bulk.action.SetPropertiesAction.ACTION_NAME;
import static org.nuxeo.ecm.core.bulk.action.SetPropertiesAction.PARAM_DISABLE_AUDIT;
import static org.nuxeo.ecm.core.bulk.action.SetPropertiesAction.PARAM_VERSIONING_OPTION;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.bulk.AbstractTestBulkActionValidation;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.bulk.validation.SetPropertiesValidation;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 10.10
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
public class TestSetPropertiesValidation extends AbstractTestBulkActionValidation<SetPropertiesValidation> {

    public TestSetPropertiesValidation() {
        super(SetPropertiesValidation.class);
    }

    @Test
    public void testSetPropertiesInvalidParams() {
        String query = "SELECT * FROM Document";
        String repository = "test";
        String user = "test";
        BulkCommand command = createBuilder(ACTION_NAME, query, repository, user).param("fake:xpath", "fake")
                                                                                 .build();
        assertInvalidCommand(command, "Unknown xpath fake:xpath in command: " + command);

        command = createBuilder(ACTION_NAME, query, repository, user).param(PARAM_DISABLE_AUDIT, "fake")
                                                                     .build();
        assertInvalidCommand(command, "Invalid " + PARAM_DISABLE_AUDIT + " in command: " + command);

        command = createBuilder(ACTION_NAME, query, repository, user).param(PARAM_VERSIONING_OPTION, true)
                                                                     .build();
        assertInvalidCommand(command, "Invalid " + PARAM_VERSIONING_OPTION + " in command: " + command);
    }
}
