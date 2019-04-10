/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.template.samples.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.template.api.adapters.TemplateBasedDocument;
import org.nuxeo.template.api.adapters.TemplateSourceDocument;
import org.nuxeo.template.samples.importer.ModelImporter;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.platform.content.template", //
        "org.nuxeo.template.manager.api", //
        "org.nuxeo.template.manager", //
        "org.nuxeo.template.manager.jaxrs", //
        "studio.extensions.template-module-demo", //
        "org.nuxeo.template.manager.samples", //
        "org.nuxeo.template.manager.samples.no-init.test", //
})
public class TestImportModel {

    @Inject
    protected CoreSession session;

    DocumentModel rootDocument;

    DocumentModel workspace;

    DocumentModel docToExport;

    @Test
    public void testImportContentTemplateArchive() throws Exception {

        ModelImporter importer = new ModelImporter(session);

        int nbImportedDocs = importer.importModels();

        session.save();
        // check result

        StringBuffer sb = new StringBuffer();
        DocumentModelList docs = session.query("select * from Document where ecm:isVersion = 0 and ecm:mixinType in ('Template','TemplateBased') order by ecm:path");
        for (DocumentModel doc : docs) {
            sb.append("path: " + doc.getPathAsString() + " type: " + doc.getType() + " title:" + doc.getTitle()
                    + " name:" + doc.getName() + " uuid:" + doc.getId());
            TemplateBasedDocument templateDoc = doc.getAdapter(TemplateBasedDocument.class);
            if (templateDoc != null) {
                for (String tName : templateDoc.getTemplateNames()) {
                    sb.append(" target: " + tName + "-" + templateDoc.getSourceTemplateDocRef(tName));
                    assertTrue(session.exists(templateDoc.getSourceTemplateDocRef(tName)));
                }
            } else {
                TemplateSourceDocument source = doc.getAdapter(TemplateSourceDocument.class);
                assertNotNull(source);
            }
            sb.append("\n");
        }

        assertEquals(nbImportedDocs, docs.size());

        // String dump = sb.toString();
        // System.out.println("Import completed : " + docs.size() + " docs");
        // System.out.println(dump);

    }
}
