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
import org.nuxeo.ecm.platform.content.template.service.NotificationDescriptor;
import org.nuxeo.ecm.platform.content.template.service.PropertyDescriptor;
import org.nuxeo.ecm.platform.content.template.service.TemplateItemDescriptor;
import org.nuxeo.ecm.platform.ec.notification.NotificationConstants;
import org.nuxeo.ecm.platform.notification.api.NotificationManager;
import org.nuxeo.runtime.api.Framework;

public class SimpleTemplateBasedFactory extends BaseContentFactory {

    protected List<TemplateItemDescriptor> template;

    protected List<ACEDescriptor> acl;

    protected List<NotificationDescriptor> notifications;

    protected NotificationManager notificationManager;

    protected boolean isTargetEmpty(DocumentModel eventDoc)
            throws ClientException {
        // If we already have children : exit !!!
        if (!session.getChildren(eventDoc.getRef()).isEmpty()) {
            return false;
        } else {
            return true;
        }

    }

    public void createContentStructure(DocumentModel eventDoc)
            throws ClientException {
        super.initSession(eventDoc);

        if (!isTargetEmpty(eventDoc)) {
            return;
        }

        setAcl(acl, eventDoc.getRef());

        for (TemplateItemDescriptor item : template) {
            String itemPath = eventDoc.getPathAsString();
            if (item.getPath() != null) {
                itemPath = itemPath + "/" + item.getPath();
            }
            DocumentModel newChild = session.createDocumentModel(itemPath,
                    item.getId(), item.getTypeName());
            newChild.setProperty("dublincore", "title", item.getTitle());
            newChild.setProperty("dublincore", "description",
                    item.getDescription());
            setProperties(item.getProperties(), newChild);
            newChild = session.createDocument(newChild);
            setAcl(item.getAcl(), newChild.getRef());
            setNotifications(item.getNotifications(), newChild.getRef());
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
            // readd the acl to invalidate the ACPImpl cache
            acp.addACL(existingACL);
            session.setACP(ref, acp, true);
        }
    }

    protected void setNotifications(List<NotificationDescriptor> notifications,
            DocumentRef ref) throws ClientException {
        if (notifications != null && !notifications.isEmpty()) {
            DocumentModel doc = session.getDocument(ref);
            for (NotificationDescriptor notification : notifications) {
                List<String> users = notification.getUsers();
                for (String user : users) {
                    getNotificationManager().addSubscription(
                            NotificationConstants.USER_PREFIX + user,
                            notification.getEvent(), doc, false,
                            null, "");
                }
                List<String> groups = notification.getGroups();
                for (String group : groups) {
                    getNotificationManager().addSubscription(
                            NotificationConstants.GROUP_PREFIX + group,
                            notification.getEvent(), doc, false,
                            null, "");

                }
            }
        }
    }

    public boolean initFactory(Map<String, String> options,
            List<ACEDescriptor> rootAcl,
            List<NotificationDescriptor> notifications,
            List<TemplateItemDescriptor> template) {
        this.template = template;
        acl = rootAcl;
        this.notifications = notifications;
        return true;
    }

    protected NotificationManager getNotificationManager() {
        if (notificationManager == null) {
            try {
                notificationManager = Framework.getService(NotificationManager.class);
            } catch (Exception e) {
                throw new RuntimeException(
                        "Could not get NotificationManager service", e);
            }
        }
        return notificationManager;
    }
}
