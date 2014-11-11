/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * Contributors:
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.localconf;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.nuxeo.ecm.platform.types.localconfiguration.UITypesConfigurationConstants.UI_TYPES_CONFIGURATION_FACET;
import static org.nuxeo.ecm.platform.types.localconfiguration.UITypesConfigurationConstants.UI_TYPES_DEFAULT_NEEDED_SCHEMA;

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
import org.nuxeo.ecm.core.api.ClientException;
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
     * @deprecated since 5.7.
     */
    @Deprecated
    public static final Comparator<? super Type> TYPE_ALPHABETICAL_ORDER = new Comparator<Type>() {

        @Override
        public int compare(Type type1, Type type2) {
            return type1.getId().compareTo(type2.getId());
        }

    };

    /**
     * @since 5.9.1
     */
    protected static class TypeLabelAlphabeticalOrder implements
            Comparator<Type> {

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

    public List<Type> getNotSelectedTypes() throws ClientException {
        DocumentModel currentDoc = navigationContext.getCurrentDocument();
        return getNotSelectedTypes(currentDoc);
    }

    /**
     * Returns a List of type not selected for the domain given as parameter
     *
     * @param document the domain to configure
     * @return a List of type of document, not currently selected for the domain
     * @throws ClientException
     * @since 5.5
     */
    public List<Type> getNotSelectedTypes(DocumentModel document)
            throws ClientException {
        if (!document.hasFacet(UI_TYPES_CONFIGURATION_FACET)) {
            return Collections.emptyList();
        }

        List<String> allowedTypes = getAllowedTypes(document);

        List<Type> notSelectedTypes = new ArrayList<Type>(
                typeManager.findAllAllowedSubTypesFrom(document.getType()));

        for (Iterator<Type> it = notSelectedTypes.iterator(); it.hasNext();) {
            Type type = it.next();
            if (allowedTypes.contains(type.getId())) {
                it.remove();
            }
        }

        Collections.sort(notSelectedTypes, new TypeLabelAlphabeticalOrder(messages));

        return notSelectedTypes;
    }

    protected List<String> getAllowedTypes(DocumentModel doc)
            throws ClientException {
        UITypesConfiguration uiTypesConfiguration = doc.getAdapter(UITypesConfiguration.class);
        if (uiTypesConfiguration == null) {
            return Collections.emptyList();
        }
        List<String> allowedTypes = new ArrayList<String>(
                uiTypesConfiguration.getAllowedTypes());
        if (allowedTypes.isEmpty()) {
            allowedTypes = computeAllowedTypes(doc);
        }
        return allowedTypes;
    }

    public List<Type> getSelectedTypes() throws ClientException {
        DocumentModel currentDoc = navigationContext.getCurrentDocument();
        return getSelectedTypes(currentDoc);
    }

    /**
     * Returns a List of type selected for the domain given as parameter
     *
     * @param document the domain to configure
     * @return List of documen type selected for the domain
     * @throws ClientException
     * @since 5.5
     */
    public List<Type> getSelectedTypes(DocumentModel document)
            throws ClientException {
        if (!document.hasFacet(UI_TYPES_CONFIGURATION_FACET)) {
            return Collections.emptyList();
        }

        List<String> allowedTypes = getAllowedTypes(document);

        List<Type> selectedTypes = new ArrayList<Type>();
        for (String type : allowedTypes) {
            Type existingType = typeManager.getType(type);
            if (existingType != null) {
                selectedTypes.add(existingType);
            }
        }

        Collections.sort(selectedTypes, new TypeLabelAlphabeticalOrder(messages));
        return selectedTypes;
    }

    protected List<String> computeAllowedTypes(DocumentModel currentDoc)
            throws ClientException {
        List<String> types = new ArrayList<String>();

        DocumentModel parent = documentManager.getRootDocument();

        DocumentRef parentRef = currentDoc.getParentRef();
        if (parentRef != null
                && documentManager.hasPermission(parentRef,
                        SecurityConstants.READ)) {
            parent = documentManager.getDocument(parentRef);
        }

        for (Type type : typeManager.findAllAllowedSubTypesFrom(
                currentDoc.getType(), parent)) {
            types.add(type.getId());
        }

        return types;
    }

    public List<Type> getTypesWithSchemaFile() throws ClientException {
        DocumentModel document = navigationContext.getCurrentDocument();
        return getTypesWithSchemaFile(document);
    }

    /**
     * Returns a List of Document Types associated with Schema file for the
     * domain given as parameter, if they're allowed for it.
     *
     * @param document the domain
     * @return List of Document types which have assoctiated Schema files.
     * @throws ClientException
     * @Since 5.5
     */
    public List<Type> getTypesWithSchemaFile(DocumentModel document)
            throws ClientException {
        List<Type> types = new ArrayList<Type>();
        for (String type : getAllowedTypes(document)) {
            DocumentType documentType = getSchemaManager().getDocumentType(type);
            if (documentType != null
                    && documentType.hasSchema(UI_TYPES_DEFAULT_NEEDED_SCHEMA)) {
                types.add(typeManager.getType(type));
            }
        }
        Collections.sort(types, new TypeLabelAlphabeticalOrder(messages));
        return Collections.unmodifiableList(types);
    }

    protected SchemaManager getSchemaManager() throws ClientException {
        if (schemaManager == null) {
            try {
                schemaManager = Framework.getService(SchemaManager.class);
            } catch (Exception e) {
                throw new ClientException("can NOT obtain schema manager", e);
            }
        }
        return schemaManager;
    }

}
