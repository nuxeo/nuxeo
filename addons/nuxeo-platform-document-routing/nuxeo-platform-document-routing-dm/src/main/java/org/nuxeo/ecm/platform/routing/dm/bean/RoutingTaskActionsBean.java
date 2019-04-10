/*
 * (C) Copyright 2012 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *    Mariana Cedica
 *
 * $Id$
 */
package org.nuxeo.ecm.platform.routing.dm.bean;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.component.EditableValueHolder;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.platform.routing.dm.api.RoutingTaskConstants.EvaluationOperators;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;

/**
 * 
 * Task validators
 * 
 * @author mcedica
 * @since 5.6
 * 
 */
@Scope(CONVERSATION)
@Name("routingTaskActions")
public class RoutingTaskActionsBean {

    public static final String SUBJECT_PATTERN = "([a-zA-Z_0-9]*(:)[a-zA-Z_0-9]*)";

    private static final Log log = LogFactory.getLog(RoutingTaskActionsBean.class);

    public void validateTaskDueDate(FacesContext context,
            UIComponent component, Object value) {
        final String DATE_FORMAT = "dd/MM/yyyy";
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);

        String messageString = null;
        if (value != null) {
            Date today = null;
            Date dueDate = null;
            try {
                dueDate = dateFormat.parse(dateFormat.format((Date) value));
                today = dateFormat.parse(dateFormat.format(new Date()));
            } catch (ParseException e) {
                messageString = "label.workflow.error.date_parsing";
            }
            if (dueDate.before(today)) {
                messageString = "label.workflow.error.outdated_duedate";
            }
        }

        if (messageString != null) {
            FacesMessage message = new FacesMessage(
                    FacesMessage.SEVERITY_ERROR, ComponentUtils.translate(
                            context, "label.workflow.error.outdated_duedate"),
                    null);
            ((EditableValueHolder) component).setValid(false);
            context.addMessage(component.getClientId(context), message);
        }
    }

    public void validateSubject(FacesContext context, UIComponent component,
            Object value) {
        if (!((value instanceof String) && ((String) value).matches(SUBJECT_PATTERN))) {
            FacesMessage message = new FacesMessage(
                    FacesMessage.SEVERITY_ERROR, ComponentUtils.translate(
                            context, "label.document.routing.invalid.subject"),
                    null);
            context.addMessage(null, message);
            throw new ValidatorException(message);
        }
    }

    public void validateValueForOperator(FacesContext context,
            UIComponent component, Object value) {
        Map<String, Object> attributes = component.getAttributes();
        String operatorInputId = (String) attributes.get("operatorId");

        UIInput operatorInputComp = (UIInput) component.findComponent(operatorInputId);
        if (operatorInputComp == null) {
            log.error("Cannot validate value: operator not found");
            return;
        }
        String operatorValue = (String) operatorInputComp.getLocalValue();

        if (operatorValue == null) {
            log.error("Cannot validate value: value(s) not found");
            return;
        }

        String valueInputId = (String) attributes.get("valueId");
        UIInput valueInputComp = (UIInput) component.findComponent(valueInputId);
        value = valueInputComp.getLocalValue();

        if (!(value instanceof String)) {
            FacesMessage message = new FacesMessage(
                    FacesMessage.SEVERITY_ERROR, ComponentUtils.translate(
                            context, "label.document.routing.invalid.value"),
                    null);
            throw new ValidatorException(message);
        }

        if ((EvaluationOperators.greater_than.name().equals(operatorValue) || EvaluationOperators.less_than.name().equals(
                operatorValue))
                || (EvaluationOperators.greater_or_equal_than.name().equals(operatorValue))
                || (EvaluationOperators.less_or_equal_than.name().equals(operatorValue))) {
            // try converting value to int
            try {
                Integer intValue = Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                FacesMessage message = new FacesMessage(
                        FacesMessage.SEVERITY_ERROR, ComponentUtils.translate(
                                context,
                                "label.document.routing.invalid.operator"),
                        null);
                throw new ValidatorException(message);
            }
        }

    }
}