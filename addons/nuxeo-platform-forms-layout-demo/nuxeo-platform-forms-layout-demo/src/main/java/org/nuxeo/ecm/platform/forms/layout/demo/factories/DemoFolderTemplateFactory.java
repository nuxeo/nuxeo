/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Guillaume Renard
 */
package org.nuxeo.ecm.platform.forms.layout.demo.factories;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.content.template.factories.SimpleTemplateBasedFactory;
import org.nuxeo.ecm.platform.content.template.service.TemplateItemDescriptor;

/**
 * Create a Folder with a couple of documents with titles starting with 'aaa'.
 *
 * @since 5.7.2
 */
public class DemoFolderTemplateFactory extends SimpleTemplateBasedFactory {

    private final static String[] TOKENS = { "aaaabb", "aaaccc", "aaaddd",
            "aaaaeee", "aaaafff", "aaaggg", "aaahhh", "aaaiii", "aaaajjj",
            "aaakkk", };

    @Override
    public void createContentStructure(DocumentModel eventDoc)
            throws ClientException {
        initSession(eventDoc);

        if (eventDoc.isVersion() || !isTargetEmpty(eventDoc)) {
            return;
        }

        setAcl(acl, eventDoc.getRef());

        DocumentModel newChild = null;

        char a = 'a';
        for (TemplateItemDescriptor item : template) {
            String itemPath = eventDoc.getPathAsString();
            if (item.getPath() != null) {
                itemPath += "/" + item.getPath();
            }
            newChild = session.createDocumentModel(itemPath, item.getId(),
                    item.getTypeName());
            newChild.setProperty("dublincore", "title", item.getTitle());
            newChild.setProperty("dublincore", "description",
                    item.getDescription());
            setProperties(item.getProperties(), newChild);
            newChild = session.createDocument(newChild);
            setAcl(item.getAcl(), newChild.getRef());

            if (newChild.isFolder()) {
                DocumentModel newGrantChild = session.createDocumentModel(
                        newChild.getPathAsString() + "/", "defaultId", "File");
                newGrantChild.setProperty("dublincore", "title", "Some sample text");
                newGrantChild = session.createDocument(newGrantChild);
                for (String token : TOKENS) {
                    final String id = token + a;
                    newGrantChild = session.createDocumentModel(
                            newChild.getPathAsString() + "/", id, "File");
                    newGrantChild.setProperty("dublincore", "title", id);
                    newGrantChild = session.createDocument(newGrantChild);
                }
            }
            a++;
        }
    }

}
