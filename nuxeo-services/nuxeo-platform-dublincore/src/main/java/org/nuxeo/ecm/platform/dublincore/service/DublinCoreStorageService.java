/*
 * (C) Copyright 2006-2018 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.dublincore.service;

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.SYSTEM_USERNAME;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.SystemPrincipal;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Service that writes Metadata.
 *
 * @author <a href="td@nuxeo.com">Thierry Delprat</a>
 */
public class DublinCoreStorageService extends DefaultComponent {

    public static Log log = LogFactory.getLog(DublinCoreStorageService.class);

    public static final String ID = "DublinCoreStorageService";

    public void setCreationDate(DocumentModel doc, Calendar creationDate, Event event) {
        doc.setProperty("dublincore", "created", creationDate);
        addContributor(doc, event);
    }

    public void setModificationDate(DocumentModel doc, Calendar modificationDate, Event event) {
        doc.setProperty("dublincore", "modified", modificationDate);
        if (doc.getProperty("dublincore", "created") == null) {
            setCreationDate(doc, modificationDate, event);
        }
    }

    public void addContributor(DocumentModel doc, Event event) {
        Principal principal = event.getContext().getPrincipal();
        if (principal == null) {
            return;
        }

        String principalName = principal.getName();
        if (principal instanceof SystemPrincipal) {
            SystemPrincipal nxp = (SystemPrincipal) principal;
            String originatingUser = nxp.getActingUser();
            if (SYSTEM_USERNAME.equals(originatingUser) && !DOCUMENT_CREATED.equals(event.getName())) {
                return;
            } else {
                principalName = originatingUser;
            }
        }

        if (doc.getProperty("dublincore", "creator") == null) {
            // First time only => creator
            doc.setProperty("dublincore", "creator", principalName);
        }

        List<String> contributors = getSanitizedExistingContributors(doc);
        if (!contributors.contains(principalName)) {
            contributors.add(principalName);
        }
        doc.setProperty("dublincore", "contributors", contributors);
        doc.setProperty("dublincore", "lastContributor", principalName);
    }

    public void setIssuedDate(DocumentModel doc, Calendar issuedDate) {
        doc.setPropertyValue("dc:issued", issuedDate);
    }

    /**
     * Returns a "Sanitized" list of contributors according to NXP-25005
     *
     * @param doc The document from which the contributors list will be retrieved.
     * @return A list of contributors without repetitions and prefixed entries.
     */
    protected List<String> getSanitizedExistingContributors(DocumentModel doc) {
        String[] contributorsArray = (String[]) doc.getProperty("dublincore", "contributors");
        if (ArrayUtils.isEmpty(contributorsArray)) {
            return new ArrayList<>();
        }
        return Arrays.stream(contributorsArray)
                .map(DublinCoreStorageService::stripPrincipalPrefix)
                .distinct()
                .collect(Collectors.toList());
    }

    protected static String stripPrincipalPrefix(String principal) {
        if (principal.startsWith(NuxeoPrincipal.PREFIX)) {
            return principal.substring(NuxeoPrincipal.PREFIX.length());
        } else {
            return principal;
        }
    }

}
