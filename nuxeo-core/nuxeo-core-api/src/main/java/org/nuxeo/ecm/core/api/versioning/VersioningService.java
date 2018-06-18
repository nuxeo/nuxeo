/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.api.versioning;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.model.Document;

/**
 * The versioning service holds the versioning policy used to define what happens to a document's version when it is
 * created, saved, checked in, checked out or restored, and what version increment options (none, minor, major) are made
 * available to the user.
 *
 * @since 5.4
 */
public interface VersioningService {

    /** Document property in which the major version is stored. */
    String MAJOR_VERSION_PROP = "uid:major_version";

    /** Document property in which the minor version is stored. */
    String MINOR_VERSION_PROP = "uid:minor_version";

    /**
     * Context data that can be used to skip versioning on document creation, in case the supplied version is enough.
     */
    String SKIP_VERSIONING = "SKIP_VERSIONING";

    /**
     * Context data to provide a user-level choice to the versioning policy. Value is a {@link VersioningOption}.
     */
    String VERSIONING_OPTION = "VersioningOption";

    /**
     * Context data to disable auto-checkout of checked-in documents at pre-save time. This option should only be used
     * when updating a facet that's considered pure metadata and holds information about the document but external to
     * it. Value is a {@link Boolean}.
     *
     * @since 5.7, 5.6.0-HF09
     */
    String DISABLE_AUTO_CHECKOUT = "DisableAutoCheckOut";

    /**
     * Context data to provide a checkin comment for operations that potentially check in (save, publish, checkin).
     */
    String CHECKIN_COMMENT = "CheckinComment";

    /**
     * Gets the version label to display for a given document.
     *
     * @param doc the document
     * @return the version label, like {@code "2.1"}
     */
    String getVersionLabel(DocumentModel doc);

    /**
     * Checks what options are available on a document at save time.
     *
     * @param doc the document
     * @return the options, the first being the default
     */
    List<VersioningOption> getSaveOptions(DocumentModel doc);

    /**
     * Applies versioning after document creation.
     *
     * @param doc the document
     * @param options map event info
     */
    void doPostCreate(Document doc, Map<String, Serializable> options);

    /**
     * Checks if {@link #doPreSave} will do a checkout when called with the same arguments.
     * <p>
     * Needed to be able to send "about to checkin" events.
     *
     * @param doc the document
     * @param isDirty {@code true} if there is actual data to save
     * @param option an option chosen by the user or framework
     * @param options map event info and options
     * @return {@code true} if {@link #doPreSave} will do a checkout
     */
    boolean isPreSaveDoingCheckOut(Document doc, boolean isDirty, VersioningOption option,
            Map<String, Serializable> options);

    /**
     * Applies versioning options before document save.
     *
     * @param session the core session
     * @param doc the document
     * @param isDirty {@code true} if there is actual data to save
     * @param option an option chosen by the user or framework
     * @param checkinComment a checkin comment
     * @param options map event info
     * @return the validated option (to use in doPostSave)
     * @since 9.3
     */
    VersioningOption doPreSave(CoreSession session, Document doc, boolean isDirty, VersioningOption option, String checkinComment,
            Map<String, Serializable> options);

    /**
     * Checks if {@link #doPostSave} will do a checkin when called with the same arguments.
     *
     * @param doc the document
     * @param option an option chosen by the user or framework
     * @param options map event info
     * @return {@code true} if {@link #doPostSave} will do a checkin
     */
    boolean isPostSaveDoingCheckIn(Document doc, VersioningOption option, Map<String, Serializable> options);

    /**
     * Applies versioning options after document save. If a new version is checked in during the operation, the document
     * for this version is returned to the caller.
     *
     * @param session the core session
     * @param doc the document
     * @param option an option chosen by the user or framework
     * @param checkinComment a checkin comment
     * @param options map event info
     * @return checkedInDocument or null
     * @since 9.3
     */
    Document doPostSave(CoreSession session, Document doc, VersioningOption option, String checkinComment,
            Map<String, Serializable> options);

    /**
     * Applies version increment option and does a checkin.
     *
     * @param doc the document
     * @param option an option chosen by the user or framework
     * @param checkinComment a checkin comment
     * @return the version
     */
    Document doCheckIn(Document doc, VersioningOption option, String checkinComment);

    /**
     * Apply modifications after doing a checkout.
     *
     * @param doc the document
     */
    void doCheckOut(Document doc);

    /**
     * Does automatic versioning if a policy exists for the current input context. Currently automatic versioning is
     * either before or after document update, never both.
     *
     * @param before the flag to trigger a before or after automatic versioning (used to retrieve the right policy)
     * @since 9.1
     */
    void doAutomaticVersioning(DocumentModel previousDocument, DocumentModel currentDocument, boolean before);

}
