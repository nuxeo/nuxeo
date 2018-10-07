/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.ecm.platform.rendition.extension;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.platform.rendition.service.RenditionDefinition;
import org.nuxeo.runtime.api.Framework;

public class DefaultAutomationRenditionProvider implements RenditionProvider {

    private static final Log log = LogFactory.getLog(DefaultAutomationRenditionProvider.class);

    public static final String VARIANT_POLICY_USER = "user";

    @Override
    public boolean isAvailable(DocumentModel doc, RenditionDefinition def) {
        String chain = def.getOperationChain();
        if (chain == null) {
            log.error("Can not run Automation rendition if chain is not defined");
            return false;
        }
        AutomationService as = Framework.getService(AutomationService.class);

        try {
            if (as.getOperation(chain) == null) {
                log.error("Chain " + chain + " is not defined : rendition can not be used");
                return false;
            }
        } catch (Exception e) {
            log.error("Unable to test Rendition availability", e);
            return false;
        }

        if (!def.isEmptyBlobAllowed()) {
            BlobHolder bh = doc.getAdapter(BlobHolder.class);
            if (bh == null) {
                return false;
            }
            try {
                Blob blob = bh.getBlob();
                if (blob == null) {
                    return false;
                }
            } catch (Exception e) {
                log.error("Unable to get Blob to test Rendition availability", e);
                return false;
            }
        }
        return true;
    }

    @Override
    public List<Blob> render(DocumentModel doc, RenditionDefinition definition) {
        return AutomationRenderer.render(doc, definition, null);
    }

    /**
     * Gets the optional {@link org.nuxeo.ecm.platform.rendition.Constants#RENDITION_VARIANT_PROPERTY
     * RENDITION_VARIANT_PROPERTY} value for a given {@link RenditionDefinition}.
     *
     * @param doc the target document
     * @param definition the rendition definition to use
     * @return the generated {@link org.nuxeo.ecm.platform.rendition.Constants#RENDITION_VARIANT_PROPERTY
     *         RENDITION_VARIANT_PROPERTY} value, or {@code null}
     * @since 8.1
     */
    @Override
    public String getVariant(DocumentModel doc, RenditionDefinition definition) {
        if (VARIANT_POLICY_USER.equals(definition.getVariantPolicy())) {
            NuxeoPrincipal principal = doc.getCoreSession().getPrincipal();
            if (principal.isAdministrator()) {
                return org.nuxeo.ecm.platform.rendition.Constants.RENDITION_VARIANT_PROPERTY_ADMINISTRATOR_USER;
            } else {
                return org.nuxeo.ecm.platform.rendition.Constants.RENDITION_VARIANT_PROPERTY_USER_PREFIX
                        + principal.getName();
            }
        }
        return null;
    }

}
