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
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.platform.tag.Tag;
import org.nuxeo.ecm.platform.tag.TagService;
import org.nuxeo.ecm.platform.tag.TagServiceImpl;
import org.nuxeo.ecm.platform.tag.TagServiceLocal;
import org.nuxeo.ecm.platform.tag.TagServiceRemote;
import org.nuxeo.ecm.platform.tag.WeightedTag;
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

    private TagService tagService;

    @PostConstruct
    public void initialize() {
    }

    private TagService getLocalTagService() throws ClientException {
        if (tagService == null) {
            try {
                tagService = (TagServiceImpl) Framework.getRuntime().getComponent(
                        TagService.ID);
            } catch (Exception e) {
                log.error("Problems retrieveing the TagService ... ", e);
            }
        }
        if (null == tagService) {
            throw new ClientException("Tag Service not available");
        }
        return tagService;
    }

    public DocumentModel getOrCreateTag(DocumentModel parent, String label,
            boolean privateFlag) throws ClientException {
        return getLocalTagService().getOrCreateTag(parent, label, privateFlag);
    }

    public List<WeightedTag> getPopularCloud(DocumentModel document)
            throws ClientException {
        return getLocalTagService().getPopularCloud(document);
    }

    public WeightedTag getPopularTag(DocumentModel document, String tagId)
            throws ClientException {
        return getLocalTagService().getPopularTag(document, tagId);
    }

    public DocumentModel getRootTag(CoreSession session) throws ClientException {
        return getLocalTagService().getRootTag(session);
    }

    public List<WeightedTag> getVoteCloud(DocumentModel document)
            throws ClientException {
        return getLocalTagService().getVoteCloud(document);
    }

    public WeightedTag getVoteTag(DocumentModel document, String tagId)
            throws ClientException {
        return getLocalTagService().getVoteTag(document, tagId);
    }

    public List<Tag> listTagsAppliedOnDocument(DocumentModel document)
            throws ClientException {
        return getLocalTagService().listTagsAppliedOnDocument(document);
    }

    public List<Tag> listTagsAppliedOnDocumentByUser(DocumentModel document)
            throws ClientException {
        return getLocalTagService().listTagsAppliedOnDocumentByUser(document);
    }

    public DocumentModelList listTagsInGroup(DocumentModel tag)
            throws ClientException {
        return getLocalTagService().listTagsInGroup(tag);
    }

    public void tagDocument(DocumentModel document, String tagId,
            boolean privateFlag) throws ClientException {
        getLocalTagService().tagDocument(document, tagId, privateFlag);
    }

    public void untagDocument(DocumentModel document, String tagId)
            throws ClientException {
        getLocalTagService().untagDocument(document, tagId);
    }

    public List<String> listDocumentsForTag(String tagId, String user)
            throws ClientException {
        return getLocalTagService().listDocumentsForTag(tagId, user);
    }

    public void initialize(Properties properties) throws ClientException {
        getLocalTagService().initialize(properties);

    }

    public String getTaggingId(String docId, String tagLabel, String author)
            throws ClientException {
        return getLocalTagService().getTaggingId(docId, tagLabel, author);
    }

    public void completeUntagDocument(DocumentModel document, String tagId)
            throws ClientException {
        getLocalTagService().completeUntagDocument(document, tagId);
    }
}
