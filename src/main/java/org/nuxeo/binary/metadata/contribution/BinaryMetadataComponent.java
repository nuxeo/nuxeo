/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.binary.metadata.contribution;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.binary.metadata.api.BinaryMetadataConstants;
import org.nuxeo.binary.metadata.api.service.BinaryMetadataRegistryService;
import org.nuxeo.binary.metadata.api.service.BinaryMetadataService;
import org.nuxeo.binary.metadata.api.service.BinaryMetadataServiceImpl;
import org.nuxeo.runtime.api.Framework;
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

    protected BinaryMetadataServiceImpl metadataService;

    @Override
    public void activate(ComponentContext context) {
        super.activate(context);
        this.metadataService = new BinaryMetadataServiceImpl();
    }

    @Override
    public void deactivate(ComponentContext context) {
        super.deactivate(context);
        this.metadataService = null;
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (BinaryMetadataConstants.METADATA_MAPPING_EP.equals(extensionPoint)) {
            this.metadataService.addMappingContribution((MetadataMappingDescriptor) contribution);
        } else if (BinaryMetadataConstants.METADATA_RULES_EP.equals(extensionPoint)) {
            this.metadataService.addRuleContribution((MetadataRuleDescriptor) contribution);
        } else if (BinaryMetadataConstants.METADATA_PROCESSORS_EP.equals(extensionPoint)) {
            this.metadataService.addProcessorContribution((MetadataProcessorDescriptor) contribution);
        } else {
            log.error("Unknown extension point " + extensionPoint);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (BinaryMetadataConstants.METADATA_MAPPING_EP.equals(extensionPoint)) {
            this.metadataService.removeMappingContribution((MetadataMappingDescriptor) contribution);
        } else if (BinaryMetadataConstants.METADATA_RULES_EP.equals(extensionPoint)) {
            this.metadataService.removeRuleContribution((MetadataRuleDescriptor) contribution);
        } else if (BinaryMetadataConstants.METADATA_PROCESSORS_EP.equals(extensionPoint)) {
            this.metadataService.removeProcessorContribution((MetadataProcessorDescriptor) contribution);
        } else {
            log.error("Unknown extension point " + extensionPoint);
        }
    }

    @Override
    public void applicationStarted(ComponentContext context) {
        super.applicationStarted(context);
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == BinaryMetadataRegistryService.class || adapter == BinaryMetadataService.class) {
            return adapter.cast(metadataService);
        }
        return null;
    }
}
