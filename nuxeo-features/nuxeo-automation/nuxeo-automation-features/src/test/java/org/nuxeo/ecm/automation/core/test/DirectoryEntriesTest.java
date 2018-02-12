/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     qlamerand
 */
package org.nuxeo.ecm.automation.core.test;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.directory.test.DirectoryFeature;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationParameters;
import org.nuxeo.ecm.automation.core.operations.FetchContextDocument;
import org.nuxeo.ecm.automation.core.operations.services.GetDirectoryEntries;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.api.localconfiguration.LocalConfigurationService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.directory.localconfiguration.DirectoryConfigurationConstants;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

/**
 * @author <a href="mailto:qlamerand@nuxeo.com">Quentin Lamerand</a>
 */
@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, DirectoryFeature.class })
@RepositoryConfig(init = DefaultRepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.automation.io")
@Deploy("org.nuxeo.ecm.automation.features")
@Deploy("org.nuxeo.ecm.automation.features:test-directories-sql-contrib.xml")
public class DirectoryEntriesTest {

    @Inject
    protected CoreSession session;

    @Inject
    protected DirectoryService directoryService;

    @Inject
    protected LocalConfigurationService localConfigurationService;

    @Inject
    AutomationService service;

    protected static final String continentContentJson = "["
            + "{\"id\":\"europe\",\"obsolete\":0,\"ordering\":10000000,\"label\":\"label.directories.continent.europe\"},"
            + "{\"id\":\"africa\",\"obsolete\":0,\"ordering\":10000000,\"label\":\"label.directories.continent.africa\"},"
            + "{\"id\":\"north-america\",\"obsolete\":0,\"ordering\":10000000,\"label\":\"label.directories.continent.north-america\"},"
            + "{\"id\":\"south-america\",\"obsolete\":0,\"ordering\":10000000,\"label\":\"label.directories.continent.south-america\"},"
            + "{\"id\":\"asia\",\"obsolete\":0,\"ordering\":10000000,\"label\":\"label.directories.continent.asia\"},"
            + "{\"id\":\"oceania\",\"obsolete\":0,\"ordering\":10000000,\"label\":\"label.directories.continent.oceania\"},"
            + "{\"id\":\"antarctica\",\"obsolete\":0,\"ordering\":10000000,\"label\":\"label.directories.continent.antarctica\"}"
            + "]";

    protected static final String continentLocalContentJson = "["
            + "{\"id\":\"atlantis\",\"obsolete\":0,\"ordering\":10000000,\"label\":\"Atlantis\"},"
            + "{\"id\":\"middleearth\",\"obsolete\":0,\"ordering\":10000000,\"label\":\"Middle-earth\"},"
            + "{\"id\":\"mu\",\"obsolete\":0,\"ordering\":10000000,\"label\":\"Mu\"}]";

    @Test
    public void testGlobalDirectoryEntries() throws Exception {
        StringBlob result = getDirectoryEntries(session.getDocument(new PathRef("/default-domain/workspaces/test")));
        JSONAssert.assertEquals(continentContentJson, result.getString(), JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    public void testLocalDirectoryEntries() throws Exception {
        DocumentModel doc = session.getDocument(new PathRef("/default-domain/workspaces/test"));
        doc.setPropertyValue(DirectoryConfigurationConstants.DIRECTORY_CONFIGURATION_FIELD, "local");
        doc = session.saveDocument(doc);
        session.save();

        StringBlob result = getDirectoryEntries(doc);
        JSONAssert.assertEquals(continentLocalContentJson, result.getString(), JSONCompareMode.NON_EXTENSIBLE);
    }

    protected StringBlob getDirectoryEntries(DocumentModel doc) throws Exception {
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(doc);

        OperationChain chain = new OperationChain("fakeChain");
        chain.add(FetchContextDocument.ID);
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("directoryName", "continent");
        OperationParameters oparams = new OperationParameters(GetDirectoryEntries.ID, params);
        chain.add(oparams);

        return (StringBlob) service.run(ctx, chain);
    }
}
