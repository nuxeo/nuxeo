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

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.persistence.EntityManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.platform.tag.entity.DublincoreEntity;
import org.nuxeo.ecm.platform.tag.entity.TagEntity;
import org.nuxeo.ecm.platform.tag.entity.TaggingEntity;
import org.nuxeo.ecm.platform.tag.persistence.TagPersistenceProvider;
import org.nuxeo.ecm.platform.tag.persistence.TaggingProvider;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * The implementation of tag service. For the API see {@link #TagService()}
 *
 * @author rux
 */
public class TagServiceImpl extends DefaultComponent implements TagService {

    private TaggingProvider taggingProvider = null;

    private static final Log log = LogFactory.getLog(TagServiceImpl.class);

    public void initialize(Properties properties) throws ClientException {
        if (null != taggingProvider) {
            log.warn("Call for intializing the service is too late");
            return;
        }
        getTaggingProvider(properties);
    }

    public DocumentModel getRootTag(CoreSession session) throws ClientException {
        if (log.isDebugEnabled()) {
            log.debug("Going to look for root tag");
        }
        // use unrestricted session to get / create RootTag
        UnrestrictedSessionCreateRootTag runner = new UnrestrictedSessionCreateRootTag(
                session);
        runner.runUnrestricted();
        if (runner.rootTagDocumentId == null) {
            throw new ClientException("Error creating the root tag document");
        }
        return session.getDocument(new IdRef(runner.rootTagDocumentId));
    }

    public void tagDocument(DocumentModel document, String tagId,
            boolean privateFlag) throws ClientException {
        if (null == document) {
            throw new ClientException("Can't tag document null.");
        }
        TaggingProvider provider = getTaggingProvider();
        TagEntity tagEntity = provider.getTagById(tagId);
        if (tagEntity == null) {
            throw new ClientException("Tag " + tagId + " doesn't exist");
        }
        if (log.isDebugEnabled()) {
            log.debug("Going to tag document " + document.getTitle() + " with "
                    + tagEntity.getLabel());
        }
        String user = obtainCurrentPrincipalName(document);
        // check if already tag applied
        if (provider.existTagging(tagId, document.getId(), user)) {
            log.warn(String.format(
                    "Tag %s already applied on %s by %s, don't create it",
                    tagEntity.getLabel(), document.getTitle(), user));
            return;
        }
        TaggingEntity taggingEntry = new TaggingEntity();
        DublincoreEntity dc = provider.getDcById(document.getId());
        taggingEntry.setId(IdUtils.generateStringId());
        taggingEntry.setTag(tagEntity);
        taggingEntry.setTargetDocument(dc);
        taggingEntry.setCreationDate(new Date());
        taggingEntry.setAuthor(user);
        taggingEntry.setIsPrivate(Boolean.valueOf(privateFlag));
        getTaggingProvider().addTagging(taggingEntry);
    }

    public DocumentModel getOrCreateTag(DocumentModel parent, String label,
            boolean privateFlag) throws ClientException {
        if (parent == null) {
            log.warn("Can't create tag in null parent");
            throw new ClientException("Need a parent to create a tag");
        }
        if (log.isDebugEnabled()) {
            log.debug("Going to look for label " + label);
        }
        UnrestrictedSessionCreateTag runner = new UnrestrictedSessionCreateTag(
                parent, label, privateFlag);
        runner.runUnrestricted();
        if (runner.tagDocumentId == null) {
            throw new ClientException("Error creating the tag document");
        }
        return parent.getCoreSession().getDocument(
                new IdRef(runner.tagDocumentId));
    }

    public List<WeightedTag> getPopularCloud(DocumentModel document)
            throws ClientException {
        // the NXSQL queries can't be used together with the native queries, for
        // moment the queries are performed sequentially
        if (null == document) {
            throw new ClientException("Can't get cloud for domain null.");
        }
        if (log.isDebugEnabled()) {
            log.debug("Going for popular cloud for " + document.getTitle());
        }
        String query = String.format(
                TagConstants.DOCUMENTS_IN_DOMAIN_QUERY_TEMPLATE,
                document.getPathAsString());
        UnrestrictedSessionRunQuery runner = new UnrestrictedSessionRunQuery(
                document.getCoreSession(), query);
        runner.runUnrestricted();
        // add the current doc also
        runner.result.add(document);
        return getTaggingProvider().getPopularCloud(runner.result,
                obtainCurrentPrincipalName(document));
    }

    public WeightedTag getPopularTag(DocumentModel document, String tagId)
            throws ClientException {
        if (null == document || null == tagId) {
            throw new ClientException(
                    "Can't get popular for document or tag null.");
        }
        CoreSession session = document.getCoreSession();
        String user = obtainCurrentPrincipalName(document);
        DocumentModel tag = session.getDocument(new IdRef(tagId));
        if (log.isDebugEnabled()) {
            log.debug("Going to look for popularity of " + tag.getTitle()
                    + " for " + document.getTitle());
        }
        if (!isTagAllowed(tag, user)) {
            log.warn("Tag " + tag.getTitle() + " not allowed for " + user);
            return new WeightedTag(tag.getId(), tag.getTitle(), 0);
        }
        // TODO int weight =
        // getTaggingProvider().getPopularTag(document.getId(), tag.getId(),
        // user);
        return new WeightedTag(tag.getId(),
                (String) tag.getPropertyValue(TagConstants.TAG_LABEL_FIELD), 0);
    }

    public List<WeightedTag> getVoteCloud(DocumentModel document)
            throws ClientException {
        // TODO
        return null;
    }

    public WeightedTag getVoteTag(DocumentModel document, String tagId)
            throws ClientException {
        if (null == document) {
            throw new ClientException(
                    "Can't get list of documents from domain null.");
        }
        CoreSession session = document.getCoreSession();
        String user = obtainCurrentPrincipalName(document);
        DocumentModel tag = session.getDocument(new IdRef(tagId));
        if (log.isDebugEnabled()) {
            log.debug("Going to look for votes of " + tag.getTitle() + " for "
                    + document.getTitle());
        }
        if (!isTagAllowed(tag, user)) {
            log.warn("Tag " + tag.getTitle() + " not allowed for " + user);
            return new WeightedTag(tag.getId(), tag.getTitle(), 0);
        }
        TaggingProvider taggingProvider = getTaggingProvider();
        Long result = taggingProvider.getVoteTag(document.getId(), tag.getId(),
                user);
        return new WeightedTag(tag.getId(),
                (String) tag.getPropertyValue(TagConstants.TAG_LABEL_FIELD),
                result.intValue());
    }

    public List<Tag> listTagsAppliedOnDocument(DocumentModel document)
            throws ClientException {
        if (null == document) {
            throw new ClientException("Can't get list of tags from group null.");
        }
        if (log.isDebugEnabled()) {
            log.debug("Going to look for tags applied on "
                    + document.getTitle());
        }
        return getTaggingProvider().listTagsForDocument(document.getId(),
                obtainCurrentPrincipalName(document));
    }

    public List<Tag> listTagsAppliedOnDocumentByUser(DocumentModel document)
            throws ClientException {
        if (null == document) {
            throw new ClientException("Can't get list of tags from group null.");
        }
        if (log.isDebugEnabled()) {
            log.debug("Going to look only current user tags applied on "
                    + document.getTitle());
        }
        return getTaggingProvider().listTagsForDocumentAndUser(
                document.getId(), obtainCurrentPrincipalName(document));
    }

    public DocumentModelList listTagsInGroup(DocumentModel tag)
            throws ClientException {
        if (null == tag) {
            throw new ClientException("Can't get list of tags from group null.");
        }
        if (log.isDebugEnabled()) {
            log.debug("Going to list tags in " + tag.getTitle());
        }
        String user = obtainCurrentPrincipalName(tag);
        String query = String.format(
                TagConstants.TAGS_IN_DOMAIN_QUERY_TEMPLATE,
                tag.getPathAsString(), user);
        UnrestrictedSessionRunQuery runner = new UnrestrictedSessionRunQuery(
                tag.getCoreSession(), query);
        runner.runUnrestricted();
        return runner.result;
    }

    public void untagDocument(DocumentModel document, String tagId)
            throws ClientException {
        if (null == document || tagId == null) {
            throw new ClientException("Can't untag document or tag null.");
        }
        if (log.isDebugEnabled()) {
            log.debug("Going to untag " + tagId + " for " + document.getTitle());
        }
        getTaggingProvider().removeTagging(document.getId(), tagId,
                obtainCurrentPrincipalName(document));
    }

    public void completeUntagDocument(DocumentModel document, String tagId)
            throws ClientException {
        if (null == document || tagId == null) {
            throw new ClientException("Can't untag document or tag null.");
        }
        if (log.isDebugEnabled()) {
            log.debug("Going to untag " + tagId + " for " + document.getTitle());
        }
        getTaggingProvider().removeAllTagging(document.getId(), tagId);
    }

    public List<String> listDocumentsForTag(String tagId, String user)
            throws ClientException {
        if (null == tagId) {
            throw new ClientException(
                    "Can't get list of documents for tag null.");
        }
        return getTaggingProvider().getDocumentsForTag(tagId, user);
    }

    protected TaggingProvider getTaggingProvider() {
        if (null == taggingProvider) {
            EntityManager entityManager = TagPersistenceProvider.getInstance().getEntityManager(
                    null);
            taggingProvider = TaggingProvider.createProvider(entityManager);
        }
        return taggingProvider;
    }

    protected TaggingProvider getTaggingProvider(Properties properties) {
        if (null == taggingProvider) {
            EntityManager entityManager = TagPersistenceProvider.getInstance().getEntityManager(
                    properties);
            taggingProvider = TaggingProvider.createProvider(entityManager);
        }
        return taggingProvider;
    }

    /**
     * Checks if the tag is allowed to be used be the user
     *
     * @param tag
     * @param user
     * @return
     * @throws ClientException
     */
    protected static boolean isTagAllowed(DocumentModel tag, String user)
            throws ClientException {
        if (tag == null) {
            throw new ClientException(
                    "Can't get list of documents from tag null.");
        }
        Boolean isPrivate = (Boolean) tag.getPropertyValue(TagConstants.TAG_IS_PRIVATE_FIELD);
        if (isPrivate == null || !isPrivate) {
            return true;
        }
        String owner = (String) tag.getPropertyValue("dc:creator");
        if (user != null && user.equals(owner)) {
            return true;
        }
        return false;
    }

    protected static String obtainCurrentPrincipalName(DocumentModel document) {
        return document.getCoreSession().getPrincipal().getName();
    }

    /**
     * The unrestricted session runner to find / create root tag document.
     *
     * @author rux
     *
     */
    protected static class UnrestrictedSessionCreateRootTag extends
            UnrestrictedSessionRunner {
        public UnrestrictedSessionCreateRootTag(CoreSession session) {
            super(session);
            rootTagDocumentId = null;
        }

        // need to return somehow the result
        public String rootTagDocumentId;

        @Override
        public void run() throws ClientException {
            DocumentModel documentRoot = session.getRootDocument();
            DocumentModelList rootHiddenChildren = session.getChildren(
                    documentRoot.getRef(), TagConstants.HIDDEN_FOLDER_TYPE);
            if (null != rootHiddenChildren) {
                for (DocumentModel hiddenFolder : rootHiddenChildren) {
                    if (TagConstants.TAGS_DIRECTORY.equals(hiddenFolder.getTitle())) {
                        rootTagDocumentId = hiddenFolder.getId();
                        return;
                    }
                }
            }
            log.debug("Creating the RootTag holder");
            DocumentModel rootTag = session.createDocumentModel(
                    documentRoot.getPathAsString(),
                    IdUtils.generateId(TagConstants.TAGS_DIRECTORY),
                    TagConstants.HIDDEN_FOLDER_TYPE);
            rootTag.setPropertyValue("dc:title", TagConstants.TAGS_DIRECTORY);
            rootTag.setPropertyValue("dc:description", "");
            rootTag.setPropertyValue("dc:created", Calendar.getInstance());
            rootTag = session.createDocument(rootTag);
            rootTag = session.saveDocument(rootTag);
            session.save();
            // and set ID for retrieval
            rootTagDocumentId = rootTag.getId();
        }
    }

    /**
     * The unrestricted runner to find / create a tag document.
     *
     * @author rux
     *
     */
    protected static class UnrestrictedSessionCreateTag extends
            UnrestrictedSessionRunner {
        public UnrestrictedSessionCreateTag(DocumentModel parent, String label,
                boolean privateFlag) {
            super(parent.getCoreSession());
            this.parent = parent;
            tagDocumentId = null;
            this.label = label;
            user = parent.getCoreSession().getPrincipal().getName();
            this.privateFlag = privateFlag;
        }

        // need to return somehow the result
        public String tagDocumentId;

        // and to store the arguments
        private DocumentModel parent;

        private String label;

        private String user;

        private boolean privateFlag;

        @Override
        public void run() throws ClientException {
            // label can be in fact a composed label: labels separated by /
            String[] labels = label.split("/");
            DocumentModel relativeParent = parent;
            for (String atomicLabel : labels) {
                // for each label look for a public or user owned tag. If not,
                // create it
                String query = String.format(
                        "SELECT * FROM Tag WHERE ecm:parentId = '%s' AND tag:label = '%s'",
                        relativeParent.getId(), atomicLabel);
                DocumentModelList tags = session.query(query);

                DocumentModel foundTag = null;
                if (tags != null && tags.size() > 0) {
                    // it should be only one, but it is possible to have more
                    // than one tag
                    // with the specified label in a group. Need to check the
                    // flag / user
                    for (DocumentModel aTag : tags) {
                        Boolean isPrivate = (Boolean) aTag.getPropertyValue(TagConstants.TAG_IS_PRIVATE_FIELD);
                        if (isPrivate != null && !isPrivate) {
                            // public tag, should be ok
                            foundTag = aTag;
                            break;
                        }
                        String tagUser = (String) aTag.getPropertyValue("dc:creator");
                        if (user.equals(tagUser)) {
                            // it pertains to this user
                            foundTag = aTag;
                            break;
                        }
                    }
                }
                if (foundTag != null) {
                    relativeParent = foundTag;
                } else {
                    // couldn't find the tag, create it
                    relativeParent = createTagModel(session, relativeParent,
                            atomicLabel, user, privateFlag);
                }
            }
            // and set ID for retrieval
            tagDocumentId = relativeParent.getId();
        }
    }

    /**
     * The unrestricted runner for running a query.
     *
     * @author rux
     *
     */
    protected static class UnrestrictedSessionRunQuery extends
            UnrestrictedSessionRunner {

        public UnrestrictedSessionRunQuery(CoreSession session, String query) {
            super(session);
            this.query = query;
            result = null;
        }

        // need to have somehow result
        public DocumentModelList result;

        // need to provide somehow the arguments
        private String query;

        @Override
        public void run() throws ClientException {
            result = session.query(query);
        }

    }

    protected static DocumentModel createTagModel(CoreSession session,
            DocumentModel parent, String label, String user, boolean privateFlag)
            throws ClientException {
        DocumentModel tagDocument = session.createDocumentModel(
                parent.getPathAsString(), IdUtils.generateId(label), "Tag");
        tagDocument = session.createDocument(tagDocument);
        tagDocument.setPropertyValue("dc:title", label);
        tagDocument.setPropertyValue("dc:description", "");
        tagDocument.setPropertyValue("dc:created", Calendar.getInstance());
        tagDocument.setPropertyValue("dc:creator", user);
        tagDocument.setPropertyValue(TagConstants.TAG_LABEL_FIELD, label);
        tagDocument.setPropertyValue(TagConstants.TAG_IS_PRIVATE_FIELD,
                privateFlag);
        tagDocument = session.saveDocument(tagDocument);
        session.save();
        return tagDocument;
    }

    public String getTaggingId(String docId, String tagLabel, String author) {
        return getTaggingProvider().getTaggingId(docId, tagLabel, author);
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        TagPersistenceProvider.getInstance().closePersistenceUnit();
    }
}
