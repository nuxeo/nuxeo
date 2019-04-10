/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Guillaume Renard
 */
package org.nuxeo.ecm.platform.forms.layout.demo.factories;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.content.template.factories.SimpleTemplateBasedFactory;
import org.nuxeo.ecm.platform.content.template.service.TemplateItemDescriptor;

/**
 * Create a Folder with a couple of documents with titles starting with 'aaa'.
 *
 * @since 5.7.2
 */
public class DemoFolderTemplateFactory extends SimpleTemplateBasedFactory {

    private final static String[] TOKENS = { "aaaabb", "aaaccc", "aaaddd", "aaaaeee", "aaaafff", "aaaggg", "aaahhh",
            "aaaiii", "aaaajjj", "aaakkk", };

    @Override
    public void createContentStructure(DocumentModel eventDoc) {
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
            newChild = session.createDocumentModel(itemPath, item.getId(), item.getTypeName());
            newChild.setProperty("dublincore", "title", item.getTitle());
            newChild.setProperty("dublincore", "description", item.getDescription());
            setProperties(item.getProperties(), newChild);
            newChild = session.createDocument(newChild);
            setAcl(item.getAcl(), newChild.getRef());

            if (newChild.isFolder()) {
                DocumentModel newGrantChild = session.createDocumentModel(newChild.getPathAsString() + "/",
                        "defaultId", "File");
                newGrantChild.setProperty("dublincore", "title", "Some sample text");
                newGrantChild = session.createDocument(newGrantChild);
                for (String token : TOKENS) {
                    final String id = token + a;
                    newGrantChild = session.createDocumentModel(newChild.getPathAsString() + "/", id, "File");
                    newGrantChild.setProperty("dublincore", "title", id);
                    newGrantChild = session.createDocument(newGrantChild);
                }
            }
            a++;
        }
    }

}
