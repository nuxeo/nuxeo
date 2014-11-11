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

import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.localconfiguration.LocalConfigurationService;
import org.nuxeo.ecm.platform.localconfiguration.simple.SimpleConfiguration;

/**
 * Operation to put parameters on the Simple Configuration of the input
 * Document.
 * <p>
 * The parameters are specified as <i>key=value</i> pairs separated by a new
 * line.
 * <p>
 * The <code>SimpleConfiguration</code> facet is added to the input document if
 * needed.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
@Operation(id = PutSimpleConfigurationParameters.ID, category = CAT_LOCAL_CONFIGURATION, label = "Put Simple Configuration parameters", description = "Put Simple Configuration parameters "
        + "on the input document. "
        + "Add the 'SimpleConfiguration' facet on the input document if needed. "
        + "The parameters are specified as <i>key=value</i> pairs separated by a new line. "
        + "The user adding parameters must have WRITE access on the input document.")
public class PutSimpleConfigurationParameters {

    public static final String ID = "LocalConfiguration.PutSimpleConfigurationParameters";

    @Context
    protected CoreSession session;

    @Context
    protected LocalConfigurationService localConfigurationService;

    @Param(name = "parameters")
    protected Properties parameters;

    @Param(name = "save", required = false, values = "true")
    protected boolean save = true;

    @OperationMethod
    public DocumentModel run(DocumentModel doc) throws Exception {
        if (!doc.hasFacet(SIMPLE_CONFIGURATION_FACET)) {
            doc.addFacet(SIMPLE_CONFIGURATION_FACET);
            doc = session.saveDocument(doc);
        }

        SimpleConfiguration simpleConfiguration = localConfigurationService.getConfiguration(
                SimpleConfiguration.class, SIMPLE_CONFIGURATION_FACET, doc);
        simpleConfiguration.putAll(parameters);
        simpleConfiguration.save(session);

        if (save) {
            doc = session.saveDocument(doc);
        }
        return doc;
    }

}
