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
 *     Stephane Lacoin 
 */
package org.nuxeo.ecm.core.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.test.annotations.BackendType;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfigs;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;
import com.google.inject.name.Named;

@RunWith(FeaturesRunner.class)
@Features( { MultiRepositoriesCoreFeature.class } )
@RepositoryConfigs( {
    @RepositoryConfig(type=BackendType.H2, repositoryName="repo1"),
    @RepositoryConfig(type=BackendType.H2, repositoryName="repo2")
})
public class MultiRepositoriesAreTestable {

    @Inject @Named("repo1") CoreSession repo1;
    @Inject @Named("repo2") CoreSession repo2;

    @Test public void theRepo1IsUsable() throws ClientException  {
        theRepoIsUsable(repo1);
    }

    @Test public void theRepo2IsUsable() throws ClientException  {
        theRepoIsUsable(repo2);
    }

    @Test public void theReposAreMutableInTheSameTransaction() throws ClientException {
        alterRepo(repo1);
        alterRepo(repo2);
        repo1.save();
        repo2.save();
    }

    protected void theRepoIsUsable(CoreSession repo) throws ClientException {
        assertThat(repo, notNullValue());
        assertThat(repo.getDocument(new PathRef("/default-domain")), notNullValue());
    }

    protected void alterRepo(CoreSession repo) throws ClientException {
        DocumentModel doc = repo.createDocumentModel("/default-domain/workspaces", "workspace",
                "Workspace");
        doc.setProperty("dublincore", "title", "workspace");
        doc = repo.createDocument(doc);
        repo.saveDocument(doc);
    }
}
