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

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;

@Operation(id = EvaluateCondition.ID, category = DocumentRoutingConstants.OPERATION_CATEGORY_ROUTING_NAME, label = "Set Task Done", description = "Set the task as done.")
public class EvaluateCondition extends AbstractTaskStepOperation {
    public final static String ID = "Document.Routing.EvaluateCondition";

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

        String subjectValue = "";
        try {
            subjectValue = (String) doc.getPropertyValue(subject);
        } catch (PropertyException e) {
            throw new ClientException("Invalid subject ", e);
        }
        int result = subjectValue.compareTo(value);
        if ((result == 0 && "equal".equals(operator))
                || (result != 0 && "not_equal".equals(operator))
                || (result < 0 && "smaller".equals(operator))
                || (result > 0 && "greater".equals(operator))) {
            context.put("nextStepPos", "1");
        } else {
            context.put("nextStepPos", "2");
        }
    }
}
