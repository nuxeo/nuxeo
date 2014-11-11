/*
 * (C) Copyright 2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.directory.localconfiguration;

import static org.nuxeo.ecm.directory.localconfiguration.DirectoryConfigurationConstants.DIRECTORY_CONFIGURATION_FIELD;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.localconfiguration.AbstractLocalConfiguration;

/**
 * Default implementation of {@code DirectoryConfiguration}.
 *
 * @author <a href="mailto:qlamerand@nuxeo.com">Benjamin JALON</a>
 * @since 5.4.2
 */
public class DirectoryConfigurationAdapter extends
        AbstractLocalConfiguration<DirectoryConfiguration> implements
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
        } catch (ClientException e) {
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
        throw new UnsupportedOperationException(
                "Directory configurations can't be merged");
    }

    @Override
    public String getDirectorySuffix() {
        return lcDirectorySuffix;
    }

}
