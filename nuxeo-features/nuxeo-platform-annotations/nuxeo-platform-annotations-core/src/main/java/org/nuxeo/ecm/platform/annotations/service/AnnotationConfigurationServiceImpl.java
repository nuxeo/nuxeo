/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.service;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.platform.annotations.api.AnnotationException;
import org.nuxeo.ecm.platform.annotations.api.AnnotationsConstants;
import org.nuxeo.ecm.platform.annotations.api.UriResolver;
import org.nuxeo.ecm.platform.annotations.descriptors.PermissionMapperDescriptor;

/**
 * @author Alexandre Russel
 *
 */
public class AnnotationConfigurationServiceImpl implements
        AnnotationConfigurationService {

    private final List<EventListener> listeners = new ArrayList<EventListener>();

    private UriResolver resolver;

    private URLPatternFilter filter;

    private MetadataMapper mapper;

    private PermissionManager permissionManager;

    private AnnotabilityManager annotabilityManager;

    private AnnotationIDGenerator idGenerator;

    private String baseUrl = AnnotationsConstants.annotationBaseUrl;

    private PermissionMapperDescriptor permissionMapper;

    public UriResolver getUriResolver() {
        return resolver;
    }

    public void setUriResolver(UriResolver resolver) {
        this.resolver = resolver;
    }

    public URLPatternFilter getUrlPatternFilter() {
        return filter;
    }

    public void setFilter(URLPatternFilter filter) {
        this.filter = filter;
    }

    public MetadataMapper getMetadataMapper() {
        return mapper;
    }

    public void setMetadataMapper(MetadataMapper mapper) {
        this.mapper = mapper;
    }

    public PermissionManager getPermissionManager() {
        return permissionManager;
    }

    public void setPermissionManager(PermissionManager permissionManager) {
        this.permissionManager = permissionManager;
    }

    public AnnotabilityManager getAnnotabilityManager() {
        return annotabilityManager;
    }

    public void setAnnotabilityManager(AnnotabilityManager annotabilityManager) {
        this.annotabilityManager = annotabilityManager;
    }

    public AnnotationIDGenerator getIDGenerator() {
        return idGenerator;
    }

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

    public void setPermissionMapper(PermissionMapperDescriptor permissionMapper) {
        this.permissionMapper = permissionMapper;
    }

    public List<EventListener> getListeners() {
        return listeners;
    }

    public void addListener(EventListener listener) {
        listeners.add(listener);
    }

    public String getCreateAnnotationPermission() throws AnnotationException {
        return permissionMapper.getCreateAnnotationValue();
    }

    public String getDeleteAnnotationPermission() throws AnnotationException {
        return permissionMapper.getDeleteAnnotationValue();
    }

    public String getReadAnnotationPermission() throws AnnotationException {
        return permissionMapper.getReadAnnotationValue();
    }

    public String getUpdateAnnotationPermission() throws AnnotationException {
        return permissionMapper.getUpdateAnnotationValue();
    }

}
