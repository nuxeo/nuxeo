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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * POJO implementation of the publisher service.
 *
 * @author tiry
 */
public class PublisherServiceImpl extends DefaultComponent implements PublisherService {

    private final Log log = LogFactory.getLog(PublisherServiceImpl.class);

    protected Map<String, PublicationTreeDescriptor> treeDescriptors = new HashMap<>();

    protected Map<String, PublishedDocumentFactoryDescriptor> factoryDescriptors = new HashMap<>();

    protected Map<String, PublicationTreeConfigDescriptor> treeConfigDescriptors = new HashMap<>();

    protected Map<String, ValidatorsRuleDescriptor> validatorsRuleDescriptors = new HashMap<>();

    protected Map<String, PublicationTreeConfigDescriptor> pendingDescriptors = new HashMap<>();

    protected RootSectionFinderFactory rootSectionFinderFactory = null;

    public static final String TREE_EP = "tree";

    public static final String TREE_CONFIG_EP = "treeInstance";

    public static final String VALIDATORS_RULE_EP = "validatorsRule";

    public static final String FACTORY_EP = "factory";

    public static final String ROOT_SECTION_FINDER_FACTORY_EP = "rootSectionFinderFactory";

    protected static final String ROOT_PATH_KEY = "RootPath";

    protected static final String RELATIVE_ROOT_PATH_KEY = "RelativeRootPath";

    @Override
    public void start(ComponentContext context) {
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
    public void activate(ComponentContext context) {
        treeDescriptors = new HashMap<>();
        factoryDescriptors = new HashMap<>();
        treeConfigDescriptors = new HashMap<>();
        validatorsRuleDescriptors = new HashMap<>();
        pendingDescriptors = new HashMap<>();
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {

        log.debug("Registry contribution for EP " + extensionPoint);

        if (TREE_EP.equals(extensionPoint)) {
            PublicationTreeDescriptor desc = (PublicationTreeDescriptor) contribution;
            treeDescriptors.put(desc.getName(), desc);
        } else if (TREE_CONFIG_EP.equals(extensionPoint)) {
            PublicationTreeConfigDescriptor desc = (PublicationTreeConfigDescriptor) contribution;
            registerTreeConfig(desc);
        } else if (FACTORY_EP.equals(extensionPoint)) {
            PublishedDocumentFactoryDescriptor desc = (PublishedDocumentFactoryDescriptor) contribution;
            factoryDescriptors.put(desc.getName(), desc);
        } else if (VALIDATORS_RULE_EP.equals(extensionPoint)) {
            ValidatorsRuleDescriptor desc = (ValidatorsRuleDescriptor) contribution;
            validatorsRuleDescriptors.put(desc.getName(), desc);
        } else if (ROOT_SECTION_FINDER_FACTORY_EP.equals(extensionPoint)) {
            RootSectionFinderFactoryDescriptor desc = (RootSectionFinderFactoryDescriptor) contribution;
            try {
                rootSectionFinderFactory = desc.getFactory().getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException t) {
                log.error("Unable to load custom RootSectionFinderFactory", t);
            }
        }
    }

    protected void registerTreeConfig(PublicationTreeConfigDescriptor desc) {
        if (desc.getParameters().get("RelativeRootPath") != null) {
            pendingDescriptors.put(desc.getName(), desc);
        } else {
            treeConfigDescriptors.put(desc.getName(), desc);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (contribution instanceof PublicationTreeDescriptor) {
            treeDescriptors.remove(((PublicationTreeDescriptor) contribution).getName());
        } else if (contribution instanceof PublicationTreeConfigDescriptor) {
            String name = ((PublicationTreeConfigDescriptor) contribution).getName();
            pendingDescriptors.remove(name);
            treeConfigDescriptors.remove(name);
        } else if (contribution instanceof ValidatorsRuleDescriptor) {
            validatorsRuleDescriptors.remove(((ValidatorsRuleDescriptor) contribution).getName());
        } else if (contribution instanceof RootSectionFinderFactoryDescriptor) {
            rootSectionFinderFactory = null;
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
        String validatorsRuleName = factoryDesc.getValidatorsRuleName();
        ValidatorsRule validatorsRule = null;
        if (validatorsRuleName != null) {
            ValidatorsRuleDescriptor validatorsRuleDesc = validatorsRuleDescriptors.get(validatorsRuleName);
            if (validatorsRuleDesc == null) {
                throw new NuxeoException("Unable to find validatorsRule" + validatorsRuleName);
            }
            try {
                validatorsRule = validatorsRuleDesc.getKlass().getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                throw new NuxeoException("Error while creating validatorsRule " + validatorsRuleName, e);
            }
        }
        return validatorsRule;
    }

    protected PublishedDocumentFactoryDescriptor getPublishedDocumentFactoryDescriptor(
            PublicationTreeConfigDescriptor config, PublicationTreeDescriptor treeDescriptor) {
        String factoryName = config.getFactory();
        if (factoryName == null) {
            factoryName = treeDescriptor.getFactory();
        }

        PublishedDocumentFactoryDescriptor factoryDesc = factoryDescriptors.get(factoryName);
        if (factoryDesc == null) {
            throw new NuxeoException("Unable to find factory" + factoryName);
        }
        return factoryDesc;
    }

    protected PublicationTreeConfigDescriptor getPublicationTreeConfigDescriptor(String treeConfigName) {
        if (!treeConfigDescriptors.containsKey(treeConfigName)) {
            throw new NuxeoException("Unknow treeConfig :" + treeConfigName);
        }
        return treeConfigDescriptors.get(treeConfigName);
    }

    protected PublicationTreeDescriptor getPublicationTreeDescriptor(PublicationTreeConfigDescriptor config) {
        String treeImplName = config.getTree();
        if (!treeDescriptors.containsKey(treeImplName)) {
            throw new NuxeoException("Unknow treeImplementation :" + treeImplName);
        }
        return treeDescriptors.get(treeImplName);
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
            log.error("Unable to get PublicationTree for " + doc.getPathAsString()
                    + ". Fallback on first PublicationTree accepting this document.", e);
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
        // TODO what to do with multiple repositories?
        RepositoryManager repositoryManager = Framework.getService(RepositoryManager.class);
        String repositoryName = repositoryManager.getDefaultRepositoryName();
        List<DocumentModel> domains = new DomainsFinder(repositoryName).getDomains();
        for (DocumentModel domain : domains) {
            registerTreeConfigFor(domain);
        }
    }

    public void registerTreeConfigFor(DocumentModel domain) {
        for (PublicationTreeConfigDescriptor desc : pendingDescriptors.values()) {
            PublicationTreeConfigDescriptor newDesc = new PublicationTreeConfigDescriptor(desc);
            String newTreeName = desc.getName() + "-" + domain.getName();
            newDesc.setName(newTreeName);
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
            String treeName = desc.getName() + "-" + domainName;
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
