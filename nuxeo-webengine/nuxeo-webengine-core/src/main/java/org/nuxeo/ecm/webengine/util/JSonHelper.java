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

package org.nuxeo.ecm.webengine.util;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.webengine.WebException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
// TODO: Not used. Remove?
public class JSonHelper {

    private static final Log log = LogFactory.getLog(JSonHelper.class);

    // Utility class.
    private JSonHelper() {
    }

    public static String toJSon(DocumentModel doc) {
        return doc2JSon(doc).toString();
    }

    public static String toChildrenList(DocumentModel doc) {
        return getChildrenList(doc).toString();
    }

    public static JSONArray asJSON(DocumentModelList docList)  {
        JSONArray list  = new JSONArray();
        if (docList != null) {
            for (DocumentModel d : docList) {
                JSONObject o = new JSONObject();
                o.put("id", d.getId());
                o.put("name", d.getName());
                o.put("path", d.getPathAsString());
                o.put("type", d.getType());
                try {
                    o.put("title", d.getTitle());
                } catch (ClientException e) {
                    o.put("title", d.getName());
                }
                o.put("isFolderish", d.hasFacet("Folderish"));
                list.add(o);
            }
        }
        return list;
    }

    public static JSONArray getChildrenList(DocumentModel doc) {
        CoreSession session = CoreInstance.getInstance().getSession(doc.getSessionId());
        try {
            DocumentModelList docs = session.getChildren(doc.getRef());
            return asJSON(docs);
        } catch (ClientException e) {
            log.error(e, e);
        }
        return null;
    }

    public static String toJSon(DocumentModel doc, String ... schemas) {
        return doc2JSon(doc).toString();
    }

    public static JSONObject doc2JSon(DocumentModel doc) {
        return doc2JSon(doc, (String[]) null);
    }

    public static JSONObject doc2JSon(DocumentModel doc, String... schemas) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("id", doc.getId());
            obj.put("name", doc.getName());
            obj.put("path", doc.getPathAsString());
            obj.put("isLocked", doc.isLocked());
            obj.put("lifeCycleState", doc.getCurrentLifeCycleState());
            obj.put("lifeCyclePolicy", doc.getLifeCyclePolicy());
            obj.put("type", doc.getType());
            obj.put("isVersion", doc.isVersion());
            obj.put("isProxy", doc.isProxy());
            obj.put("sourceId", doc.getSourceId());
            obj.put("facets", doc.getFacets());
            obj.put("schemas", doc.getSchemas());
            JSonDocumentExporter jde = new JSonDocumentExporter();
            if (schemas != null) {
                for (String schema : schemas) {
                    obj.put(schema, jde.run(doc.getPart(schema)));
                }
            } else {
                for (DocumentPart part : doc.getParts()) {
                    obj.put(part.getName(), jde.run(part));
                }
            }
            return obj;
        } catch (Exception e) {
            throw WebException.wrap("Failed to export document as json: "
                    + doc.getPath(), e);
        }
    }

//  public static DocumentModel fromJSon(JSONObject obj) {
//  String id = obj.getString("id");
//  String type = obj.getString("type");
//  String name = obj.getString("name");
//  String path = obj.getString("path");
//  String parentPath = new Path(path).removeLastSegments(1).toString();
//  DocumentModelImpl doc = new DocumentModelImpl(String sid, String type, String id, Path path,
//  String lock, DocumentRef docRef, DocumentRef parentRef,
//  String[] schemas, Set<String> facets, String sourceId,
//  String repositoryName);
//  return doc;
//  }

}
