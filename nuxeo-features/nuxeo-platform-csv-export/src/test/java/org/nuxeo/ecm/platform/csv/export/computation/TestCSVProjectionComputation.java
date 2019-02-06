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
 *     pierre
 */
package org.nuxeo.ecm.platform.csv.export.computation;

import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.nuxeo.ecm.platform.csv.export.action.CSVExportAction.ACTION_NAME;
import static org.nuxeo.ecm.platform.csv.export.computation.CSVProjectionComputation.PARAM_LANG;
import static org.nuxeo.ecm.platform.csv.export.computation.CSVProjectionComputation.PARAM_SCHEMAS;
import static org.nuxeo.ecm.platform.csv.export.computation.CSVProjectionComputation.PARAM_XPATHS;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;

/**
 * @since 10.3
 */
public class TestCSVProjectionComputation {

    // do not re-order
    public static final List<Serializable> getValues() {
        List<Serializable> values = new ArrayList<>();
        values.add(null);
        values.add(Boolean.TRUE);
        values.add("toto");
        values.add("toto, tutu, tata");
        values.add(new String[] { "tutu", "toto" });
        values.add(new String[] { "tutu", null, "toto" });
        values.add(new ArrayList<>(Arrays.asList("true", "false")));
        return values;
    }

    private static class TestableComputation extends CSVProjectionComputation {
        private TestableComputation(BulkCommand command) {
            this.command = command;
        }
    }

    @Test
    public void testListParameters() {
        List<Serializable> values = getValues();
        List<Object> expected = new ArrayList<>();
        expected.add(emptyList());
        expected.add(emptyList());
        expected.add(emptyList());
        expected.add(emptyList());
        expected.add(emptyList());
        expected.add(emptyList());
        expected.add(Arrays.asList("false", "true"));
        // tests
        testParamParsing(PARAM_SCHEMAS, values, expected);
        testParamParsing(PARAM_XPATHS, values, expected);
    }

    @Test
    public void testStringParameter() {
        List<Object> expected = new ArrayList<>();
        expected.add(null);
        expected.add(null);
        expected.add("toto");
        expected.add("toto, tutu, tata");
        expected.add(null);
        expected.add(null);
        expected.add(null);
        // tests
        testParamParsing(PARAM_LANG, getValues(), expected);
    }

    protected void testParamParsing(String param, List<Serializable> values, List<Object> expected) {
        for (int i = 0; i < values.size(); i++) {
            Serializable value = values.get(i);
            BulkCommand command = new BulkCommand.Builder(ACTION_NAME, "query", "user").param(param, value).build();
            CSVProjectionComputation computation = new TestableComputation(command);
            computation.startBucket(null);
            assertEquals("failed for '" + value + "'", expected.get(i), computation.renderingCtx.getParameter(param));
        }
    }

}
