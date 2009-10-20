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

import javax.persistence.EntityManager;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.persistence.HibernateConfiguration;
import org.nuxeo.ecm.core.persistence.HibernateConfigurator;
import org.nuxeo.ecm.core.persistence.PersistenceProvider;
import org.nuxeo.ecm.core.persistence.PersistenceProviderFactory;
import org.nuxeo.ecm.core.persistence.PersistenceProvider.RunCallback;
import org.nuxeo.ecm.core.persistence.PersistenceProvider.RunVoid;
import org.nuxeo.ecm.platform.tag.entity.DublincoreEntity;
import org.nuxeo.ecm.platform.tag.entity.TagEntity;
import org.nuxeo.ecm.platform.tag.entity.TaggingEntity;
import org.nuxeo.ecm.platform.tag.persistence.TagSchemaUpdater;
import org.nuxeo.ecm.platform.tag.persistence.TaggingProvider;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;

/**
 * The implementation of tag service. For the API see {@link #TagService}
 *
 * @author rux
 */
public class TagServiceImpl extends DefaultComponent implements TagService,
        TagConfigurator {

    private static final Log log = LogFactory.getLog(TagServiceImpl.class);

    private static Boolean enabled = null;

    @Override
    public void activate(ComponentContext context) throws Exception {
        enabled = null;
        context.getRuntimeContext().getBundle().getBundleContext().addFrameworkListener(
                new FrameworkListener() {
                    public void frameworkEvent(FrameworkEvent event) {
                        if (event.getType() != FrameworkEvent.STARTED) {
                            return;
                        }

                        ClassLoader jbossCL = Thread.currentThread().getContextClassLoader();
                        ClassLoader nuxeoCL = TagServiceImpl.class.getClassLoader();
                        try {
                            // needs to be in Nuxeo ClassLoader to do a Login !
                            Thread.currentThread().setContextClassLoader(nuxeoCL);
                            if (isEnabled()) {
                                updateSchema();
                            }
                        }
                        finally {
                            Thread.currentThread().setContextClassLoader(jbossCL);
                            log.debug("JBoss ClassLoader restored");
                        }
                    }
                });

        TagServiceInitializer tagServiceInitializer = new TagServiceInitializer();
        tagServiceInitializer.install();
    }

    protected void checkEnable() {
        LoginContext lc = null;
        try {
            lc = Framework.login();
            RepositoryManager rm = Framework.getService(RepositoryManager.class);
            if (rm.getDefaultRepository().supportsTags()) {
                log.info("Activating tag service");
                enabled = true;
            } else {
                enabled = false;
                log.info("Default repository does not support Tag feature : Tag service won't be available.");
            }
        } catch (Exception e) {
            enabled = false;
            log.error("Unable to test repository for Tag feature.", e);
        } finally {
            if (lc!=null) {
                try {
                    lc.logout();
                } catch (LoginException e) {
                    log.error("Error durint Framework.logout", e);
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        deactivatePersistenceProvider();
        super.deactivate(context);
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if ("configs".equals(extensionPoint)) {
            setConfig((TagConfig) contribution);
        }
    }

    protected TagConfig config = new TagConfig();

    public void setConfig(TagConfig config) {
        this.config = config;
    }

    public TagConfig getConfig() {
        return config;
    }

    protected PersistenceProvider persistenceProvider;

    public PersistenceProvider getOrCreatePersistenceProvider() {
        if (persistenceProvider != null) {
            return persistenceProvider;
        }
        PersistenceProviderFactory persistenceProviderFactory = Framework.getLocalService(PersistenceProviderFactory.class);
        persistenceProvider = persistenceProviderFactory.newProvider("nxtags");
        return persistenceProvider;
    }

    protected void deactivatePersistenceProvider() {
        if (persistenceProvider == null) {
            return;
        }
        persistenceProvider.closePersistenceUnit();
        persistenceProvider = null;
    }

    public DocumentModel getRootTag(final CoreSession session)
            throws ClientException {
        return getOrCreatePersistenceProvider().run(true,
                new RunCallback<DocumentModel>() {
                    public DocumentModel runWith(EntityManager em)
                            throws ClientException {
                        return getRootTag(em, session);
                    }
                });
    }

    public DocumentModel getRootTag(EntityManager em, CoreSession session)
            throws ClientException {
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

    public void tagDocument(final CoreSession session,
            final DocumentModel document, final String tagId,
            final boolean privateFlag) throws ClientException {
        getOrCreatePersistenceProvider().run(true, new RunVoid() {
            public void runWith(EntityManager em) throws ClientException {
                tagDocument(em, session, document, tagId, privateFlag);
            }
        });
    }

    public void tagDocument(EntityManager em, CoreSession session,
            DocumentModel document, String tagId, boolean privateFlag)
            throws ClientException {
        if (null == document) {
            throw new ClientException("Can't tag document null.");
        }
        TaggingProvider provider = TaggingProvider.createProvider(em);
        TagEntity tagEntity = provider.getTagById(tagId);
        if (tagEntity == null) {
            throw new ClientException("Tag " + tagId + " doesn't exist");
        }
        if (log.isDebugEnabled()) {
            log.debug("Going to tag document " + document.getTitle() + " with "
                    + tagEntity.getLabel());
        }
        String user = getUserName(session);
        // check if already tag applied
        if (provider.existTagging(tagId, document.getId(), user)) {
            log.warn(String.format(
                    "Tag %s already applied on %s by %s, don't create it",
                    tagEntity.getLabel(), document.getTitle(), user));
            return;
        }
        TaggingEntity taggingEntry = new TaggingEntity();
        String targetDocId = document.getId();
        if (document.isProxy()) {
            targetDocId = document.getSourceId();
        }
        DublincoreEntity dc = provider.getDcById(targetDocId);
        taggingEntry.setId(IdUtils.generateStringId());
        taggingEntry.setTag(tagEntity);
        taggingEntry.setTargetDocument(dc);
        taggingEntry.setCreationDate(new Date());
        taggingEntry.setAuthor(user);
        taggingEntry.setIsPrivate(privateFlag ? 1 : 0);
        TaggingProvider.createProvider(em).addTagging(taggingEntry);
    }

    protected static String getUserName(CoreSession session)
            throws ClientException {
        if (session == null) {
            throw new ClientException("No session available");
        }
        return session.getPrincipal().getName();
    }

    public DocumentModel getOrCreateTag(final CoreSession session,
            final DocumentModel parent, final String label,
            final boolean privateFlag) throws ClientException {
        return getOrCreatePersistenceProvider().run(true,
                new RunCallback<DocumentModel>() {
                    public DocumentModel runWith(EntityManager em)
                            throws ClientException {
                        return getOrCreateTag(em, session, parent, label,
                                privateFlag);
                    }
                });
    }

    public DocumentModel getOrCreateTag(EntityManager em, CoreSession session,
            DocumentModel parent, String label, boolean privateFlag)
            throws ClientException {
        if (parent == null) {
            log.warn("Can't create tag in null parent");
            throw new ClientException("Need a parent to create a tag");
        }
        if (log.isDebugEnabled()) {
            log.debug("Going to look for label " + label);
        }
        UnrestrictedSessionCreateTag runner = new UnrestrictedSessionCreateTag(
                session, parent, label, privateFlag);
        runner.runUnrestricted();
        if (runner.tagDocument == null) {
            throw new ClientException("Error creating the tag document");
        }
        return runner.tagDocument;
    }

    public List<WeightedTag> getPopularCloud(final CoreSession session,
            final DocumentModel document) throws ClientException {
        return getOrCreatePersistenceProvider().run(false,
                new RunCallback<List<WeightedTag>>() {
                    public List<WeightedTag> runWith(EntityManager em)
                            throws ClientException {
                        return getPopularCloud(em, session, document);
                    }
                });
    }

    public List<WeightedTag> getPopularCloud(EntityManager em,
            CoreSession session, DocumentModel document) throws ClientException {
        // the NXSQL queries can't be used together with the native queries, for
        // moment the queries are performed sequentially
        if (null == document) {
            throw new ClientException("Can't get cloud for domain null.");
        }
        if (log.isDebugEnabled()) {
            log.debug("Going for popular cloud for " + document.getTitle());
        }
        String query = String.format(
                "SELECT * FROM Document WHERE ecm:path STARTSWITH '%s' AND ecm:isProxy = %d",
                document.getPathAsString(), config.isQueryingForProxy() ? 1 : 0);
        UnrestrictedSessionRunQuery runner = new UnrestrictedSessionRunQuery(
                session, query);
        runner.runUnrestricted();
        // add the current doc also
        runner.result.add(document);
        return TaggingProvider.createProvider(em).getPopularCloud(
                runner.result, getUserName(session));
    }

    public List<WeightedTag> getPopularCloudOnAllDocuments(final CoreSession session) throws ClientException {
         return getOrCreatePersistenceProvider().run(false,
                 new RunCallback<List<WeightedTag>>() {
                     public List<WeightedTag> runWith(EntityManager em)
                             throws ClientException {
                         return getPopularCloudOnAllDocuments(em, session);
                     }
                 });
    }


    public List<WeightedTag> getPopularCloudOnAllDocuments(EntityManager em,
            CoreSession session) throws ClientException {
        return TaggingProvider.createProvider(em).getPopularCloudOnAllDocuments(getUserName(session));
    }

    public WeightedTag getPopularTag(final CoreSession session,
            final DocumentModel document, final String tagId)
            throws ClientException {
        return getOrCreatePersistenceProvider().run(false,
                new RunCallback<WeightedTag>() {
                    public WeightedTag runWith(EntityManager em)
                            throws ClientException {
                        return getPopularTag(em, session, document, tagId);
                    }
                });
    }

    public WeightedTag getPopularTag(EntityManager em, CoreSession session,
            DocumentModel document, String tagId) throws ClientException {
        if (null == document || null == tagId) {
            throw new ClientException(
                    "Can't get popular for document or tag null.");
        }
        String user = getUserName(session);
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

    public List<WeightedTag> getVoteCloud(final CoreSession session,
            final DocumentModel document) throws ClientException {
        return getOrCreatePersistenceProvider().run(false,
                new RunCallback<List<WeightedTag>>() {
                    public List<WeightedTag> runWith(EntityManager em)
                            throws ClientException {
                        return getVoteCloud(em, session, document);
                    }
                });
    }

    public List<WeightedTag> getVoteCloud(EntityManager em,
            CoreSession session, DocumentModel document) throws ClientException {
        throw new UnsupportedOperationException();
    }

    public WeightedTag getVoteTag(final CoreSession session,
            final DocumentModel document, final String tagId)
            throws ClientException {
        return getOrCreatePersistenceProvider().run(false,
                new RunCallback<WeightedTag>() {
                    public WeightedTag runWith(EntityManager em)
                            throws ClientException {
                        return getVoteTag(em, session, document, tagId);
                    }
                });
    }

    public WeightedTag getVoteTag(EntityManager em, CoreSession session,
            DocumentModel document, String tagId) throws ClientException {
        if (null == document) {
            throw new ClientException(
                    "Can't get list of documents from domain null.");
        }
        String user = getUserName(session);
        DocumentModel tag = session.getDocument(new IdRef(tagId));
        if (log.isDebugEnabled()) {
            log.debug("Going to look for votes of " + tag.getTitle() + " for "
                    + document.getTitle());
        }
        if (!isTagAllowed(tag, user)) {
            log.warn("Tag " + tag.getTitle() + " not allowed for " + user);
            return new WeightedTag(tag.getId(), tag.getTitle(), 0);
        }
        TaggingProvider taggingProvider = TaggingProvider.createProvider(em);
        Long result = taggingProvider.getVoteTag(document.getId(), tag.getId(),
                user);
        return new WeightedTag(tag.getId(),
                (String) tag.getPropertyValue(TagConstants.TAG_LABEL_FIELD),
                result.intValue());
    }

    public List<Tag> listTagsAppliedOnDocument(final CoreSession session,
            final DocumentModel document) throws ClientException {
        return getOrCreatePersistenceProvider().run(false,
                new RunCallback<List<Tag>>() {
                    public List<Tag> runWith(EntityManager em)
                            throws ClientException {
                        return listTagsAppliedOnDocument(em, session, document);
                    }
                });
    }

    public List<Tag> listTagsAppliedOnDocument(EntityManager em,
            CoreSession session, DocumentModel document) throws ClientException {
        if (null == document) {
            throw new ClientException("Can't get list of tags from group null.");
        }
        if (log.isDebugEnabled()) {
            log.debug("Going to look for tags applied on "
                    + document.getTitle());
        }
        return TaggingProvider.createProvider(em).listTagsForDocument(
                document.getId(), getUserName(session));
    }

    public List<Tag> listTagsAppliedOnDocumentByUser(final CoreSession session,
            final DocumentModel document) throws ClientException {
        return getOrCreatePersistenceProvider().run(false,
                new RunCallback<List<Tag>>() {
                    public List<Tag> runWith(EntityManager em)
                            throws ClientException {
                        return listTagsAppliedOnDocumentByUser(em, session,
                                document);
                    }
                });
    }

    public List<Tag> listTagsAppliedOnDocumentByUser(EntityManager em,
            CoreSession session, DocumentModel document) throws ClientException {
        if (null == document) {
            throw new ClientException("Can't get list of tags from group null.");
        }
        if (log.isDebugEnabled()) {
            log.debug("Going to look only current user tags applied on "
                    + document.getTitle());
        }
        return TaggingProvider.createProvider(em).listTagsForDocumentAndUser(
                document.getId(), getUserName(session));
    }

    public DocumentModelList listTagsInGroup(final CoreSession session,
            final DocumentModel tag) throws ClientException {
        return getOrCreatePersistenceProvider().run(false,
                new RunCallback<DocumentModelList>() {
                    public DocumentModelList runWith(EntityManager em)
                            throws ClientException {
                        return listTagsInGroup(em, session, tag);
                    }
                });
    }

    public DocumentModelList listTagsInGroup(final DocumentModel tag)
            throws ClientException {
        return listTagsInGroup(tag.getCoreSession(), tag);
    }

    public DocumentModelList listTagsInGroup(EntityManager em,
            CoreSession session, DocumentModel tag) throws ClientException {
        if (null == tag) {
            throw new ClientException("Can't get list of tags from group null.");
        }
        if (log.isDebugEnabled()) {
            log.debug("Going to list tags in " + tag.getTitle());
        }
        String user = getUserName(session);
        String query = String.format(
                "SELECT * FROM Tag WHERE ecm:path STARTSWITH '%s' AND (tag:private = 0 or dc:creator = '%s') AND ecm:isProxy = %d",
                tag.getPathAsString(), user, config.isQueryingForProxy() ? 1
                        : 0);
        UnrestrictedSessionRunQuery runner = new UnrestrictedSessionRunQuery(
                session, query);
        runner.runUnrestricted();
        return runner.result;
    }

    public void untagDocument(final CoreSession session,
            final DocumentModel document, final String tagId)
            throws ClientException {
        getOrCreatePersistenceProvider().run(true, new RunVoid() {
            public void runWith(EntityManager em) throws ClientException {
                untagDocument(em, session, document, tagId);
            }
        });
    }

    public void untagDocument(EntityManager em, CoreSession session,
            DocumentModel document, String tagId) throws ClientException {
        if (null == document || tagId == null) {
            throw new ClientException("Can't untag document or tag null.");
        }
        if (log.isDebugEnabled()) {
            log.debug("Going to untag " + tagId + " for " + document.getTitle());
        }
        TaggingProvider.createProvider(em).removeTagging(document.getId(),
                tagId, getUserName(session));
    }

    public void completeUntagDocument(final CoreSession session,
            final DocumentModel document, final String tagId)
            throws ClientException {
        getOrCreatePersistenceProvider().run(true, new RunVoid() {
            public void runWith(EntityManager em) throws ClientException {
                completeUntagDocument(em, session, document, tagId);
            }
        });
    }

    public void completeUntagDocument(EntityManager em, CoreSession session,
            DocumentModel document, String tagId) throws ClientException {
        if (null == document || tagId == null) {
            throw new ClientException("Can't untag document or tag null.");
        }
        if (log.isDebugEnabled()) {
            log.debug("Going to untag " + tagId + " for " + document.getTitle());
        }
        TaggingProvider.createProvider(em).removeAllTagging(document.getId(),
                tagId);
    }

    public List<String> listDocumentsForTag(final CoreSession session,
            final String tagId, final String user) throws ClientException {
        return getOrCreatePersistenceProvider().run(true,
                new RunCallback<List<String>>() {
                    public List<String> runWith(EntityManager em)
                            throws ClientException {
                        return listDocumentsForTag(em, session, tagId, user);
                    }
                });
    }

    public List<String> listDocumentsForTag(EntityManager em,
            CoreSession session, String tagId, String user)
            throws ClientException {
        if (null == tagId) {
            throw new ClientException(
                    "Can't get list of documents for tag null.");
        }
        return TaggingProvider.createProvider(em).getDocumentsForTag(tagId,
                user);
    }

    /**
     * Checks if the tag is allowed to be used by the user.
     */
    protected static boolean isTagAllowed(DocumentModel tag, String user)
            throws ClientException {
        if (tag == null) {
            throw new ClientException(
                    "Can't get list of documents from tag null.");
        }
        Long isPrivate = (Long) tag.getPropertyValue(TagConstants.TAG_IS_PRIVATE_FIELD);
        if (isPrivate == null || isPrivate == 0) {
            return true;
        }
        String owner = (String) tag.getPropertyValue("dc:creator");
        return user != null && user.equals(owner);
    }

    /**
     * The unrestricted session runner to find / create root tag document.
     *
     * @author rux
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
        }
    }

    /**
     * The unrestricted runner to find / create a tag document.
     *
     * @author rux
     */
    protected class UnrestrictedSessionCreateTag extends
            UnrestrictedSessionRunner {

        // need to return somehow the result
        public DocumentModel tagDocument;

        // and to store the arguments
        private final DocumentModel parent;

        private final String label;

        private final String user;

        private final boolean privateFlag;

        public UnrestrictedSessionCreateTag(CoreSession session,
                DocumentModel parent, String label, boolean privateFlag)
                throws ClientException {
            super(session);
            this.parent = parent;
            tagDocument = null;
            this.label = label;
            user = TagServiceImpl.getUserName(session);
            this.privateFlag = privateFlag;
        }

        @Override
        public void run() throws ClientException {
            // label can be in fact a composed label: labels separated by /
            String[] labels = label.split("/");
            DocumentModel relativeParent = parent;
            for (String atomicLabel : labels) {
                // for each label look for a public or user owned tag. If not,
                // create it
                String query = String.format(
                        "SELECT * FROM Tag WHERE ecm:parentId = '%s' AND tag:label = '%s' AND ecm:isProxy = %d",
                        relativeParent.getId(), atomicLabel,
                        config.queryProxy ? 1 : 0);
                DocumentModelList tags = session.query(query);

                DocumentModel foundTag = null;
                if (tags != null && tags.size() > 0) {
                    // it should be only one, but it is possible to have more
                    // than one tag with the specified label in a group. Need to
                    // check the flag / user
                    for (DocumentModel aTag : tags) {
                        Long isPrivate = (Long) aTag.getPropertyValue(TagConstants.TAG_IS_PRIVATE_FIELD);
                        if (isPrivate == null || isPrivate == 0) {
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
            tagDocument = relativeParent;
            ((DocumentModelImpl) tagDocument).detach(true);
        }
    }

    /**
     * The unrestricted runner for running a query.
     *
     * @author rux
     */
    protected static class UnrestrictedSessionRunQuery extends
            UnrestrictedSessionRunner {

        // need to have somehow result
        public DocumentModelList result;

        // need to provide somehow the arguments
        private final String query;

        public UnrestrictedSessionRunQuery(CoreSession session, String query) {
            super(session);
            this.query = query;
            result = null;
        }

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
                privateFlag?1:0);
        tagDocument = session.saveDocument(tagDocument);
        session.save();
        return tagDocument;
    }

    public String getTaggingId(final CoreSession session, final String docId,
            final String tagLabel, final String author) throws ClientException {
        return getOrCreatePersistenceProvider().run(false,
                new RunCallback<String>() {
                    public String runWith(EntityManager em)
                            throws ClientException {
                        return getTaggingId(em, session, docId, tagLabel,
                                author);
                    }
                });
    }

    public String getTaggingId(EntityManager em, CoreSession session,
            String docId, String tagLabel, String author) {
        return TaggingProvider.createProvider(em).getTaggingId(docId, tagLabel,
                author);
    }

    public void updateSchema() {
        HibernateConfigurator configurator = Framework.getLocalService(HibernateConfigurator.class);
        HibernateConfiguration configuration = configurator.getHibernateConfiguration("nxtags");
        TagSchemaUpdater updater = new TagSchemaUpdater(
                configuration.hibernateProperties);
        updater.update();
    }

    public boolean isEnabled() {
        if (enabled==null) {
            checkEnable();
        }
        return enabled;
    }

}
