/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.smart.query.jsf;

import java.io.Serializable;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.platform.smart.query.SmartQuery;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;

/**
 * @author Anahide Tchertchian
 */
@Name("smartQueryActions")
@Scope(ScopeType.CONVERSATION)
public class SmartQueryActions implements Serializable {

    private static final long serialVersionUID = 1L;

    protected SmartQuery currentSmartQuery;

    @Factory(value = "currentSmartQuery", scope = ScopeType.EVENT)
    public SmartQuery getCurrentSmartQuery() {
        if (currentSmartQuery == null) {
            currentSmartQuery = new NXQLIncrementalSmartQuery("");
        }
        return currentSmartQuery;
    }

    public void buildQueryString() {
        if (currentSmartQuery != null) {
            currentSmartQuery.buildQuery();
        }
    }

    public void validateQuery(FacesContext context, UIComponent component,
            Object value) {
        if (value == null || !(value instanceof String)) {
            FacesMessage message = new FacesMessage(
                    FacesMessage.SEVERITY_ERROR, ComponentUtils.translate(
                            context, "error.smart.query.invalidQuery"), null);
            // also add global message
            context.addMessage(null, message);
            throw new ValidatorException(message);
        }
        String query = (String) value;
        if (!NXQLIncrementalSmartQuery.isValid(query)) {
            FacesMessage message = new FacesMessage(
                    FacesMessage.SEVERITY_ERROR, ComponentUtils.translate(
                            context, "error.smart.query.invalidQuery"), null);
            // also add global message
            context.addMessage(null, message);
            throw new ValidatorException(message);
        }
    }

}
