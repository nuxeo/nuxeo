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

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.dublincore.constants.DublinCoreConstants;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
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

    String getArtifactType();

    String getHierarchyPath();

}
