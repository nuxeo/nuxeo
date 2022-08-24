/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id$
 */

package org.nuxeo.ecm.platform.content.template.factories;

import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.platform.content.template.service.ACEDescriptor;
import org.nuxeo.ecm.platform.content.template.service.PropertyDescriptor;
import org.nuxeo.ecm.platform.content.template.service.TemplateItemDescriptor;

public class SimpleTemplateBasedFactory extends BaseContentFactory {

    protected List<TemplateItemDescriptor> template;

    protected List<ACEDescriptor> acl;

    protected boolean isTargetEmpty(DocumentModel eventDoc) {
        // If we already have children : exit !!!
        return session.getChildren(eventDoc.getRef()).isEmpty();
    }

    public void createContentStructure(DocumentModel eventDoc) {
        initSession(eventDoc);

        if (eventDoc.isVersion() || !isTargetEmpty(eventDoc)) {
            return;
        }

        setAcl(acl, eventDoc.getRef());

        for (TemplateItemDescriptor item : template) {
            String itemPath = eventDoc.getPathAsString();
            if (item.getPath() != null) {
                itemPath += "/" + item.getPath();
            }
            DocumentModel newChild = session.createDocumentModel(itemPath, item.getId(), item.getTypeName());
            newChild.setProperty("dublincore", "title", item.getTitle());
            newChild.setProperty("dublincore", "description", item.getDescription());
            setProperties(item.getProperties(), newChild);
            newChild = session.createDocument(newChild);
            setAcl(item.getAcl(), newChild.getRef());
        }
    }

    protected void setProperties(List<PropertyDescriptor> properties, DocumentModel doc) {
        if (properties != null && !properties.isEmpty()) {
            for (PropertyDescriptor property : properties) {
                doc.setPropertyValue(property.getXpath(), property.getValue());
            }
        }
    }

    protected void setAcl(List<ACEDescriptor> aces, DocumentRef ref) {
        // Templates are created programmatically with their ACLs from a listener, according to static xml contribs.
        // The origin of the call doesn't matter.
        CoreInstance.doPrivileged(session, session -> {
            if (aces != null && !aces.isEmpty()) {
                ACP acp = session.getACP(ref);
                ACL existingACL = acp.getOrCreateACL();

                // clean any existing ACL (should a merge strategy be adopted
                // instead?)
                existingACL.clear();

                // add the the ACL defined in the descriptor
                for (ACEDescriptor ace : aces) {
                    existingACL.add(new ACE(ace.getPrincipal(), ace.getPermission(), ace.getGranted()));
                }
                // read the acl to invalidate the ACPImpl cache
                acp.addACL(existingACL);
                session.setACP(ref, acp, true);
            }
        });
    }

    public boolean initFactory(Map<String, String> options, List<ACEDescriptor> rootAcl,
            List<TemplateItemDescriptor> template) {
        this.template = template;
        acl = rootAcl;
        return true;
    }

}
