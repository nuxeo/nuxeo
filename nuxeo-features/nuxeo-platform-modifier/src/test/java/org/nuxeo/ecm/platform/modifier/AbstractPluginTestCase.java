/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.modifier;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.NXCore;
import org.nuxeo.ecm.core.api.impl.blob.ByteArrayBlob;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryTestCase;

public abstract class AbstractPluginTestCase extends RepositoryTestCase {

    final Log log = LogFactory.getLog(AbstractPluginTestCase.class);

    private Repository repository;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        deployContrib("org.nuxeo.ecm.platform.modifier.tests",
                "DemoRepository.xml");
        deployContrib("org.nuxeo.ecm.platform.modifier.tests",
                "CoreEventListenerService.xml");

        // needed to avoid npe when creating DocumentModel
        deployContrib("org.nuxeo.ecm.platform.modifier.tests",
                "LifeCycleService.xml");

        deployContrib("org.nuxeo.ecm.platform.modifier.tests",
                "nxtransform-bundle-dmtest.xml");
        deployContrib("org.nuxeo.ecm.platform.modifier.tests",
                "nxtransform-plugins-bundle-dmtest.xml");

        deployContrib("org.nuxeo.ecm.platform.modifier.tests",
                "nxdocmodifier-bundle.xml");
        deployContrib("org.nuxeo.ecm.platform.modifier.tests",
                "nxdocmodifier-test-contrib-bundle.xml");
    }

    @Override
    public void tearDown() throws Exception {
        releaseRepository();
        super.tearDown();
    }

    @Override
    public Repository getRepository() throws Exception {
        if (repository == null) {
            // the repository should be deployed the last
            // after any other bundle that is deploying doctypes
            deployContrib("org.nuxeo.ecm.platform.modifier.tests",
                    "DemoRepository.xml");
            repository = NXCore.getRepositoryService().getRepositoryManager()
                    .getRepository("demo");
        }
        return repository;
    }

    @Override
    public void releaseRepository() {
        if (repository != null) {
            repository.shutdown();
            repository = null;
        }
    }

    /**
     * Utility method to
     * create a ByteArrayBlob with the content of the given file.
     */
    protected ByteArrayBlob getFileContent(String filePath, String mimeType) {
        File file = new File(filePath);
        try {
            final byte[] fileContent = FileUtils.readBytes(file);
            return new ByteArrayBlob(fileContent, "application/msword");
        } catch (IOException e) {
            log.error("Error reading file: " + file.getAbsolutePath());
            e.printStackTrace();
            fail("Error reading file: " + file.getAbsolutePath());
        }

        // should not reach this point
        return null;
    }
}
