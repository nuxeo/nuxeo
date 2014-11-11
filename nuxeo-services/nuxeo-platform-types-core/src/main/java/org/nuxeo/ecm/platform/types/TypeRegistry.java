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

import org.nuxeo.runtime.model.ContributionFragmentRegistry;

public class TypeRegistry extends ContributionFragmentRegistry<Type> {

    protected Map<String, Type> types = new HashMap<String, Type>();

    @Override
    public String getContributionId(Type contrib) {
        return contrib.getId();
    }

    @Override
    public void contributionUpdated(String id, Type contrib, Type newOrigContrib) {
        if (contrib.getRemove()) {
            types.remove(id);
        } else {
            types.put(id, contrib);
        }
    }

    @Override
    public void contributionRemoved(String id, Type origContrib) {
        types.remove(id);
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
                    oldTypeAllowedSubTypes.put(newTypeKey,
                            newTypeAllowedSubTypes.get(newTypeKey));
                }

            }

            // Code added to delete the denied SubType from allowed subtypes

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
                Map<String, Layouts> layoutsMerged = new HashMap<String, Layouts>(
                        oldType.getLayouts());
                for (Map.Entry<String, Layouts> entry : layouts.entrySet()) {
                    String key = entry.getKey();
                    Layouts newLayouts = entry.getValue();
                    if (layoutsMerged.containsKey(key)
                            && newLayouts.getAppend()) {
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
        }

        Map<String, DocumentContentViews> contentViews = newType.getContentViews();
        if (wasRemoved) {
            oldType.setContentViews(contentViews);
        } else {
            if (contentViews != null) {
                Map<String, DocumentContentViews> cvMerged = new HashMap<String, DocumentContentViews>(
                        oldType.getContentViews());
                for (Map.Entry<String, DocumentContentViews> entry : contentViews.entrySet()) {
                    String key = entry.getKey();
                    DocumentContentViews newContentViews = entry.getValue();
                    if (cvMerged.containsKey(key)
                            && newContentViews.getAppend()) {
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

}
