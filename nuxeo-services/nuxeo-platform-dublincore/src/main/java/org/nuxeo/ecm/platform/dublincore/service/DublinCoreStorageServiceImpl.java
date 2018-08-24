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
 *     Thierry Delprat (td@nuxeo.com)
 *     Nuno Cunha (ncunha@nuxeo.com)
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.dublincore.service;

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.ABOUT_TO_CREATE;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.SYSTEM_USERNAME;
import static org.nuxeo.ecm.platform.dublincore.constants.DublinCoreConstants.DUBLINCORE_CONTRIBUTORS_PROPERTY;
import static org.nuxeo.ecm.platform.dublincore.constants.DublinCoreConstants.DUBLINCORE_CREATED_DATE_PROPERTY;
import static org.nuxeo.ecm.platform.dublincore.constants.DublinCoreConstants.DUBLINCORE_CREATOR_PROPERTY;
import static org.nuxeo.ecm.platform.dublincore.constants.DublinCoreConstants.DUBLINCORE_ISSUED_DATE_PROPERTY;
import static org.nuxeo.ecm.platform.dublincore.constants.DublinCoreConstants.DUBLINCORE_LAST_CONTRIBUTOR_PROPERTY;
import static org.nuxeo.ecm.platform.dublincore.constants.DublinCoreConstants.DUBLINCORE_MODIFIED_DATE_PROPERTY;

import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.SystemPrincipal;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * DublinCore Storage Service Implementation.
 *
 * @since 10.2
 */
public class DublinCoreStorageServiceImpl extends DefaultComponent implements DublinCoreStorageService {

    @Override
    public void setCreationDate(DocumentModel doc, Calendar creationDate) {
        doc.setPropertyValue(DUBLINCORE_CREATED_DATE_PROPERTY, creationDate);
    }

    @Override
    public void setCreationDate(DocumentModel doc, Calendar creationDate, Event event) {
        setCreationDate(doc, creationDate);
    }

    @Override
    public void setIssuedDate(DocumentModel doc, Calendar issuedDate) {
        doc.setPropertyValue(DUBLINCORE_ISSUED_DATE_PROPERTY, issuedDate);
    }

    @Override
    public void setModificationDate(DocumentModel doc, Calendar modificationDate) {
        doc.setPropertyValue(DUBLINCORE_MODIFIED_DATE_PROPERTY, modificationDate);
        if (doc.getPropertyValue(DUBLINCORE_CREATED_DATE_PROPERTY) == null) {
            setCreationDate(doc, modificationDate);
        }
    }

    @Override
    public void setModificationDate(DocumentModel doc, Calendar modificationDate, Event event) {
        setModificationDate(doc, modificationDate);
    }

    @Override
    public void addContributor(DocumentModel doc, Event event) {
        Principal principal = Objects.requireNonNull(event.getContext().getPrincipal());

        String principalName = principal.getName();
        if (principal instanceof SystemPrincipal) {
            principalName = ((SystemPrincipal) principal).getActingUser();
            if (SYSTEM_USERNAME.equals(principalName) && !ABOUT_TO_CREATE.equals(event.getName())) {
                return;
            }
        }

        if (doc.getPropertyValue(DUBLINCORE_CREATOR_PROPERTY) == null) {
            doc.setPropertyValue(DUBLINCORE_CREATOR_PROPERTY, principalName);
        }

        List<String> contributorsList = getSanitizedExistingContributors(doc);
        if (!contributorsList.contains(principalName)) {
            contributorsList.add(principalName);
        }
        doc.setPropertyValue(DUBLINCORE_CONTRIBUTORS_PROPERTY, (Serializable) contributorsList);
        doc.setPropertyValue(DUBLINCORE_LAST_CONTRIBUTOR_PROPERTY, principalName);
    }

    /**
     * Returns a "Sanitized" list of contributors according to NXP-25005
     *
     * @param doc The document from which the contributors list will be retrieved.
     * @return A list of contributors without repetitions and prefixed entries.
     */
    protected List<String> getSanitizedExistingContributors(DocumentModel doc) {
        String[] contributorsArray = (String[]) doc.getPropertyValue(DUBLINCORE_CONTRIBUTORS_PROPERTY);
        if (ArrayUtils.isEmpty(contributorsArray)) {
            return new ArrayList<>();
        }
        return Arrays.stream(contributorsArray)
                     .map(DublinCoreStorageServiceImpl::stripPrincipalPrefix)
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
