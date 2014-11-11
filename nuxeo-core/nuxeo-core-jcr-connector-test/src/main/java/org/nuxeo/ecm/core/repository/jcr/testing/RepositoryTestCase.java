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
 * $Id$
 */

package org.nuxeo.ecm.core.repository.jcr.testing;

import org.nuxeo.ecm.core.NXCore;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class RepositoryTestCase extends NXRuntimeTestCase {

    private Repository repository;

    protected RepositoryTestCase() {
    }

    protected RepositoryTestCase(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // the core bundle
        deployContrib(CoreJCRConnectorTestConstants.CORE_BUNDLE,
                "OSGI-INF/CoreService.xml");
        deployContrib(CoreJCRConnectorTestConstants.CORE_BUNDLE,
                "OSGI-INF/SecurityService.xml");
        deployContrib(CoreJCRConnectorTestConstants.CORE_BUNDLE,
                "OSGI-INF/RepositoryService.xml");

        deployContrib(CoreJCRConnectorTestConstants.BUNDLE,
                "TypeService.xml");
        deployContrib(CoreJCRConnectorTestConstants.BUNDLE,
                "test-CoreExtensions.xml");
    }

    @Override
    public void tearDown() throws Exception {
        releaseRepository();
        super.tearDown();
    }

    public Repository getRepository() throws Exception {
        if (repository == null) {
            // the repository should be deployContribed the last
            // after any other bundle that is deployContribing doctypes
            deployContrib(CoreJCRConnectorTestConstants.BUNDLE,
                    "DemoRepository.xml");
            repository = NXCore.getRepositoryService()
                .getRepositoryManager().getRepository("demo");
        }
        return repository;
    }

    public void releaseRepository() {
        if (repository != null) {
            repository.shutdown();
            repository = null;
        }
    }

    protected Session getSession() throws Exception {
        return getRepository().getSession(null);
    }

}
