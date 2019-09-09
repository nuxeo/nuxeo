/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nour AL KOTOB
 */
package org.nuxeo.ecm.automation.core.operations.document;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.automation.core:OSGI-INF/copy-schema-test-contrib.xml")
@RepositoryConfig(cleanup = Granularity.METHOD)
public class CopyDocumentTest {

    public static final String COPY_DOC_NAME = "CopyDoc";

    public static final String ROOT = "/";

    public static final String TARGET_PROPERTY_KEY = "target";

    public static final String NAME_PROPERTY_KEY = "name";

    public static final String SPECIAL_CHILD_DOC_NAME = "specialChildDoc";

    public static final String REGULAR_UNDER_SPECIAL_CHILD_DOC_NAME = "regularUnderSpecialChildDoc";

    public static final String REGULAR_CHILD_DOC_NAME = "regularChildDoc";

    public static final String COMMENT_ROOT_TYPE = "CommentRoot";

    public static final String FILE = "File";

    @Inject
    protected CoreSession session;

    @Inject
    protected AutomationService automationService;

    @Inject
    protected TransactionalFeature txFeature;

    @Test
    public void testCopyWithoutExcludedType() throws OperationException {
        DocumentModel file = session.createDocumentModel(ROOT, "SourceDoc", FILE);
        file = session.createDocument(file);

        DocumentModel specialChildDoc = session.createDocumentModel(file.getPathAsString(), SPECIAL_CHILD_DOC_NAME,
                COMMENT_ROOT_TYPE);
        session.createDocument(specialChildDoc);

        DocumentModel regularUnderSpecialDoc = session.createDocumentModel(specialChildDoc.getPathAsString(),
                REGULAR_UNDER_SPECIAL_CHILD_DOC_NAME, FILE);
        session.createDocument(regularUnderSpecialDoc);

        DocumentModel regularChildDoc = session.createDocumentModel(file.getPathAsString(), REGULAR_CHILD_DOC_NAME,
                FILE);
        session.createDocument(regularChildDoc);

        assertEquals(2, session.getChildren(file.getRef()).size());

        // test nominal copy
        try (OperationContext context = new OperationContext(session)) {
            context.setInput(file);
            Map<String, Serializable> params = new HashMap<>();
            params.put(TARGET_PROPERTY_KEY, ROOT);
            params.put(NAME_PROPERTY_KEY, COPY_DOC_NAME);
            DocumentModel result = (DocumentModel) automationService.run(context, CopyDocument.ID, params);
            result = session.getDocument(result.getRef());
            assertNotEquals(file.getId(), result.getId());
            assertEquals(COPY_DOC_NAME, result.getName());
            DocumentModelList children = session.getChildren(result.getRef());
            // special children shall not be copied
            assertEquals(1, children.size());
            DocumentModel copiedRegularChild = children.get(0);
            assertEquals(REGULAR_CHILD_DOC_NAME, copiedRegularChild.getName());
            assertNotEquals(regularChildDoc.getRef(), copiedRegularChild.getRef());
        }

        // test checkin copy, only special children shall be copied
        DocumentRef checkedIn = file.checkIn(VersioningOption.MAJOR, "JustForFun");
        DocumentModelList children = session.getChildren(checkedIn);
        assertEquals(1, children.totalSize());
        DocumentModel versionedChild = children.get(0);
        assertEquals(COMMENT_ROOT_TYPE, versionedChild.getType());
        assertEquals(SPECIAL_CHILD_DOC_NAME, versionedChild.getName());
        assertNotEquals(regularChildDoc.getRef(), versionedChild.getRef());

        // regular documments under the special children shall be copied as well
        children = session.getChildren(versionedChild.getRef());
        assertEquals(1, children.totalSize());
        DocumentModel versionedSubChild = children.get(0);
        assertEquals(FILE, versionedSubChild.getType());
        assertEquals(REGULAR_UNDER_SPECIAL_CHILD_DOC_NAME, versionedSubChild.getName());
        assertNotEquals(regularUnderSpecialDoc.getRef(), versionedSubChild.getRef());

        // test restore copy. Live document shall keep both special and regular children.
        // No version children shall be added during restore
        DocumentModel restored = session.restoreToVersion(file.getRef(), checkedIn);
        children = session.getChildren(restored.getRef());
        assertEquals(2, children.totalSize());
    }
}
