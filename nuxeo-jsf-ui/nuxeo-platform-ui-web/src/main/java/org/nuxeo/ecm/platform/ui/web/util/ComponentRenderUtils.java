/*
 * Copyright (C) 2007 Exadel, Inc.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License version 2.1 as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 *
 * Rich Faces - Natural Ajax for Java Server Faces (JSF)
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.ui.web.util;

import java.util.Iterator;

import javax.faces.component.NamingContainer;
import javax.faces.component.UIComponent;
import javax.faces.component.UINamingContainer;
import javax.faces.context.FacesContext;

/**
 * Helper methods for manipulating components thanks to given ids.
 * <p>
 * Most of helpers come from RichFaces 3.3.1 code (aligned on JSF 1.2).
 *
 * @since 6.0
 */
public class ComponentRenderUtils {

    public static String getComponentAbsoluteId(UIComponent base, String targetId) {
        if (targetId == null || targetId.startsWith(":")) {
            return targetId;
        }
        String id = targetId;
        UIComponent target = findComponentFor(base, id);
        if (target != null) {
            id = getAbsoluteId(target);
        }
        return id;
    }

    public static UIComponent getComponent(UIComponent base, String targetId) {
        String id = getComponentAbsoluteId(base, targetId);
        FacesContext ctx = FacesContext.getCurrentInstance();
        UIComponent anchor = ctx.getViewRoot().findComponent(id);
        return anchor;
    }

    public static UIComponent findComponentFor(UIComponent component, String id) {
        if (id == null) {
            throw new NullPointerException("id is null!");
        }
        if (id.length() == 0) {
            return null;
        }
        UIComponent target = null;
        UIComponent parent = component;
        UIComponent root = component;
        while (target == null && parent != null) {
            target = findUIComponentBelow(parent, id);
            root = parent;
            parent = parent.getParent();
        }
        if (target == null) {
            target = findUIComponentBelow(root, id);
        }
        return target;
    }

    protected static UIComponent findUIComponentBelow(UIComponent root, String id) {
        UIComponent target = null;
        for (Iterator<UIComponent> iter = root.getFacetsAndChildren(); iter.hasNext();) {
            UIComponent child = iter.next();
            if (child instanceof NamingContainer) {
                try {
                    target = child.findComponent(id);
                } catch (IllegalArgumentException iae) {
                    continue;
                }
            }
            if (target == null && child.getChildCount() > 0) {
                target = findUIComponentBelow(child, id);
            }
            if (target != null) {
                break;
            }
        }
        return target;
    }

    public static String getAbsoluteId(UIComponent component) {
        FacesContext faces = FacesContext.getCurrentInstance();
        return UINamingContainer.getSeparatorChar(faces) + component.getClientId(FacesContext.getCurrentInstance());
    }

}
