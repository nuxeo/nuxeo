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
 *     "Stephane Lacoin (aka matic) <slacoin@nuxeo.com>"
 */
package org.nuxeo.ecm.platform.tag;

import java.util.Calendar;

import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.repository.RepositoryInitializationHandler;

/**
 * @author "Stephane Lacoin (aka matic) <slacoin@nuxeo.com>"
 */
public class TagServiceInitializer extends RepositoryInitializationHandler {

    @Override
    public void doInitializeRepository(CoreSession session)
            throws ClientException {

        Path rootTagPath = new Path(session.getRootDocument().getPathAsString());
        String rootTagName = IdUtils.generateId(TagConstants.TAGS_DIRECTORY);
        rootTagPath = rootTagPath.append(rootTagName);

        if (!session.exists(new PathRef(rootTagPath.toString()))) {
            DocumentModel rootTag = session.createDocumentModel(
                    session.getRootDocument().getPathAsString(),
                    rootTagName,
                    TagConstants.HIDDEN_FOLDER_TYPE);
            rootTag.setPropertyValue("dc:title", TagConstants.TAGS_DIRECTORY);
            rootTag.setPropertyValue("dc:description", "");
            rootTag.setPropertyValue("dc:created", Calendar.getInstance());
            rootTag = session.createDocument(rootTag);
            rootTag = session.saveDocument(rootTag);

            // Add default permission: Read for everybody
            ACE ace = new ACE("Everyone", SecurityConstants.READ, true);
            ACL acl = new ACLImpl();
            acl.add(ace);
            ACP acp = new ACPImpl();
            acp.addACL(acl);
            session.setACP(rootTag.getRef(), acp, true);
            session.save();
        }
    }

}
