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

package org.nuxeo.ecm.platform.syndication.serializer;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.restlet.data.Response;

public class SerializerHelper {

    // Utility class.
    private SerializerHelper() {
    }

    public static void formatResult(ResultSummary summary,
            DocumentModelList dmList, Response res, String format,
            String columnsDefinition, HttpServletRequest req)
            throws ClientException {
        formatResult(summary, dmList, res, format, columnsDefinition, req,
                null, null);
    }

    public static void formatResult(ResultSummary summary,
            DocumentModelList dmList, Response res, String format,
            String columnsDefinition, HttpServletRequest req,
            List<String> labels, String lang) throws ClientException {
        DocumentModelListSerializer dms;

        if (format.equalsIgnoreCase("JSON")) {
            dms = new DMJSONSerializer();
        } else if (format.equalsIgnoreCase("XML")) {
            dms = new SimpleXMLSerializer();
        } else if (format.equalsIgnoreCase("RSS")) {
            dms = new RSSSerializer();
        } else if (format.equalsIgnoreCase("ATOM")) {
            dms = new ATOMSerializer();
        } else {
            dms = new SimpleXMLSerializer();
        }

        if (lang != null) {
            dms.serialize(summary, dmList, columnsDefinition, res, req, labels,
                    lang);
        } else {
            dms.serialize(summary, dmList, columnsDefinition, res, req);
        }

    }

}
