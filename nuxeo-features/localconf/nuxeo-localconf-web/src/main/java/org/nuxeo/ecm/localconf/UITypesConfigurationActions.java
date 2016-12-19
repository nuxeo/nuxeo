/*
 * (C) Copyright 2010-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.localconf;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.nuxeo.ecm.platform.types.localconfiguration.UITypesConfigurationConstants
        .UI_TYPES_CONFIGURATION_FACET;
import static org.nuxeo.ecm.platform.types.localconfiguration.UITypesConfigurationConstants
        .UI_TYPES_DEFAULT_NEEDED_SCHEMA;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.platform.types.Type;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.ecm.platform.types.localconfiguration.UITypesConfiguration;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
@Name("typesConfigurationActions")
@Scope(CONVERSATION)
@Install(precedence = Install.FRAMEWORK)
public class UITypesConfigurationActions implements Serializable {

    private static final long serialVersionUID = 1L;

    @In(create = true)
    protected Map<String, String> messages;

    /**
     * @since 5.9.1
     */
    protected static class TypeLabelAlphabeticalOrder implements Comparator<Type> {

        private final Map<String, String> messages;

        public TypeLabelAlphabeticalOrder(Map<String, String> messages) {
            super();
            this.messages = messages;
        }

        @Override
        public int compare(Type type1, Type type2) {
            String label1 = messages.get(type1.getLabel());
            String label2 = messages.get(type2.getLabel());
            return label1.compareTo(label2);
        }
    }

    @In(create = true)
    protected transient TypeManager typeManager;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    protected transient SchemaManager schemaManager;

    public List<Type> getNotSelectedTypes() {
        DocumentModel currentDoc = navigationContext.getCurrentDocument();
        return getNotSelectedTypes(currentDoc);
    }

    /**
     * Returns a List of type not selected for the domain given as parameter
     *
     * @param document the domain to configure
     * @return a List of type of document, not currently selected for the domain
     * @since 5.5
     */
    public List<Type> getNotSelectedTypes(DocumentModel document) {
        if (!document.hasFacet(UI_TYPES_CONFIGURATION_FACET)) {
            return Collections.emptyList();
        }

        List<String> allowedTypes = getAllowedTypes(document);

        List<Type> notSelectedTypes = new ArrayList<>(typeManager.findAllAllowedSubTypesFrom(document.getType()));

        for (Iterator<Type> it = notSelectedTypes.iterator(); it.hasNext();) {
            Type type = it.next();
            if (allowedTypes.contains(type.getId())) {
                it.remove();
            }
        }

        Collections.sort(notSelectedTypes, new TypeLabelAlphabeticalOrder(messages));

        return notSelectedTypes;
    }

    protected List<String> getAllowedTypes(DocumentModel doc) {
        UITypesConfiguration uiTypesConfiguration = doc.getAdapter(UITypesConfiguration.class);
        if (uiTypesConfiguration == null) {
            return Collections.emptyList();
        }
        List<String> allowedTypes = new ArrayList<>(uiTypesConfiguration.getAllowedTypes());
        if (allowedTypes.isEmpty()) {
            allowedTypes = computeAllowedTypes(doc);
        }
        return allowedTypes;
    }

    public List<Type> getSelectedTypes() {
        DocumentModel currentDoc = navigationContext.getCurrentDocument();
        return getSelectedTypes(currentDoc);
    }

    /**
     * Returns a List of type selected for the domain given as parameter
     *
     * @param document the domain to configure
     * @return List of documen type selected for the domain
     * @since 5.5
     */
    public List<Type> getSelectedTypes(DocumentModel document) {
        if (!document.hasFacet(UI_TYPES_CONFIGURATION_FACET)) {
            return Collections.emptyList();
        }

        List<String> allowedTypes = getAllowedTypes(document);

        List<Type> selectedTypes = new ArrayList<>();
        for (String type : allowedTypes) {
            Type existingType = typeManager.getType(type);
            if (existingType != null) {
                selectedTypes.add(existingType);
            }
        }

        Collections.sort(selectedTypes, new TypeLabelAlphabeticalOrder(messages));
        return selectedTypes;
    }

    protected List<String> computeAllowedTypes(DocumentModel currentDoc) {
        List<String> types = new ArrayList<>();

        DocumentModel parent = documentManager.getRootDocument();

        DocumentRef parentRef = currentDoc.getParentRef();
        if (parentRef != null && documentManager.hasPermission(parentRef, SecurityConstants.READ)) {
            parent = documentManager.getDocument(parentRef);
        }

        for (Type type : typeManager.findAllAllowedSubTypesFrom(currentDoc.getType(), parent)) {
            types.add(type.getId());
        }

        return types;
    }

    public List<Type> getTypesWithSchemaFile() {
        DocumentModel document = navigationContext.getCurrentDocument();
        return getTypesWithSchemaFile(document);
    }

    /**
     * Returns a List of Document Types associated with Schema file for the domain given as parameter, if they're
     * allowed for it.
     *
     * @param document the domain
     * @return List of Document types which have assoctiated Schema files.
     * @Since 5.5
     */
    public List<Type> getTypesWithSchemaFile(DocumentModel document) {
        List<Type> types = new ArrayList<>();
        for (String type : getAllowedTypes(document)) {
            DocumentType documentType = getSchemaManager().getDocumentType(type);
            if (documentType != null && documentType.hasSchema(UI_TYPES_DEFAULT_NEEDED_SCHEMA)) {
                types.add(typeManager.getType(type));
            }
        }
        Collections.sort(types, new TypeLabelAlphabeticalOrder(messages));
        return Collections.unmodifiableList(types);
    }

    protected SchemaManager getSchemaManager() {
        if (schemaManager == null) {
            schemaManager = Framework.getService(SchemaManager.class);
        }
        return schemaManager;
    }

}
