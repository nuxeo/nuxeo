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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.rest.servlet.jersey;

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.webengine.rest.ResourceBinding;
import org.nuxeo.ecm.webengine.rest.WebContext2;
import org.nuxeo.ecm.webengine.rest.WebEngine2;
import org.nuxeo.ecm.webengine.rest.model.WebApplication;
import org.nuxeo.ecm.webengine.rest.servlet.jersey.patch.WebApplicationContext;
import org.nuxeo.runtime.api.Framework;

import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.api.uri.UriTemplate;
import com.sun.jersey.impl.model.RulesMap;
import com.sun.jersey.impl.uri.PathPattern;
import com.sun.jersey.impl.uri.PathTemplate;
import com.sun.jersey.impl.uri.rules.ResourceClassRule;
import com.sun.jersey.impl.uri.rules.ResourceObjectRule;
import com.sun.jersey.impl.uri.rules.RightHandPathRule;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.uri.rules.UriRule;

/**
 * Extends the Jersey WebApplication to allow binding root resources on paths
 * from configuration files (and not using annotations)
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public class WebEngineApplication extends
        org.nuxeo.ecm.webengine.rest.servlet.jersey.patch.WebApplicationImpl {

    private static final Log log = LogFactory.getLog(WebEngineApplication.class);

    @Override
    protected WebApplicationContext createWebAcpplicationContext(
            ContainerRequest request, ContainerResponse response) {
        WebContext2 ctx = new WebContextImpl(request);
        return new WebEngineApplicationContext(ctx, this, request, response);
    }
    
    @Override
    protected void setThreadLocalContext(WebApplicationContext localContext) {
        super.setThreadLocalContext(localContext);
        WebContext2 ctx = localContext != null ? ((WebEngineApplicationContext)localContext).getContext() : null; 
        WebEngine2.setActiveContext(ctx);        
    }

    @Override
    protected RulesMap<UriRule> processRootResources(Set<Class<?>> classes) {
        RulesMap<UriRule> rules = new RulesMap<UriRule>();
        // super.processRootResources(classes);
        try {
            addRootResources(rules);
        } catch (Exception e) {
            throw new Error("Failed to load root resources", e);
        }
        return rules;
    }

    // TODO: refactor webapplication and rename it as ResourceContainer ?
    protected void addRootResources(RulesMap<UriRule> rules) throws Exception {
        boolean redirect = resourceConfig.getFeature(ResourceConfig.FEATURE_REDIRECT);
        WebEngine2 engine = Framework.getLocalService(WebEngine2.class);
        // register regular resources
        for (ResourceBinding binding : engine.getBindings()) {
            String path = binding.path;
            boolean pathEndsInSlash = false;
            if (binding.path == null || binding.path.equals("/")) {
                path = "/";
                pathEndsInSlash = true;
            }
            UriTemplate t = new PathTemplate(path, false);
            PathPattern p = new PathPattern(t, true);

            UriRule rule = null;
            Class<?> rc = null;
            if (binding.className != null) {
                rc = engine.getScripting().loadClass(binding.className);
                if (binding.singleton) { // TODO use a factory to create
                                            // singletons and remove singleton
                                            // property
                    rule = new ResourceObjectRule(t, rc.newInstance());
                } else {
                    rule = new ResourceClassRule(t, rc);
                }
            } else {
                log.error("Invalid resource binding: " + binding.path
                        + ". No resource class specified.");
                continue;
            }
            getResourceClass(rc); // TODO here we must be able to modify
                                    // resource uri path...

            rules.put(p, new RightHandPathRule(redirect, pathEndsInSlash, rule));
        }

        // add managed resources
        for (WebApplication app : engine.getApplicationRegistry().getApplications()) {
            if (app.isFragment()) {
                continue;
            }
            String path = app.getPath();
            boolean pathEndsInSlash = false;
            if (path == null || path.equals("/")) {
                path = "/";
                pathEndsInSlash = true;
            }
            
            UriTemplate t = new PathTemplate(path, app.getPathEncode());
            PathPattern p = new PathPattern(t, app.getPathLimited());
            UriRule rule = new ResourceObjectRule(t, app);
                        
            getResourceClass(app.getClass()); // TODO here we must be able to modify

            rules.put(p, new RightHandPathRule(redirect, pathEndsInSlash, rule));
        }
        
    }

}
