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
 * Contributors:
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.directory.localconfiguration;

import static org.nuxeo.ecm.directory.localconfiguration.DirectoryConfigurationConstants.DIRECTORY_CONFIGURATION_FIELD;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.localconfiguration.AbstractLocalConfiguration;

/**
 * Default implementation of {@code DirectoryConfiguration}.
 *
 * @author <a href="mailto:qlamerand@nuxeo.com">Benjamin JALON</a>
 * @since 5.4.2
 */
public class DirectoryConfigurationAdapter extends AbstractLocalConfiguration<DirectoryConfiguration> implements
        DirectoryConfiguration {

    private static final Log log = LogFactory.getLog(DirectoryConfigurationAdapter.class);

    protected DocumentRef documentRef;

    protected String lcDirectorySuffix;

    public DirectoryConfigurationAdapter(DocumentModel doc) {
        documentRef = doc.getRef();
        try {
            lcDirectorySuffix = (String) doc.getPropertyValue(DIRECTORY_CONFIGURATION_FIELD);
            if (lcDirectorySuffix != null) {
                lcDirectorySuffix = lcDirectorySuffix.trim();
            }
        } catch (PropertyException e) {
            log.error("Failed to get DirectoryConfiguration", e);
        }
    }

    @Override
    public boolean canMerge() {
        return false;
    }

    @Override
    public DocumentRef getDocumentRef() {
        return documentRef;
    }

    @Override
    public DirectoryConfiguration merge(DirectoryConfiguration other) {
        throw new UnsupportedOperationException("Directory configurations can't be merged");
    }

    @Override
    public String getDirectorySuffix() {
        return lcDirectorySuffix;
    }

}
