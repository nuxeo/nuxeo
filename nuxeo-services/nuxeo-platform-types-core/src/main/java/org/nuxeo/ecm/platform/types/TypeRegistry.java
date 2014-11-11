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

package org.nuxeo.ecm.platform.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.model.Extension;
import org.nuxeo.runtime.model.ExtensionRegistry;

public class TypeRegistry extends ExtensionRegistry<Type> {

    private static final Log log = LogFactory.getLog(TypeRegistry.class);

    protected volatile Map<String, Type> types = new HashMap<String, Type>();

    @Override
    public void removeContributions() {
        types = new HashMap<String, Type>();
    }

    @Override
    public void addContribution(Type contrib, Extension extension) {
        Type type = contrib;
        String typeId = type.getId();
        if (type.getRemove()) {
            log.debug("Removing type with id " + typeId);
            removeType(typeId);
        } else {
            if (hasType(typeId)) {
                type = mergeTypes(getType(typeId), type);
                removeType(typeId);
                log.debug("Merging type with id " + typeId);
            }
            addType(type);
            log.info("Registered platform document type: " + typeId);
        }
    }

    public synchronized void addType(Type type) {
        if (log.isDebugEnabled()) {
            log.debug("Registering type: " + type);
        }
        String id = type.getId();
        // do not add twice a type
        if (!types.containsKey(id)) {
            types.put(id, type);
        }
    }

    public synchronized boolean hasType(String id) {
        return types.containsKey(id);
    }

    public synchronized Type removeType(String id) {
        if (log.isDebugEnabled()) {
            log.debug("Unregistering type: " + id);
        }
        return types.remove(id);
    }

    public synchronized Collection<Type> getTypes() {
        return Collections.unmodifiableCollection(types.values());
    }

    public Type getType(String id) {
        return types.get(id);
    }

    public static Type mergeTypes(Type oldType, Type newType) {
        String icon = newType.getIcon();
        if (icon != null) {
            oldType.setIcon(icon);
        }
        String iconExpanded = newType.getIconExpanded();
        if (iconExpanded != null) {
            oldType.setIconExpanded(iconExpanded);
        }
        String bigIcon = newType.getBigIcon();
        if (bigIcon != null) {
            oldType.setBigIcon(bigIcon);
        }
        String bigIconExpanded = newType.getBigIconExpanded();
        if (bigIconExpanded != null) {
            oldType.setBigIconExpanded(bigIconExpanded);
        }
        String label = newType.getLabel();
        if (label != null) {
            oldType.setLabel(label);
        }
        String description = newType.getDescription();
        if (description != null) {
            oldType.setDescription(description);
        }
        String category = newType.getCategory();
        if (category != null) {
            oldType.setCategory(category);
        }
        Map<String, SubType> newTypeAllowedSubTypes = newType.getAllowedSubTypes();
        if (newTypeAllowedSubTypes != null) {
            Set<String> newTypeKeySet = newTypeAllowedSubTypes.keySet();
            Map<String, SubType> oldTypeAllowedSubTypes = oldType.getAllowedSubTypes();
            for (String newTypeKey : newTypeKeySet) {
                oldTypeAllowedSubTypes.put(newTypeKey,
                        newTypeAllowedSubTypes.get(newTypeKey));
            }
        }

        // Code added to delete the denied SubType from allowed subtype

        List<String> result = new ArrayList<String>();
        String[] deniedSubTypes = newType.getDeniedSubTypes();
        Map<String, SubType> oldTypeAllowedSubTypes = oldType.getAllowedSubTypes();
        boolean toAdd = true;

        if (oldTypeAllowedSubTypes != null) {
            Set<String> oldTypeKeySet = oldTypeAllowedSubTypes.keySet();
            for (String allowedSubType : oldTypeKeySet) {
                for (String deniedSubType : deniedSubTypes) {
                    if (deniedSubType.equals(allowedSubType)) {
                        toAdd = false;
                        break;
                    }
                }
                if (toAdd) {
                    result.add(allowedSubType);
                }
                toAdd = true;
            }
        }

        Map<String, SubType> mapResult = new HashMap<String, SubType>();
        for (String resultTypeName : result) {
            mapResult.put(resultTypeName,
                    oldTypeAllowedSubTypes.get(resultTypeName));
        }

        oldType.setAllowedSubTypes(mapResult);

        // end of added code

        String defaultView = newType.getDefaultView();
        if (defaultView != null) {
            oldType.setDefaultView(defaultView);
        }
        String createView = newType.getCreateView();
        if (createView != null) {
            oldType.setCreateView(createView);
        }
        String editView = newType.getEditView();
        if (editView != null) {
            oldType.setEditView(editView);
        }

        for (TypeView view : newType.getViews()) {
            oldType.setView(view);
        }

        // overwrite old layout
        FieldWidget[] layout = newType.getLayout();
        if (layout != null && layout.length != 0) {
            oldType.setLayout(layout);
        }

        Map<String, Layouts> layouts = newType.getLayouts();
        if (layouts != null) {
            Map<String, Layouts> layoutsMerged = new HashMap<String, Layouts>(
                    oldType.getLayouts());
            for (Map.Entry<String, Layouts> entry : layouts.entrySet()) {
                String key = entry.getKey();
                Layouts newLayouts = entry.getValue();
                if (layoutsMerged.containsKey(key) && newLayouts.getAppend()) {
                    List<String> allLayouts = new ArrayList<String>();
                    for (String layoutName : layoutsMerged.get(key).getLayouts()) {
                        allLayouts.add(layoutName);
                    }
                    for (String layoutName : newLayouts.getLayouts()) {
                        allLayouts.add(layoutName);
                    }
                    Layouts mergedLayouts = new Layouts();
                    mergedLayouts.layouts = allLayouts.toArray(new String[allLayouts.size()]);
                    layoutsMerged.put(key, mergedLayouts);
                } else {
                    layoutsMerged.put(key, newLayouts);
                }
            }
            oldType.setLayouts(layoutsMerged);
        }

        Map<String, DocumentContentViews> contentViews = newType.getContentViews();
        if (contentViews != null) {
            Map<String, DocumentContentViews> cvMerged = new HashMap<String, DocumentContentViews>(
                    oldType.getContentViews());
            for (Map.Entry<String, DocumentContentViews> entry : contentViews.entrySet()) {
                String key = entry.getKey();
                DocumentContentViews newContentViews = entry.getValue();
                if (cvMerged.containsKey(key) && newContentViews.getAppend()) {
                    List<DocumentContentView> allContentViews = new ArrayList<DocumentContentView>();
                    for (DocumentContentView cv : cvMerged.get(key).getContentViews()) {
                        allContentViews.add(cv);
                    }
                    for (DocumentContentView cv : newContentViews.getContentViews()) {
                        allContentViews.add(cv);
                    }
                    DocumentContentViews mergedContentViews = new DocumentContentViews();
                    mergedContentViews.contentViews = allContentViews.toArray(new DocumentContentView[allContentViews.size()]);
                    cvMerged.put(key, mergedContentViews);
                } else {
                    cvMerged.put(key, newContentViews);
                }
            }
            oldType.setContentViews(cvMerged);
        }

        // TODO: actions

        return oldType;
    }

}
