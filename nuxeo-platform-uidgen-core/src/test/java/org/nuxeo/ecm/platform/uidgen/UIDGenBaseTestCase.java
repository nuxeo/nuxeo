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

package org.nuxeo.ecm.platform.uidgen;

import org.nuxeo.ecm.core.NXCore;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 *
 * @author DM
 *
 */
public abstract class UIDGenBaseTestCase extends NXRuntimeTestCase {

    private Repository repository;

    public Repository getRepository() throws Exception {
        if (repository == null) {
            // the repository should be deployed the last
            // after any other bundle that is deploying doctypes
            deploy("DemoRepository.xml");
            repository = NXCore.getRepositoryService().getRepositoryManager()
                    .getRepository("demo");
        }
        return repository;
    }

    public void releaseRepository() {
        if (repository != null) {
            repository.shutdown();
            repository = null;
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // the core bundle
        deploy("CoreService.xml");
        deploy("TypeService.xml");
        deploy("SecurityService.xml");
        deploy("RepositoryService.xml");
        deploy("test-CoreExtensions.xml");

        deploy("DemoRepository.xml");
        deploy("CoreEventListenerService.xml");
        deploy("LifeCycleService.xml");

        // UID specific
        deploy("nxuidgenerator-bundle.xml");
        deploy("nxuidgenerator-bundle-contrib.xml");

        deploy("GeideDocTypes.xml");
    }

    @Override
    protected void tearDown() throws Exception {
        releaseRepository();
        super.tearDown();
    }

    protected Session getSession() throws Exception {
        return getRepository().getSession(null);
    }

}
