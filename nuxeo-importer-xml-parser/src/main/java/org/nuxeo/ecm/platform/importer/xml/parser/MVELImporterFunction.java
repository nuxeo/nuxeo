/*
 * (C) Copyright 2002-2013 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.ecm.platform.importer.xml.parser;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.dom4j.Element;
import org.nuxeo.ecm.automation.core.scripting.CoreFunctions;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.model.NoSuchDocumentException;

/**
 * Some helper function that are injected inside MVEL context
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
public class MVELImporterFunction extends CoreFunctions {

    protected final CoreSession session;

    protected final Stack<DocumentModel> docsStack;

    protected final Map<Element, DocumentModel> elToDoc;

    protected final Element el;

    public MVELImporterFunction(CoreSession session, Stack<DocumentModel> docsStack,
            Map<Element, DocumentModel> elToDoc, Element el) {
        super();
        this.session = session;
        this.docsStack = docsStack;
        this.elToDoc = elToDoc;
        this.el = el;
    }

    public Calendar parseDate(String source, String format) throws ParseException {
        DateFormat df = new SimpleDateFormat(format);
        Date date = df.parse(source);
        Calendar result = Calendar.getInstance();
        result.setTime(date);
        return result;
    }

    public DocumentModel mkdir(DocumentModel parent, String regexp, String data, String typeName)
            throws ClientException {

        String[] parts = data.split(regexp);
        List<DocumentModel> result = new ArrayList<DocumentModel>();
        DocumentModel root = parent;

        for (String part : parts) {
            DocumentModel child = null;
            try {
                child = session.getChild(root.getRef(), part);
            } catch (NoSuchDocumentException e) {
                child = session.createDocumentModel(root.getPathAsString(), part, typeName);
                child.setPropertyValue("dc:title", part);
                child = session.createDocument(child);
            }
            result.add(child);
            docsStack.push(child);
            root = child;
        }

        if (result.size() > 0) {
            elToDoc.put(el, result.get(result.size() - 1));
            return result.get(result.size() - 1);
        }
        return null;
    }

}
