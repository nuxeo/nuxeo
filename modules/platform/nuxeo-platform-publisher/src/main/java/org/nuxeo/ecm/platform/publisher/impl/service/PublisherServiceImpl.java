/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nuxeo
 */
package org.nuxeo.ecm.platform.publisher.impl.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublicationTree;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocumentFactory;
import org.nuxeo.ecm.platform.publisher.api.PublisherService;
import org.nuxeo.ecm.platform.publisher.descriptors.PublicationTreeConfigDescriptor;
import org.nuxeo.ecm.platform.publisher.descriptors.PublicationTreeDescriptor;
import org.nuxeo.ecm.platform.publisher.descriptors.PublishedDocumentFactoryDescriptor;
import org.nuxeo.ecm.platform.publisher.descriptors.RootSectionFinderFactoryDescriptor;
import org.nuxeo.ecm.platform.publisher.helper.PublicationRelationHelper;
import org.nuxeo.ecm.platform.publisher.helper.RootSectionFinder;
import org.nuxeo.ecm.platform.publisher.helper.RootSectionFinderFactory;
import org.nuxeo.ecm.platform.publisher.impl.finder.DefaultRootSectionsFinder;
import org.nuxeo.ecm.platform.publisher.rules.ValidatorsRule;
import org.nuxeo.ecm.platform.publisher.rules.ValidatorsRuleDescriptor;
import org.nuxeo.runtime.RuntimeMessage.Level;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentManager;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * POJO implementation of the publisher service.
 *
 * @author tiry
 */
public class PublisherServiceImpl extends DefaultComponent implements PublisherService, ComponentManager.Listener {

    private static final Logger log = LogManager.getLogger(PublisherServiceImpl.class);

    public static final String TREE_EP = "tree";

    public static final String TREE_CONFIG_EP = "treeInstance";

    public static final String VALIDATORS_RULE_EP = "validatorsRule";

    public static final String FACTORY_EP = "factory";

    public static final String ROOT_SECTION_FINDER_FACTORY_EP = "rootSectionFinderFactory";

    protected static final String ROOT_PATH_KEY = "RootPath";

    protected static final String RELATIVE_ROOT_PATH_KEY = "RelativeRootPath";

    protected Map<String, PublicationTreeConfigDescriptor> treeConfigDescriptors;

    protected Map<String, PublicationTreeConfigDescriptor> pendingDescriptors;

    protected RootSectionFinderFactory rootSectionFinderFactory;

    @Override
    public void start(ComponentContext context) {
        treeConfigDescriptors = new ConcurrentHashMap<>();
        pendingDescriptors = new ConcurrentHashMap<>();
        this.<PublicationTreeConfigDescriptor> getRegistryContributions(TREE_CONFIG_EP).forEach(desc -> {
            if (desc.getParameters().get(RELATIVE_ROOT_PATH_KEY) != null) {
                pendingDescriptors.put(desc.getName(), desc);
            } else {
                treeConfigDescriptors.put(desc.getName(), desc);
            }
        });

        this.<RootSectionFinderFactoryDescriptor> getRegistryContribution(ROOT_SECTION_FINDER_FACTORY_EP)
            .ifPresent(desc -> {
                try {
                    rootSectionFinderFactory = desc.getFactory().getDeclaredConstructor().newInstance();
                } catch (ReflectiveOperationException e) {
                    String message = String.format("Unable to load custom RootSectionFinderFactory (%s)",
                            e.getMessage());
                    addRuntimeMessage(Level.ERROR, message);
                    log.error(message, e);
                }
            });
    }

    @Override
    public void stop(ComponentContext context) throws InterruptedException {
        treeConfigDescriptors = null;
        pendingDescriptors = null;
        rootSectionFinderFactory = null;
    }

    @Override
    public void afterRuntimeStart(ComponentManager mgr, boolean isResume) {
        RepositoryService repositoryService = Framework.getService(RepositoryService.class);
        if (repositoryService == null) {
            // RepositoryService failed to start, no need to go further
            return;
        }

        boolean txWasStartedOutsideComponent = TransactionHelper.isTransactionActiveOrMarkedRollback();
        if (txWasStartedOutsideComponent || TransactionHelper.startTransaction()) {
            boolean completedAbruptly = true;
            try {
                doApplicationStarted();
                completedAbruptly = false;
            } finally {
                if (completedAbruptly) {
                    TransactionHelper.setTransactionRollbackOnly();
                }
                if (!txWasStartedOutsideComponent) {
                    TransactionHelper.commitOrRollbackTransaction();
                }
            }
        } else {
            doApplicationStarted();
        }
    }

