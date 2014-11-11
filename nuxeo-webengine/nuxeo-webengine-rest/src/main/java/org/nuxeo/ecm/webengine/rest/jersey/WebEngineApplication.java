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

package org.nuxeo.ecm.webengine.rest.jersey;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.nuxeo.ecm.webengine.RootDescriptor;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.rest.WebEngine2;
import org.nuxeo.ecm.webengine.rest.domains.DefaultWebDomain;
import org.nuxeo.ecm.webengine.rest.domains.DocumentDomain;
import org.nuxeo.ecm.webengine.rest.domains.DomainDescriptor;
import org.nuxeo.ecm.webengine.rest.domains.ScriptDomain;
import org.nuxeo.ecm.webengine.rest.jersey.patch.ServletContainer;
import org.nuxeo.ecm.webengine.rest.jersey.patch.ServletContainerRequest;
import org.nuxeo.ecm.webengine.rest.jersey.patch.WebApplicationContext;
import org.nuxeo.runtime.api.Framework;

import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.api.uri.UriTemplate;
import com.sun.jersey.impl.model.RulesMap;
import com.sun.jersey.impl.uri.PathPattern;
import com.sun.jersey.impl.uri.PathTemplate;
import com.sun.jersey.impl.uri.rules.ResourceObjectRule;
import com.sun.jersey.impl.uri.rules.RightHandPathRule;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.uri.rules.UriRule;


/**
 * Extends the Jersey WebApplication to allow
 * binding root resources on paths from configuration files
 * (and not using annotations)
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class WebEngineApplication extends org.nuxeo.ecm.webengine.rest.jersey.patch.WebApplicationImpl {

     @Override
    protected WebApplicationContext createWebAcpplicationContext(
            ContainerRequest request, ContainerResponse response) {
        WebContextImpl ctx = new WebContextImpl(this, request, response);
        ctx.setAction(((ServletContainerRequest)request).getAction());
        return ctx;
    }

    @Override
    protected RulesMap<UriRule> processRootResources(Set<Class<?>> classes) {
        RulesMap<UriRule> rules = new RulesMap<UriRule>();
            //super.processRootResources(classes);
        try {
            addRootResources(rules);
        } catch (Exception e) {
            throw new Error("Failed to load root resources", e);
        }
        return rules;
    }

    //TODO: refactor webapplication and rename it as ResourceContainer ?
    protected void addRootResources(RulesMap<UriRule> rules) throws Exception {
        boolean redirect = resourceConfig.getFeature(ResourceConfig.FEATURE_REDIRECT);
        for (DefaultWebDomain<?> domain : getDomains()) {
            getResourceClass(domain.getClass()); //TODO here we must be able to modify resource uri path...
            String path = domain.getPath();
            String pathStr = null;
            boolean pathEndsInSlash = false;
            if (path == null || path.equals("/")) {
                pathStr = "/";
                pathEndsInSlash = true;
            } else {
                pathStr = path;
            }
            UriTemplate t = new PathTemplate(pathStr, false);
            PathPattern p = new PathPattern(t, true);
            rules.put(p, new RightHandPathRule(
                    redirect,
                    pathEndsInSlash,
                    new ResourceObjectRule(t, domain)));
        }
    }



    protected DefaultWebDomain<?>[] getDomains() throws WebException {
        WebEngine2 engine = Framework.getLocalService(WebEngine2.class);

        List<RootDescriptor> roots = Arrays.asList(new RootDescriptor[] {new RootDescriptor("/default", 0)});
        List<RootDescriptor> roots2 = Arrays.asList(new RootDescriptor[] {new RootDescriptor("/resources", 0)});

        DomainDescriptor desc = new DomainDescriptor();
        desc.id = "default";
        desc.path = "/";
        desc.roots = roots;
        ScriptDomain d1 = new ScriptDomain(engine, desc);

        desc = new DomainDescriptor();
        desc.id = "resources";
        desc.path = "/resources";
        desc.roots = roots2;
        ScriptDomain d2 = new ScriptDomain(engine, desc);

        desc = new DomainDescriptor();
        desc.id = "repository";
        desc.root = "/default-domain";
        desc.path = "/repository";
        desc.roots = roots;
        DocumentDomain d3 = new DocumentDomain(engine, desc);

        // a groovy domain for testing groovy resources
        desc = new DomainDescriptor();
        desc.id = "groovy";
        desc.path = "/groovy";
        desc.type="Groovy";
        desc.roots = roots;
        DefaultWebDomain d4 = new DefaultWebDomain(engine, desc);

        return new DefaultWebDomain<?>[] {
                d1, d2, d3, d4
        };
    }

}
