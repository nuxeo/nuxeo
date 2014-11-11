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

package org.nuxeo.ecm.platform.localconfiguration.web.types;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.nuxeo.ecm.platform.types.localconfiguration.UITypesConfigurationConstants.UI_TYPES_CONFIGURATION_FACET;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.types.Type;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.ecm.platform.types.localconfiguration.UITypesConfiguration;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
@Name("typesConfigurationActions")
@Scope(CONVERSATION)
@Install(precedence = Install.FRAMEWORK)
public class UITypesConfigurationActions implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final Comparator<? super Type> TYPE_ALPHABETICAL_ORDER = new Comparator<Type>() {

        @Override
        public int compare(Type type1, Type type2) {
            return type1.getId().compareTo(type2.getId());
        }

    };

    @In(create = true)
    protected transient TypeManager typeManager;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    public List<Type> getNotSelectedTypes() throws ClientException {
        DocumentModel currentDoc = navigationContext.getCurrentDocument();
        if (!currentDoc.hasFacet(UI_TYPES_CONFIGURATION_FACET)) {
            return Collections.emptyList();
        }

        List<String> allowedTypes = getAllowedTypes(currentDoc);

        List<Type> notSelectedTypes = new ArrayList<Type>(
                typeManager.findAllAllowedSubTypesFrom(currentDoc.getType()));

        for (Iterator<Type> it = notSelectedTypes.iterator(); it.hasNext();) {
            Type type = it.next();
            if (allowedTypes.contains(type.getId())) {
                it.remove();
            }
        }
        Collections.sort(notSelectedTypes, TYPE_ALPHABETICAL_ORDER);

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
        if (!currentDoc.hasFacet(UI_TYPES_CONFIGURATION_FACET)) {
            return Collections.emptyList();
        }

        List<String> allowedTypes = getAllowedTypes(currentDoc);
        Collections.sort(allowedTypes);

        List<Type> selectedTypes = new ArrayList<Type>();
        for (String type : allowedTypes) {
            selectedTypes.add(typeManager.getType(type));
        }

        return selectedTypes;
    }

    protected List<String> computeAllowedTypes(DocumentModel currentDoc)
            throws ClientException {
        List<String> types = new ArrayList<String>();

        DocumentModel parent = documentManager.getParentDocument(currentDoc.getRef());
        for (Type type : typeManager.findAllAllowedSubTypesFrom(
                currentDoc.getType(), parent)) {
            types.add(type.getId());
        }

        return types;
    }

}
