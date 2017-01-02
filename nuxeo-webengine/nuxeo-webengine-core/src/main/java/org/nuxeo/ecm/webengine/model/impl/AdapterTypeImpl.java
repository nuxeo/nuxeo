/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 */
public class AdapterTypeImpl extends AbstractResourceType implements AdapterType {

    // we are using arrays and not sets since the targetTypes and targetFacets have usually very small sizes
    protected String targetType;

    protected String[] targetFacets;

    protected final String adapterName;

    public AdapterTypeImpl(WebEngine engine, ModuleImpl module, ResourceTypeImpl superType, String name,
            String adapterName, ClassProxy clazz, int visibility) {
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
                throw WebException.wrap("Failed to parse guard: " + g + " on WebObject " + c.getName(), e);
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
