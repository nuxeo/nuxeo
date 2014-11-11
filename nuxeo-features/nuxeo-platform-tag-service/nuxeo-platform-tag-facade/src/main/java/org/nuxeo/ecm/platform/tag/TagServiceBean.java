/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.ecm.platform.tag;

import java.util.List;

import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.runtime.api.Framework;

/**
 * Stateless bean allowing to query the tags.
 *
 * @author rux
 *
 */

@Stateless
@Local(TagServiceLocal.class)
@Remote(TagServiceRemote.class)
public class TagServiceBean implements TagService {

    private static final Log log = LogFactory.getLog(TagServiceBean.class);

    protected TagService tagService;

    @PersistenceContext(unitName="nxtags")
    protected EntityManager em;


    protected TagServiceImpl getLocalTagService() throws ClientException {
        if (tagService == null) {
            try {
                tagService = (TagService) Framework.getRuntime().getComponent(
                        TagService.ID);
            } catch (Exception e) {
                log.error("Problems retrieveing the TagService ... ", e);
            }
        }
        if (null == tagService) {
            throw new ClientException("Tag Service not available");
        }
        return (TagServiceImpl)tagService;
    }

    public DocumentModel getOrCreateTag(DocumentModel parent, String label,
            boolean privateFlag) throws ClientException {
        return getLocalTagService().getOrCreateTag(em,parent, label, privateFlag);
    }

    public List<WeightedTag> getPopularCloud(DocumentModel document)
            throws ClientException {
        return getLocalTagService().getPopularCloud(em,document);
    }

    public WeightedTag getPopularTag(DocumentModel document, String tagId)
            throws ClientException {
        return getLocalTagService().getPopularTag(em,document, tagId);
    }

    public DocumentModel getRootTag(CoreSession session) throws ClientException {
        return getLocalTagService().getRootTag(em,session);
    }

    public List<WeightedTag> getVoteCloud(DocumentModel document)
            throws ClientException {
        return getLocalTagService().getVoteCloud(em,document);
    }

    public WeightedTag getVoteTag(DocumentModel document, String tagId)
            throws ClientException {
        return getLocalTagService().getVoteTag(em,document, tagId);
    }

    public List<Tag> listTagsAppliedOnDocument(DocumentModel document)
            throws ClientException {
        return getLocalTagService().listTagsAppliedOnDocument(em,document);
    }

    public List<Tag> listTagsAppliedOnDocumentByUser(DocumentModel document)
            throws ClientException {
        return getLocalTagService().listTagsAppliedOnDocumentByUser(em,document);
    }

    public DocumentModelList listTagsInGroup(DocumentModel tag)
            throws ClientException {
        return getLocalTagService().listTagsInGroup(em,tag);
    }

    public void tagDocument(DocumentModel document, String tagId,
            boolean privateFlag) throws ClientException {
        getLocalTagService().tagDocument(em,document, tagId, privateFlag);
    }

    public void untagDocument(DocumentModel document, String tagId)
            throws ClientException {
        getLocalTagService().untagDocument(em,document, tagId);
    }

    public List<String> listDocumentsForTag(String tagId, String user)
            throws ClientException {
        return getLocalTagService().listDocumentsForTag(em,tagId, user);
    }

    public String getTaggingId(String docId, String tagLabel, String author)
            throws ClientException {
        return getLocalTagService().getTaggingId(em,docId, tagLabel, author);
    }

    public void completeUntagDocument(DocumentModel document, String tagId)
            throws ClientException {
        getLocalTagService().completeUntagDocument(em,document, tagId);
    }

}
