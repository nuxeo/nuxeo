/*
 * (C) Copyright 2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id: RelationCreationBean.java 19155 2007-05-22 16:19:48Z sfermigier $
 */

package org.nuxeo.ecm.platform.relations.web.listener.ejb;

import java.util.Locale;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.i18n.I18NUtils;

/**
 * Helper for creation form validation and display.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 * @author <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 */
public class RelationCreationBean {

    private static final Log log = LogFactory.getLog(RelationCreationBean.class);

    public void validateObject(FacesContext context, UIComponent component, Object value) {
        Map<String, Object> attributes = component.getAttributes();
        final String objectTypeInputId = (String) attributes.get("objectTypeInputId");
        final String objectLiteralValueInputId = (String) attributes.get("objectLiteralValueInputId");
        final String objectUriInputId = (String) attributes.get("objectUriInputId");
        final String objectDocumentUidInputId = (String) attributes.get("objectDocumentUidInputId");

        if (StringUtils.isBlank(objectTypeInputId) || StringUtils.isBlank(objectLiteralValueInputId)
                || StringUtils.isBlank(objectUriInputId) || StringUtils.isBlank(objectDocumentUidInputId)) {
            log.error("Cannot validate relation creation: input id(s) not found");
            return;
        }

        final UIInput objectTypeInput = (UIInput) component.findComponent(objectTypeInputId);
        final UIInput objectLiteralValueInput = (UIInput) component.findComponent(objectLiteralValueInputId);
        final UIInput objectUriInput = (UIInput) component.findComponent(objectUriInputId);
        final UIInput objectDocumentUidInput = (UIInput) component.findComponent(objectDocumentUidInputId);

        if (objectTypeInput == null || objectLiteralValueInput == null || objectUriInput == null
                || objectDocumentUidInput == null) {
            log.error("Cannot validate relation creation: input(s) not found");
            return;
        }

        FacesMessage message;
        String objectType = (String) objectTypeInput.getLocalValue();
        String objectValue = null;
        String bundleName = context.getApplication().getMessageBundle();
        Locale locale = context.getViewRoot().getLocale();
        String msg;

        if (objectType == null) {
            msg = I18NUtils.getMessageString(bundleName, "error.relation.required.object.type", null, locale);
            message = new FacesMessage(msg);
        } else if (objectType.equals("literal")) {
            objectValue = StringUtils.trim((String) objectLiteralValueInput.getLocalValue());
            msg = I18NUtils.getMessageString(bundleName, "error.relation.required.object.text", null, locale);
            message = new FacesMessage(msg);
        } else if (objectType.equals("uri")) {
            // XXX maybe perform better validation on uri
            objectValue = StringUtils.trim((String) objectUriInput.getLocalValue());
            msg = I18NUtils.getMessageString(bundleName, "error.relation.required.object.uri", null, locale);
            message = new FacesMessage(msg);
        } else if (objectType.equals("document")) {
            objectValue = StringUtils.trim((String) objectDocumentUidInput.getLocalValue());
            msg = I18NUtils.getMessageString(bundleName, "error.relation.required.object.document", null, locale);
            message = new FacesMessage(msg);
        } else {
            msg = I18NUtils.getMessageString(bundleName, "error.relation.invalid.object.type", null, locale);
            message = new FacesMessage(msg);
        }

        if (objectValue == null || objectValue.length() == 0) {
            message.setSeverity(FacesMessage.SEVERITY_ERROR);
            throw new ValidatorException(message);
        }
    }

}
