/*
 * (C) Copyright 2011-2018 Nuxeo (http://nuxeo.com/) and others.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.apidoc.documentation.DocumentationComponent;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;

public abstract class AbstractDocumentationItem implements DocumentationItem {

    protected static final Log log = LogFactory.getLog(AbstractDocumentationItem.class);

    protected AbstractDocumentationItem(String typeLabel) {
        this.typeLabel = typeLabel;
    }

    final String typeLabel;

    @Override
    public int compareTo(DocumentationItem o) {

        List<String> myVersions = new ArrayList<>(getApplicableVersion());
        List<String> otherVersions = new ArrayList<>(o.getApplicableVersion());

        Collections.sort(myVersions);
        Collections.sort(otherVersions);
        Collections.reverse(myVersions);
        Collections.reverse(otherVersions);

        if (myVersions.isEmpty()) {
            if (otherVersions.isEmpty()) {
                return 0;
            }
            return 1;
        } else if (otherVersions.isEmpty()) {
            return -1;
        }

        return myVersions.get(0).compareTo(otherVersions.get(0));
    }

    @Override
    public String getTypeLabel() {
        return typeLabel;
    }

    public static String typeLabelOf(String type) {
        if (StringUtils.isBlank(type)) {
            return "";
        }

        DirectoryService dm = Framework.getService(DirectoryService.class);
        try (Session session = dm.open(DocumentationComponent.DIRECTORY_NAME)) {
            DocumentModel entry = session.getEntry(type);
            if (entry != null) {
                return (String) entry.getProperty("vocabulary", "label");
            }
        } catch (DirectoryException | PropertyException e) {
            log.error("Error while resolving typeLabel", e);
        }
        return type;
    }

}
