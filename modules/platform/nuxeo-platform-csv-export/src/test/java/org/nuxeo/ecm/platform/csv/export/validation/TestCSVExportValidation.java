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

package org.nuxeo.ecm.platform.csv.export.validation;

import static org.nuxeo.ecm.core.bulk.action.computation.SortBlob.SORT_PARAMETER;
import static org.nuxeo.ecm.core.bulk.action.computation.ZipBlob.ZIP_PARAMETER;
import static org.nuxeo.ecm.platform.csv.export.action.CSVExportAction.ACTION_NAME;
import static org.nuxeo.ecm.platform.csv.export.computation.CSVProjectionComputation.PARAM_LANG;
import static org.nuxeo.ecm.platform.csv.export.computation.CSVProjectionComputation.PARAM_SCHEMAS;
import static org.nuxeo.ecm.platform.csv.export.computation.CSVProjectionComputation.PARAM_XPATHS;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.bulk.AbstractTestBulkActionValidation;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 10.10
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
public class TestCSVExportValidation extends AbstractTestBulkActionValidation<CSVExportValidation> {

    public TestCSVExportValidation() {
        super(CSVExportValidation.class);
    }

    @Test
    public void testExportWithInvalidParams() {

        String query = "SELECT * FROM Document";
        String repository = "test";
        String user = "test";

        // Test unknown parameter
        BulkCommand command = createBuilder(ACTION_NAME, query, repository, user).param("unknown", "param")
                                                                                 .build();
        assertInvalidCommand(command, "Unknown parameter unknown in command: " + command);

        // Test sort and zip parameters
        command = createBuilder(ACTION_NAME, query, repository, user).param(SORT_PARAMETER, "fake")
                                                                     .build();
        assertInvalidCommand(command, "Invalid " + SORT_PARAMETER + " in command: " + command);

        command = createBuilder(ACTION_NAME, query, repository, user).param(ZIP_PARAMETER, "fake")
                                                                     .build();
        assertInvalidCommand(command, "Invalid " + ZIP_PARAMETER + " in command: " + command);

        // Test lang parameter
        command = createBuilder(ACTION_NAME, query, repository, user).param(PARAM_LANG, "fakeLang")
                                                                     .build();
        assertInvalidCommand(command, "Invalid " + PARAM_LANG + " in command: " + command);

        // Test invalid schemas and xpaths parameters
        command = createBuilder(ACTION_NAME, query, repository, user).param(PARAM_SCHEMAS, true)
                                                                     .build();
        assertInvalidCommand(command, "Invalid " + PARAM_SCHEMAS + " in command: " + command);

        command = createBuilder(ACTION_NAME, query, repository, user).param(PARAM_XPATHS, "notAList")
                                                                     .build();
        assertInvalidCommand(command, "Invalid " + PARAM_XPATHS + " in command: " + command);

        List<Object> fake = Arrays.asList(true, "false", 10);
        command = createBuilder(ACTION_NAME, query, repository, user).param(PARAM_SCHEMAS, (Serializable) fake)
                                                                     .build();
        assertInvalidCommand(command, "Invalid " + PARAM_SCHEMAS + " in command: " + command);
        command = createBuilder(ACTION_NAME, query, repository, user).param(PARAM_XPATHS, (Serializable) fake)
                                                                     .build();
        assertInvalidCommand(command, "Invalid " + PARAM_XPATHS + " in command: " + command);

        // Test unknown schema
        List<String> unknownSchema = Arrays.asList("dublincore", "wonderland");
        command = createBuilder(ACTION_NAME, query, repository, user).param(PARAM_SCHEMAS, (Serializable) unknownSchema)
                                                                     .build();
        assertInvalidCommand(command, "Unknown schema wonderland in command: " + command);

        // Test unknown xpath
        List<String> unknownXpath = Arrays.asList("dc:title", "dc:wonderland");
        command = createBuilder(ACTION_NAME, query, repository, user).param(PARAM_XPATHS, (Serializable) unknownXpath)
                                                                     .build();
        assertInvalidCommand(command, "Unknown xpath dc:wonderland in command: " + command);
    }
}
