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
import org.nuxeo.ecm.webengine.model.Access;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.security.PermissionService;
import org.nuxeo.ecm.webengine.security.guards.And;
import org.nuxeo.ecm.webengine.security.guards.IsAdministratorGuard;
import org.nuxeo.runtime.annotations.AnnotationManager;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ResourceTypeImpl extends AbstractResourceType {

    public ResourceTypeImpl(WebEngine engine, ModuleImpl module, ResourceTypeImpl superType, String name, ClassProxy clazz, int visibility) {
        super(engine, module, superType, name, clazz, visibility);
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
                throw WebException.wrap("Failed to parse guard: "+g+" on WebObject "+c.getName(), e);
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
