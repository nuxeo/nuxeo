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

import java.util.List;

import org.nuxeo.ecm.platform.annotations.api.UriResolver;
import org.nuxeo.ecm.platform.annotations.descriptors.PermissionMapperDescriptor;

/**
 * @author Alexandre Russel
 */
public interface AnnotationConfigurationService {

    void setUriResolver(UriResolver resolver);

    UriResolver getUriResolver();

    void setFilter(URLPatternFilter filter);

    URLPatternFilter getUrlPatternFilter();

    void setMetadataMapper(MetadataMapper mapper);

    MetadataMapper getMetadataMapper();

    void setPermissionManager(PermissionManager manager);

    PermissionManager getPermissionManager();

    void setAnnotabilityManager(AnnotabilityManager annotabilityManager);

    AnnotabilityManager getAnnotabilityManager();

    void addListener(EventListener listener);

    List<EventListener> getListeners();

    void setIDGenerator(AnnotationIDGenerator generator);

    AnnotationIDGenerator getIDGenerator();

    void setPermissionMapper(PermissionMapperDescriptor contribution);

    String getCreateAnnotationPermission();

    String getDeleteAnnotationPermission();

    String getReadAnnotationPermission();

    String getUpdateAnnotationPermission();
}
