/*
 * (C) Copyright 2010-2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 *     Thierry Delprat
 *     Florent Guillaume
 *     ron1
 */
package org.nuxeo.ecm.platform.rendition.service;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static org.apache.commons.lang3.ObjectUtils.getIfNull;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.rendition.extension.RenditionProvider;
import org.nuxeo.runtime.model.Descriptor;

/**
 * Definition of a rendition.
 *
 * @since 5.4.1
 */
@XObject("renditionDefinition")
public class RenditionDefinition implements Descriptor {

    public static final String DEFAULT_SOURCE_DOCUMENT_MODIFICATION_DATE_PROPERTY_NAME = "dc:modified";

    /** True if the boolean is null or TRUE, false otherwise. */
    private static boolean defaultTrue(Boolean bool) {
        return !FALSE.equals(bool);
    }

    /** False if the boolean is null or FALSE, true otherwise. */
    private static boolean defaultFalse(Boolean bool) {
        return TRUE.equals(bool);
    }

    @XNode("@name")
    protected String name;

    @Override
    public String getId() {
        return name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * @since 7.3
     */
    @XNode("@cmisName")
    protected String cmisName;

    public String getCmisName() {
        return cmisName;
    }

    public void setCmisName(String cmisName) {
        this.cmisName = cmisName;
    }

    @XNode("@enabled")
    protected Boolean enabled;

    public boolean isEnabled() {
        return defaultTrue(enabled);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = Boolean.valueOf(enabled);
    }

    @XNode("label")
    protected String label;

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @XNode("icon")
    protected String icon;

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    @XNode("kind")
    protected String kind;

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    @XNode("operationChain")
    protected String operationChain;

    public String getOperationChain() {
        return operationChain;
    }

    public void setOperationChain(String operationChain) {
        this.operationChain = operationChain;
    }

    /**
     * @since 6.0
     */
    @XNode("allowEmptyBlob")
    protected Boolean allowEmptyBlob;

    /**
     * @since 7.3
     */
    public boolean isEmptyBlobAllowed() {
        return defaultFalse(allowEmptyBlob);
    }

    public void setAllowEmptyBlob(boolean allowEmptyBlob) {
        this.allowEmptyBlob = Boolean.valueOf(allowEmptyBlob);
    }

    /**
     * @since 6.0
     */
    @XNode("@visible")
    protected Boolean visible;

    public boolean isVisible() {
        return defaultTrue(visible);
    }

    public void setVisible(boolean visible) {
        this.visible = Boolean.valueOf(visible);
    }

    @XNode("@class")
    protected Class<? extends RenditionProvider> providerClass;

    public Class<? extends RenditionProvider> getProviderClass() {
        return providerClass;
    }

    public void setProviderClass(Class<? extends RenditionProvider> providerClass) {
        this.providerClass = providerClass;
    }

    // computed from providerClass
    protected RenditionProvider provider;

    public RenditionProvider getProvider() {
        return provider;
    }

    public String getProviderType() {
        RenditionProvider provider = getProvider();
        if (provider == null) {
            return null;
        }
        return provider.getClass().getSimpleName();
    }

    public void setProvider(RenditionProvider provider) {
        this.provider = provider;
    }

    @XNode("contentType")
    protected String contentType;

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * @since 7.2
     */
    @XNodeList(value = "filters/filter-id", type = ArrayList.class, componentType = String.class)
    protected List<String> filterIds;

    public List<String> getFilterIds() {
        return filterIds;
    }

    public void setFilterIds(List<String> filterIds) {
        this.filterIds = filterIds;
    }

    /**
     * @since 7.10
     */
    @XNode("sourceDocumentModificationDatePropertyName")
    protected String sourceDocumentModificationDatePropertyName;

    /**
     * @since 7.10
     */
    public String getSourceDocumentModificationDatePropertyName() {
        return defaultIfBlank(sourceDocumentModificationDatePropertyName,
                DEFAULT_SOURCE_DOCUMENT_MODIFICATION_DATE_PROPERTY_NAME);
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
    @XNode("storeByDefault")
    protected Boolean storeByDefault;

    /**
     * @since 7.10
     */
    public boolean isStoreByDefault() {
        return defaultFalse(storeByDefault);
    }

    /**
     * @since 7.10
     */
    public void setStoreByDefault(boolean storeByDefault) {
        this.storeByDefault = Boolean.valueOf(storeByDefault);
    }

    /**
     * @since 8.1
     */
    @XNode("variantPolicy")
    protected String variantPolicy;

    /**
     * @since 8.1
     */
    public String getVariantPolicy() {
        return variantPolicy;
    }

    @Override
    public Descriptor merge(Descriptor o) {
        var other = (RenditionDefinition) o;
        var merged = new RenditionDefinition();
        merged.name = getIfNull(other.name, name);
        merged.cmisName = defaultIfBlank(other.cmisName, cmisName);
        merged.enabled = getIfNull(other.enabled, enabled);
        merged.label = defaultIfBlank(other.label, label);
        merged.icon = defaultIfBlank(other.icon, icon);
        merged.kind = defaultIfBlank(other.kind, kind);
        merged.operationChain = defaultIfBlank(other.operationChain, operationChain);
        merged.allowEmptyBlob = getIfNull(other.allowEmptyBlob, allowEmptyBlob);
        merged.visible = getIfNull(other.visible, visible);
        merged.providerClass = getIfNull(other.providerClass, providerClass);
        merged.contentType = defaultIfBlank(other.contentType, contentType);
        merged.filterIds = new ArrayList<>(emptyIfNull(filterIds));
        merged.filterIds.addAll(emptyIfNull(other.filterIds));
        merged.sourceDocumentModificationDatePropertyName = defaultIfBlank(
                other.sourceDocumentModificationDatePropertyName, sourceDocumentModificationDatePropertyName);
        merged.storeByDefault = getIfNull(other.storeByDefault, storeByDefault);
        merged.variantPolicy = defaultIfBlank(other.variantPolicy, variantPolicy);
        return merged;
    }
}
