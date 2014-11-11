/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.versioning;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.model.Document;

/**
 * The versioning service holds the versioning policy used to define what
 * happens to a document's version when it is created, saved, checked in,
 * checked out or restored, and what version increment options (none, minor,
 * major) are made available to the user.
 *
 * @since 5.4
 */
public interface VersioningService {

    /** Document property in which the major version is stored. */
    String MAJOR_VERSION_PROP = "uid:major_version";

    /** Document property in which the minor version is stored. */
    String MINOR_VERSION_PROP = "uid:minor_version";

    /**
     * Context data that can be used to skip versioning on document creation, in
     * case the supplied version is enough.
     */
    String SKIP_VERSIONING = "SKIP_VERSIONING";

    /**
     * Context data to provide a user-level choice to the versioning policy.
     * Value is a {@link VersioningOption}.
     */
    String VERSIONING_OPTION = "VersioningOption";

    /**
     * Context data to disable auto-checkout of checked-in documents at pre-save
     * time. This option should only be used when updating a facet that's
     * considered pure metadata and holds information about the document but
     * external to it. Value is a {@link Boolean}.
     *
     * @since 5.7, 5.6.0-HF09
     */
    String DISABLE_AUTO_CHECKOUT = "DisableAutoCheckOut";

    /**
     * Context data to provide a checkin comment for operations that potentially
     * check in (save, publish, checkin).
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
    List<VersioningOption> getSaveOptions(DocumentModel doc)
            throws ClientException;

    /**
     * Applies versioning after document creation.
     *
     * @param doc the document
     * @param context map event info
     */
    void doPostCreate(Document doc, Map<String, Serializable> options)
            throws DocumentException;

    /**
     * Checks if {@link #doPreSave} will do a checkout when called with the
     * same arguments.
     * <p>
     * Needed to be able to send "about to checkin" events.
     *
     * @param doc the document
     * @param isDirty {@code true} if there is actual data to save
     * @param option an option chosen by the user or framework
     * @param options map event info and options
     * @return {@code true} if {@link #doPreSave} will do a checkout
     * @throws DocumentException
     */
    boolean isPreSaveDoingCheckOut(Document doc, boolean isDirty,
            VersioningOption option, Map<String, Serializable> options)
            throws DocumentException;

    /**
     * Applies versioning options before document save.
     *
     * @param doc the document
     * @param isDirty {@code true} if there is actual data to save
     * @param option an option chosen by the user or framework
     * @param checkinComment a checkin comment
     * @param context map event info
     * @return the validated option (to use in doPostSave)
     */
    VersioningOption doPreSave(Document doc, boolean isDirty,
            VersioningOption option, String checkinComment,
            Map<String, Serializable> options) throws DocumentException;

    /**
     * Checks if {@link #doPostSave} will do a checkin when called with the same arguments.
     *
     * @param doc the document
     * @param option an option chosen by the user or framework
     * @param options map event info
     * @return {@code true} if {@link #doPostSave} will do a checkin
     * @throws DocumentException
     */
    boolean isPostSaveDoingCheckIn(Document doc, VersioningOption option,
            Map<String, Serializable> options) throws DocumentException;

    /**
     * Applies versioning options after document save. If a new version is
     * checked in during the operation, the document for this version is
     * returned to the caller.
     *
     * @param doc the document
     * @param option an option chosen by the user or framework
     * @param checkinComment a checkin comment
     * @param context map event info
     * @return checkedInDocument or null
     */
    Document doPostSave(Document doc, VersioningOption option,
            String checkinComment, Map<String, Serializable> options)
            throws DocumentException;

    /**
     * Applies version increment option and does a checkin.
     *
     * @param doc the document
     * @param option an option chosen by the user or framework
     * @param checkinComment a checkin comment
     * @return the version
     */
    Document doCheckIn(Document doc, VersioningOption option,
            String checkinComment) throws DocumentException;

    /**
     * Apply modifications after doing a checkout.
     *
     * @param doc the document
     */
    void doCheckOut(Document doc) throws DocumentException;

}
