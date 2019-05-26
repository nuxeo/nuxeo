/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Marwane Kalam-Alami
 */
package org.nuxeo.ecm.platform.publisher.web;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessorBean;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import junit.framework.Assert;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.platform.publisher.web")
@Deploy("org.nuxeo.ecm.platform.publisher.web:OSGI-INF/core-types-contrib.xml")
@RepositoryConfig(init = DefaultRepositoryInit.class, cleanup = Granularity.METHOD)
public class TestPublishActions {

    @Inject
    private CoreSession documentManager;

    @Inject
    private ResourcesAccessorBean resourcesAccessor;

    @Test
    public void testGetPathFragments() {
        // Create file in standard domain
        DocumentModel fileModel = documentManager.createDocumentModel("/default-domain/workspaces/", "myfile", "File");
        fileModel = documentManager.createDocument(fileModel);

        // Create file in custom domain with specific type
        DocumentModel customDomainModel = documentManager.createDocumentModel("/", "mydomain", "CustomDomain");
        customDomainModel = documentManager.createDocument(customDomainModel);
        DocumentModel file2Model = documentManager.createDocumentModel(customDomainModel.getPathAsString(), "myfile2",
                "File");
        file2Model = documentManager.createDocument(file2Model);

        // Create file directly below the root
        DocumentModel file3Model = documentManager.createDocumentModel("/", "myfile3", "File");
        file3Model = documentManager.createDocument(file3Model);

        // Test paths (every fragment is null because the message map is empty)
        DummyPublishActions publishActions = new DummyPublishActions(documentManager, resourcesAccessor);
        Assert.assertEquals("null>null>null", publishActions.getFormattedPath(fileModel));
        Assert.assertEquals("null>null", publishActions.getFormattedPath(file2Model));
        Assert.assertEquals("null>null", publishActions.getFormattedPath(file3Model));
    }

}
