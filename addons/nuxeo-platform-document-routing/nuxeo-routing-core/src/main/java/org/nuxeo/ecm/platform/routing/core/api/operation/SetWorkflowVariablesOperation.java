/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Mariana Cedica
 */
package org.nuxeo.ecm.platform.routing.core.api.operation;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.DocumentHelper;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PropertyException;

/**
 * Set a list of variable on the workflow instance with the given id
 *
 * @since 5.7.2
 */
@Operation(id = SetWorkflowVariablesOperation.ID, category = Constants.CAT_WORKFLOW, label = "Set workflow variables", requires = Constants.WORKFLOW_CONTEXT, description = "Set a list of variable on the workflow instance with the given id."
        + "The variables are specified as <i>key=value</i> pairs separated by a new line. The key used for a variable is the property xpath."
        + "The xpath is given by the prefix of the schema storing the variables (in the \"Workflow Variables\" tab) and the variable name."
        + "To specify multi-line values you can use a \\ character followed by a new line. <p>Example:<pre>dc:title=The Document Title<br>dc:description=foo bar</pre>For updating a date, you will need to expose the value as ISO 8601 format, "
        + "for instance : <p>Example:<pre>dc:title=The Document Title<br>dc:issued=@{org.nuxeo.ecm.core.schema.utils.DateParser.formatW3CDateTime(CurrentDate.date)}</pre><p>")
public class SetWorkflowVariablesOperation {

    public static final String ID = "Workflow.SetWorkflowVariables";

    private static Log log = LogFactory.getLog(SetWorkflowVariablesOperation.class);

    @Context
    protected CoreSession session;

    @Param(name = "id", required = true)
    protected String id;

    @Param(name = "properties", required = true)
    protected Properties properties;

    @OperationMethod
    public void run() {
        DocumentModel workflowInstance = session.getDocument(new IdRef(id));
        try {
            DocumentHelper.setProperties(session, workflowInstance, properties);
        } catch (PropertyException e) {
            e.addInfo("Cannot set properties on workflow instance with the id: " + id);
            throw e;
        } catch (IOException e) {
            throw new NuxeoException("Cannot set properties on workflow instance with the id: " + id, e);
        }
    }
}
