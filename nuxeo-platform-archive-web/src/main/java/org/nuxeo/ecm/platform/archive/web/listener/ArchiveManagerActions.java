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
 * $Id: ArchiveManagerActions.java 13422 2007-03-08 12:26:45Z tdelprat $
 */

package org.nuxeo.ecm.platform.archive.web.listener;

import java.io.Serializable;
import java.util.List;
import javax.ejb.Remove;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;
import org.jboss.seam.annotations.Destroy;
import org.nuxeo.ecm.platform.archive.api.ArchiveRecord;

/**
 * Archive manager actions business interface.
 *
 * @author <a href="mailto:bt@nuxeo.com">Bogdan Tatar</a>
 */
public interface ArchiveManagerActions extends Serializable {

    /**
     * Invalidates archive records.
     *
     * @throws Exception
     */
    void invalidateArchiveRecords() throws Exception;

    /**
     * Computes all archive records.
     *
     * @see
     * @factory
     *
     * @throws Exception
     */
    void computeArchiveRecords() throws Exception;

    /**
     * Adds a new archive record.
     *
     * @return where to forward to after the archive record is added
     * @throws Exception
     */
    String addArchiveRecord() throws Exception;

    /**
     * Edits an archive record.
     *
     * @return where to forward to after the archive record is modified
     * @throws Exception
     */
    String editArchiveRecord() throws Exception;

    /**
     * Views an archive record.
     *
     * @return where to forward to after the archive record is viewed
     * @throws Exception
     */
    String viewArchiveRecord() throws Exception;

    /**
     * Deletes an archive record.
     *
     * @return where to forward to after the archive record is deleted
     * @throws Exception
     */
    String deleteArchiveRecord() throws Exception;

    /**
     * Used to decide if the add new archive record button should appear or not.
     * The 'event' parameter is not used in the implementation of the method,
     * and it had to be included in the signature of the method in order to
     * accomodate the jsf specification for an 'actionListener' method added to
     * an 'h:commandLink' tag.
     *
     */
    void toggleCreateForm(ActionEvent event);

    /**
     * Tells if the "add new archive" record button should appear or not.
     *
     * @return true if the "add new archive" record button should appear, false
     *         otherwise.
     */
    boolean getShowCreateForm();

    /**
     * This method is used to mark the fields that are required to be given a
     * value when an archive record is created.This method is mostly used in the
     * xhtml for an archive record.
     *
     * @return true - if the field needs to have a value
     *         false- otherwise
     */
    boolean getRequired();

    /**
     * This method is used when the cancel button on the page that is used to
     * add an archive recod, is clicked.This way the user is not asked to give
     * values for the fields that needs to have a value.The 'event' parameter is
     * introduced in the signature of the method in order to be use this method
     * as an 'actionListener' for the 'h:commandButton' tag
     *
     * @param event
     */
    void setRequired(ActionEvent event);

    @Destroy
    @Remove
    void destroy();

    /**
     * Returns a list of SelectItems with the versions of the current document.
     *
     * @return a list of SelectItems with the versions of the current document
     * @throws Exception
     */
    List<SelectItem> getDocumentVersions() throws Exception;

    /**
     * Returns the archive record to add/edit.
     *
     * @return the archive record to add/edit
     */
    ArchiveRecord getSelectedArchiveRecord();

    /**
     * Sets the archive record to add/edit.
     *
     * @param selectedArchiveRecord
     */
    void setSelectedArchiveRecord(ArchiveRecord selectedArchiveRecord);

    /**
     * Cancels the process of adding or editing an archive record.
     *
     * @return where to forward to after canceling
     * @throws Exception
     */
    String cancel() throws Exception;

    /**
     * Sets the first button command name from the page used to add/edit an
     * archive record.
     *
     * @return - the name of the command button
     */
    String getCommandName1();

    /**
     * Sets the first command button name.
     *
     * @param commandName - the name of the command button
     */
    void setCommandName1(String commandName);

    /**
     * Sets the second button command name from the page used to add/edit an
     * archive record.
     *
     * @return - the name of the command button
     */
    String getCommandName2();

    /**
     * Sets the second command button name.
     *
     * @param commandName - the name of the command button
     */
    void setCommandName2(String commandName);

    /**
     * Checks if the page used to add/edit an archive record is editable.
     *
     * @return - true if the page is editable
     *         <p> - false otherwise
     */
    boolean getEditable();

    /**
     * Sets the page for add/edit archive record as editable or not.
     *
     * @param editable - if true then the page is editable
     */
    void setEditable(boolean editable);

    /**
     * Returns the style of the components that appear in the add/edit archive
     * record page.
     *
     * @return - the style used for the components of the page
     */
    String getStyle();

    /**
     * Sets the style of the components that appear in the add/edit archive
     * record page.
     *
     * @param style - the style used for the components of the page
     */
    void setStyle(String style);

}
