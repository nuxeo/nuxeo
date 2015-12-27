/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.core.api.localconfiguration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Default implementation of {@code LocalConfigurationService}.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.4.2
 */
public class LocalConfigurationServiceImpl extends DefaultComponent implements LocalConfigurationService {

    private static final Log log = LogFactory.getLog(LocalConfigurationServiceImpl.class);

    @Override
    public <T extends LocalConfiguration> T getConfiguration(Class<T> configurationClass, String configurationFacet,
            DocumentModel currentDoc) {
        if (currentDoc == null || currentDoc.getRef() == null) {
            return null;
        }

        CoreSession session = currentDoc.getCoreSession();
        if (session == null) {
            return null;
        }

        T localConfiguration = session.adaptFirstMatchingDocumentWithFacet(currentDoc.getRef(), configurationFacet,
                configurationClass);
        if (localConfiguration == null) {
            // no local configuration found
            return null;
        }
        while (localConfiguration.canMerge()) {
            DocumentRef parentRef = session.getParentDocumentRef(localConfiguration.getDocumentRef());
            if (parentRef == null) {
                DocumentModel parentDoc = session.getParentDocument(localConfiguration.getDocumentRef());
                if (parentDoc == null) {
                    break;
                }
                parentRef = parentDoc.getRef();
                if (parentRef == null) {
                    break;
                }
            }
            T parentConfiguration = session.adaptFirstMatchingDocumentWithFacet(parentRef, configurationFacet,
                    configurationClass);
            if (parentConfiguration == null) {
                // stop merging
                break;
            }
            localConfiguration.merge(parentConfiguration);
        }
        return localConfiguration;
    }

}
