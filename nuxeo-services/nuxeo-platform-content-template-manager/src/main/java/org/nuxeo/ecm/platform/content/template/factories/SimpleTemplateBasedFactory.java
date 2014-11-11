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

package org.nuxeo.ecm.platform.content.template.factories;

import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
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

    protected boolean isTargetEmpty(DocumentModel eventDoc) throws ClientException  {
        // If we already have children : exit !!!
        return session.getChildren(eventDoc.getRef()).isEmpty();
    }

    public void createContentStructure(DocumentModel eventDoc)
            throws ClientException {
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
            DocumentModel newChild = session.createDocumentModel(itemPath,
                    item.getId(), item.getTypeName());
            newChild.setProperty("dublincore", "title", item.getTitle());
            newChild.setProperty("dublincore", "description",
                    item.getDescription());
            setProperties(item.getProperties(), newChild);
            newChild = session.createDocument(newChild);
            setAcl(item.getAcl(), newChild.getRef());
        }
    }

    protected void setProperties(List<PropertyDescriptor> properties,
            DocumentModel doc) throws ClientException {
        if (properties != null && !properties.isEmpty()) {
            for (PropertyDescriptor property : properties) {
                doc.setPropertyValue(property.getXpath(), property.getValue());
            }
        }
    }

    protected void setAcl(List<ACEDescriptor> aces, DocumentRef ref)
            throws ClientException {
        if (aces != null && !aces.isEmpty()) {
            ACP acp = session.getACP(ref);
            ACL existingACL = acp.getOrCreateACL();

            // clean any existing ACL (should a merge strategy be adopted
            // instead?)
            existingACL.clear();

            // add the the ACL defined in the descriptor
            for (ACEDescriptor ace : aces) {
                existingACL.add(new ACE(ace.getPrincipal(),
                        ace.getPermission(), ace.getGranted()));
            }
            // read the acl to invalidate the ACPImpl cache
            acp.addACL(existingACL);
            session.setACP(ref, acp, true);
        }
    }

    public boolean initFactory(Map<String, String> options,
            List<ACEDescriptor> rootAcl, List<TemplateItemDescriptor> template) {
        this.template = template;
        acl = rootAcl;
        return true;
    }

}
