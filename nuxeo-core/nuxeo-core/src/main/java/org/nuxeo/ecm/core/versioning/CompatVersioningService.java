/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.versioning;

import java.io.Serializable;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.model.Document;

/**
 * Compatibility implementation of the versioning service in Nuxeo.
 *
 * @deprecated since 9.3, seems not needed anymore
 */
@Deprecated
public class CompatVersioningService extends StandardVersioningService {

    private static final Log log = LogFactory.getLog(CompatVersioningService.class);

    @Override
    public String getVersionLabel(DocumentModel doc) {
        try {
            return getMajor(doc) + "." + getMinor(doc);
        } catch (PropertyNotFoundException e) {
            return "";
        }
    }

    @Override
    protected void setInitialVersion(Document doc) {
        setVersion(doc, 1, 0);
    }

    @Override
    public boolean isPreSaveDoingCheckOut(Document doc, boolean isDirty, VersioningOption option,
            Map<String, Serializable> options) {
        option = validateOption(doc, option);
        boolean increment = option != VersioningOption.NONE;
        return increment || (isDirty && !doc.isCheckedOut());
    }

    /*
     * Create a pre-save snapshot, and re-checkout the document if there's a pending save or we want to increment the
     * version.
     */
    @Override
    public VersioningOption doPreSave(CoreSession session, Document doc, boolean isDirty, VersioningOption option,
            String checkinComment, Map<String, Serializable> options) {
        option = validateOption(doc, option);
        boolean increment = option != VersioningOption.NONE;
        if (increment) {
            if (doc.isCheckedOut()) {
                doc.checkIn(null, checkinComment); // auto-label
            }
        }
        if (!doc.isCheckedOut() && (isDirty || increment)) {
            doc.checkOut();
        }
        return option;
    }

    @Override
    public Document doPostSave(CoreSession session, Document doc, VersioningOption option, String checkinComment,
            Map<String, Serializable> options) {
        if (!doc.isCheckedOut()) {
            return null;
        }
        // option = validateOption(doc, option);
        incrementByOption(doc, option);
        followTransitionByOption(null, doc, options);
        return null;
    }

    @Override
    public Document doCheckIn(Document doc, VersioningOption option, String checkinComment) {
        return doc.checkIn(null, checkinComment); // auto-label
    }

    @Override
    public void doCheckOut(Document doc) {
        Document base = doc.getBaseVersion();
        doc.checkOut();
        // set version number to that of the last version + inc minor
        Document last;
        if (base.isLatestVersion()) {
            last = base;
        } else {
            last = doc.getLastVersion();
        }
        if (last != null) {
            try {
                setVersion(doc, getMajor(last), getMinor(last) + 1);
            } catch (PropertyNotFoundException e) {
                // ignore
            }
        }
    }

}
