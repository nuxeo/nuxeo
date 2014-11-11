/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.platform.localconfiguration.simple.operations;

import static org.nuxeo.ecm.automation.core.Constants.CAT_LOCAL_CONFIGURATION;
import static org.nuxeo.ecm.platform.localconfiguration.simple.SimpleConfiguration.SIMPLE_CONFIGURATION_FACET;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.localconfiguration.LocalConfigurationService;
import org.nuxeo.ecm.platform.localconfiguration.simple.SimpleConfiguration;

/**
 * Operation to set a context variable with the value of the given parameter
 * name of the SimpleConfiguration retrieve from the input Document.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
@Operation(id = SetSimpleConfigurationParameterAsVar.ID, category = CAT_LOCAL_CONFIGURATION, label = "Set Context Variable From a Simple Configuration Parameter", description = "Set a context variable "
        + "that points to the value of the given parameter name in "
        + "the SimpleConfiguration from the input Document. "
        + "You must give a name for the variable.")
public class SetSimpleConfigurationParameterAsVar {

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
    public DocumentModel run(DocumentModel doc) throws Exception {
        SimpleConfiguration simpleConfiguration = localConfigurationService.getConfiguration(
                SimpleConfiguration.class, SIMPLE_CONFIGURATION_FACET, doc);
        String value = simpleConfiguration.get(parameterName, defaultValue);
        ctx.put(name, value);
        return doc;
    }

}
