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

import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.localconfiguration.LocalConfigurationService;

/**
 * Operation to put a parameter on the Simple Configuration of the input Document.
 * <p>
 * The <code>SimpleConfiguration</code> facet is added to the input document if needed.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
@Operation(id = PutSimpleConfParam.ID, category = CAT_LOCAL_CONFIGURATION, label = "Put a Simple Configuration Parameter", description = "Put a Simple Configuration parameter "
        + "on the input document. "
        + "Add the 'SimpleConfiguration' facet on the input document if needed. "
        + "The user adding a parameter must have WRITE access on the input document")
public class PutSimpleConfParam {

    public static final String ID = "LocalConfiguration.PutSimpleConfigurationParameter";

    @Context
    protected CoreSession session;

    @Context
    protected LocalConfigurationService localConfigurationService;

    @Param(name = "key")
    protected String key;

    @Param(name = "value")
    protected String value;

    @Param(name = "save", required = false, values = "true")
    protected boolean save = true;

    @OperationMethod
    public DocumentModel run(DocumentModel doc) {
        if (!doc.hasFacet(SIMPLE_CONFIGURATION_FACET)) {
            doc.addFacet(SIMPLE_CONFIGURATION_FACET);
            doc = session.saveDocument(doc);
        }

        SimpleConfiguration simpleConfiguration = localConfigurationService.getConfiguration(SimpleConfiguration.class,
                SIMPLE_CONFIGURATION_FACET, doc);
        simpleConfiguration.put(key, value);
        simpleConfiguration.save(session);

        if (save) {
            doc = session.saveDocument(doc);
        }
        return doc;
    }

}
