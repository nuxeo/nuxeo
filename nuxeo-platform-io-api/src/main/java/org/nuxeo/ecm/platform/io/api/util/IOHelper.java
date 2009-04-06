/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.io.api.util;

import java.io.IOException;
import java.util.Collection;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;

/**
 * Exposes a copy method that works between different repositories (optionally on different hosts).
 * The copy method must be moved to IOManager.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class IOHelper {

    // Utility class.
    private IOHelper() {
    }

    /**
     * Copy from one location to another. The source and/or target repositories may be the same or may be
     * located on different hosts.
     *
     * TODO: because of some methods not exposed in IOManager this method is not optimized
     * to avoid handling exports / imports from/to local repositories through remote streams.
     */
    public static void copy(IOConfiguration src, IOConfiguration dest,
            Collection<String> ioAdapters) throws IOException, ClientException {
        String uri = exportAsStream(src, ioAdapters);
        try {
            importFromStream(dest, uri);
        } finally {
            if (uri != null) {

            }
        }
        /*
          TODO optimize local exports/imports
        if (src.isLocal() && dest.isLocal()) {
            File file = exportAsFile(src, ioAdapters);
            importFromFile(dest, file);
        } else {
            String uri = exportAsStream(src, ioAdapters);
            importFromStream(dest, uri);
        }
        */
    }

    public static String exportAsStream(IOConfiguration location, Collection<String> ioAdapters) throws ClientException {
        return location.getManager().externalizeExport(location.getRepositoryName(), location.getDocuments(),
                (String) location.getProperty(IOConfiguration.DOC_READER_FACTORY),
                location.getProperties(), ioAdapters);
    }

    public static void importFromStream(IOConfiguration location, String streamUri) throws ClientException {
        String docWriterFactoryName = (String) location.getProperty(IOConfiguration.DOC_WRITER_FACTORY);
        // call remote server to do the upload
        DocumentLocation targetLocation = new DocumentLocationImpl(
                location.getRepositoryName(), location.getFirstDocument());
        if (docWriterFactoryName == null) {
            // default import
            location.getManager().importExportedFile(streamUri, targetLocation);
        } else {
            location.getManager().importExportedFile(streamUri, targetLocation,
                    docWriterFactoryName, location.getProperties());
        }
    }

}
