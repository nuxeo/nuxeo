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

import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.loader.ClassProxy;
import org.nuxeo.ecm.webengine.model.Guard;
import org.nuxeo.ecm.webengine.model.ModuleType;
import org.nuxeo.ecm.webengine.model.WebModule;
import org.nuxeo.ecm.webengine.security.PermissionService;
import org.nuxeo.runtime.annotations.AnnotationManager;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ModuleTypeImpl extends AbstractResourceType implements ModuleType {

    public ModuleTypeImpl(ModuleImpl module, AbstractResourceType superType, String name, ClassProxy clazz) {
        super(module, superType, name, clazz);
    }

    @Override
    protected void loadAnnotations(AnnotationManager annoMgr) {
        Class<?> c = clazz.get();
        WebModule wm = c.getAnnotation(WebModule.class);
        if (wm == null) {
            return;
        }
        String g = wm.guard();
        if (g != null && g.length() > 0) {
            try {
                guard = PermissionService.parse(g);
            } catch (ParseException e) {
                throw WebException.wrap("Failed to parse guard: "+g+" on WebObject "+c.getName(), e);
            }
        } else {
            loadGuardFromAnnoation(c);
        }
        String[] facets = wm.facets();
        if (facets != null && facets.length > 0) {
            this.facets = new HashSet<String>(Arrays.asList(facets));
        }
    }

    @Override
    protected void loadGuardFromAnnoation(Class<?> c) {
        Guard ag = c.getAnnotation(Guard.class);
        if (ag != null) {
            String g = ag.value();
            if (g != null && g.length() > 0) {
                try {
                    guard = PermissionService.parse(g);
                } catch (ParseException e) {
                    throw WebException.wrap("Failed to parse guard: "+g+" on WebObject "+c.getName(), e);
                }
            } else {
                Class<?> gc = ag.type();
                if (gc != null) {
                    try {
                        guard = (org.nuxeo.ecm.webengine.security.Guard)gc.newInstance();
                    } catch (Exception e) {
                        throw WebException.wrap(
                                "Failed to instantiate guard handler: "+gc.getName()+" on WebObject "+c.getName(), e);
                    }
                }
            }
        }
    }

}
