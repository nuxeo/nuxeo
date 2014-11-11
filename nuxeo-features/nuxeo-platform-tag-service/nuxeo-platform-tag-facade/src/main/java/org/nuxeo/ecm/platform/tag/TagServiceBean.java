/*
 * (C) Copyright 2009-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Radu Darlea
 *     Florent Guillaume
 */

package org.nuxeo.ecm.platform.tag;

import java.util.List;

import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.runtime.api.Framework;

/**
 * Stateless bean for the tag service.
 */
@Stateless
@Local(TagServiceLocal.class)
@Remote(TagServiceRemote.class)
public class TagServiceBean implements TagService {

    private static final Log log = LogFactory.getLog(TagServiceBean.class);

    protected TagService tagService;

    protected TagService getLocalTagService() throws ClientException {
        if (tagService == null) {
            try {
                tagService = (TagService) Framework.getRuntime().getComponent(
                        TagService.ID);
            } catch (Exception e) {
                log.error("Cannot get TagService", e);
                throw new ClientException("TagService not available");
            }
        }
        return tagService;
    }

    public boolean isEnabled() throws ClientException {
        return getLocalTagService().isEnabled();
    }

    public void tag(CoreSession session, String docId, String tagLabel,
            String username) throws ClientException {
        getLocalTagService().tag(session, docId, tagLabel, username);
    }

    public void untag(CoreSession session, String docId, String tagLabel,
            String username) throws ClientException {
        getLocalTagService().untag(session, docId, tagLabel, username);
    }

    public List<Tag> getDocumentTags(CoreSession session, String docId,
            String username) throws ClientException {
        return getLocalTagService().getDocumentTags(session, docId, username);
    }

    public List<String> getTagDocumentIds(CoreSession session, String label,
            String username) throws ClientException {
        return getLocalTagService().getTagDocumentIds(session, label, username);
    }

    public List<Tag> getTagCloud(CoreSession session, String docId,
            String username, Boolean normalize) throws ClientException {
        return getLocalTagService().getTagCloud(session, docId, username,
                normalize);
    }

    public List<Tag> getSuggestions(CoreSession session, String label,
            String username) throws ClientException {
        return getLocalTagService().getSuggestions(session, label, username);
    }

}
