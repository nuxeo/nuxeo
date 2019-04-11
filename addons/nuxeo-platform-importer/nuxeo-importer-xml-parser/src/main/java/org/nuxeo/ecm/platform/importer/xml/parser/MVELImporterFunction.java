/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;

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
            {

        String[] parts = data.split(regexp);
        List<DocumentModel> result = new ArrayList<>();
        DocumentModel root = parent;

        for (String part : parts) {
            DocumentModel child = null;
            try {
                child = session.getChild(root.getRef(), part);
            } catch (DocumentNotFoundException e) {
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
