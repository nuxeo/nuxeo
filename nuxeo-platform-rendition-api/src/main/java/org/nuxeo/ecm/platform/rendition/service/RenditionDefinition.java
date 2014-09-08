/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * Contributors:
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.rendition.service;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.rendition.extension.RenditionProvider;

/**
 * Definition of a rendition.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.4.1
 */
@XObject("renditionDefinition")
public class RenditionDefinition {

    protected RenditionProvider provider;

    @XNode("@name")
    protected String name;

    @XNode("@enabled")
    boolean enabled = true;

    @XNode("label")
    protected String label;

    @XNode("icon")
    protected String icon;

    @XNode("kind")
    protected String kind;

    @XNode("operationChain")
    protected String operationChain;
    
    
    /**
     * @since 5.9.6
     */
    @XNode("allowEmptyBlob")
    protected boolean allowEmptyBlob = false;

    @XNode("@class")
    protected Class<? extends RenditionProvider> providerClass;

    @XNode("contentType")
    protected String contentType;

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getLabel() {
        return label;
    }

    public String getOperationChain() {
        return operationChain;
    }

    public Class<? extends RenditionProvider> getProviderClass() {
        return providerClass;
    }

    public RenditionProvider getProvider() {
        return provider;
    }

    public void setProvider(RenditionProvider provider) {
        this.provider = provider;
    }

    public String getIcon() {
        return icon;
    }

    public String getKind() {
        return kind;
    }

    public String getProviderType() {
        RenditionProvider provider = getProvider();
        if (provider == null) {
            return null;
        }
        return provider.getClass().getSimpleName();
    }

    public String getContentType() {
        return contentType;
    }

    public boolean isEmptyBlobAllowed() {
        return allowEmptyBlob;
    }
    
}
