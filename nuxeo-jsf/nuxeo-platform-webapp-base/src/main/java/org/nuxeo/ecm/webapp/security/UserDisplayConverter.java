/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.webapp.security;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.ui.web.directory.DirectoryFunctions;
import org.nuxeo.ecm.platform.ui.web.tag.fn.Functions;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;

/**
 * JSF Converter used for rendering, transforming a user id into the user display name.
 * <p>
 * Sample usage:
 *
 * <pre>
 * <code>
 * lt;h:outputText
 *  converter=&quot;#{userSuggestionActions.userConverter}&quot;&gt;
 *  &lt;f:attribute name=&quot;prefixed&quot; value=&quot;false&quot; /&gt;
 *  &lt;f:attribute name=&quot;userDirectory&quot; value=&quot;#{userManager.userDirectoryName}&quot; /&gt;
 *  &lt;f:attribute name=&quot;userSchema&quot; value=&quot;#{userManager.userSchemaName}&quot; /&gt;
 *  &lt;f:attribute name=&quot;firstNameField&quot; value=&quot;firstName&quot; /&gt;
 *  &lt;f:attribute name=&quot;lastNameField&quot; value=&quot;lastName&quot; /&gt;
 * lt;/h:outputText&gt;
 * </code>
 * </pre>
 *
 * @author Anahide Tchertchian
 */
public class UserDisplayConverter implements Converter {

    /**
     * Returns given value (does not do any reverse conversion)
     */
    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) {
        return value;
    }

    /**
     * Tries to build the user display name according to information passed as attribute to the component holding the
     * converter.
     * <p>
     * Handled attributes are:
     * <ul>
     * <li>userDirectory: user directory name</li>
     * <li>userSchema: user schema name</li>
     * <li>firstNameField: field storing the user first name</li>
     * <li>lastNameField: field storing the user last name</li>
     * </ul>
     */
    @Override
    public String getAsString(FacesContext context, UIComponent component, Object value) {
        if (value instanceof String && !StringUtils.isEmpty((String) value)) {
            String isPrefixed = (String) ComponentUtils.getAttributeOrExpressionValue(context, component, "prefixed",
                    "false");
            String username;
            if (Boolean.valueOf(isPrefixed)) {
                username = ((String) value).substring(NuxeoPrincipal.PREFIX.length());
            } else {
                username = (String) value;
            }
            String directory = (String) ComponentUtils.getAttributeOrExpressionValue(context, component,
                    "userDirectory", null);
            String firstName = (String) ComponentUtils.getAttributeOrExpressionValue(context, component,
                    "firstNameField", null);
            String lastName = (String) ComponentUtils.getAttributeOrExpressionValue(context, component,
                    "lastNameField", null);
            String email = (String) ComponentUtils.getAttributeOrExpressionValue(context, component, "emailField", null);
            String schema = (String) ComponentUtils.getAttributeOrExpressionValue(context, component, "userSchema",
                    null);

            if (schema != null) {
                DocumentModel doc = DirectoryFunctions.getDirectoryEntry(directory, username);
                if (doc != null) {
                    String firstNameValue = firstName != null ? (String) doc.getProperty(schema, firstName) : null;
                    String lastNameValue = lastName != null ? (String) doc.getProperty(schema, lastName) : null;
                    String emailValue = email != null ? (String) doc.getProperty(schema, email) : null;
                    return Functions.userDisplayNameAndEmail(username, firstNameValue, lastNameValue, emailValue);
                }
            } else {
                // XXX will return cached entry
                return Functions.userFullName(username);
            }
        }
        if (value != null) {
            return value.toString();
        } else {
            return null;
        }
    }

}
