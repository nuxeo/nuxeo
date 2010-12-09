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
 * $Id$
 */

package org.nuxeo.ecm.platform.ui.web.directory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.remoting.WebRemote;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;

/**
 * @deprecated: use rich:suggestionbox instead
 */
@Name("suggestBox")
@Scope(ScopeType.EVENT)
@Deprecated
public class SuggestBoxBean implements Serializable {

    private static transient DirectoryService dirService;

    private static final List<Map<String, String>> tempLabel = new ArrayList<Map<String, String>>();

    private static final long serialVersionUID = 9206507449990045737L;

    public static DirectoryService getService() {
        if (dirService == null) {
            dirService = DirectoryHelper.getDirectoryService();
        }
        return dirService;
    }

    private static boolean queryVoc(String directoryName, String input)
            throws Exception {
        Session directory = null;
        try {
            Map<String, Serializable> filter = new LinkedHashMap<String, Serializable>();
            Set<String> fulltext = new HashSet<String>();
            fulltext.add("label");
            filter.put("label", input);
            directory = getService().open(directoryName);
            DocumentModelList directoryEntries = directory.query(filter,
                    fulltext);
            for (DocumentModel documentModel : directoryEntries) {
                // XXX hack, directory entries have only one datamodel
                String schemaName = documentModel.getSchemas()[0];
                DataModel dm = documentModel.getDataModel(schemaName);
                Map<String, String> map = new HashMap<String, String>();
                map.put("label", (String) dm.getData("label"));
                map.put("id", (String) dm.getData("id"));
                tempLabel.add(map);
            }
        } finally {
            if (directory != null) {
                directory.close();
            }
        }
        return false;
    }

    // ToDo, remove this function and use filters in the directory service
    // instead.
    public static void substractTab(String input) {
        List<Map<String, String>> tmp = new ArrayList<Map<String, String>>();
        for (Map<String, String> map : tempLabel) {
            String label = map.get("label");
            if (label.equals("")) {
                label = map.get("id");
            }
            if (!label.toLowerCase().startsWith(input.toLowerCase())) {
                tmp.add(map);
            }
        }
        tempLabel.removeAll(tmp);
    }

    private static String jsonEscaping(String label) {
        return label.replace("\\", "\\\\\\\\").replace("\"", "\\\\\\\"");
    }

    @WebRemote
    public static String getSuggestedValues(String directoryName, String input)
            throws Exception {
        queryVoc(directoryName, "");
        substractTab(input);
        StringBuilder sb = new StringBuilder();
        sb.append("{\"results\": [");
        for (int i = 0; i < tempLabel.size(); i++) {
            sb.append("{\"id\": \"" + (i + 1) + "\", \"value\": \""
                    + jsonEscaping(tempLabel.get(i).get("label"))
                    + "\", \"info\": \""
                    + jsonEscaping(tempLabel.get(i).get("id")) + "\"},");
        }
        sb.append("]}");
        return sb.toString();
    }

}
