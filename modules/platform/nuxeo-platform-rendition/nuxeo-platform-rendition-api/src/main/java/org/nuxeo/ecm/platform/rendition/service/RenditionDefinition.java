/*
 * (C) Copyright 2010-2018 Nuxeo (http://nuxeo.com/) and others.
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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XEnable;
import org.nuxeo.common.xmap.registry.XRegistry;
import org.nuxeo.common.xmap.registry.XRegistryId;
import org.nuxeo.ecm.platform.rendition.extension.RenditionProvider;

/**
 * Definition of a rendition.
 *
 * @since 5.4.1
 */
@XObject("renditionDefinition")
@XRegistry(enable = false)
public class RenditionDefinition {

    private static final Logger log = LogManager.getLogger(RenditionDefinition.class);

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
    @XRegistryId
    protected String name;

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

    @XNode(value = XEnable.ENABLE, fallback = "@enabled", defaultAssignment = "true")
    @XEnable
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

    protected RenditionProvider createProvider() {
        if (providerClass != null) {
            try {
                return providerClass.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                log.error("Unable to create RenditionProvider for '{}'", getName(), e);
            }
        }
        return null;
    }

    public RenditionProvider getProvider() {
        if (provider != null) {
            return provider;
        }
        return createProvider();
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
        return StringUtils.defaultString(sourceDocumentModificationDatePropertyName,
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

    /** @since 11.5 */
    public RenditionProvider initProvider() {
        provider = createProvider();
        return provider;
    }

}
