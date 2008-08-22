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

import java.util.Set;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.webengine.WebApplicationMapping;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.rest.WebResource;
import org.nuxeo.runtime.api.Framework;

import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.api.uri.UriTemplate;
import com.sun.jersey.impl.model.RulesMap;
import com.sun.jersey.impl.uri.PathPattern;
import com.sun.jersey.impl.uri.PathTemplate;
import com.sun.jersey.impl.uri.rules.ResourceClassRule;
import com.sun.jersey.impl.uri.rules.RightHandPathRule;
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
        WebEngine engine = Framework.getService(WebEngine.class);
        boolean redirect = resourceConfig.getFeature(ResourceConfig.FEATURE_REDIRECT);
        for (WebApplicationMapping mapping : engine.getApplicationMappings()) {
            org.nuxeo.ecm.webengine.WebApplication app = engine.getApplication(mapping.getWebApp());
            Class<?> rc = app.getResourceType();
            if (rc == null) {
                rc = WebResource.class;
            }
            // Preload resource runtime meta data
            getResourceClass(rc); //TODO here we must be able to modify resource uri path...
            Path path = mapping.getPath();
//            String pathStr = path.segmentCount() == 0 ?
//                    "{traversalPath}"
//                    : path.toString()+"/{traversalPath}";
            String pathStr = null;
            boolean pathEndsInSlash = false;
            if (path.segmentCount() == 0) {
                pathStr = "/";
                pathEndsInSlash = true;
            } else {
                pathStr = path.toString();
            }
            UriTemplate t = new PathTemplate(pathStr, false);
            PathPattern p = new PathPattern(t, true);
            rules.put(p, new RightHandPathRule(
                    redirect,
                    pathEndsInSlash,
                    new ResourceClassRule(t, rc)));
        }
    }

}
