/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
