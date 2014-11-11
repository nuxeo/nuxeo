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

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;

import org.nuxeo.common.utils.i18n.I18NUtils;

/**
 * Helper for creation form validation and display.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class RelationCreationBean {

    // components for bindings to perform validation

    protected UIInput objectTypeInput;

    protected UIInput objectLiteralValueInput;

    protected UIInput objectUriInput;

    protected UIInput objectDocumentTitleInput;

    protected UIInput objectDocumentUidInput;

    public UIInput getObjectLiteralValueInput() {
        return objectLiteralValueInput;
    }

    public void setObjectLiteralValueInput(UIInput objectLiteralValueInput) {
        this.objectLiteralValueInput = objectLiteralValueInput;
    }

    public UIInput getObjectTypeInput() {
        return objectTypeInput;
    }

    public void setObjectTypeInput(UIInput objectTypeInput) {
        this.objectTypeInput = objectTypeInput;
    }

    public UIInput getObjectUriInput() {
        return objectUriInput;
    }

    public void setObjectUriInput(UIInput objectUriInput) {
        this.objectUriInput = objectUriInput;
    }

    public UIInput getObjectDocumentTitleInput() {
        return objectDocumentTitleInput;
    }

    public void setObjectDocumentTitleInput(UIInput objectDocumentTitleInput) {
        this.objectDocumentTitleInput = objectDocumentTitleInput;
    }

    public UIInput getObjectDocumentUidInput() {
        return objectDocumentUidInput;
    }

    public void setObjectDocumentUidInput(UIInput objectDocumentUidInput) {
        this.objectDocumentUidInput = objectDocumentUidInput;
    }

    public void validateObject(FacesContext context, UIComponent component,
            Object value) {
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
