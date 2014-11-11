/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bogdan Stefanescu
 */
package org.nuxeo.ecm.core.test;

import org.junit.runner.RunWith;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(user = "Administrator", init = DefaultRepositoryInit.class)
public class DocumentPropertyTest {

    @Inject
    public CoreSession session;

    @Test
    public void theSessionIsUsable() throws Exception {
        DocumentModel doc = session.createDocumentModel(
                "/default-domain/workspaces", "myfile", "File");
        StringBlob blob = new StringBlob("test", "text/plain");
        blob.setFilename("myfile");
        blob.setDigest("mydigest");
        doc.setPropertyValue("file:content", blob);
        doc = session.createDocument(doc);
        doc = session.getDocument(doc.getRef());
        assertEquals("myfile", doc.getPropertyValue("file:content/name"));
        assertEquals("mydigest",
                doc.getPropertyValue("file:content/digest"));
    }

}
