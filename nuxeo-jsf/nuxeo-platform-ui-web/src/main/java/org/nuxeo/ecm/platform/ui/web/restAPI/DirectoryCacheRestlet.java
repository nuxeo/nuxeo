/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.CharacterSet;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;

/**
 * Restlet to invalidate the cache of directories from an external application.
 * <p>
 * Warning: this restlet can only be used on the JVM that hosts the DirectoryService instance since it is using
 * non-remotable API. This means this restlet will not work with the multi machine setups of Nuxeo.
 *
 * @author ogrisel
 */
public class DirectoryCacheRestlet extends BaseNuxeoRestlet {

    private static final Log log = LogFactory.getLog(DirectoryCacheRestlet.class);

    public static final String DIRECTORY_NAME_QUERY_PARAM = "directory";

    @Override
    public void handle(Request req, Response res) {

        DOMDocumentFactory domFactory = new DOMDocumentFactory();
        DOMDocument result = (DOMDocument) domFactory.createDocument();

        try {
            DirectoryService service = Framework.getService(DirectoryService.class);
            List<Directory> directories = new LinkedList<Directory>();
            Form form = req.getResourceRef().getQueryAsForm();

            if (form.getNames().contains(DIRECTORY_NAME_QUERY_PARAM)) {
                // only invalidate the caches of the requested directories
                String[] directoryNames = form.getValues(DIRECTORY_NAME_QUERY_PARAM).split(",");
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
                invalidatedCachesElement.addElement("directory").addText(directory.getName());
            }

        } catch (DirectoryException e) {
            handleError(res, e);
            return;
        }
        Representation rep = new StringRepresentation(result.asXML(), MediaType.APPLICATION_XML);
        rep.setCharacterSet(CharacterSet.UTF_8);
        res.setEntity(rep);
    }

}
