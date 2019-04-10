/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.apidoc.api;

import java.util.List;

import org.nuxeo.ecm.automation.OperationDocumentation.Param;

/**
 * Information about an operation
 */
public interface OperationInfo extends NuxeoArtifact, Comparable<OperationInfo> {

    String TYPE_NAME = "NXOperation";

    /** Prefix for {@link #getId}. */
    String ARTIFACT_PREFIX = "op:";

    String PROP_NAME = "nxop:name";

    String PROP_VERSION = "nxop:version";

    String PROP_DESCRIPTION = "nxop:description";

    String PROP_SIGNATURE = "nxop:signature";

    String PROP_CATEGORY = "nxop:category";

    String PROP_URL = "nxop:url";

    String PROP_LABEL = "nxop:label";

    String PROP_REQUIRES = "nxop:requires";

    String PROP_SINCE = "nxop:since";

    String PROP_PARAMS = "nxop:params";

    String PROP_PARAM_NAME = "name";

    String PROP_PARAM_TYPE = "type";

    String PROP_PARAM_WIDGET = "widget";

    String PROP_PARAM_VALUES = "values";

    String PROP_PARAM_REQUIRED = "required";

    String PROP_PARAM_ORDER = "order";

    /**
     * Actual operation id. ({@link #getId} is prefixed with
     * {@link #ARTIFACT_PREFIX})
     */
    String getName();

    String getUrl();

    String[] getSignature();

    String getCategory();

    String getLabel();

    String getRequires();

    String getSince();

    String getDescription();

    List<Param> getParams();

}
