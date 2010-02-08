/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.content.template.service;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.repository.RepositoryInitializationHandler;
import org.nuxeo.ecm.platform.content.template.listener.RepositoryInitializationListener;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

public class ContentTemplateServiceImpl extends DefaultComponent implements
        ContentTemplateService {

    public static final String NAME = "org.nuxeo.ecm.platform.content.template.service.TemplateService";

    public static final String FACTORY_DECLARATION_EP = "factory";

    public static final String FACTORY_BINDING_EP = "factoryBinding";
    
    public static final String FACTORY_SELECTOR_EP = "factorySelector";

    private static final Log log = LogFactory.getLog(ContentTemplateServiceImpl.class);

    private Map<String, ContentFactoryDescriptor> factories;

    private Map<String, FactoryBindingDescriptor> factoryBindings;

    private Map<String, FactorySelector> factorySelectors;

    private RepositoryInitializationHandler initializationHandler;

    @Override
    public void activate(ComponentContext context) {
        factories = new HashMap<String, ContentFactoryDescriptor>();
        factoryBindings = new HashMap<String, FactoryBindingDescriptor>();
        factorySelectors = new TreeMap<String, FactorySelector>();

        // register our Repo init listener
        initializationHandler = new RepositoryInitializationListener();
        initializationHandler.install();
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        if (initializationHandler != null) {
            initializationHandler.uninstall();
        }
    }

    protected ContentFactory newContentFactory(ContentFactoryDescriptor desc, FactoryBindingDescriptor binding) {
        ContentFactory factory  = null;
        try {
            factory = desc.getClassName().newInstance();
        } catch (Exception e) {
            log.error("Error while creating instance of factory " + desc.getName() + " :" + e.getMessage());
            return null;
        }
        Boolean factoryOK = factory.initFactory(binding.getOptions(), binding.getRootAcl(), binding.getTemplate());
        if (!factoryOK) {
            log.error("Error while initializing instance of factory " + desc.getName());
            return null;
        }
        return factory;
    }
    
    protected void registerFactoryBinding(FactoryBindingDescriptor binding) {
        ContentFactoryDescriptor desc = factories.get(binding.getFactoryName());
        if (desc == null) {
            log.error("Factory Binding" + binding.getName() + " can not be registred since Factory "
                    + binding.getFactoryName() + " is not registred");
            return;
        }
        ContentFactory factory = newContentFactory(desc, binding);
        for (FactorySelector selector:factorySelectors.values()) {
            String key = selector.register(desc,binding,factory);
            if (key != null) {
                factoryBindings.put(key, binding);
            }
        }
    }
   
    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        if (extensionPoint.equals(FACTORY_DECLARATION_EP)) {
            // store factories
            ContentFactoryDescriptor descriptor = (ContentFactoryDescriptor) contribution;
            factories.put(descriptor.getName(), descriptor);
        } else if (extensionPoint.equals(FACTORY_BINDING_EP)) {
            registerFactoryBinding((FactoryBindingDescriptor)contribution);
        } else if (extensionPoint.equals(FACTORY_SELECTOR_EP)) {
            registerFactorySelector((FactorySelectorDescriptor)contribution);
        } else {
            log.error("Unknow extension point : " +  extensionPoint);
        }
    }

    protected void registerFactorySelector(FactorySelectorDescriptor desc) {
        try {
            factorySelectors.put(desc.name, desc.selector.newInstance());
        } catch (Exception e) {
            log.error("Error while creating instance of selector " + desc.name, e);
        }
    }

    public ContentFactory getFactoryFor(DocumentModel doc) {
        for(FactorySelector selector:factorySelectors.values()) {
            ContentFactory factory = selector.getFactoryFor(doc);
            if (factory != null) {
                return factory;
            }
        }
        return null;
    }

    public void executeFactoryForType(DocumentModel createdDocument)
            throws ClientException {
        ContentFactory factory = getFactoryFor(createdDocument);
        if (factory != null) {
            factory.createContentStructure(createdDocument);
        }
    }
    
    public ContentFactory getFactoryForType(String documentType) {
        try {
            throw new UnsupportedOperationException("deprecated ");
        } catch (UnsupportedOperationException e) {
            log.warn("use of deprecated getFactoryForType API, please correct", e);
        }
        return null;
    }

    // for testing
    public Map<String, ContentFactoryDescriptor> getFactories() {
        return factories;
    }

    public Map<String, FactoryBindingDescriptor> getFactoryBindings() {
        return factoryBindings;
    }


}
