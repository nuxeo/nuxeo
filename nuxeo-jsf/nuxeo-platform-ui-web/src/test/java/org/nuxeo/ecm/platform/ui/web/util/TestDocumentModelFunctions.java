/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.ui.web.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.ui.web.tag.fn.DocumentModelFunctions;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * @since 5.9.3
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@LocalDeploy("org.nuxeo.ecm.core:schemas-test-contrib.xml")
public class TestDocumentModelFunctions {

    @Test
    @SuppressWarnings("rawtypes")
    public void testDefaultValue() throws Exception {
        Object blobVal = DocumentModelFunctions.defaultValue("files:files");
        assertTrue(blobVal instanceof HashMap);
        HashMap blobValMap = (HashMap) blobVal;
        assertEquals(1, blobValMap.size());
        assertTrue(blobValMap.containsKey("file"));
        assertNull(blobValMap.get("file"));

        Object stringListItemVal = DocumentModelFunctions.defaultValue("lds:listOfLists/stringListItem");
        assertTrue(stringListItemVal instanceof String);
        assertEquals("", stringListItemVal);

        Object complexListVal = DocumentModelFunctions.defaultValue("customschema:activities");
        assertTrue(complexListVal instanceof HashMap);
        HashMap complexListValMap = (HashMap) complexListVal;
        assertEquals(5, complexListValMap.size());
        assertTrue(complexListValMap.containsKey("performedby"));
        assertNull(complexListValMap.get("performedby"));
        assertTrue(complexListValMap.containsKey("name"));
        assertNull(complexListValMap.get("name"));
        assertTrue(complexListValMap.containsKey("actiontype"));
        assertNull(complexListValMap.get("actiontype"));
        assertTrue(complexListValMap.containsKey("decision"));
        assertNotNull(complexListValMap.get("decision"));
        assertTrue(complexListValMap.containsKey("hasdecision"));
        assertNull(complexListValMap.get("hasdecision"));

        // non regression test for NXP-14191
        Object subComplexListVal = DocumentModelFunctions.defaultValue("customschema:activities/decision/options");
        assertTrue(subComplexListVal instanceof HashMap);
        HashMap subComplexListValMap = (HashMap) subComplexListVal;
        assertEquals(2, subComplexListValMap.size());
        assertTrue(subComplexListValMap.containsKey("optionaction"));
        assertNotNull(subComplexListValMap.get("optionaction"));
        assertEquals("defaultAction", subComplexListValMap.get("optionaction"));
        assertTrue(subComplexListValMap.containsKey("optionlabel"));
        assertNull(subComplexListValMap.get("optionlabel"));

        Object complexVal = DocumentModelFunctions.defaultValue("lds:complexField");
        assertTrue(complexVal instanceof HashMap);
        HashMap complexValMap = (HashMap) complexVal;
        assertEquals(6, complexValMap.size());
        assertTrue(complexValMap.containsKey("stringComplexItem"));
        assertNotNull(complexValMap.get("stringComplexItem"));
        assertEquals("foo", complexValMap.get("stringComplexItem"));
        assertTrue(complexValMap.containsKey("dateComplexItem"));
        assertNull(complexValMap.get("dateComplexItem"));

        Object stringVal = DocumentModelFunctions.defaultValue("lds:textField");
        assertNull(stringVal);
        Object otherStringVal = DocumentModelFunctions.defaultValue("lds:anotherTextField");
        assertEquals("foo", otherStringVal);
    }

}
