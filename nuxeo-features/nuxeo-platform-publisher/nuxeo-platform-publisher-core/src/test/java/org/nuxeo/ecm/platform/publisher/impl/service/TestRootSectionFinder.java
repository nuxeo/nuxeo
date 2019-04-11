/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     tmartins - test methods for DefaultRootSectionsFinder
 */
package org.nuxeo.ecm.platform.publisher.impl.service;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.publisher.impl.finder.DefaultRootSectionsFinder;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
public class TestRootSectionFinder {

    @Inject
    protected CoreSession session;

    Set<String> sectionRootTypes = new HashSet<>();

    Set<String> sectionTypes = new HashSet<>();

    @Test
    public void testBuildQuery() throws Exception {
        sectionTypes.add("Sections");
        RootSectionsFinderForTest rsf = new RootSectionsFinderForTest(session);

        rsf.setSectionTypes();

        String path;
        String query;

        path = "/default-domain/workspaces/space";
        query = rsf.buildQuery(path);
        assertEquals(
                "SELECT * FROM Document WHERE ecm:path STARTSWITH '/default-domain/workspaces/space' and ( ecm:primaryType = 'Section' ) order by ecm:path ",
                query);

        path = "/default-domain/workspaces/thierry's space";
        query = rsf.buildQuery(path);
        assertEquals(
                "SELECT * FROM Document WHERE ecm:path STARTSWITH '/default-domain/workspaces/thierry\\'s space' and ( ecm:primaryType = 'Section' ) order by ecm:path ",
                query);

        // test if query is valid
        DocumentModelList dml = session.query(query);
        assertEquals(0, dml.size());

    }

    /**
     * test class to call protected method
     *
     * @author tmartins
     */
    private class RootSectionsFinderForTest extends DefaultRootSectionsFinder {

        public RootSectionsFinderForTest(CoreSession userSession) {
            super(userSession);
        }

        @Override
        public String buildQuery(String path) {
            return super.buildQuery(path);
        }

        public void setSectionTypes() {
            sectionTypes = new HashSet<>();
            sectionTypes.add("Section");
        }

    }
}
