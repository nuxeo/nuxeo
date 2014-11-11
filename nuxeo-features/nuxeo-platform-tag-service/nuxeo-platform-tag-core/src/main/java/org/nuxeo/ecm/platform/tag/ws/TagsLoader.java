/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     matic
 */
package org.nuxeo.ecm.platform.tag.ws;

import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.api.ws.DocumentLoader;
import org.nuxeo.ecm.platform.api.ws.DocumentProperty;
import org.nuxeo.ecm.platform.api.ws.session.WSRemotingSession;
import org.nuxeo.ecm.platform.tag.Tag;
import org.nuxeo.ecm.platform.tag.TagService;
import org.nuxeo.runtime.api.Framework;

/**
 * @author matic
 *
 */
public class TagsLoader implements DocumentLoader {

    @Override
    public void fillProperties(DocumentModel doc,
            List<DocumentProperty> props, WSRemotingSession rs)
            throws ClientException {
        CoreSession session = rs.getDocumentManager();
        TagService srv = Framework.getLocalService(TagService.class);
        if (srv == null) {
            return;
        }
        List<Tag> tags = srv.getDocumentTags(session, doc.getId(), rs.getUsername());
        String value = "";
        String sep = "";
        for (Tag tag:tags) {
            value = value + sep + tag.getLabel();
            sep = ",";
        }
        DocumentProperty prop = new DocumentProperty("tags", value);
        props.add(prop);
    }

}
