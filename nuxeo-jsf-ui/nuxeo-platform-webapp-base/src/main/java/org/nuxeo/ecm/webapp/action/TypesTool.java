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

package org.nuxeo.ecm.webapp.action;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.ScopeType.EVENT;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.types.Type;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.ecm.webapp.seam.NuxeoSeamHotReloader;

/**
 * Document type service for document type creation.
 *
 * @author eionica@nuxeo.com
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
@Name("typesTool")
@Scope(CONVERSATION)
@Install(precedence = Install.FRAMEWORK)
public class TypesTool implements Serializable {

    private static final long serialVersionUID = -5037578301250616973L;

    protected static final Log log = LogFactory.getLog(TypesTool.class);

    private static final int COLUMN_SIZE = 4;

    @In(create = true)
    protected transient TypeManager typeManager;

    @In(create = true)
    protected NuxeoSeamHotReloader seamReload;

    protected Map<String, List<List<Type>>> typesMap;

    protected Long typesMapTimestamp;

    protected Type selectedType;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @Observer(value = { EventNames.CONTENT_ROOT_SELECTION_CHANGED, EventNames.DOCUMENT_SELECTION_CHANGED,
            EventNames.DOMAIN_SELECTION_CHANGED, EventNames.LOCAL_CONFIGURATION_CHANGED }, create = false)
    @BypassInterceptors
    public void resetTypesList() {
        typesMap = null;
        typesMapTimestamp = null;
    }

    /**
     * Retrieves the list of allowed sub types given a current type.
     * <p>
     * This is used at creation time. Current type is retrieved thanks to the document model hold and passed by the
     * event.
     */
    public void populateTypesList() {
        boolean set = false;
        DocumentModel model = getCurrentItem();
        if (model != null) {
            typesMap = getOrganizedTypeMapForDocumentType(model.getType());
            set = true;
        }
        if (!set) {
            // set an empty list
            typesMap = new HashMap<>();
        }
        typesMapTimestamp = typeManager.getLastModified();
    }

    public Map<String, List<List<Type>>> getOrganizedTypeMapForDocumentType(String type) {
        Map<String, List<Type>> docTypesMap = typeManager.getTypeMapForDocumentType(type, getConfigurationDocument());
        docTypesMap = filterTypeMap(docTypesMap);
        return organizeType(docTypesMap);
    }

    /**
     * Returns the Configuration document to be used as the local configuration of the {@code TypeManager}.
     * <p>
     * This method can be overridden by Subclasses to define a specific Configuration document.
     *
     * @since 5.4.2
     */
    protected DocumentModel getConfigurationDocument() {
        return navigationContext.getCurrentDocument();
    }

    /**
     * Method to be overridden by subclasses to filter the type Map.
     *
     * @since 5.4.2
     */
    protected Map<String, List<Type>> filterTypeMap(Map<String, List<Type>> docTypeMap) {
        return docTypeMap;
    }

    /**
     * Split each @{code List} of {@code Type} in one or more new {@code List}, with maximum 4 {@code Type}s in each new
     * {@code List} and returns the new computed {@code Map}.
     */
    protected Map<String, List<List<Type>>> organizeType(Map<String, List<Type>> types) {
        Map<String, List<List<Type>>> newTypesMap = new HashMap<>();
        Set<Entry<String, List<Type>>> typeEntrySet = types.entrySet();
        for (Entry<String, List<Type>> set : typeEntrySet) {
            List<Type> typeList = set.getValue();
            List<List<Type>> newList = new ArrayList<>();
            int index = 0;
            newList.add(index, new ArrayList<Type>());
            for (Type type : typeList) {
                List<Type> currentList = newList.get(index);
                if (currentList == null) {
                    currentList = new ArrayList<>();
                    newList.add(index, currentList);
                }
                currentList.add(type);
                if (currentList.size() % COLUMN_SIZE == 0) {
                    index++;
                    newList.add(index, new ArrayList<Type>());
                }
            }
            newTypesMap.put(set.getKey(), newList);
        }
        return newTypesMap;
    }

    public Type getSelectedType() {
        if (selectedType != null) {
            log.debug("Returning selected type with id: " + selectedType.getId());
        }
        return selectedType;
    }

    /**
     * If the selected type is supposed to be automatically injected by Seam through @DataModelSelection callback (i.e.
     * the user will select the type from a list), this method should be called with <code>null</code> parameter before.
     */
    public void setSelectedType(Type type) {
        if (typesMap == null) {
            populateTypesList();
        }
        selectedType = type;
    }

    @Factory(value = "typesMap", scope = EVENT)
    public Map<String, List<List<Type>>> getTypesList() {
        // XXX : should cache per currentDocument type
        if (typesMap == null
                || (seamReload.isDevModeSet() && seamReload.shouldResetCache(typeManager, typesMapTimestamp))) {
            // cache the list of allowed subtypes
            populateTypesList();
        }
        selectedType = null;
        return typesMap;
    }

    public void setTypesList(Map<String, List<List<Type>>> typesList) {
        this.typesMap = typesList;
    }

    public Type getType(String typeName) {
        return typeManager.getType(typeName);
    }

    public boolean hasType(String typeName) {
        return typeManager.hasType(typeName);
    }

    protected DocumentModel getCurrentItem() {
        return navigationContext.getCurrentDocument();
    }

}
