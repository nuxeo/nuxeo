/*
 * (C) Copyright 2009-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.chemistry.bindings;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.chemistry.Repository;
import org.apache.chemistry.RepositoryManager;
import org.apache.chemistry.RepositoryService;
import org.apache.chemistry.atompub.server.jaxrs.AbderaResource;
import org.apache.chemistry.atompub.server.jaxrs.AbderaResource.PathMunger;
import org.apache.chemistry.impl.simple.SimpleRepositoryService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.chemistry.impl.NuxeoRepository;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Nuxeo Runtime Component whose activation is used to register things not
 * available through extension points: additional JAX-RS Providers, repositories
 * for Chemistry.
 */
public class ChemistryComponent extends DefaultComponent {

    private static final Log log = LogFactory.getLog(ChemistryComponent.class);

    protected RepositoryService repositoryService;

    @Override
    public void activate(ComponentContext context) throws Exception {

        AbderaResource.pathMunger = new WebEnginePathMunger();

        // We have to set this so that a %2F is allowed in a URL. This is needed
        // to interpret the CMIS URI template "objectbypath".
        System.setProperty(
                "org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH",
                "true");

        // use an activator to delay registering repositories until the first
        // call to Chemistry is made, to be sure all Nuxeo repositories have
        // been initialized
        RepositoryManager.getInstance().registerActivator(new Runnable() {
            public void run() {
                try {
                    registerRepositories();
                } catch (Exception e) {
                    log.error("Cannot register repositories with Chemistry", e);
                }
            }
        });
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        unregisterRepositories();
    }

    /**
     * Registers all the Nuxeo repositories with Chemistry.
     */
    protected void registerRepositories() throws Exception {
        org.nuxeo.ecm.core.api.repository.RepositoryManager mgr = Framework.getService(org.nuxeo.ecm.core.api.repository.RepositoryManager.class);
        if (mgr == null) {
            throw new RuntimeException("Cannot lookup Nuxeo RepositoryManager");
        }
        List<Repository> repositories = new ArrayList<Repository>();
        for (org.nuxeo.ecm.core.api.repository.Repository nxrepo : mgr.getRepositories()) {
            NuxeoRepository repo = new NuxeoRepository(nxrepo.getName());
            if ("default".equals(repo.getName())) {
                // put "default" repository first (SimpleRepositoryService
                // convention)
                repositories.add(0, repo);
            } else {
                repositories.add(repo);
            }
        }
        repositoryService = new SimpleRepositoryService(repositories);
        RepositoryManager.getInstance().registerService(repositoryService);
    }

    /**
     * Unregisters the Nuxeo repositories from Chemistry.
     */
    protected void unregisterRepositories() {
        if (repositoryService == null) {
            return;
        }
        RepositoryManager.getInstance().unregisterService(repositoryService);
        repositoryService = null;
    }

    /**
     * Munges the AtomPub request path according to WebEngine rules.
     * <p>
     * Uses the Nuxeo-Webengine-Base-Path header to provide a fake context &
     * servlet path.
     */
    public static class WebEnginePathMunger implements PathMunger {
        public ContextAndServletPath munge(HttpServletRequest request,
                String contextPath, String servletPath) {
            ContextAndServletPath cs = new ContextAndServletPath();
            String basePath = request.getHeader(WebContext.NUXEO_WEBENGINE_BASE_PATH);
            if (",".equals(basePath)) {
                basePath = ""; // copied from AbstractWebContext#getBasePath
            }
            if (basePath == null) {
                cs.contextPath = contextPath;
                cs.servletPath = servletPath;
            } else {
                // replace context + servlet with our own base path
                if (!basePath.startsWith("/")) {
                    basePath = '/' + basePath;
                }
                if (basePath.equals("/")) {
                    basePath = "";
                } else if (basePath.endsWith("/")) {
                    basePath = basePath.substring(0, basePath.length() - 1);
                }
                cs.contextPath = "";
                cs.servletPath = basePath;
            }
            return cs;
        }
    }

}
