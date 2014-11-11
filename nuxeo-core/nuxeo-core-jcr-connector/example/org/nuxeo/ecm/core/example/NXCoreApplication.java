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

package org.nuxeo.ecm.core.example;

import java.net.URL;

import org.nuxeo.ecm.core.NXCore;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.runtime.NXRuntime;
import org.nuxeo.runtime.RuntimeService;

/**
 *
 * @author <a href="mailto:lgiura@nuxeo.com">Bogdan Stefanescu</a>
 */
public abstract class NXCoreApplication {

    protected static RuntimeService runtime;
    protected static Repository repository;
    protected String repositoryName;

    public NXCoreApplication(String repositoryName) {
        this.repositoryName = repositoryName;
    }

    public static void run(NXCoreApplication app) throws Exception {
        app.initialize();
        app.run();
        app.shutdown();
    }

    public void initialize() throws Exception {
        runtime = new SimpleRuntime();
        runtime.start();
        deployAll();
        repository = NXCore.getRepositoryService()
            .getRepositoryManager().getRepository("demo");
    }

    public  void shutdown() throws Exception {
        repository.shutdown();
        runtime.stop();
    }

    public void deploy(String bundle) {
        URL url = getResource(bundle);
        assert url != null;
        try {
            NXRuntime.getRuntime().getContext().deploy(url);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void undeploy(String bundle) {
        URL url = getResource(bundle);
        assert url != null;
        try {
            NXRuntime.getRuntime().getContext().undeploy(url);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public URL getResource(String resource) {
        return runtime.getContext().getResource(resource);
    }


    protected void deployAll() {
        deploy("EventService.xml");
        deploy("CoreService.xml");
        deploy("TypeService.xml");
        deploy("RepositoryService.xml");
        deploy("CoreExtensions.xml");
    }

    protected abstract void run() throws Exception;

}
