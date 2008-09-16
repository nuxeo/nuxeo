/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.restAPI;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;
import org.dom4j.dom.DOMDocument;
import org.dom4j.dom.DOMDocumentFactory;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;

/**
 * Restlet to invalidate the cache of directories from an external application.
 *
 * Warning, this restlet can only be used on the JVM that hosts the
 * DirectoryService instance since it is using non-remotable API. This means
 * this restlet will not work with the multi machine steups of Nuxeo.
 *
 * @author ogrisel
 *
 */
public class DirectoryCacheRestlet extends BaseNuxeoRestlet {

    private static final Log log = LogFactory.getLog(DirectoryCacheRestlet.class);

    final static public String DIRECTORY_NAME_QUERY_PARAM = "directory";

    @Override
    public void handle(Request req, Response res) {

        DOMDocumentFactory domfactory = new DOMDocumentFactory();
        DOMDocument result = (DOMDocument) domfactory.createDocument();

        try {
            DirectoryService service = Framework.getLocalService(DirectoryService.class);
            List<Directory> directories = new LinkedList<Directory>();
            Form form = req.getResourceRef().getQueryAsForm();

            if (form.getNames().contains(DIRECTORY_NAME_QUERY_PARAM)) {
                // only invalidate the caches of the requested directories
                String[] directoryNames = form.getValues(
                        DIRECTORY_NAME_QUERY_PARAM).split(",");
                for (String directoryName : directoryNames) {
                    Directory directory = service.getDirectory(directoryName);
                    if (directory == null) {
                        log.error("no such directory: " + directoryName);
                    } else {
                        directories.add(directory);
                    }
                }
            } else {
                // invalidate all directory caches
                directories = service.getDirectories();
            }

            Element invalidatedCachesElement = result.addElement("invalidatedCaches");
            for (Directory directory : directories) {
                directory.getCache().invalidateAll();
                invalidatedCachesElement.addElement("directory").addText(
                        directory.getName());
            }

        } catch (Exception e) {
            handleError(res, e);
            return;
        }
        res.setEntity(result.asXML(), MediaType.TEXT_XML);
    }
}
