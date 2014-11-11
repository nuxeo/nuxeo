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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.restlet.data.Response;

public interface DocumentModelListSerializer {

    String colDefinitonDelimiter = ",";

    String SchemaDelimiter = ".";

    String listIndex = "[";

    String urlField = "url";

    String iconField = "icon";

    String pathField = "path";

    String typeField = "type";

    String stateField = "currentLifecycleState";

    String authorField = "author";

    String EMPTY_LIST = "empty result";

    DateFormat DATE_PARSER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    String serialize(DocumentModelList docList, List<String> columnsDefinition, HttpServletRequest req) throws ClientException;

    String serialize(DocumentModelList docList, String columnsDefinition, HttpServletRequest req) throws ClientException;

    String serialize(ResultSummary summary, DocumentModelList docList,
            String columnsDefinition, HttpServletRequest req) throws ClientException;

    String serialize(ResultSummary summary, DocumentModelList docList,
            List<String> columnsDefinition, HttpServletRequest req) throws ClientException;

    void serialize(DocumentModelList docList, String columnsDefinition,
            Response res , HttpServletRequest req) throws ClientException;

    void serialize(ResultSummary summary, DocumentModelList docList,
            String columnsDefinition, Response res, HttpServletRequest req) throws ClientException;

    void serialize(ResultSummary summary, DocumentModelList docList,
            String columnsDefinition, Response res, HttpServletRequest req, List<String> labels, String lang) throws ClientException;

    /*
     * String serialize(List<DashBoardItem> workItems, String
     * columnsDefinition, Map<String,String> options, Response res);
     */
}
