/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.apidoc.api;

import java.util.List;
import java.util.Map;

public interface DocumentationItem {

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
     * Gets versions
     */
    List<String> getApplicableVersion();

    /**
     * Gets identifier of the target documented artifact
     */
    String getTarget();

    /**
     * Gets the Type of the target documented artifact (NXBundle, NXComponent ...)
     */
    String getTargetType();

    /**
     * Indicates if documentation has been validated by Nuxeo
     */
    boolean isApproved();

    /**
     * Local documentation identifier
     */
    String getId();

    /**
     * UUID of the underlying DocumentModel
     */
    String getUUID();

    /**
     * Returns attachments
     */
    Map<String, String> getAttachments();

}
