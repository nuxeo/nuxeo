/*
 * (C) Copyright 2006-2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Laurent Doguin
 *
 */

package org.nuxeo.ecm.core.version.test;

import static org.junit.Assert.assertEquals;

import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.versioning.VersioningService;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeHarness;

@RunWith(FeaturesRunner.class)
@Features({ TransactionalFeature.class, CoreFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD)
public class TestVersioningSaveOptions {

    @Inject
    protected RuntimeHarness runtimeHarness;

    @Inject
    protected CoreSession session;

    @Inject
    protected VersioningService vService;

    @Test
    public void testTypeSaveOptions() throws Exception {
        DocumentModel fileDoc = new DocumentModelImpl("File");
        fileDoc = session.createDocument(fileDoc);
        String versionLabel = fileDoc.getVersionLabel();
        assertEquals("0.0", versionLabel);
        List<VersioningOption> opts = vService.getSaveOptions(fileDoc);
        assertEquals(3, opts.size());
        assertEquals(VersioningOption.NONE, opts.get(0));

        runtimeHarness.deployContrib("org.nuxeo.ecm.core.test.tests", "test-versioning-contrib.xml");
        try {
            fileDoc = new DocumentModelImpl("File");
            fileDoc = session.createDocument(fileDoc);
            versionLabel = fileDoc.getVersionLabel();
            assertEquals("1.1+", versionLabel);
            opts = vService.getSaveOptions(fileDoc);
            assertEquals(2, opts.size());
            assertEquals(VersioningOption.MINOR, opts.get(0));
            session.followTransition(fileDoc.getRef(), "approve");
            opts = vService.getSaveOptions(fileDoc);
            assertEquals(0, opts.size());
            session.followTransition(fileDoc.getRef(), "backToProject");
            session.followTransition(fileDoc.getRef(), "obsolete");
            opts = vService.getSaveOptions(fileDoc);
            assertEquals(3, opts.size());

            runtimeHarness.deployContrib("org.nuxeo.ecm.core.test.tests", "test-versioning-override-contrib.xml");
            try {
                fileDoc = new DocumentModelImpl("File");
                fileDoc = session.createDocument(fileDoc);
                versionLabel = fileDoc.getVersionLabel();
                assertEquals("2.2+", versionLabel);
            } finally {
                runtimeHarness.undeployContrib("org.nuxeo.ecm.core.test.tests", "test-versioning-override-contrib.xml");
            }
        } finally {
            runtimeHarness.undeployContrib("org.nuxeo.ecm.core.test.tests", "test-versioning-contrib.xml");
        }
    }

}
