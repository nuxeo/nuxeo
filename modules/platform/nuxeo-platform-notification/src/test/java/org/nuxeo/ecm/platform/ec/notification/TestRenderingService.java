/*
 * (C) Copyright 2007-2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thierry Martins <tmartins@nuxeo.com>
 */
package org.nuxeo.ecm.platform.ec.notification;

import static org.junit.Assert.assertTrue;

import java.util.Collection;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.ec.notification.email.NotificationsRenderingEngine;
import org.nuxeo.ecm.platform.rendering.RenderingResult;
import org.nuxeo.ecm.platform.rendering.RenderingService;
import org.nuxeo.ecm.platform.rendering.impl.DocumentRenderingContext;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.platform.notification")
@Deploy("org.nuxeo.ecm.platform.notification.tests:notification-contrib.xml")
public class TestRenderingService {

    private static final String MYDESCRIPTION = "mydescription";

    @Inject
    protected CoreSession session;

    @Test
    public void testTemplateRendering() throws Exception {
        DocumentModel doc = session.createDocument(session.createDocumentModel("/", "docOne", "File"));
        doc.setPropertyValue("dc:description", MYDESCRIPTION);

        DocumentRenderingContext context = new DocumentRenderingContext();
        context.setDocument(doc);
        context.put("Runtime", Framework.getRuntime());

        RenderingService rs = Framework.getService(RenderingService.class);
        rs.registerEngine(new NotificationsRenderingEngine("test-template-doc"));
        Collection<RenderingResult> results = rs.process(context);

        for (RenderingResult result : results) {
            assertTrue(((String)result.getOutcome()).contains(MYDESCRIPTION));
        }
    }

}
