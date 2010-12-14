/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Bogdan Stefanescu
 */
package org.nuxeo.ecm.core.test;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
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
        Assert.assertEquals("myfile", doc.getPropertyValue("file:content/name"));
        Assert.assertEquals("mydigest",
                doc.getPropertyValue("file:content/digest"));
    }

}
