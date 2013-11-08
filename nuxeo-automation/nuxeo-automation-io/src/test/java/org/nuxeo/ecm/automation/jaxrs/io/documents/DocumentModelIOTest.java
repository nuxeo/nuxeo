/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     dmetzler
 */
package org.nuxeo.ecm.automation.jaxrs.io.documents;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.core.util.DocumentHelper;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;

/**
 *
 *
 * @since 5.8-HF01
 */

@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class })
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
    private void assertPropertiesAreUpdatedOnDoc(Properties props)
            throws IOException, ClientException {
        DocumentModel testDoc = session.createDocumentModel("/", "testDoc",
                "ComplexDoc");
        DocumentHelper.setProperties(null, testDoc, props);
        List<Map<String, String>> prop = (List<Map<String, String>>) testDoc.getPropertyValue("myschema:list");

        assertEquals(1, prop.size());
        assertEquals("test1", prop.get(0).get("foo"));
        assertEquals("test2", prop.get(0).get("bar"));
    }

}
