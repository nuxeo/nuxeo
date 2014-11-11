/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Damien Metzler (Leroy Merlin, http://www.leroymerlin.fr/)
 */
package org.nuxeo.ecm.core.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@RepositoryConfig(init=DefaultRepositoryInit.class, cleanup=Granularity.METHOD)
@Features(CoreFeature.class)
public class CleanUpWithInitializerTest {

    @Inject
    CoreSession session;

    @Test
    public void iCreateADoc() throws Exception {
        DocumentModel doc = session.createDocumentModel(
                "/default-domain/workspaces/", "myWorkspace", "Workspace");
        doc.setProperty("dublincore", "title", "My Workspace");
        doc = session.createDocument(doc);
        session.saveDocument(doc);
        session.save();
        assertTrue(session.exists(new PathRef(
                "/default-domain/workspaces/myWorkspace")));
    }

    @Test
    public void myWorkspaceIsNotHereAnymore() throws Exception {
        assertTrue(session.exists(new PathRef("/default-domain/workspaces/")));
        assertFalse(session.exists(new PathRef(
                "/default-domain/workspaces/myWorkspace")));
    }

}
