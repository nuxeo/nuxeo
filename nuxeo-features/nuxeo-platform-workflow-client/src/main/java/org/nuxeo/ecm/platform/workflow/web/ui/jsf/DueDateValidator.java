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
 * $Id: WorkItemsListsActionsLocal.java 19070 2007-05-21 16:05:43Z sfermigier $
 */

package org.nuxeo.ecm.platform.workflow.web.ui.jsf;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

import org.nuxeo.common.utils.i18n.I18NUtils;

/***
 *
 * @author bchaffangeon
 *
 */
public class DueDateValidator implements Validator {

    //This simply validates that dueDate in a workflow is not outdated
    public void validate(FacesContext context, UIComponent uIComponent,
            Object value) throws ValidatorException {

        final String DATE_FORMAT = "dd/MM/yyyy";
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        Date today = null;
        Date dueDate = null;

        String bundleName = context.getApplication().getMessageBundle();
        FacesMessage message = null;
        Locale locale = context.getViewRoot().getLocale();
        String msg = "";

        if (value != null) {
            try {
                dueDate = dateFormat.parse(dateFormat.format((Date) value));
                today = dateFormat.parse(dateFormat.format(new Date()));
            } catch (ParseException e) {
                msg = I18NUtils.getMessageString(bundleName,
                        "label.workflow.error.date_parsing", null, locale);
                message = new FacesMessage(msg);

            }
            if (dueDate.before(today)) {
                msg = I18NUtils.getMessageString(bundleName,
                        "label.workflow.error.outdated_duedate", null, locale);
                message = new FacesMessage(msg);

            }
        }
        if (message != null) {
            message.setSeverity(FacesMessage.SEVERITY_ERROR);
            throw new ValidatorException(message);
        }

    }

}
