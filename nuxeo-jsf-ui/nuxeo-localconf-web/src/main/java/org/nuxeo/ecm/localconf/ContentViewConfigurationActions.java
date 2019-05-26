/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 * Contributors:
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.localconf;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.model.SelectItem;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.platform.contentview.jsf.ContentViewHeader;
import org.nuxeo.ecm.platform.contentview.jsf.ContentViewService;
import org.nuxeo.ecm.platform.types.Type;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.component.SelectItemComparator;
import org.nuxeo.runtime.api.Framework;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.nuxeo.ecm.core.schema.FacetNames.FOLDERISH;

@Name("contentViewConfigurationActions")
@Scope(CONVERSATION)
@Install(precedence = Install.FRAMEWORK)
public class ContentViewConfigurationActions implements Serializable {

    private static final long serialVersionUID = 1L;

    protected transient SchemaManager schemaManager;

    @In(create = true)
    protected transient TypeManager typeManager;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In(create = true)
    protected ContentViewService contentViewService;

    @In(create = true)
    protected Map<String, String> messages;

    protected SchemaManager getSchemaManager() {
        if (schemaManager == null) {
            schemaManager = Framework.getService(SchemaManager.class);
        }
        return schemaManager;
    }

    public List<SelectItem> getAvailableDocTypes() {
        List<SelectItem> items = new ArrayList<>();
        Set<String> folderishDocTypeNames = getSchemaManager().getDocumentTypeNamesForFacet(FOLDERISH);
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        String currentDocTypeName = currentDocument.getType();
        Collection<Type> allowedSubTypes = typeManager.findAllAllowedSubTypesFrom(currentDocTypeName);
        Type currentDocType = typeManager.getType(currentDocTypeName);
        if (!allowedSubTypes.contains(currentDocType)) {
            allowedSubTypes.add(currentDocType);
        }
        for (Type type : allowedSubTypes) {
            String typeName = type.getId();
            if (!folderishDocTypeNames.contains(typeName)) {
                continue;
            }
            SelectItem item;
            if (messages.containsKey(typeName)) {
                item = new SelectItem(typeName, messages.get(typeName));
            } else {
                item = new SelectItem(typeName);
            }
            items.add(item);
        }
        Collections.sort(items, new SelectItemComparator("label", true));
        return items;
    }

    public List<SelectItem> getAvailableContentViews() {
        List<SelectItem> items = new ArrayList<>();
        for (String cvName : contentViewService.getContentViewNames()) { // TODO : use flag ?
            ContentViewHeader contentViewHeader = contentViewService.getContentViewHeader(cvName);
            String title = contentViewHeader.getTitle();
            SelectItem item;
            if (title == null) {
                item = new SelectItem(cvName);
            } else {
                if (contentViewHeader.isTranslateTitle()) {
                    title = messages.get(title);
                }
                item = new SelectItem(cvName, title);
            }
            items.add(item);
        }
        Collections.sort(items, new SelectItemComparator("label", true));
        return items;
    }

}
