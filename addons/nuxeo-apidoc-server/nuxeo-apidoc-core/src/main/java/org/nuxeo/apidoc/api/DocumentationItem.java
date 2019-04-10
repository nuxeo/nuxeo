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

import java.util.List;
import java.util.Map;

public interface DocumentationItem extends Comparable<DocumentationItem> {

    String TYPE_NAME = "NXDocumentation";

    String PROP_TARGET = "nxdoc:target";

    String PROP_TARGET_TYPE = "nxdoc:targetType";

    String PROP_APPLICABLE_VERSIONS = "nxdoc:applicableVersions";

    String PROP_DOCUMENTATION_ID = "nxdoc:documentationId";

    String PROP_NUXEO_APPROVED = "nxdoc:nuxeoApproved";

    String PROP_TYPE = "nxdoc:type";

    String PROP_RENDERING_TYPE = "nxdoc:renderingType";

    /**
     * Gets Title of the Documentation
     */
    String getTitle();

    /**
     * Gets the main content of the Documentation
     */
    String getContent();

    /**
     * Gets the type of the documentation (description, how-to, samples ...)
     */
    String getType();

    /**
     * Gets the output rendering format for the content (wiki, html ...)
     */
    String getRenderingType();

    /**
     * Gets label for the documentation type
     */
    String getTypeLabel();

    /**
     * Gets versions.
     */
    List<String> getApplicableVersion();

    /**
     * Gets identifier of the target documented artifact
     */
    String getTarget();

    /**
     * Gets the Type of the target documented artifact (NXBundle, NXComponent ...).
     */
    String getTargetType();

    /**
     * Indicates if documentation has been validated by Nuxeo.
     */
    boolean isApproved();

    /**
     * Local documentation identifier.
     */
    String getId();

    /**
     * UUID of the underlying DocumentModel.
     */
    String getUUID();

    /**
     * Returns attachments.
     */
    Map<String, String> getAttachments();

    /**
     * Return true if item is a placeholder automatically generated.
     */
    boolean isPlaceHolder();

    /**
     * Return the id used by quick editor (can contains a real UUID for existing doc or an artifact id for new one).
     */
    String getEditId();

    boolean isReadOnly();

}
