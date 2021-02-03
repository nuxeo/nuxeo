/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.theme.styling.service.registries;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.Resource;
import org.nuxeo.common.xmap.XAnnotatedObject;
import org.nuxeo.common.xmap.registry.MapRegistry;
import org.nuxeo.theme.styling.service.descriptors.FlavorDescriptor;
import org.nuxeo.theme.styling.service.descriptors.FlavorPresets;
import org.nuxeo.theme.styling.service.descriptors.SassImport;
import org.w3c.dom.Element;

/**
 * Registry for theme flavors, handling merge of registered {@link FlavorDescriptor} elements.
 *
 * @since 5.5
 */
public class FlavorRegistry extends MapRegistry {

    private static final Logger log = LogManager.getLogger(FlavorRegistry.class);

    @Override
    @SuppressWarnings("unchecked")
    protected <T> T doRegister(Context ctx, XAnnotatedObject xObject, Element element, String extensionId) {
        FlavorDescriptor flavor = super.doRegister(ctx, xObject, element, extensionId);
        updateFlavor(flavor, ctx);
        return (T) flavor;
    }

    protected void updateFlavor(FlavorDescriptor flavor, Context ctx) {
        // set flavor presets files content
        List<FlavorPresets> presets = flavor.getPresets();
        for (FlavorPresets myPreset : presets) {
            if (myPreset.getContent() != null) {
                continue;
            }
            Resource resource = myPreset.getResource();
            try {
                myPreset.setContent(new String(IOUtils.toByteArray(resource.toURL())));
            } catch (IOException e) {
                log.error("Could not find resource at '{}'", resource);
                myPreset.setContent("");
            }
        }

        // set flavor sass variables
        List<SassImport> sassVars = flavor.getSassImports();
        for (SassImport var : sassVars) {
            if (var.getContent() != null) {
                continue;
            }
            Resource resource = var.getResource();
            try {
                var.setContent(new String(IOUtils.toByteArray(resource.toURL())));
            } catch (IOException e) {
                log.error("Could not find resource at '{}'", resource);
                var.setContent("");
            }
        }
    }

    public List<FlavorDescriptor> getFlavorsExtending(String flavor) {
        return this.<FlavorDescriptor> getContributionValues().stream().filter(f -> {
            String extendsFlavor = f.getExtendsFlavor();
            return !StringUtils.isBlank(extendsFlavor) && extendsFlavor.equals(flavor);
        }).collect(Collectors.toList());
    }

}
