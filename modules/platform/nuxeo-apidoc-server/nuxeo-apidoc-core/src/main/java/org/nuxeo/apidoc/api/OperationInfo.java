/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.apidoc.api;

import java.util.List;

import org.nuxeo.ecm.automation.OperationDocumentation.Param;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Information about an operation
 */
public interface OperationInfo extends NuxeoArtifact, Comparable<OperationInfo> {

    String TYPE_NAME = "NXOperation";

    /** Prefix for {@link #getId}. */
    String ARTIFACT_PREFIX = "op:";

    String PROP_NAME = "nxop:name";

    String PROP_ALIASES = "nxop:aliases";

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

    String PROP_OP_CLASS = "operationClass";

    String PROP_CONTRIBUTING_COMPONENT = "contributingComponent";

    String BUILT_IN = "BuiltIn";

    @Override
    @JsonIgnore
    String getId();

    /**
     * Actual operation id. ({@link #getId} is prefixed with {@link #ARTIFACT_PREFIX})
     */
    String getName();

    String[] getAliases();

    String getUrl();

    String[] getSignature();

    String getCategory();

    String getLabel();

    String getRequires();

    String getSince();

    String getDescription();

    List<Param> getParams();

    String getOperationClass();

    String getContributingComponent();
}
