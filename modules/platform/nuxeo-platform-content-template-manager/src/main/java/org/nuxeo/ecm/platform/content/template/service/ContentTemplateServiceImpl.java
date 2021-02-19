/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.content.template.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.xmap.registry.MapRegistry;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.repository.RepositoryInitializationHandler;
import org.nuxeo.ecm.platform.content.template.listener.RepositoryInitializationListener;
import org.nuxeo.runtime.RuntimeMessage.Level;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

public class ContentTemplateServiceImpl extends DefaultComponent implements ContentTemplateService {

    private static final Logger log = LogManager.getLogger(ContentTemplateServiceImpl.class);

    public static final String NAME = "org.nuxeo.ecm.platform.content.template.service.TemplateService";

    public static final String FACTORY_DECLARATION_EP = "factory";

    public static final String FACTORY_BINDING_EP = "factoryBinding";

    public static final String POST_CONTENT_CREATION_HANDLERS_EP = "postContentCreationHandlers";

    protected List<PostContentCreationHandler> postContentCreationHandlers;

    private RepositoryInitializationHandler initializationHandler;

    @Override
    public void activate(ComponentContext context) {
        // register our Repo init listener
        initializationHandler = new RepositoryInitializationListener();
        initializationHandler.install();
    }

    @Override
    public void deactivate(ComponentContext context) {
        if (initializationHandler != null) {
            initializationHandler.uninstall();
        }
    }

    @Override
    public void start(ComponentContext context) {
        postContentCreationHandlers = getOrderedHandlers();

        // check factories referenced by bindings
        Set<String> factories = this.<MapRegistry<?>> getExtensionPointRegistry(FACTORY_DECLARATION_EP)
                                    .getContributions()
                                    .keySet();
        this.<FactoryBindingDescriptor> getRegistryContributions(FACTORY_BINDING_EP)
            .stream()
            .filter(binding -> !factories.contains(binding.getFactoryName()))
            .forEach(binding -> {
                String msg = String.format("Factory Binding '%s' references unknown factory '%s'", binding.getName(),
                        binding.getFactoryName());
                log.error(msg);
                addRuntimeMessage(Level.ERROR, msg);
            });
    }

    @Override
    public void stop(ComponentContext context) throws InterruptedException {
        postContentCreationHandlers = null;
    }

    protected List<PostContentCreationHandler> getOrderedHandlers() {
        List<PostContentCreationHandlerDescriptor> descs = getRegistryContributions(POST_CONTENT_CREATION_HANDLERS_EP);
        Collections.sort(descs);

        List<PostContentCreationHandler> handlers = new ArrayList<>();
        for (PostContentCreationHandlerDescriptor desc : descs) {
            try {
                handlers.add(desc.getClazz().getDeclaredConstructor().newInstance());
            } catch (ReflectiveOperationException e) {
                String msg = String.format("Unable to instantiate class for handler: %s (%s)", desc.getName(),
                        e.getMessage());
                addRuntimeMessage(Level.ERROR, msg);
                log.error(msg, e);
            }
        }
        return handlers;
    }

    @Override
    public ContentFactory getFactoryForType(String documentType) {
        return getFactoryInstance(documentType);
    }

    public ContentFactory getFactoryForFacet(String facet) {
        return getFactoryInstance(facet);
    }

    /**
     * Instantiate a new factory for each caller, because factories are actually stateful: they contain the session of
     * their root.
     */
    protected ContentFactory getFactoryInstance(String documentTypeOrFacet) {
        if (documentTypeOrFacet == null) {
            return null;
        }
        Optional<FactoryBindingDescriptor> optBinding = getRegistryContribution(FACTORY_BINDING_EP,
                documentTypeOrFacet);
        if (optBinding.isPresent()) {
            FactoryBindingDescriptor binding = optBinding.get();
            Optional<ContentFactoryDescriptor> optFactory = getRegistryContribution(FACTORY_DECLARATION_EP,
                    binding.getFactoryName());
            if (optFactory.isPresent()) {
                ContentFactoryDescriptor factoryDesc = optFactory.get();
                try {
                    ContentFactory factory = factoryDesc.getClassName().getConstructor().newInstance();
                    boolean factoryOK = factory.initFactory(binding.getOptions(), binding.getRootAcl(),
                            binding.getTemplate());
                    if (!factoryOK) {
                        log.error("Error while initializing instance of factory {}", factoryDesc.getName());
                        return null;
                    }
                    return factory;
                } catch (ReflectiveOperationException e) {
                    log.error("Error while creating instance of factory {}: {}", factoryDesc.getName(), e.getMessage());
                    return null;
                }
            }
        }
        return null;
    }

    @Override
    public void executeFactoryForType(DocumentModel createdDocument) {
        ContentFactory factory = getFactoryForType(createdDocument.getType());
        if (factory != null) {
            factory.createContentStructure(createdDocument);
        }
        Set<String> facets = createdDocument.getFacets();
        for (String facet : facets) {
            factory = getFactoryForFacet(facet);
            if (factory != null) {
                factory.createContentStructure(createdDocument);
            }
        }
    }

    @Override
    public void executePostContentCreationHandlers(CoreSession session) {
        for (PostContentCreationHandler handler : postContentCreationHandlers) {
            handler.execute(session);
        }
    }

    // for testing

    public Map<String, ContentFactoryDescriptor> getFactories() {
        Map<String, ContentFactoryDescriptor> contribs = this.<MapRegistry<ContentFactoryDescriptor>> getExtensionPointRegistry(
                FACTORY_DECLARATION_EP).getContributions();
        return Collections.unmodifiableMap(contribs);
    }

    public Map<String, FactoryBindingDescriptor> getFactoryBindings() {
        Map<String, FactoryBindingDescriptor> contribs = this.<MapRegistry<FactoryBindingDescriptor>> getExtensionPointRegistry(
                FACTORY_BINDING_EP).getContributions();
        return Collections.unmodifiableMap(contribs);
    }

}
