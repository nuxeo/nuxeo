/*
 * (C) Copyright 2012-2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *      Vladimir Pasquier <vpasquier@nuxeo.com>
 *      Thibaud Arguillere <targuillere@nuxeo.com>
 */
package org.nuxeo.ecm.platform.tag;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.tag.operations.RemoveDocumentTags;
import org.nuxeo.ecm.platform.tag.operations.TagDocument;
import org.nuxeo.ecm.platform.tag.operations.UntagDocument;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import static org.junit.Assert.assertEquals;

@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, AutomationFeature.class })
@Deploy({ "org.nuxeo.ecm.platform.tag" })
public class TestTagOperations {

    private static final String TAG_1 = "1";

    private static final String TAG_2 = "2";

    private static final String TAG_3 = "3";

    private static final String TAGS = TAG_1 + "," + TAG_2 + ", " + TAG_3;

    private static final String TAGS_COMMA = TAG_1 + "," + TAG_2 + ", " + TAG_3 + ",";

    protected DocumentModel document;

    protected String docId;

    @Inject
    CoreSession session;

    @Inject
    AutomationService automationService;

    @Inject
    TagService tagService;

    @Test
    public void testTagOperationsSuite() throws Exception {
        // quick init
        document = session.createDocumentModel("/", "File", "File");
        document = session.createDocument(document);
        docId = document.getId();

        testTagDocument(TAGS);
        testRemoveTags();
        testTagDocument(TAGS_COMMA);
        testUntagDocument();
        testRemoveTags();
    }

    public void testTagDocument(String inputTags) throws Exception {
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(document);
        Map<String, Object> params = new HashMap<>();
        params.put("tags", inputTags);
        automationService.run(ctx, TagDocument.ID, params);
        List<Tag> tags = tagService.getDocumentTags(session, docId, "Administrator");
        assertEquals(TAG_1, tags.get(0).getLabel());
        assertEquals(3, tags.size());
    }

    public void testUntagDocument() throws Exception {
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(document);
        Map<String, Object> params = new HashMap<>();
        params.put("tags", TAG_1);
        automationService.run(ctx, UntagDocument.ID, params);
        List<Tag> tags = tagService.getDocumentTags(session, docId, "Administrator");
        assertEquals(TAG_2, tags.get(0).getLabel());
        assertEquals(2, tags.size());
    }

    public void testRemoveTags() throws Exception {
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(document);
        automationService.run(ctx, RemoveDocumentTags.ID);
        List<Tag> tags = tagService.getDocumentTags(session, docId, "Administrator");
        assertEquals(0, tags.size());
    }

}
