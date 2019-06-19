/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.service;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.platform.annotations.api.AnnotationsConstants;
import org.nuxeo.ecm.platform.annotations.api.UriResolver;
import org.nuxeo.ecm.platform.annotations.descriptors.PermissionMapperDescriptor;

/**
 * @author Alexandre Russel
 */
public class AnnotationConfigurationServiceImpl implements AnnotationConfigurationService {

    private final List<EventListener> listeners = new ArrayList<>();

    private UriResolver resolver;

    private URLPatternFilter filter;

    private MetadataMapper mapper;

    private PermissionManager permissionManager;

    private AnnotabilityManager annotabilityManager;

    private AnnotationIDGenerator idGenerator;

    private String baseUrl = AnnotationsConstants.annotationBaseUrl;

    private PermissionMapperDescriptor permissionMapper;

    @Override
    public UriResolver getUriResolver() {
        return resolver;
    }

    @Override
    public void setUriResolver(UriResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public URLPatternFilter getUrlPatternFilter() {
        return filter;
    }

    @Override
    public void setFilter(URLPatternFilter filter) {
        this.filter = filter;
    }

    @Override
    public MetadataMapper getMetadataMapper() {
        return mapper;
    }

    @Override
    public void setMetadataMapper(MetadataMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public PermissionManager getPermissionManager() {
        return permissionManager;
    }

    @Override
    public void setPermissionManager(PermissionManager permissionManager) {
        this.permissionManager = permissionManager;
    }

    @Override
    public AnnotabilityManager getAnnotabilityManager() {
        return annotabilityManager;
    }

    @Override
    public void setAnnotabilityManager(AnnotabilityManager annotabilityManager) {
        this.annotabilityManager = annotabilityManager;
    }

    @Override
    public AnnotationIDGenerator getIDGenerator() {
        return idGenerator;
    }

    @Override
    public void setIDGenerator(AnnotationIDGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public PermissionMapperDescriptor getPermissionMapper() {
        return permissionMapper;
    }

    @Override
    public void setPermissionMapper(PermissionMapperDescriptor permissionMapper) {
        this.permissionMapper = permissionMapper;
    }

    @Override
    public List<EventListener> getListeners() {
        return listeners;
    }

    @Override
    public void addListener(EventListener listener) {
        listeners.add(listener);
    }

    @Override
    public String getCreateAnnotationPermission() {
        return permissionMapper.getCreateAnnotationValue();
    }

    @Override
    public String getDeleteAnnotationPermission() {
        return permissionMapper.getDeleteAnnotationValue();
    }

    @Override
    public String getReadAnnotationPermission() {
        return permissionMapper.getReadAnnotationValue();
    }

    @Override
    public String getUpdateAnnotationPermission() {
        return permissionMapper.getUpdateAnnotationValue();
    }

}
