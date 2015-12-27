/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *    mcedica@nuxeo.com
 *
 * $Id$
 */
package org.nuxeo.ecm.webapp.widgets;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

/**
 * Compares two dates in a date range widget and throws a validation error if the second date is not superior to the
 * first date.
 * <p>
 * Looks up component ids by reytrieving attributes on the validated component, named "startDateComponentId" and
 * "endDateComponentId".
 *
 * @since 5.7
 */
@Name("dateRangeValidator")
@Scope(ScopeType.CONVERSATION)
public class DateRangeValidator implements Serializable {

    private static final long serialVersionUID = 1L;

    @In(create = true)
    protected Map<String, String> messages;

    private static final Log log = LogFactory.getLog(DateRangeValidator.class);

    public void validateDateRange(FacesContext context, UIComponent component, Object value) {
        Map<String, Object> attributes = component.getAttributes();
        String startDateComponentId = (String) attributes.get("startDateComponentId");
        String endDateComponentId = (String) attributes.get("endDateComponentId");
        if (startDateComponentId == null || endDateComponentId == null) {
            return;
        }

        UIInput startDateComp = (UIInput) component.findComponent(startDateComponentId);
        UIInput endDateComp = (UIInput) component.findComponent(endDateComponentId);
        if (startDateComp == null) {
            log.error("Can not find component with id " + startDateComponentId);
            return;
        }

        if (endDateComp == null) {
            log.error("Can not find component with id " + endDateComponentId);
            return;
        }
        Date stratDate = (Date) startDateComp.getLocalValue();
        Date endDate = (Date) endDateComp.getLocalValue();

        if (stratDate != null && endDate != null && endDate.compareTo(stratDate) < 0) {
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, String.format(
                    messages.get("error.dateRangeValidator.invalidDateRange"), stratDate, endDate), null);
            throw new ValidatorException(message);
        }
    }
}
