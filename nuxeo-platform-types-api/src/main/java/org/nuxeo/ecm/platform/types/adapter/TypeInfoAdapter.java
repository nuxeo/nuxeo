/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.types.adapter;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.types.FieldWidget;
import org.nuxeo.ecm.platform.types.Type;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.ecm.platform.types.TypeView;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 */
public class TypeInfoAdapter implements TypeInfo {

    private final Type type;

    public TypeInfoAdapter(DocumentModel doc) {
        try {
            TypeManager mgr = Framework.getService(TypeManager.class);
            type = mgr.getType(doc.getType());
        } catch (Exception e) {
            throw new RuntimeException("Failed to get document type", e);
        }
    }

    public String[] getActions() {
        if (type != null) {
            return type.getActions();
        }
        return null;
    }

    public String[] getAllowedSubTypes() {
        if (type != null) {
            return type.getAllowedSubTypes();
        }

        return null;
    }

    /**
     * @deprecated Use {@link #getId} instead.
     */
    @Deprecated
    public String getCoreType() {
        if (type != null) {
            return type.getId();
        }

        return null;
    }

    public String getCreateView() {
        if (type != null) {
            return type.getCreateView();
        }

        return null;
    }

    public String getDefaultView() {
        if (type != null) {
            return type.getDefaultView();
        }

        return null;
    }

    public String getEditView() {
        if (type != null) {
            return type.getEditView();
        }

        return null;
    }

    public String getIcon() {
        if (type != null) {
            return type.getIcon();
        }

        return null;
    }

    public String getIconExpanded() {
        if (type != null) {
            return type.getIconExpanded();
        }

        return null;
    }

    public String getId() {
        if (type != null) {
            return type.getId();
        }

        return null;
    }

    public String getLabel() {
        if (type != null) {
            return type.getLabel();
        }

        return null;
    }

    public FieldWidget[] getLayout() {
        if (type != null) {
            return type.getLayout();
        }

        return null;
    }

    public String[] getLayouts(String mode) {
        if (type != null) {
            return type.getLayouts(mode);
        }
        return null;
    }

    public String getView(String viewId) {
        if (type != null) {
            TypeView view = type.getView(viewId);
            if (view != null) {
                return view.getValue();
            }
        }
        return null;
    }

    public TypeView[] getViews() {
        if (type != null) {
            return type.getViews();
        }

        return null;
    }

}
