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
import org.nuxeo.ecm.webengine.model.Access;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.security.PermissionService;
import org.nuxeo.ecm.webengine.security.guards.And;
import org.nuxeo.ecm.webengine.security.guards.IsAdministratorGuard;
import org.nuxeo.runtime.annotations.AnnotationManager;

import com.sun.jersey.server.spi.component.ResourceComponentConstructor;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ResourceTypeImpl extends AbstractResourceType {

    public ResourceTypeImpl(WebEngine engine, ModuleImpl module, ResourceTypeImpl superType, String name,
            ClassProxy clazz, ResourceComponentConstructor constructor, int visibility) {
        super(engine, module, superType, name, clazz, constructor, visibility);
    }

    @Override
    protected void loadAnnotations(AnnotationManager annoMgr) {
        Class<?> c = clazz.get();
        WebObject wo = c.getAnnotation(WebObject.class);
        if (wo == null) {
            return;
        }
        String g = wo.guard();
        if (g != null && g.length() > 0) {
            try {
                guard = PermissionService.parse(g);
            } catch (ParseException e) {
                throw WebException.wrap("Failed to parse guard: " + g + " on WebObject " + c.getName(), e);
            }
        } else {
            loadGuardFromAnnoation(c);
        }
        Access requireAdministrators = wo.administrator();
        if (requireAdministrators != Access.NULL) {
            if (guard != null) {
                guard = new And(new IsAdministratorGuard(requireAdministrators), guard);
            } else {
                guard = new IsAdministratorGuard(requireAdministrators);
            }
        }
        String[] facets = wo.facets();
        if (facets != null && facets.length > 0) {
            this.facets = new HashSet<String>(Arrays.asList(facets));
        }
    }

}
