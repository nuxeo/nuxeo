/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     qlamerand
 */
package org.nuxeo.ecm.automation.core.test;

import static org.junit.Assert.assertEquals;
import static org.nuxeo.ecm.directory.localconfiguration.DirectoryConfigurationConstants.DIRECTORY_CONFIGURATION_FIELD;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
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
import org.nuxeo.ecm.core.test.annotations.BackendType;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;

/**
 * @author <a href="mailto:qlamerand@nuxeo.com">Quentin Lamerand</a>
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(repositoryName = "default", type = BackendType.H2, init = DefaultRepositoryInit.class, user = "Administrator", cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.directory.api", "org.nuxeo.ecm.directory",
        "org.nuxeo.ecm.directory.sql", "org.nuxeo.ecm.directory.types.contrib",
        "org.nuxeo.ecm.automation.core", "org.nuxeo.ecm.automation.features" })
@LocalDeploy("org.nuxeo.ecm.automation.features:test-directories-sql-contrib.xml")
public class DirectoryEntriesTest {

    @Inject
    protected CoreSession session;

    @Inject
    protected DirectoryService directoryService;

    @Inject
    protected LocalConfigurationService localConfigurationService;

    @Inject
    AutomationService service;

    protected static final String continentContentJson = "[{\"id\":\"europe\",\"obsolete\":0,\"ordering\":10000000,\"label\":\"label.directories.continent.europe\"},{\"id\":\"africa\",\"obsolete\":0,\"ordering\":10000000,\"label\":\"label.directories.continent.africa\"},{\"id\":\"north-america\",\"obsolete\":0,\"ordering\":10000000,\"label\":\"label.directories.continent.north-america\"},{\"id\":\"south-america\",\"obsolete\":0,\"ordering\":10000000,\"label\":\"label.directories.continent.south-america\"},{\"id\":\"asia\",\"obsolete\":0,\"ordering\":10000000,\"label\":\"label.directories.continent.asia\"},{\"id\":\"oceania\",\"obsolete\":0,\"ordering\":10000000,\"label\":\"label.directories.continent.oceania\"},{\"id\":\"antarctica\",\"obsolete\":0,\"ordering\":10000000,\"label\":\"label.directories.continent.antarctica\"}]";

    protected static final String continentLocalContentJson = "[{\"id\":\"atlantis\",\"obsolete\":0,\"ordering\":10000000,\"label\":\"Atlantis\"},{\"id\":\"middleearth\",\"obsolete\":0,\"ordering\":10000000,\"label\":\"Middle-earth\"},{\"id\":\"mu\",\"obsolete\":0,\"ordering\":10000000,\"label\":\"Mu\"}]";

    protected static String sortJson(String json) {
        String a = json.substring(2, json.length() - 2); // remove [{ and }]
        String[] b = a.split("\\},\\{"); // remove },{ and split
        List<String> c = new ArrayList<String>(Arrays.asList(b));
        Collections.sort(c);
        return "[{" + StringUtils.join(c, "},{") + "}]";
    }

    @Test
    public void testGlobalDirectoryEntries() throws Exception {
        StringBlob result = getDirectoryEntries(session.getDocument(new PathRef("/default-domain/workspaces/test")));
        assertEquals(sortJson(continentContentJson),
                sortJson(result.getString()));
    }

    @Test
    public void testLocalDirectoryEntries() throws Exception {
        DocumentModel doc = session.getDocument(new PathRef("/default-domain/workspaces/test"));
        doc.setPropertyValue(DIRECTORY_CONFIGURATION_FIELD, "local");
        doc = session.saveDocument(doc);
        session.save();

        StringBlob result = getDirectoryEntries(doc);
        assertEquals(sortJson(continentLocalContentJson),
                sortJson(result.getString()));
    }

    protected StringBlob getDirectoryEntries(DocumentModel doc) throws Exception {
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(doc);

        OperationChain chain = new OperationChain("fakeChain");
        chain.add(FetchContextDocument.ID);
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("directoryName", "continent");
        OperationParameters oparams = new OperationParameters(
                GetDirectoryEntries.ID, params);
        chain.add(oparams);

        return (StringBlob) service.run(ctx, chain);
    }
}
