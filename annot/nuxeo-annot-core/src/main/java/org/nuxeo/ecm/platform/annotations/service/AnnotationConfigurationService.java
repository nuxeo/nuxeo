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

import java.util.List;

import org.nuxeo.ecm.platform.annotations.api.AnnotationException;
import org.nuxeo.ecm.platform.annotations.api.UriResolver;
import org.nuxeo.ecm.platform.annotations.descriptors.PermissionMapperDescriptor;

/**
 * @author Alexandre Russel
 *
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

    String getCreateAnnotationPermission() throws AnnotationException;

    String getDeleteAnnotationPermission() throws AnnotationException;

    String getReadAnnotationPermission() throws AnnotationException;

    String getUpdateAnnotationPermission() throws AnnotationException;
}
