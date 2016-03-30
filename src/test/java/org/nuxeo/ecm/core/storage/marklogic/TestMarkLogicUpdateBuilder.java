/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Kevin Leturc
 */
package org.nuxeo.ecm.core.storage.marklogic;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;

import org.junit.BeforeClass;
import org.junit.Test;
import org.nuxeo.ecm.core.storage.State.StateDiff;

public class TestMarkLogicUpdateBuilder extends AbstractTest {

    private static MarkLogicUpdateBuilder UPDATE_BUILDER;

    @BeforeClass
    public static void beforeClass() {
        AbstractTest.beforeClass();
        UPDATE_BUILDER = new MarkLogicUpdateBuilder(CLIENT.newXMLDocumentManager()::newPatchBuilder);
    }

    @Test
    public void testBasicUpdate() throws Exception {
        StateDiff stateDiff = new StateDiff();
        Calendar modified = Calendar.getInstance();
        modified.setTime(Date.from(LocalDateTime.parse("1970-01-01T00:00:00.000", DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                                                .atZone(ZoneId.systemDefault())
                                                .toInstant()));
        stateDiff.put("dc:modified", modified);
        stateDiff.put("dc:title", "The title");
        stateDiff.put("ecm:fulltextJobId", "6306c216-ef0c-4168-9a36-14ef96ebbcce");
        stateDiff.put("ecm:acl", new String[] { "Administrator", "Members" });
        String patch = UPDATE_BUILDER.apply(stateDiff).toString();
        assertXMLFileAgainstString("update-builder/basic-update.xml", patch);
    }

}
