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
 *     troger
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.configuration.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.platform.annotations.gwt.server.configuration.UserInfoMapper;
import org.nuxeo.ecm.platform.annotations.gwt.server.configuration.WebPermission;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @author <a href="mailto:qlamerand@nuxeo.com">Quentin Lamerand</a>
 *
 */
public interface WebAnnotationConfigurationService {

    List<WebAnnotationDefinitionDescriptor> getAllWebAnnotationDefinitions();

    List<WebAnnotationDefinitionDescriptor> getEnabledWebAnnotationDefinitions();

    UserInfoMapper getUserInfoMapper();

    WebPermission getWebPermission();

    Map<String, FilterDescriptor> getFilterDefinitions();

    Set<String> getDisplayedFields();

    Map<String, String> getFieldLabels();

}
