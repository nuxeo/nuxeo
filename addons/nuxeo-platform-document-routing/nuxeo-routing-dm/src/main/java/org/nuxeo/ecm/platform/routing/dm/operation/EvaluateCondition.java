/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Mariana Cedica
 */

package org.nuxeo.ecm.platform.routing.dm.operation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.ecm.platform.routing.dm.api.RoutingTaskConstants.EvaluationOperators;

/***
 * Evaluates the condition specified by the parameters against the input
 * document. Supports only Integer and String parameters
 *
 * @author mcedica
 * @deprecated since 5.9.2 - Use only routes of type 'graph'
 */
@Deprecated
@Operation(id = EvaluateCondition.ID, category = DocumentRoutingConstants.OPERATION_CATEGORY_ROUTING_NAME, label = "Set Task Done", description = "Set the task as done.", addToStudio = false)
public class EvaluateCondition extends AbstractTaskStepOperation {

    public final static String ID = "Document.Routing.EvaluateCondition";

    private Log log = LogFactory.getLog(EvaluateCondition.class);

    @Context
    protected OperationContext context;

    @Param(name = "subject")
    protected String subject;

    @Param(name = "operator")
    protected String operator;

    @Param(name = "value")
    protected String value;

    @OperationMethod
    public void evaluateCondition(DocumentModel doc) throws ClientException {
        int result = 0;
        Long longValue;
        Object subjectValue = getPropertyValue(doc, subject);
        if (subjectValue instanceof Long) {
            try {
                longValue = Long.parseLong(value);
            } catch (NumberFormatException e) {
                log.error("Invalid long value");
                throw new ClientException(e);
            }
            result = ((Long) subjectValue).compareTo(longValue);
        }
        if (subjectValue instanceof String) {
            result = ((String) subjectValue).compareTo(value);
        }

        if ((result == 0 && EvaluationOperators.equal.name().equals(operator))
                || (result != 0 && EvaluationOperators.not_equal.name().equals(
                        operator))
                || (result < 0 && EvaluationOperators.less_than.name().equals(
                        operator))
                || (result > 0 && EvaluationOperators.greater_than.name().equals(
                        operator))
                || (result <= 0 && EvaluationOperators.less_or_equal_than.name().equals(
                        operator))
                || (result >= 0 && EvaluationOperators.greater_or_equal_than.name().equals(
                        operator))) {
            context.put("nextStepPos", "1");
        } else {
            context.put("nextStepPos", "2");
        }
    }

    @SuppressWarnings("unchecked")
    protected <T> T getPropertyValue(DocumentModel doc, String propertyName) {
        try {
            return (T) doc.getPropertyValue(propertyName);
        } catch (PropertyException e) {
            throw new ClientRuntimeException(e);
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }
}