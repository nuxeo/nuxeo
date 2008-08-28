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

import java.lang.reflect.Constructor;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.webengine.rest.ResourceBinding;
import org.nuxeo.ecm.webengine.rest.WebEngine2;
import org.nuxeo.ecm.webengine.rest.domains.DomainDescriptor;
import org.nuxeo.ecm.webengine.rest.domains.DomainRegistry;
import org.nuxeo.ecm.webengine.rest.domains.WebDomain;
import org.nuxeo.ecm.webengine.rest.servlet.jersey.patch.ServletContainerRequest;
import org.nuxeo.ecm.webengine.rest.servlet.jersey.patch.WebApplicationContext;
import org.nuxeo.runtime.api.Framework;

import sun.awt.ConstrainableGraphics;
import sun.nio.cs.SingleByteDecoder;

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
        WebContextImpl ctx = new WebContextImpl(this, request, response);
        ctx.setAction(((ServletContainerRequest) request).getAction());
        return ctx;
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
        DomainRegistry domains = engine.getDomainRegistry();
        for (ResourceBinding binding : engine.getBindings()) {
            String path = binding.pattern;
            boolean pathEndsInSlash = false;
            if (binding.pattern == null || binding.pattern.equals("/")) {
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
            } else if (binding.domain != null) {
                WebDomain domain = domains.getDomain(binding.domain);
                if (domain == null) {
                    log.error("Invalid resource binding: " + binding.pattern
                            + ". Domain not found: " + binding.domain);
                    continue;
                }
                binding.singleton = true;
                rc = domain.getClass();
                rule = new ResourceObjectRule(t, domain);
            } else {
                log.error("Invalid resource binding: " + binding.pattern
                        + ". No resource class specified.");
                continue;
            }
            getResourceClass(rc); // TODO here we must be able to modify
                                    // resource uri path...

            rules.put(p, new RightHandPathRule(redirect, pathEndsInSlash, rule));
        }
    }

}
