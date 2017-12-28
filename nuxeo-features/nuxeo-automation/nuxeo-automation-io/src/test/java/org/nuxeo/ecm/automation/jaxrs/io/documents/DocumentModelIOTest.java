/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     dmetzler
 */
package org.nuxeo.ecm.automation.jaxrs.io.documents;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.core.util.DocumentHelper;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @since 5.8-HF01
 */

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.automation.io" })
@LocalDeploy("org.nuxeo.ecm.automation.io:testdoc-core-contrib.xml")
public class DocumentModelIOTest {

    private static final String JSON = "{" + //
            "  \"myschema:list\": [" + //
            "    {" + //
            "      \"foo\":\"test1\"," + //
            "      \"bar\":\"test2\" " + //
            "    }" + //
            "  ]" + //
            "}";

    @Inject
    CoreSession session;

    @Test
    public void listOfComplexPropertiesAreDecoded() throws Exception {

        ObjectMapper om = new ObjectMapper();
        JsonNode jsonProps = om.readTree(JSON);

        Properties props = new Properties(jsonProps);

        assertPropertiesAreUpdatedOnDoc(props);

    }

    @SuppressWarnings("unchecked")
    private void assertPropertiesAreUpdatedOnDoc(Properties props) throws IOException {
        DocumentModel testDoc = session.createDocumentModel("/", "testDoc", "ComplexDoc");
        DocumentHelper.setProperties(null, testDoc, props);
        List<Map<String, String>> prop = (List<Map<String, String>>) testDoc.getPropertyValue("myschema:list");

        assertEquals(1, prop.size());
        assertEquals("test1", prop.get(0).get("foo"));
        assertEquals("test2", prop.get(0).get("bar"));
    }

}
