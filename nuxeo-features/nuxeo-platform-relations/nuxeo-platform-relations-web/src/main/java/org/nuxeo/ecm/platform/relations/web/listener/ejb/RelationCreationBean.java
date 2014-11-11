/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

import org.apache.commons.lang.StringUtils;
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

    public void validateObject(FacesContext context, UIComponent component,
            Object value) {
        Map<String, Object> attributes = component.getAttributes();
        final String objectTypeInputId = (String) attributes.get("objectTypeInputId");
        final String objectLiteralValueInputId = (String) attributes.get("objectLiteralValueInputId");
        final String objectUriInputId = (String) attributes.get("objectUriInputId");
        final String objectDocumentUidInputId = (String) attributes.get("objectDocumentUidInputId");

        if (StringUtils.isBlank(objectTypeInputId) || StringUtils.isBlank(objectLiteralValueInputId) || StringUtils.isBlank(objectUriInputId) || StringUtils.isBlank(objectDocumentUidInputId)) {
            log.error("Cannot validate relation creation: input id(s) not found");
            return;
        }

        final UIInput objectTypeInput = (UIInput) component.findComponent(objectTypeInputId);
        final UIInput objectLiteralValueInput = (UIInput) component.findComponent(objectLiteralValueInputId);
        final UIInput objectUriInput = (UIInput) component.findComponent(objectUriInputId);
        final UIInput objectDocumentUidInput = (UIInput) component.findComponent(objectDocumentUidInputId);

        if (objectTypeInput == null || objectLiteralValueInput == null || objectUriInput == null || objectDocumentUidInput == null) {
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
            msg = I18NUtils.getMessageString(bundleName,
                    "error.relation.required.object.type", null, locale);
            message = new FacesMessage(msg);
        } else if (objectType.equals("literal")) {
            objectValue = ((String) objectLiteralValueInput.getLocalValue())
                    .trim();
            msg = I18NUtils.getMessageString(bundleName,
                    "error.relation.required.object.text", null, locale);
            message = new FacesMessage(msg);
        } else if (objectType.equals("uri")) {
            // XXX maybe perform better validation on uri
            objectValue = ((String) objectUriInput.getLocalValue()).trim();
            msg = I18NUtils.getMessageString(bundleName,
                    "error.relation.required.object.uri", null, locale);
            message = new FacesMessage(msg);
        } else if (objectType.equals("document")) {
            if (null != objectDocumentUidInput) {
                objectValue = ((String) objectDocumentUidInput.getLocalValue())
                        .trim();
            }
            msg = I18NUtils.getMessageString(bundleName,
                    "error.relation.required.object.document", null, locale);
            message = new FacesMessage(msg);
        } else {
            msg = I18NUtils.getMessageString(bundleName,
                    "error.relation.invalid.object.type", null, locale);
            message = new FacesMessage(msg);
        }

        if (objectValue == null || objectValue.length() == 0) {
            message.setSeverity(FacesMessage.SEVERITY_ERROR);
            throw new ValidatorException(message);
        }
    }

}
