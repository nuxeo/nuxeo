/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     ataillefer
 */
package org.nuxeo.ecm.diff.service;

import java.text.ParseException;
import java.util.List;

import junit.framework.TestCase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.BackendType;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.diff.DocumentDiffRepositoryInit;
import org.nuxeo.ecm.diff.model.DiffDisplayBlock;
import org.nuxeo.ecm.diff.model.DocumentDiff;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

/**
 * Tests the {@link DiffDisplayService}.
 * 
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(repositoryName = "default", type = BackendType.H2, init = DocumentDiffRepositoryInit.class, user = "Administrator", cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.diff", "org.nuxeo.diff.test",
        "org.nuxeo.diff.test:OSGI-INF/test-no-default-diff-display-contrib.xml" })
public class TestDiffDisplayServiceNoDefaultContrib extends TestCase {

    @Inject
    protected CoreSession session;

    @Inject
    protected DiffDisplayService diffDisplayService;

    @Inject
    protected DocumentDiffService docDiffService;

    /**
     * Test diff display service with no default (Document) diffDisplay contrib.
     * 
     * @throws ClientException the client exception
     * @throws ParseException
     */
    @Test
    public void testDiffDisplayServiceNoDefaultContrib()
            throws ClientException, ParseException {

        // Get left and right docs
        DocumentModel leftDoc = session.getDocument(new PathRef(
                DocumentDiffRepositoryInit.LEFT_DOC_PATH));
        DocumentModel rightDoc = session.getDocument(new PathRef(
                DocumentDiffRepositoryInit.RIGHT_DOC_PATH));

        // Do doc diff
        DocumentDiff docDiff = docDiffService.diff(session, leftDoc, rightDoc);

        // Get diff display blocks
        List<DiffDisplayBlock> diffDisplayBlocks = diffDisplayService.getDiffDisplayBlocks(
                docDiff, leftDoc, rightDoc);
        assertNotNull(diffDisplayBlocks);
        assertEquals(0, diffDisplayBlocks.size());
        // TODO: once implemented, check diffDisplayBlocks!

    }
}
