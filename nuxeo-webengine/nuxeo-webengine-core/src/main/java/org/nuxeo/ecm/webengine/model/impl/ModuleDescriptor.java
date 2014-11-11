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

import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.webengine.ResourceBinding;
import org.nuxeo.ecm.webengine.model.LinkDescriptor;
import org.nuxeo.ecm.webengine.model.Utils;
import org.nuxeo.ecm.webengine.model.WebModule;
import org.nuxeo.ecm.webengine.model.exceptions.WebSecurityException;
import org.nuxeo.ecm.webengine.security.Guard;
import org.nuxeo.ecm.webengine.security.GuardDescriptor;
import org.nuxeo.runtime.model.Adaptable;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@XObject("module")
public class ModuleDescriptor implements Cloneable {

    /**
     * The application directory.
     * Must be set by the client before registering the descriptor.
     */
    public File directory;

    @XNode("@name")
    public String name;

    /**
     * A fragment id to be used only if this contribution is patching another one
     */
    @XNode("@fragment")
    public String fragment;

    @XNode("@extends")
    public String base;

    @XNodeList(value="types/type", componentType=TypeDescriptor.class, type=ArrayList.class, nullByDefault=false)
    public ArrayList<TypeDescriptor> types;

    @XNodeList(value="actions/action", componentType=AdapterTypeImpl.class, type=ArrayList.class, nullByDefault=false)
    public ArrayList<AdapterTypeImpl> actions;

    @XNodeList(value="links/link", type=ArrayList.class, componentType=LinkDescriptor.class, nullByDefault=true)
    public List<LinkDescriptor> links;

    @XNodeList(value="resources/resource", type=ArrayList.class, componentType=ResourceBinding.class, nullByDefault=true)
    public List<ResourceBinding> resources;

    @XNode("permission")
    public  GuardDescriptor guardDescriptor;

    @XNode("templateFileExt")
    public String templateFileExt = "ftl";

    @XNodeList(value="media-types/media-type", type=MediaTypeRef[].class, componentType=MediaTypeRef.class, nullByDefault=true)
    public MediaTypeRef[] mediatTypeRefs;

    public ResourceBinding binding;

    private Guard guard;

    public void checkPermission(Adaptable adaptable) {
        if (!getGuard().check(adaptable)) {
            throw new WebSecurityException("Access Restricted");
        }
    }

    public Guard getGuard() {
        if (guard == null) {
            try {
                guard = guardDescriptor != null? guardDescriptor.getGuard() : Guard.DEFAULT;
            } catch (ParseException e) {
                e.printStackTrace();
                return null;
            }
        }
        return guard;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj.getClass() == ModuleDescriptor.class) {
            ModuleDescriptor dd = (ModuleDescriptor)obj;
            return dd.name.equals(name) && Utils.streq(dd.fragment, fragment);
        }
        return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ModuleDescriptor clone() {
        try {
            ModuleDescriptor cfg = (ModuleDescriptor) super.clone();
            cfg.actions = (ArrayList) actions.clone();
            cfg.types = (ArrayList) types.clone();
            return cfg;
        } catch (CloneNotSupportedException e) {
            throw new Error("Should never happen");
        }
    }

    public static ModuleDescriptor fromAnnotation(Class<?> clazz) {
        WebModule anno = clazz.getAnnotation(WebModule.class);
        if (anno == null) {
            return null;
        }
        ModuleDescriptor ad = new ModuleDescriptor();
        ad.name = anno.name();
        ad.fragment = Utils.nullIfEmpty(anno.fragment());
        ad.base = Utils.nullIfEmpty(anno.base());
        String guard = Utils.nullIfEmpty(anno.guard());
        if (guard != null) {
            ad.guardDescriptor = new GuardDescriptor();
            ad.guardDescriptor.setExpression(guard);
        }
        ad.actions = new ArrayList<AdapterTypeImpl>();
        ad.types = new ArrayList<TypeDescriptor>();
        return ad;
    }

}
