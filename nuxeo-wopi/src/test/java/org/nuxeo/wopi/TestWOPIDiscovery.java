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
 *     Thomas Roger
 */

package org.nuxeo.wopi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.nuxeo.common.utils.FileUtils;

/**
 * @since 10.3
 */
public class TestWOPIDiscovery {

    protected static Map<String, String> EXPECTED_EXCEL_ACTIONS = new HashMap<>();

    protected static Map<String, String> EXPECTED_WORD_ACTIONS = new HashMap<>();

    static {
        EXPECTED_EXCEL_ACTIONS.put("view", "xlsx");
        EXPECTED_EXCEL_ACTIONS.put("edit", "xlsx");

        EXPECTED_WORD_ACTIONS.put("view", "docx");
        EXPECTED_WORD_ACTIONS.put("edit", "rtf");
    }

    @Test
    public void testReadDiscovery() {
        File discoveryFile = FileUtils.getResourceFileFromContext("test-discovery.xml");
        WOPIDiscovery discovery = WOPIDiscovery.read(discoveryFile);
        assertNotNull(discovery);
        List<WOPIDiscovery.App> apps = discovery.getNetZone().getApps();
        assertNotNull(apps);
        assertEquals(2, apps.size());

        checkApp(apps.get(0), "Excel", EXPECTED_EXCEL_ACTIONS);
        checkApp(apps.get(1), "Word", EXPECTED_WORD_ACTIONS);
    }

    protected void checkApp(WOPIDiscovery.App app, String expectedAppName, Map<String, String> expectedActions) {
        assertEquals(expectedAppName, app.getName());
        List<WOPIDiscovery.Action> actions = app.getActions();
        assertNotNull(actions);
        assertEquals(expectedActions.size(), actions.size());

        actions.forEach(action -> {
            assertEquals(expectedActions.get(action.getName()), action.getExt());
            assertTrue(action.getUrl().startsWith("https://"));
        });
    }
}
