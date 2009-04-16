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

package org.nuxeo.ecm.webapp.action;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.ScopeType.EVENT;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.types.Type;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.jboss.seam.annotations.intercept.BypassInterceptors;

/**
 * Document type service for document type creation.
 *
 * @author eionica@nuxeo.com
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
@Name("typesTool")
@Scope(CONVERSATION)
public class TypesTool implements Serializable {

    private static final long serialVersionUID = -5037578301250616973L;

    private static final Log log = LogFactory.getLog(TypesTool.class);

    @In
    private transient TypeManager typeManager;

    private List<Type> typesList;

    private Type selectedType;

    @In(create = true)
    private transient NavigationContext navigationContext;

    @Observer(value = { EventNames.CONTENT_ROOT_SELECTION_CHANGED,
            EventNames.DOCUMENT_SELECTION_CHANGED }, create = false, inject=false)
    @BypassInterceptors
    public void resetTypesList() {
        typesList = null;
    }

    /**
     * Retrieves the list of allowed sub types given a current type.
     * <p>
     * This is used at creation time. Current type is retrieved thanks to the
     * document model hold and passed by the event.
     */
    public void populateTypesList() {
        boolean set = false;
        DocumentModel model = getCurrentItem();
        if (model != null) {
            Type docType = typeManager.getType(model.getType());
            if (docType != null) {
                List<Type> allowed = new ArrayList<Type>();
                for (String typeName : docType.getAllowedSubTypes()) {
                    Type subType = typeManager.getType(typeName);
                    if (subType != null) {
                        allowed.add(subType);
                    }
                }
                typesList = allowed;
                set = true;
            }
        }
        if (!set) {
            // set an empty list
            typesList = new ArrayList<Type>();
        }
    }

    /**
     * Retrieves the list of allowed sub types given a current type.
     *
     */
    public List<String> getAllowedSubTypesFor(String docType) {
        Type documentType = typeManager.getType(docType);
        String[] types = documentType.getAllowedSubTypes();
        List<String> allowedSubTypes = new ArrayList<String>();
        allowedSubTypes.addAll(Arrays.asList(types));
        return allowedSubTypes;
    }


    public Type getSelectedType() {
        if (selectedType != null) {
            log.debug("Returning selected type with id: " + selectedType.getId());
        }
        return selectedType;
    }

    /**
     * If the selected type is supposed to be automatically injected by
     * Seam through @DataModelSelection callback (i.e. the user
     * will select the type from a list), this method should be
     * called with <code>null</code> parameter before.
     *
     * @param type
     */
    public void setSelectedType(Type type) {
        if (typesList == null) {
            populateTypesList();
        }
        selectedType = type;
    }

    @Factory(value = "typesList", scope = EVENT)
    public List<Type> getTypesList() {
        // XXX : should cache per currentDocument type
        if (typesList == null) {
            // cache the list of allowed subtypes
            populateTypesList();
        }
        selectedType = null;
        return typesList;
    }

    public void setTypesList(List<Type> typesList) {
        this.typesList = typesList;
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
