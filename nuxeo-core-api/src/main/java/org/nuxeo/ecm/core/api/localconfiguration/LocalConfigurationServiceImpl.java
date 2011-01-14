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

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class LocalConfigurationServiceImpl extends DefaultComponent implements
        LocalConfigurationService {

    @Override
    public <T extends LocalConfiguration> T getConfiguration(
            Class<T> configurationClass, String configurationFacet,
            DocumentModel currentDoc) {
        if (currentDoc == null) {
            return null;
        }

        ConfigurationFinder configurationFinder = new ConfigurationFinder(
                currentDoc, configurationFacet);
        return configurationFinder.find(configurationClass);
    }

    protected static class ConfigurationFinder extends
            UnrestrictedSessionRunner {

        protected DocumentRef documentRef;

        protected String facetName;

        protected Class<?> configurationClass;

        protected Object configuration;

        public ConfigurationFinder(DocumentModel doc, String facetName) {
            super(doc.getCoreSession());
            this.documentRef = doc.getRef();
            this.facetName = facetName;
        }

        public <T extends LocalConfiguration> T find(Class<T> configurationClass) {
            this.configurationClass = configurationClass;
            try {
                runUnrestricted();
            } catch (ClientException e) {
                return null;
            }

            return configuration != null ? configurationClass.cast(configuration)
                    : null;
        }

        @Override
        public void run() throws ClientException {
            DocumentModel doc = session.getDocument(documentRef);
            DocumentModel parent = getFirstParentWithFacet(doc, facetName);
            if (parent != null) {
                LocalConfiguration localConfiguration = LocalConfiguration.class.cast(parent.getAdapter(configurationClass));
                while (localConfiguration.readapt()) {
                    doc = session.getParentDocument(parent.getRef());
                    parent = getFirstParentWithFacet(doc, facetName);
                    if (parent == null) {
                        // stop merging
                        break;
                    }
                    localConfiguration.merge(parent.getAdapter(configurationClass));
                }
                configuration = localConfiguration;
            }
        }

        protected DocumentModel getFirstParentWithFacet(DocumentModel doc,
                String facetName) throws ClientException {
            if (doc.hasFacet(facetName)) {
                return doc;
            } else {
                DocumentModel parent = session.getDocument(doc.getParentRef());
                if (parent == null || "/".equals(parent.getPathAsString())) {
                    return null;
                }
                return getFirstParentWithFacet(parent, facetName);
            }
        }

    }

}
