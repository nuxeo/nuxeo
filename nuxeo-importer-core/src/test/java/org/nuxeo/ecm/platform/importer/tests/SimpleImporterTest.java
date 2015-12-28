/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.importer.tests;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.importer.executor.DefaultImporterExecutor;
import org.nuxeo.ecm.platform.importer.source.RandomTextSourceNode;
import org.nuxeo.ecm.platform.importer.source.SourceNode;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.content.template")
public class SimpleImporterTest {

    /*
     * @Test
     * @Ignore public void testImport() throws Exception { String inputPath = "/home/tiry/docs/tests/"; String
     * targetPath = "/default-domain/workspaces/"; DefaultImporterExecutor executor = new
     * DefaultImporterExecutor(session); executor.run(inputPath, targetPath, 5, 5, true); long createdDocs =
     * executor.getCreatedDocsCounter(); assertTrue(createdDocs > 0); }
     */

    @Test
    public void testRamdomImport() throws Exception {

        // System.out.println("Starting prefill");
        SourceNode src = RandomTextSourceNode.init(500);
        // System.out.println("prefill done");
        String targetPath = "/default-domain/workspaces/";

        DefaultImporterExecutor executor = new DefaultImporterExecutor();

        executor.run(src, targetPath, false, 10, 5, true);

        long createdDocs = executor.getCreatedDocsCounter();
        assertTrue(createdDocs > 0);

        // System.out.println("total content size = "
        // + RandomTextSourceNode.getSize() / (1024 * 1024) + " MB");
        // System.out.println("nb nodes = " +
        // RandomTextSourceNode.getNbNodes());
    }

}
