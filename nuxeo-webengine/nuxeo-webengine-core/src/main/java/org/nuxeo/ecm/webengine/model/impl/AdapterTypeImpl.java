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

package org.nuxeo.ecm.webengine.model.impl;

import java.text.ParseException;
import java.util.Arrays;
import java.util.HashSet;

import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.loader.ClassProxy;
import org.nuxeo.ecm.webengine.model.AdapterType;
import org.nuxeo.ecm.webengine.model.Resource;
import org.nuxeo.ecm.webengine.model.ResourceType;
import org.nuxeo.ecm.webengine.model.WebAdapter;
import org.nuxeo.ecm.webengine.security.PermissionService;
import org.nuxeo.runtime.annotations.AnnotationManager;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class AdapterTypeImpl extends AbstractResourceType implements AdapterType {

    // we are using arrays and not sets since the targetTypes and targetFacets have usually very small sizes
    protected String targetType;
    protected String[] targetFacets;
    protected final String adapterName;


    public AdapterTypeImpl(WebEngine engine, ModuleImpl module, ResourceTypeImpl superType, String name, String adapterName, ClassProxy clazz, int visibility) {
        super(engine, module, superType, name, clazz, visibility);
        this.adapterName = adapterName;
    }

    public String getAdapterName() {
        return adapterName;
    }

    public String getTargetType() {
        return targetType;
    }

    public String[] getTargetFacets() {
        return targetFacets;
    }

    public boolean acceptResource(Resource resource) {
        if (acceptType(resource.getType())) {
            if (targetFacets != null && targetFacets.length > 0) {
                String[] facets = targetFacets; // make a local copy to avoid parallel type definition updates
                for (String f : facets) {
                    if (!resource.hasFacet(f)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public boolean acceptType(ResourceType type) {
        if (targetType == null || targetType == ROOT_TYPE_NAME) {
            return true;
        }
        return type.isDerivedFrom(targetType);
    }

    @Override
    protected void loadAnnotations(AnnotationManager annoMgr) {
        Class<?> c = clazz.get();
        WebAdapter ws = c.getAnnotation(WebAdapter.class);
        if (ws == null) {
            return;
        }
        String g = ws.guard();
        if (g != null && g.length() > 0) {
            try {
                guard = PermissionService.parse(g);
            } catch (ParseException e) {
                throw WebException.wrap("Failed to parse guard: "+g+" on WebObject "+c.getName(), e);
            }
        } else {
            loadGuardFromAnnoation(c);
        }
        String[] facets = ws.facets();
        if (facets != null && facets.length > 0) {
            this.facets = new HashSet<String>(Arrays.asList(facets));
        }
        targetType = ws.targetType();
        String[] targetFacets = ws.targetFacets();
        if (targetFacets != null && targetFacets.length > 0) {
            this.targetFacets = targetFacets;
        }
    }

}
