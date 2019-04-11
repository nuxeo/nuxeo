/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.types.adapter;

import java.util.LinkedHashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.types.DocumentContentViews;
import org.nuxeo.ecm.platform.types.SubType;
import org.nuxeo.ecm.platform.types.Type;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.ecm.platform.types.TypeView;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class TypeInfoAdapter implements TypeInfo {

    private final Type type;

    public TypeInfoAdapter(DocumentModel doc) {
        TypeManager mgr = Framework.getService(TypeManager.class);
        type = mgr.getType(doc.getType());
    }

    public String[] getActions() {
        if (type != null) {
            return type.getActions();
        }
        return null;
    }

    @Override
    public Map<String, SubType> getAllowedSubTypes() {
        if (type != null) {
            return type.getAllowedSubTypes();
        }

        return null;
    }

    @Override
    public String getCreateView() {
        if (type != null) {
            return type.getCreateView();
        }

        return null;
    }

    @Override
    public String getDefaultView() {
        if (type != null) {
            return type.getDefaultView();
        }

        return null;
    }

    @Override
    public String getEditView() {
        if (type != null) {
            return type.getEditView();
        }

        return null;
    }

    @Override
    public String getIcon() {
        if (type != null) {
            return type.getIcon();
        }

        return null;
    }

    @Override
    public String getIconExpanded() {
        if (type != null) {
            return type.getIconExpanded();
        }

        return null;
    }

    @Override
    public String getBigIcon() {
        if (type != null) {
            return type.getBigIcon();
        }
        return null;
    }

    @Override
    public String getBigIconExpanded() {
        if (type != null) {
            return type.getBigIconExpanded();
        }
        return null;
    }

    @Override
    public String getId() {
        if (type != null) {
            return type.getId();
        }

        return null;
    }

    @Override
    public String getLabel() {
        if (type != null) {
            return type.getLabel();
        }

        return null;
    }

    @Override
    public String getDescription() {
        if (type != null) {
            return type.getDescription();
        }

        return null;
    }

    @Override
    public String[] getLayouts(String mode) {
        if (type != null) {
            return type.getLayouts(mode);
        }
        return null;
    }

    @Override
    public String[] getLayouts(String mode, String defaultMode) {
        if (type != null) {
            return type.getLayouts(mode, defaultMode);
        }
        return null;
    }

    @Override
    public String getView(String viewId) {
        if (type != null) {
            TypeView view = type.getView(viewId);
            if (view != null) {
                return view.getValue();
            }
        }
        return null;
    }

    @Override
    public TypeView[] getViews() {
        if (type != null) {
            return type.getViews();
        }

        return null;
    }

    @Override
    public String[] getContentViews(String category) {
        if (type != null) {
            return type.getContentViews(category);
        }
        return null;
    }

    @Override
    public Map<String, String[]> getContentViews() {
        if (type != null) {
            Map<String, String[]> res = new LinkedHashMap<>();
            Map<String, DocumentContentViews> defs = type.getContentViews();
            if (defs != null) {
                for (Map.Entry<String, DocumentContentViews> def : defs.entrySet()) {
                    res.put(def.getKey(), def.getValue().getContentViewNames());
                }
            }
            return res;
        }
        return null;
    }

    @Override
    public Map<String, String[]> getContentViewsForExport() {
        if (type != null) {
            Map<String, String[]> res = new LinkedHashMap<>();
            Map<String, DocumentContentViews> defs = type.getContentViews();
            if (defs != null) {
                for (Map.Entry<String, DocumentContentViews> def : defs.entrySet()) {
                    String[] cvsByCat = def.getValue().getContentViewNamesForExport();
                    if (cvsByCat != null && cvsByCat.length > 0) {
                        res.put(def.getKey(), cvsByCat);
                    }
                }
            }
            return res;
        }
        return null;
    }
}