    protected void doApplicationStarted() {
        ClassLoader jbossCL = Thread.currentThread().getContextClassLoader();
        ClassLoader nuxeoCL = PublisherServiceImpl.class.getClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(nuxeoCL);
            log.info("Publisher Service initialization");
            registerPendingDescriptors();
        } finally {
            Thread.currentThread().setContextClassLoader(jbossCL);
            log.debug("JBoss ClassLoader restored");
        }
    }

    @Override
    public List<String> getAvailablePublicationTree() {
        List<String> treeConfigs = new ArrayList<>();
        treeConfigs.addAll(treeConfigDescriptors.keySet());
        return treeConfigs;
    }

    @Override
    public Map<String, String> getAvailablePublicationTrees() {
        Map<String, String> trees = new HashMap<>();
        for (PublicationTreeConfigDescriptor desc : treeConfigDescriptors.values()) {
            String title = desc.getTitle() == null ? desc.getName() : desc.getTitle();
            trees.put(desc.getName(), title);
        }
        return trees;
    }

    @Override
    public PublicationTree getPublicationTree(String treeName, CoreSession coreSession, Map<String, String> params) {
        return getPublicationTree(treeName, coreSession, params, null);
    }

    @Override
    public PublicationTree getPublicationTree(String treeName, CoreSession coreSession, Map<String, String> params,
            DocumentModel currentDocument) {
        PublicationTree tree = buildTree(treeName, coreSession, params);
        if (tree == null) {
            return null;
        }
        if (currentDocument != null) {
            tree.setCurrentDocument(currentDocument);
        }
        return tree;
    }

    protected PublicationTree buildTree(String treeConfigName, CoreSession coreSession, Map<String, String> params) {
        PublicationTreeConfigDescriptor config = getPublicationTreeConfigDescriptor(treeConfigName);
        Map<String, String> allParameters = computeAllParameters(config, params);
        PublicationTreeDescriptor treeDescriptor = getPublicationTreeDescriptor(config);
        PublishedDocumentFactory publishedDocumentFactory = getPublishedDocumentFactory(config, treeDescriptor,
                coreSession, allParameters);
        return getPublicationTree(treeDescriptor, coreSession, allParameters, publishedDocumentFactory,
                config.getName(), config.getTitle());
    }

    protected Map<String, String> computeAllParameters(PublicationTreeConfigDescriptor config,
            Map<String, String> params) {
        final Map<String, String> allParameters = config.getParameters();
        if (params != null) {
            allParameters.putAll(params);
        }
        return allParameters;
    }

    protected PublishedDocumentFactory getPublishedDocumentFactory(PublicationTreeConfigDescriptor config,
            PublicationTreeDescriptor treeDescriptor, CoreSession coreSession, Map<String, String> params) {
        PublishedDocumentFactoryDescriptor factoryDesc = getPublishedDocumentFactoryDescriptor(config, treeDescriptor);
        ValidatorsRule validatorsRule = getValidatorsRule(factoryDesc);

        PublishedDocumentFactory factory;
        try {
            factory = factoryDesc.getKlass().getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new NuxeoException("Error while creating factory " + factoryDesc.getName(), e);
        }
        factory.init(coreSession, validatorsRule, params);
        return factory;
    }

    protected ValidatorsRule getValidatorsRule(PublishedDocumentFactoryDescriptor factoryDesc) {
        String ruleName = factoryDesc.getValidatorsRuleName();
        ValidatorsRule rule = null;
        if (ruleName != null) {
            ValidatorsRuleDescriptor desc = this.<ValidatorsRuleDescriptor> getRegistryContribution(VALIDATORS_RULE_EP,
                    ruleName).orElseThrow(() -> new NuxeoException("Unable to find validatorsRule" + ruleName));
            try {
                rule = desc.getKlass().getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                throw new NuxeoException("Error while creating validatorsRule " + ruleName, e);
            }
        }
        return rule;
    }

    protected PublishedDocumentFactoryDescriptor getPublishedDocumentFactoryDescriptor(
            PublicationTreeConfigDescriptor config, PublicationTreeDescriptor treeDescriptor) {
        String factoryName = config.getFactory();
        if (factoryName == null) {
            factoryName = treeDescriptor.getFactory();
        }
        if (factoryName == null) {
            throw new NuxeoException(
                    String.format("No factory for descriptors %s and %s", config.getName(), treeDescriptor.getName()));
        }
        final String name = factoryName;
        return this.<PublishedDocumentFactoryDescriptor> getRegistryContribution(FACTORY_EP, name)
                   .orElseThrow(() -> new NuxeoException("Unable to find factory " + name));
    }

    protected PublicationTreeConfigDescriptor getPublicationTreeConfigDescriptor(String treeConfigName) {
        if (!treeConfigDescriptors.containsKey(treeConfigName)) {
            throw new NuxeoException("Unknow treeConfig :" + treeConfigName);
        }
        return treeConfigDescriptors.get(treeConfigName);
    }

    protected PublicationTreeDescriptor getPublicationTreeDescriptor(PublicationTreeConfigDescriptor config) {
        String treeImplName = config.getTree();
        return this.<PublicationTreeDescriptor> getRegistryContribution(TREE_EP, treeImplName)
                   .orElseThrow(() -> new NuxeoException("Unknow treeImplementation:" + treeImplName));
    }

    protected PublicationTree getPublicationTree(PublicationTreeDescriptor treeDescriptor, CoreSession coreSession,
            Map<String, String> parameters, PublishedDocumentFactory factory, String configName, String treeTitle) {
        PublicationTree treeImpl;
        try {
            treeImpl = treeDescriptor.getKlass().getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new NuxeoException("Error while creating tree implementation", e);
        }
        treeImpl.initTree(coreSession, parameters, factory, configName, treeTitle);
        return treeImpl;
    }

    @Override
    public PublishedDocument publish(DocumentModel doc, PublicationNode targetNode) {
        return publish(doc, targetNode, null);
    }

    @Override
    public PublishedDocument publish(DocumentModel doc, PublicationNode targetNode, Map<String, String> params) {
        return targetNode.getTree().publish(doc, targetNode, params);
    }

    @Override
    public void unpublish(DocumentModel doc, PublicationNode targetNode) {
        targetNode.getTree().unpublish(doc, targetNode);
    }

    @Override
    public boolean isPublishedDocument(DocumentModel documentModel) {
        return PublicationRelationHelper.isPublished(documentModel);
    }

    @Override
    public PublicationTree getPublicationTreeFor(DocumentModel doc, CoreSession coreSession) {
        PublicationTree tree = null;
        try {
            tree = PublicationRelationHelper.getPublicationTreeUsedForPublishing(doc, coreSession);
        } catch (NuxeoException e) {
            // TODO catch proper exception
            log.error(
                    "Unable to get PublicationTree for {}. Fallback on first PublicationTree accepting this document.",
                    doc.getPathAsString(), e);
            for (String treeName : treeConfigDescriptors.keySet()) {
                tree = getPublicationTree(treeName, coreSession, null);
                if (tree.isPublicationNode(doc)) {
                    break;
                }
            }
        }
        return tree;
    }

    @Override
    public PublicationNode wrapToPublicationNode(DocumentModel documentModel, CoreSession coreSession) {
        for (String name : getAvailablePublicationTree()) {
            PublicationTree tree = getPublicationTree(name, coreSession, null);
            if (tree.isPublicationNode(documentModel)) {
                return tree.wrapToPublicationNode(documentModel);
            }
        }
        return null;
    }

    protected void registerPendingDescriptors() {
        // TODO what if there are multiple repositories?
        RepositoryManager repositoryManager = Framework.getService(RepositoryManager.class);
        String repositoryName = repositoryManager.getDefaultRepositoryName();
        List<DocumentModel> domains = new DomainsFinder(repositoryName).getDomains();
        for (DocumentModel domain : domains) {
            registerTreeConfigFor(domain);
        }
    }

    protected String getPendingTreeConfigName(PublicationTreeConfigDescriptor desc, String domainName) {
        return desc.getName() + "-" + domainName;
    }

    public void registerTreeConfigFor(DocumentModel domain) {
        for (PublicationTreeConfigDescriptor desc : pendingDescriptors.values()) {
            String newTreeName = getPendingTreeConfigName(desc, domain.getName());
            PublicationTreeConfigDescriptor newDesc = new PublicationTreeConfigDescriptor(newTreeName, desc);
            Path newPath = domain.getPath();
            Map<String, String> parameters = newDesc.getParameters();
            newPath = newPath.append(parameters.remove(RELATIVE_ROOT_PATH_KEY));
            parameters.put(ROOT_PATH_KEY, newPath.toString());
            parameters.put(PublisherService.DOMAIN_NAME_KEY, domain.getTitle());
            treeConfigDescriptors.put(newDesc.getName(), newDesc);
        }
    }

    public void unRegisterTreeConfigFor(DocumentModel domain) {
        unRegisterTreeConfigFor(domain.getName());
    }

    /**
     * @since 7.3
     */
    public void unRegisterTreeConfigFor(String domainName) {
        for (PublicationTreeConfigDescriptor desc : pendingDescriptors.values()) {
            String treeName = getPendingTreeConfigName(desc, domainName);
            treeConfigDescriptors.remove(treeName);
        }
    }

    @Override
    public Map<String, String> getParametersFor(String treeConfigName) {
        PublicationTreeConfigDescriptor desc = treeConfigDescriptors.get(treeConfigName);
        Map<String, String> parameters = new HashMap<>();
        if (desc != null) {
            parameters.putAll(desc.getParameters());
        }
        return parameters;
    }

    @Override
    public RootSectionFinder getRootSectionFinder(CoreSession session) {
        if (rootSectionFinderFactory != null) {
            return rootSectionFinderFactory.getRootSectionFinder(session);
        }
        return new DefaultRootSectionsFinder(session);
    }
}
