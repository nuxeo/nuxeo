/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.localconf;

import static org.nuxeo.ecm.automation.core.Constants.CAT_LOCAL_CONFIGURATION;
import static org.nuxeo.ecm.localconf.SimpleConfiguration.SIMPLE_CONFIGURATION_FACET;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.localconfiguration.LocalConfigurationService;

/**
 * Operation to set a context variable with the value of the given parameter name of the SimpleConfiguration retrieve
 * from the input Document.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
@Operation(id = SetSimpleConfParamVar.ID, category = CAT_LOCAL_CONFIGURATION, label = "Set Context Variable From a Simple Configuration Parameter", description = "Set a context variable "
        + "that points to the value of the given parameter name in "
        + "the SimpleConfiguration from the input Document. " + "You must give a name for the variable.")
public class SetSimpleConfParamVar {

    public static final String ID = "LocalConfiguration.SetSimpleConfigurationParameterAsVar";

    @Context
    protected OperationContext ctx;

    @Context
    protected LocalConfigurationService localConfigurationService;

    @Param(name = "name")
    protected String name;

    @Param(name = "parameterName")
    protected String parameterName;

    @Param(name = "defaultValue", required = false)
    protected String defaultValue;

    @OperationMethod
    public DocumentModel run(DocumentModel doc) {
        SimpleConfiguration simpleConfiguration = localConfigurationService.getConfiguration(SimpleConfiguration.class,
                SIMPLE_CONFIGURATION_FACET, doc);
        String value = simpleConfiguration.get(parameterName, defaultValue);
        ctx.put(name, value);
        return doc;
    }

}
