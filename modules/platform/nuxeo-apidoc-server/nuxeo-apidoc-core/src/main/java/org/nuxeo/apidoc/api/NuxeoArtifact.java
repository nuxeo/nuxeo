/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.apidoc.api;

import org.nuxeo.apidoc.introspection.BundleGroupImpl;
import org.nuxeo.apidoc.introspection.BundleInfoImpl;
import org.nuxeo.apidoc.introspection.ComponentInfoImpl;
import org.nuxeo.apidoc.introspection.ExtensionInfoImpl;
import org.nuxeo.apidoc.introspection.ExtensionPointInfoImpl;
import org.nuxeo.apidoc.introspection.OperationInfoImpl;
import org.nuxeo.apidoc.introspection.ServiceInfoImpl;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.dublincore.constants.DublinCoreConstants;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@type")
@JsonSubTypes({ //
        @JsonSubTypes.Type(value = BundleGroupImpl.class, name = BundleGroup.TYPE_NAME),
        @JsonSubTypes.Type(value = BundleInfoImpl.class, name = BundleInfo.TYPE_NAME),
        @JsonSubTypes.Type(value = ComponentInfoImpl.class, name = ComponentInfo.TYPE_NAME),
        @JsonSubTypes.Type(value = ServiceInfoImpl.class, name = ServiceInfo.TYPE_NAME),
        @JsonSubTypes.Type(value = ExtensionPointInfoImpl.class, name = ExtensionPointInfo.TYPE_NAME),
        @JsonSubTypes.Type(value = ExtensionInfoImpl.class, name = ExtensionInfo.TYPE_NAME),
        @JsonSubTypes.Type(value = OperationInfoImpl.class, name = OperationInfo.TYPE_NAME), //
})
public interface NuxeoArtifact {

    /**
     * @since 11.1
     */
    public static String TITLE_PROPERTY_PATH = DublinCoreConstants.DUBLINCORE_TITLE_PROPERTY;

    /**
     * @since 11.1
     */
    public static String CONTENT_PROPERTY_PATH = "file:content";

    String getId();

    AssociatedDocuments getAssociatedDocuments(CoreSession session);

    String getVersion();

    @JsonIgnore // already held by type, see annotation on class
    String getArtifactType();

    String getHierarchyPath();

}
