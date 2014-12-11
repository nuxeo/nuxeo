/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     vpasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.binary.metadata.api.service;

import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * {@inheritDoc}
 *
 * @since 7.1
 */
public class BinaryMetadataServiceImpl implements BinaryMetadataService {

    /**
     * {@inheritDoc}
     */
    @Override
    public void readMetadata(DocumentModel doc) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeMetadata(DocumentModel doc) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> readMetadata(String processorName, Blob blob, List<String> metadataNames) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeMetadata(String processorName, Blob blob, Map<String, String> metadata) {

    }
}
