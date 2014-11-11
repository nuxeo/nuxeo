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

package org.nuxeo.ecm.platform.syndication.restAPI;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.dom4j.dom.DOMDocument;
import org.dom4j.dom.DOMDocumentFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.syndication.vocabularies.SimpleVocabulary;
import org.nuxeo.ecm.platform.syndication.vocabularies.Tree;
import org.nuxeo.ecm.platform.ui.web.restAPI.BaseStatelessNuxeoRestlet;
import org.nuxeo.runtime.api.Framework;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.w3c.dom.Element;

/**
 * Simple restlet to export vocabularies content.
 *
 * @author tiry
 */
public class VocabularyRestlet extends BaseStatelessNuxeoRestlet {

    // XXX TODO : fix lang management : unable to get something else that
    // default language
    // XXX TODO : add an API to be able to get one entry ?
    // XXX TODO : add an API to browse hierarchical voc

    private String getTranslation(String key, java.util.Locale local) {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("messages", local,
                    Thread.currentThread().getContextClassLoader());
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            return key;
        }
    }

    @Override
    public void handle(Request req, Response res) {

        DOMDocumentFactory domfactory = new DOMDocumentFactory();
        DOMDocument result = (DOMDocument) domfactory.createDocument();

        java.util.Locale translationLocal = java.util.Locale.getDefault();
        String lang = req.getResourceRef().getQueryAsForm().getFirstValue(
                "lang");

        if (lang != null && !"".equals(lang)) {
            for (java.util.Locale loc : java.util.Locale.getAvailableLocales()) {
                if (loc.getLanguage().equalsIgnoreCase(lang)) {
                    translationLocal = loc;
                    continue;
                }
            }
        }

        DirectoryService directoryService;
        try {
            directoryService = Framework.getService(DirectoryService.class);
            if (directoryService == null) {
                handleError(result, res, "Unable to get Directory Service");
                return;
            }
        } catch (Exception e) {
            handleError(result, res, e);
            return;
        }

        String vocName = (String) req.getAttributes().get("vocName");
        if ("".equals(vocName)) {
            handleError(result, res, "You must specify a vocabulary name");
            return;
        }

        Session dirSession;
        try {
            dirSession = directoryService.open(vocName);
            String directorySchema = directoryService.getDirectorySchema(vocName);

            if (directorySchema.equals("vocabulary")) {
                Element current = result.createElement("entries");
                result.setRootElement((org.dom4j.Element) current);

                for (DocumentModel entry : dirSession.getEntries()) {
                    Element el = result.createElement("entry");
                    el.setAttribute("id", entry.getId());
                    el.setAttribute("label", (String) entry.getProperty(
                            "vocabulary", "label"));
                    el.setAttribute("translatedLabel", getTranslation(
                            (String) entry.getProperty("vocabulary", "label"),
                            translationLocal));
                    current.appendChild(el);
                }
            } else if (directorySchema.equals("xvocabulary")) {
                final Tree.Builder treeBuilder = new Tree.Builder();
                for (final DocumentModel doc : dirSession.getEntries()) {
                    final String id = doc.getId();
                    final String label = (String) doc.getProperty(
                            "xvocabulary", "label");
                    final String translatedLabel = getTranslation(label,
                            translationLocal);
                    final String parent = (String) doc.getProperty(
                            "xvocabulary", "parent");

                    final SimpleVocabulary voca = new SimpleVocabulary(id,
                            label, translatedLabel);
                    treeBuilder.addElement(parent, voca);
                }
                final Tree tree = treeBuilder.build();
                tree.buildXML(result);
            } else {
                handleError(result, res,
                        "Selected directory is not a vocabulary");
                return;
            }
        } catch (ClientException e) {
            handleError(result, res, e);
            return;
        }

        try {
            dirSession.close();
        } catch (ClientException e) {
            handleError(result, res, e);
            return;
        }

        res.setEntity(result.asXML(), MediaType.TEXT_XML);
    }

}
