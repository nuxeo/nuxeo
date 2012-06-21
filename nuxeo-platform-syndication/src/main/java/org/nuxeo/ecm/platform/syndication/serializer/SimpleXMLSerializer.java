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

import org.dom4j.dom.DOMDocument;
import org.dom4j.dom.DOMDocumentFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.w3c.dom.Element;

public class SimpleXMLSerializer extends AbstractDocumentModelSerializer {

    private static final String rootNodeName = "results";

    private static final String docNodeName = "document";

    @Override
    public String serialize(ResultSummary summary, DocumentModelList docList,
            List<String> columnsDefinition, HttpServletRequest req)
            throws ClientException {
        if (docList == null) {
            return EMPTY_LIST;
        }

        DOMDocumentFactory domfactory = new DOMDocumentFactory();
        DOMDocument result = (DOMDocument) domfactory.createDocument();

        Element current = result.createElement(rootNodeName);
        result.setRootElement((org.dom4j.Element) current);

        Element pagesElement = result.createElement("pages");
        pagesElement.setAttribute("pages", Integer.toString(summary.getPages()));
        pagesElement.setAttribute("pageNumber",
                Integer.toString(summary.getPageNumber()));
        current.appendChild(pagesElement);

        for (DocumentModel doc : docList) {
            Element el = result.createElement(docNodeName);
            el.setAttribute("id", doc.getId());

            for (String colDef : columnsDefinition) {
                ResultField res = getDocumentProperty(doc, colDef);
                el.setAttribute(res.getName(), res.getValue());
            }
            current.appendChild(el);
        }

        return result.asXML();
    }

}
