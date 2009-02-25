/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.webapp.helpers;

import static org.jboss.seam.ScopeType.APPLICATION;
import static org.jboss.seam.annotations.Install.FRAMEWORK;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Startup;
import org.jboss.seam.core.Events;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Knows what events need to be raised based on the user selected document.
 *
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 */
@Name("eventManager")
@Scope(APPLICATION)
@Startup
@Install(precedence=FRAMEWORK)
public class EventManager implements Serializable {

    private static final long serialVersionUID = -7572053704069819975L;

    private static final Log log = LogFactory.getLog(EventManager.class);

    /**
     * Raises events on going home, will be processed immediately.
     *
     * @return events fired
     */
    public static List<String> raiseEventsOnGoingHome() {
        List<String> eventsFired = new ArrayList<String>();

        Events evtManager = Events.instance();

        log.debug("Fire Event: " + EventNames.LOCATION_SELECTION_CHANGED);
        evtManager.raiseEvent(EventNames.LOCATION_SELECTION_CHANGED);
        eventsFired.add(EventNames.LOCATION_SELECTION_CHANGED);

        log.debug("Fire Event: " + EventNames.GO_HOME);
        evtManager.raiseEvent(EventNames.GO_HOME);
        eventsFired.add(EventNames.GO_HOME);

        return eventsFired;
    }

    /**
     * Raises events on location selection change, will be processed immediately.
     *
     * @return events fired
     */
    public static List<String> raiseEventsOnLocationSelectionChanged() {
        List<String> eventsFired = new ArrayList<String>();

        Events evtManager = Events.instance();

        log.debug("Fire Event: " + EventNames.LOCATION_SELECTION_CHANGED);
        evtManager.raiseEvent(EventNames.LOCATION_SELECTION_CHANGED);
        eventsFired.add(EventNames.LOCATION_SELECTION_CHANGED);

        log.debug("Fire Event: "
                + EventNames.USER_ALL_DOCUMENT_TYPES_SELECTION_CHANGED);
        evtManager.raiseEvent(
                EventNames.USER_ALL_DOCUMENT_TYPES_SELECTION_CHANGED);
        eventsFired.add(EventNames.USER_ALL_DOCUMENT_TYPES_SELECTION_CHANGED);

        return eventsFired;
    }

    /**
     * Fires the necessary events so that the nuxeo infrastructure components get
     * updated. The raised events will be processed immediately, before this
     * call is ended. Intended to be used when a document gets selected. If the
     * docType is NULL then the GO_HOME event is fired.
     *
     * @return events fired
     */
    public static List<String> raiseEventsOnDocumentSelected(DocumentModel document) {
        List<String> eventsFired = new ArrayList<String>();

        if (document == null) {
            // XXX AT: kind of BBB, not sure why this was used like this
            eventsFired = raiseEventsOnLocationSelectionChanged();
        } else {
            Events evtManager = Events.instance();

            String docType = document.getType();
            String eventName;

            if ("Domain".equals(docType)) {
                eventName = EventNames.DOMAIN_SELECTION_CHANGED;
            } else if ("Root".equals(docType)) {
                eventName = EventNames.GO_HOME;
            } else if ("WorkspaceRoot".equals(docType)
                    || "SectionRoot".equals(docType)) {
                eventName = EventNames.CONTENT_ROOT_SELECTION_CHANGED;
            } else {
                // regular document is selected
                eventName = EventNames.DOCUMENT_SELECTION_CHANGED;
            }

            if (document.isFolder()) {
                evtManager.raiseEvent(
                        EventNames.FOLDERISHDOCUMENT_SELECTION_CHANGED,
                        document);
            }

            log.debug("Fire Event: " + eventName);
            evtManager.raiseEvent(eventName, document);
            eventsFired.add(eventName);

            log.debug("Fire Event: "
                    + EventNames.USER_ALL_DOCUMENT_TYPES_SELECTION_CHANGED);
            evtManager.raiseEvent(
                    EventNames.USER_ALL_DOCUMENT_TYPES_SELECTION_CHANGED);
            eventsFired.add(EventNames.USER_ALL_DOCUMENT_TYPES_SELECTION_CHANGED);
        }

        return eventsFired;
    }

    /**
     * Fires the necessary events so that the nuxeo infrastructure components get
     * updated. The raised events will be processed immediately, before this
     * call is ended. Intended to be used when a document gets edited/changed.
     *
     * @return events fired
     */
    public static List<String> raiseEventsOnDocumentChange(DocumentModel document) {
        List<String> eventsFired = new ArrayList<String>();
        // TODO: parameterize on document type
        Events evtManager = Events.instance();
        log.debug("Fire Event: " + EventNames.DOCUMENT_CHANGED);
        evtManager.raiseEvent(EventNames.DOCUMENT_CHANGED, document);
        eventsFired.add(EventNames.DOCUMENT_CHANGED);

        log.debug("Fire Event: "
                + EventNames.USER_ALL_DOCUMENT_TYPES_SELECTION_CHANGED);
        evtManager.raiseEvent(
                EventNames.USER_ALL_DOCUMENT_TYPES_SELECTION_CHANGED);
        eventsFired.add(EventNames.USER_ALL_DOCUMENT_TYPES_SELECTION_CHANGED);
        return eventsFired;
    }

    /**
     * Dispatches an event to get interested components informed when a changeable
     * document was created (thus not saved) and before the form is displayed.
     */
    public static void raiseEventsOnDocumentCreate(DocumentModel document) {
        Events.instance().raiseEvent(EventNames.NEW_DOCUMENT_CREATED);
   }

    /**
     * Fires the necessary events so that the nuxeo infrastructure components get
     * updated. The raised events will be processed immediately, before this
     * call is ended. Intended to be used when a the content of a folderish
     * document gets changed.
     *
     * @return events fired
     */
    public static List<String> raiseEventsOnDocumentChildrenChange(DocumentModel document) {
        List<String> eventsFired = new ArrayList<String>();
        Events.instance().raiseEvent(EventNames.DOCUMENT_CHILDREN_CHANGED, document);
        eventsFired.add(EventNames.DOCUMENT_CHILDREN_CHANGED);
        return eventsFired;
    }

}
