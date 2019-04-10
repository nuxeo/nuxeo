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
 *     Mariana Cedica
 */
package org.nuxeo.adobe.cc;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(AutomationFeature.class)
@RepositoryConfig(init = DefaultRepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.adobe.cc.nuxeo-adobe-connector-core")
public class TestSetCompoundDocs {

    @Inject
    protected CoreSession session;

    @Inject
    protected AutomationService automationService;

    @Test
    @Ignore
    // Ignore in 9.10 because of NXP-24924
    public void shouldsetCompoundDocs() throws OperationException {
        DocumentModel doc = session.createDocumentModel("/default-domain/workspaces", "folder", "Folder");
        doc = session.createDocument(doc);
        doc = session.saveDocument(doc);
        List<String> compoundDocs = new ArrayList<>();

        DocumentModel file = session.createDocumentModel("/default-domain/workspaces/folder/", "file", "File");
        file = session.createDocument(file);
        file = session.saveDocument(file);
        compoundDocs.add(file.getId());

        doc = session.createDocumentModel("/default-domain/workspaces/folder/", "compoundDoc", "File");
        doc = session.createDocument(doc);
        doc = session.saveDocument(doc);

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(doc);
        Map<String, Object> params = new HashMap<>();
        params.put("compoundDocs", compoundDocs);

        doc = (DocumentModel) automationService.run(ctx, CompoundAttach.ID, params);
        String[] docs = (String[]) doc.getPropertyValue("compound:docs");
        assertEquals(1, docs.length);
        assertEquals(file.getId(), docs[0]);
    }
}
