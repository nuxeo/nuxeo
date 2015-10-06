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

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
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

    public static final String DEFAULT_SOURCE_DOCUMENT_MODIFICATION_DATE_PROPERTY_NAME = "dc:modified";

    protected RenditionProvider provider;

    @XNode("@name")
    protected String name;

    /**
     * @since 7.3
     */
    @XNode("@cmisName")
    protected String cmisName;

    @XNode("@enabled")
    Boolean enabled;

    @XNode("label")
    protected String label;

    @XNode("icon")
    protected String icon;

    @XNode("kind")
    protected String kind;

    @XNode("operationChain")
    protected String operationChain;

    /**
     * @since 6.0
     */
    @XNode("allowEmptyBlob")
    protected Boolean allowEmptyBlob;

    /**
     * @since 6.0
     */
    @XNode("@visible")
    protected Boolean visible;

    @XNode("@class")
    protected Class<? extends RenditionProvider> providerClass;

    @XNode("contentType")
    protected String contentType;

    /**
     * @since 7.2
     */
    @XNodeList(value = "filters/filter-id", type = ArrayList.class, componentType = String.class)
    protected List<String> filterIds;

    /**
     * @since 7.10
     */
    @XNode("sourceDocumentModificationDatePropertyName")
    protected String sourceDocumentModificationDatePropertyName =
            DEFAULT_SOURCE_DOCUMENT_MODIFICATION_DATE_PROPERTY_NAME;

    /**
     * @since 7.10
     */
    @XNode("storeByDefault")
    protected Boolean storeByDefault;

    public String getName() {
        return name;
    }

    public String getCmisName() {
        return cmisName;
    }

    public boolean isEnabled() {
        return enabled == null || enabled;
    }

    /**
     * @since 7.3
     */
    public boolean isEnabledSet() {
        return enabled != null;
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

    /**
     * @since 7.3
     */
    public boolean isEmptyBlobAllowed() {
        return allowEmptyBlob != null && allowEmptyBlob;
    }

    public boolean isEmptyBlobAllowedSet() {
        return allowEmptyBlob != null;
    }

    public boolean isVisible() {
        return visible == null || visible;
    }

    /**
     * @since 7.3
     */
    public boolean isVisibleSet() {
        return visible != null;
    }

    public List<String> getFilterIds() {
        return filterIds;
    }

    /**
     * @since 7.10
     */
    public String getSourceDocumentModificationDatePropertyName() {
        return sourceDocumentModificationDatePropertyName;
    }

    /**
     * @since 7.10
     */
    public boolean isStoreByDefault() {
        return storeByDefault != null && storeByDefault;
    }

    /**
     * @since 7.10
     */
    public boolean isStoreByDefaultSet() {
        return storeByDefault != null;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCmisName(String cmisName) {
        this.cmisName = cmisName;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public void setOperationChain(String operationChain) {
        this.operationChain = operationChain;
    }

    public void setAllowEmptyBlob(boolean allowEmptyBlob) {
        this.allowEmptyBlob = allowEmptyBlob;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public void setProviderClass(Class<? extends RenditionProvider> providerClass) {
        this.providerClass = providerClass;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public void setFilterIds(List<String> filterIds) {
        this.filterIds = filterIds;
    }

    /**
     * @since 7.10
     */
    public void setSourceDocumentModificationDatePropertyName(String sourceDocumentModificationDatePropertyName) {
        this.sourceDocumentModificationDatePropertyName = sourceDocumentModificationDatePropertyName;
    }

    /**
     * @since 7.10
     */
    public void setStoreByDefault(boolean storeByDefault) {
        this.storeByDefault = storeByDefault;
    }


    /**
     * @since 7.3
     */
    @Override
    public RenditionDefinition clone() {
        RenditionDefinition clone = new RenditionDefinition();
        clone.name = name;
        clone.cmisName = cmisName;
        clone.enabled = enabled;
        clone.label = label;
        clone.icon = icon;
        clone.kind = kind;
        clone.operationChain = operationChain;
        clone.allowEmptyBlob = allowEmptyBlob;
        clone.visible = visible;
        clone.providerClass = providerClass;
        clone.contentType = contentType;
        if (filterIds != null) {
            clone.filterIds = new ArrayList<>();
            clone.filterIds.addAll(filterIds);
        }
        clone.sourceDocumentModificationDatePropertyName = sourceDocumentModificationDatePropertyName;
        clone.storeByDefault = storeByDefault;
        return clone;
    }
}
