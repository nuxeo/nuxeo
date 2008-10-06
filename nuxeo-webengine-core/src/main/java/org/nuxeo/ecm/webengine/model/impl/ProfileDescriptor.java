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
import org.nuxeo.ecm.webengine.exceptions.WebSecurityException;
import org.nuxeo.ecm.webengine.model.WebProfile;
import org.nuxeo.ecm.webengine.security.Guard;
import org.nuxeo.ecm.webengine.security.GuardDescriptor;
import org.nuxeo.runtime.model.Adaptable;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@XObject("profile")
public class ProfileDescriptor implements Cloneable {

    /**
     * The application directory
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

    @XNodeList(value="actions/action", componentType=ServiceTypeImpl.class, type=ArrayList.class, nullByDefault=false)
    public ArrayList<ServiceTypeImpl> actions;

    @XNode("script-extension")
    public String scriptExtension = "groovy";

    @XNode("template-extension")
    public String templateExtension = "ftl";

    @XNodeList(value="roots/root", type=ArrayList.class, componentType=String.class, nullByDefault=true)
    public List<String> roots;

    @XNode("permission")
    public  GuardDescriptor guardDescriptor;

    private Guard guard;

    public void checkPermission(Adaptable adaptable) throws WebSecurityException {
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
        if (obj.getClass() == ProfileDescriptor.class) {
            ProfileDescriptor dd = (ProfileDescriptor)obj;
            return dd.name.equals(name) && Utils.streq(dd.fragment, fragment);
        }
        return false;
    }
    
    @Override
    public ProfileDescriptor clone() {
        try {
            ProfileDescriptor cfg = (ProfileDescriptor)super.clone();
            cfg.actions = (ArrayList)actions.clone();
            cfg.types = (ArrayList)types.clone();
            return cfg; 
        } catch (CloneNotSupportedException e) {
            throw new Error("Should never happen");
        }
    }

    
    public static ProfileDescriptor fromAnnotation(Class<?> clazz) {
        WebProfile anno = clazz.getAnnotation(WebProfile.class);
        if (anno == null) {
            return null;
        }
        ProfileDescriptor ad = new ProfileDescriptor();
        ad.name = anno.name();
        ad.templateExtension = anno.templateExtension();
        ad.scriptExtension = anno.scriptExtension();
        ad.fragment = Utils.nullIfEmpty(anno.fragment());
        ad.base = Utils.nullIfEmpty(anno.base());
        String guard = Utils.nullIfEmpty(anno.guard());
        if (guard != null) {
            ad.guardDescriptor = new GuardDescriptor();
            ad.guardDescriptor.setExpression(guard);
        }
        String[] roots = anno.roots();
        if (roots.length > 0) {
            ad.roots = new ArrayList<String>(); 
            for (int i=0; i<roots.length; i++) {
                ad.roots.add(roots[i]);
            }
        }
        ad.actions = new ArrayList<ServiceTypeImpl>();
        ad.types = new ArrayList<TypeDescriptor>();        
        return ad;
    }

}
