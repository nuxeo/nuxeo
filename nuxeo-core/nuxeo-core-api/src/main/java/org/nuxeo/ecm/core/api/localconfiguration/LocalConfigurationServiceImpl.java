/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * Contributors:
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.core.api.localconfiguration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Default implementation of {@code LocalConfigurationService}.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.4.1
 */
public class LocalConfigurationServiceImpl extends DefaultComponent implements
        LocalConfigurationService {

    private static final Log log = LogFactory.getLog(LocalConfigurationServiceImpl.class);

    @Override
    public <T extends LocalConfiguration> T getConfiguration(
            Class<T> configurationClass, String configurationFacet,
            DocumentModel currentDoc) {
        if (currentDoc == null) {
            return null;
        }

        try {
            CoreSession session = currentDoc.getCoreSession();
            T localConfiguration = session.adaptFirstMatchingDocumentWithFacet(
                    currentDoc.getRef(), configurationFacet, configurationClass);
            if (localConfiguration == null) {
                // no local configuration found
                return null;
            }
            while (localConfiguration.canMerge()) {
                DocumentRef parentRef = session.getParentDocumentRef(localConfiguration.getDocumentRef());
                T parentConfiguration = session.adaptFirstMatchingDocumentWithFacet(
                        parentRef, configurationFacet, configurationClass);
                if (parentConfiguration == null) {
                    // stop merging
                    break;
                }
                localConfiguration.merge(parentConfiguration);
            }
            return localConfiguration;
        } catch (ClientException e) {
            String message = String.format(
                    "Unable to retrieve local configuration for '%s' and '%s' facet: %s",
                    currentDoc, configurationFacet, e.getMessage());
            log.warn(message);
            log.debug(e, e);
        }
        return null;
    }

}
