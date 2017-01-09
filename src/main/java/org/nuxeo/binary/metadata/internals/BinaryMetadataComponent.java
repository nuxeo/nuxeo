/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.binary.metadata.internals;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.binary.metadata.api.BinaryMetadataConstants;
import org.nuxeo.binary.metadata.api.BinaryMetadataService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.management.metrics.MetricInvocationHandler;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Binary metadata component which registers all binary metadata contributions.
 *
 * @since 7.1
 */
public class BinaryMetadataComponent extends DefaultComponent {

    private static final Log log = LogFactory.getLog(BinaryMetadataComponent.class);

    protected BinaryMetadataService metadataService = new BinaryMetadataServiceImpl(this);

    protected final MetadataMappingRegistry mappingRegistry = new MetadataMappingRegistry();

    protected final MetadataProcessorRegistry processorRegistry = new MetadataProcessorRegistry();

    protected final MetadataRuleRegistry ruleRegistry = new MetadataRuleRegistry();

    @Override
    public void activate(ComponentContext context) {
        super.activate(context);
        if (Boolean.valueOf(Framework.getProperty(BinaryMetadataConstants.BINARY_METADATA_MONITOR,
                Boolean.toString(log.isTraceEnabled())))) {
            metadataService = MetricInvocationHandler.newProxy(metadataService, BinaryMetadataService.class);
        }
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (BinaryMetadataConstants.METADATA_MAPPING_EP.equals(extensionPoint)) {
            mappingRegistry.addContribution((MetadataMappingDescriptor) contribution);
        } else if (BinaryMetadataConstants.METADATA_RULES_EP.equals(extensionPoint)) {
            ruleRegistry.addContribution((MetadataRuleDescriptor) contribution);
        } else if (BinaryMetadataConstants.METADATA_PROCESSORS_EP.equals(extensionPoint)) {
            processorRegistry.addContribution((MetadataProcessorDescriptor) contribution);
        } else {
            log.error("Unknown extension point " + extensionPoint);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (BinaryMetadataConstants.METADATA_MAPPING_EP.equals(extensionPoint)) {
            mappingRegistry.removeContribution((MetadataMappingDescriptor) contribution);
        } else if (BinaryMetadataConstants.METADATA_RULES_EP.equals(extensionPoint)) {
            ruleRegistry.removeContribution((MetadataRuleDescriptor) contribution);
        } else if (BinaryMetadataConstants.METADATA_PROCESSORS_EP.equals(extensionPoint)) {
            processorRegistry.removeContribution((MetadataProcessorDescriptor) contribution);
        } else {
            log.error("Unknown extension point " + extensionPoint);
        }
    }

    @Override
    public void applicationStarted(ComponentContext context) {
        super.applicationStarted(context);
        ruleRegistry.handleApplicationStarted();
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(BinaryMetadataService.class)) {
            return adapter.cast(metadataService);
        }
        return null;
    }

}
