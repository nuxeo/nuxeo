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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import org.junit.BeforeClass;
import org.junit.Test;
import org.nuxeo.ecm.core.api.model.DeltaLong;
import org.nuxeo.ecm.core.storage.State;
import org.nuxeo.ecm.core.storage.State.ListDiff;
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

    @Test
    public void testDeltaUpdate() throws Exception {
        StateDiff stateDiff = new StateDiff();
        stateDiff.put("ecm:minorVersion", new DeltaLong(1, 1));
        String patch = UPDATE_BUILDER.apply(stateDiff).toString();
        assertXMLFileAgainstString("update-builder/delta-update.xml", patch);
    }

    @Test
    public void testListUpdate() throws Exception {
        ListDiff listDiff = new ListDiff();
        listDiff.diff = new ArrayList<>(3);
        listDiff.diff.add(null);
        listDiff.diff.add(State.NOP);
        StateDiff vignette = new StateDiff();
        vignette.put("width", 100L);
        listDiff.diff.add(vignette);
        listDiff.rpush = new ArrayList<>(1);
        vignette = new StateDiff();
        vignette.put("width", 200L);
        listDiff.rpush.add(vignette);
        StateDiff stateDiff = new StateDiff();
        stateDiff.put("vignettes", listDiff);
        String patch = UPDATE_BUILDER.apply(stateDiff).toString();
        assertXMLFileAgainstString("update-builder/list-update.xml", patch);
    }

}
