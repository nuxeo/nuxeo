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
 *     Guillaume Renard
 */
package org.nuxeo.elasticsearch.test;

import java.util.stream.Collectors;

import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.storage.sql.jdbc.NXQLQueryMaker;
import org.nuxeo.runtime.api.Framework;
import org.skyscreamer.jsonassert.JSONAssert;

/**
 * @since 2023.47
 */
public class ESTestUtils {

    public static void assertEqualsEvenUnderWindows(String expected, String actual) {
        JSONAssert.assertEquals(expected, actual, true);
    }

    public static String getAllDocumentPrimaryTypeClauseValue() {
        return Framework.getService(SchemaManager.class)
                        .getDocumentTypeNamesExtending(NXQLQueryMaker.TYPE_DOCUMENT)
                        .stream()
                        .map(t -> "\"" + t + "\"")
                        .collect(Collectors.joining(",\n"));
    }
}
