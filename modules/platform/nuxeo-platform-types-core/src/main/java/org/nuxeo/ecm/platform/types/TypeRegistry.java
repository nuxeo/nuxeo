/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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

import org.nuxeo.ecm.core.schema.DocumentTypeDescriptor;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.SchemaManagerImpl;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;

public class TypeRegistry extends ContributionFragmentRegistry<Type> {

    protected Map<String, Type> types = new HashMap<>();

    protected Map<String, DocumentTypeDescriptor> dtds = new HashMap<>();

    @Override
    public String getContributionId(Type contrib) {
        return contrib.getId();
    }

    @Override
    public void contributionUpdated(String id, Type contrib, Type newOrigContrib) {
        if (contrib.getRemove()) {
            types.remove(id);
            removeCoreContribution(id);
        } else {
            types.put(id, contrib);
            updateCoreContribution(id, contrib);
        }
    }

    @Override
    public void contributionRemoved(String id, Type origContrib) {
        types.remove(id);
        removeCoreContribution(id);
    }

    @Override
    public Type clone(Type orig) {
        if (orig != null) {
            return orig.clone();
        }
        return null;
    }

    @Override
    public void merge(Type newType, Type oldType) {
        boolean remove = newType.getRemove();
        // keep old remove info: if old type was removed, new type should
        // replace the old one completely
        boolean wasRemoved = oldType.getRemove();
        oldType.setRemove(remove);
        if (remove) {
            // don't bother merging
            return;
        }

        String icon = newType.getIcon();
        if (icon != null || wasRemoved) {
            oldType.setIcon(icon);
        }
        String iconExpanded = newType.getIconExpanded();
        if (iconExpanded != null || wasRemoved) {
            oldType.setIconExpanded(iconExpanded);
        }
        String bigIcon = newType.getBigIcon();
        if (bigIcon != null || wasRemoved) {
            oldType.setBigIcon(bigIcon);
        }
        String bigIconExpanded = newType.getBigIconExpanded();
        if (bigIconExpanded != null || wasRemoved) {
            oldType.setBigIconExpanded(bigIconExpanded);
        }
        String label = newType.getLabel();
        if (label != null || wasRemoved) {
            oldType.setLabel(label);
        }
        String description = newType.getDescription();
        if (description != null || wasRemoved) {
            oldType.setDescription(description);
        }
        String category = newType.getCategory();
        if (category != null || wasRemoved) {
            oldType.setCategory(category);
        }

        Map<String, SubType> newTypeAllowedSubTypes = newType.getAllowedSubTypes();
        if (wasRemoved) {
            oldType.setAllowedSubTypes(newTypeAllowedSubTypes);
        } else {
            if (newTypeAllowedSubTypes != null) {
                Set<String> newTypeKeySet = newTypeAllowedSubTypes.keySet();
                Map<String, SubType> oldTypeAllowedSubTypes = oldType.getAllowedSubTypes();
                for (String newTypeKey : newTypeKeySet) {
                    oldTypeAllowedSubTypes.put(newTypeKey, newTypeAllowedSubTypes.get(newTypeKey));
                }

            }

            // Code added to delete the denied SubType from allowed subtypes

            List<String> result = new ArrayList<>();
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

            Map<String, SubType> mapResult = new HashMap<>();
            for (String resultTypeName : result) {
                mapResult.put(resultTypeName, oldTypeAllowedSubTypes.get(resultTypeName));
            }

            oldType.setAllowedSubTypes(mapResult);

            // end of added code
        }

        String defaultView = newType.getDefaultView();
        if (defaultView != null || wasRemoved) {
            oldType.setDefaultView(defaultView);
        }
        String createView = newType.getCreateView();
        if (createView != null || wasRemoved) {
            oldType.setCreateView(createView);
        }
        String editView = newType.getEditView();
        if (editView != null || wasRemoved) {
            oldType.setEditView(editView);
        }

        for (TypeView view : newType.getViews()) {
            oldType.setView(view);
        }

        Map<String, Layouts> layouts = newType.getLayouts();
        if (wasRemoved) {
            oldType.setLayouts(layouts);
        } else {
            if (layouts != null) {
                Map<String, Layouts> layoutsMerged = new HashMap<>(oldType.getLayouts());
                for (Map.Entry<String, Layouts> entry : layouts.entrySet()) {
                    String key = entry.getKey();
                    Layouts newLayouts = entry.getValue();
                    if (layoutsMerged.containsKey(key) && newLayouts.getAppend()) {
                        List<String> allLayouts = new ArrayList<>();
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
        }

        Map<String, DocumentContentViews> contentViews = newType.getContentViews();
        if (wasRemoved) {
            oldType.setContentViews(contentViews);
        } else {
            if (contentViews != null) {
                Map<String, DocumentContentViews> cvMerged = new HashMap<>(
                        oldType.getContentViews());
                for (Map.Entry<String, DocumentContentViews> entry : contentViews.entrySet()) {
                    String key = entry.getKey();
                    DocumentContentViews newContentViews = entry.getValue();
                    if (cvMerged.containsKey(key) && newContentViews.getAppend()) {
                        List<DocumentContentView> allContentViews = new ArrayList<>();
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
        }
    }

    public boolean hasType(String id) {
        return types.containsKey(id);
    }

    public Collection<Type> getTypes() {
        return Collections.unmodifiableCollection(types.values());
    }

    public Type getType(String id) {
        return types.get(id);
    }

    /**
     * @since 8.10
     */
    protected void recomputeTypes() {
        for (Type type : types.values()) {
            type.setAllowedSubTypes(getCoreAllowedSubtypes(type));
            //  do not need to add denied subtypes because allowed subtypes already come filtered from core
            type.setDeniedSubTypes(new String[0]);
        }
    }

    /**
     * @since 8.10
     */
    protected Map<String, SubType> getCoreAllowedSubtypes(Type type) {
        SchemaManager schemaManager = Framework.getService(SchemaManager.class);
        Collection<String> coreAllowedSubtypes = schemaManager.getAllowedSubTypes(type.getId());
        if (coreAllowedSubtypes == null) {
            // there are no subtypes to take care of
            return Collections.emptyMap();
        }

        Map<String, SubType> ecmSubTypes = type.getAllowedSubTypes();
        Map<String, SubType> allowedSubTypes = new HashMap<>();
        SubType subtype;
        for (String name : coreAllowedSubtypes) {
            if (ecmSubTypes.containsKey(name)) {
                subtype = ecmSubTypes.get(name);
            } else {
                subtype = new SubType();
                subtype.setName(name);
            }
            allowedSubTypes.put(name, subtype);
        }

        return allowedSubTypes;
    }

    /**
     * @since 8.4
     */
    protected void updateCoreContribution(String id, Type contrib) {
        SchemaManagerImpl schemaManager = (SchemaManagerImpl) Framework.getService(SchemaManager.class);

        // if there's already a core contribution, unregiser it and register a new one
        if (dtds.containsKey(id)) {
            schemaManager.unregisterDocumentType(dtds.get(id));
            dtds.remove(id);
        }

        DocumentTypeDescriptor dtd = new DocumentTypeDescriptor();
        dtd.name = contrib.getId();
        dtd.subtypes = contrib.getAllowedSubTypes().keySet().toArray(new String[contrib.getAllowedSubTypes().size()]);
        dtd.forbiddenSubtypes = contrib.getDeniedSubTypes();
        dtd.append = true;

        // only make a core contrib if there are changes on subtypes
        if (dtd.subtypes.length > 0 || dtd.forbiddenSubtypes.length > 0) {
            dtds.put(id, dtd);
            schemaManager.registerDocumentType(dtd);
        }
    }

    /**
     * @since 8.4
     */
    protected void removeCoreContribution(String id) {
        if (dtds.containsKey(id)) {
            SchemaManagerImpl schemaManager = (SchemaManagerImpl) Framework.getService(SchemaManager.class);
            schemaManager.unregisterDocumentType(dtds.get(id));
            dtds.remove(id);
        }
    }

}
