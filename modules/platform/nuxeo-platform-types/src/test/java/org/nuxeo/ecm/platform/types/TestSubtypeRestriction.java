/*
 * (C) Copyright 2025 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard
 */
package org.nuxeo.ecm.platform.types;

import static org.junit.Assert.assertThrows;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.types.listener.SubtypeRestrictionListener;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.WithFrameworkProperty;

/**
 * @since 2025.7
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.platform.types")
@WithFrameworkProperty(name = SubtypeRestrictionListener.SUBTYPE_RESTRICTION_PROP_NAME, value = "true")
public class TestSubtypeRestriction {

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected CoreSession session;

    @Test
    public void testEnforceCheckAllowedSubtypesCreate() {
        DocumentModel root = session.getRootDocument();
        var folder = session.createDocumentModel(root.getPathAsString(), "folder", "Folder");
        assertThrows(IllegalArgumentException.class, () -> session.createDocument(folder));
    }

    @Test
    public void testEnforceCheckAllowedSubtypesCopy() {
        DocumentModel root = session.getRootDocument();
        var domain = session.createDocument(session.createDocumentModel(root.getPathAsString(), "domain", "Domain"));
        var workspaceRoot = session.createDocument(
                session.createDocumentModel(domain.getPathAsString(), "workspaceRoot", "WorkspaceRoot"));
        assertThrows(IllegalArgumentException.class, () -> session.copy(workspaceRoot.getRef(), root.getRef(), "top"));
    }

    @Test
    public void testEnforceCheckAllowedSubtypesMove() {
        DocumentModel root = session.getRootDocument();
        var domain = session.createDocument(session.createDocumentModel(root.getPathAsString(), "domain", "Domain"));
        var workspaceRoot = session.createDocument(
                session.createDocumentModel(domain.getPathAsString(), "workspaceRoot", "WorkspaceRoot"));
        assertThrows(IllegalArgumentException.class, () -> session.move(workspaceRoot.getRef(), root.getRef(), "top"));
    }
}
