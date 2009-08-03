/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.webapp.edit.vocabularies;

import java.io.Serializable;
import java.util.List;

import javax.ejb.Remove;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;

import org.jboss.seam.annotations.Destroy;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.platform.ui.web.directory.VocabularyEntry;
import org.nuxeo.ecm.webapp.directory.DirectoryUIActionsBean;

/**
 * Interface for an action listener that will provide methods to edit a
 * vocabulary.
 *
 * @author <a href="mailto:bt@nuxeo.com">Bogdan Tatar</a>
 * @deprecated use {@link DirectoryUIActionsBean}
 */
@Deprecated
public interface VocabularyActions extends Serializable {

    /**
     * Initializes the vocabulary bean Seam component.
     */
    void initialize();

    /**
     * Edits a vocabulary.
     *
     * @return the page to which to forward after the edit process.
     * @throws ClientException
     */
    String editVocabulary() throws ClientException;

    /**
     * Erases all the entries of a vocabulary.
     *
     * @return the page to which to forward after the clear process.
     * @throws ClientException
     */
    String clearVocabulary() throws ClientException;

    /**
     * Initializes the list with all the available vocabularies.
     *
     * @return the page to which to forward after.
     * @throws ClientException
     */
    String viewAllVocabularies() throws ClientException;

    /**
     * Adds an entry to a vocabulary.
     *
     * @return the page to which to forward after.
     * @throws ClientException
     */
    String addVocabularyEntry() throws ClientException;

    /**
     * Edit an entry of a vocabulary.
     *
     * @return the page to which to forward after.
     */
    String editVocabularyEntry();

    /**
     * Views (does not to edit) the entry of a vocabulary.
     *
     * @return the page to which to forward after.
     */
    String viewVocabularyEntry();

    /**
     * Deletes an entry of a vocabulary.
     *
     * @return the page to which to forward after.
     * @throws ClientException
     */
    String deleteVocabularyEntry() throws ClientException;

    /**
     * Sets the name of vocabulary which the user is going to use for modifying actions.
     */
    void setSelectedVocabularyName(String name);

    /**
     * Returns the name of vocabulary chosen by the user to modify.
     *
     * @return - the chosen vocabulary.
     */
    String getSelectedVocabularyName();

    /**
     * Sets the vocabulary entry the user is going to use for modifying actions.
     *
     * @param entry - the entry selected for modifying actions.
     */
    void setSelectedVocabularyEntry(VocabularyEntry entry);

    /**
     * Returns the selected vocabulary entry chosen for modifying actions.
     *
     * @return the selected vocabulary entry.
     */
    VocabularyEntry getSelectedVocabularyEntry();

    /**
     * Used to decide what icon should appear for the add new entry button.
     */
    void toggleAddEntryForm(ActionEvent event);

    /**
     * Decides what icon will be rendered for the add entry button.
     *
     * @return
     */
    boolean getShowAddEntryForm();

    /**
     * Decides if the edit or view action was selected.
     *
     * @return true if the view action was selected - false otherwise
     */
    boolean getEditable();

    /**
     * Sets the boolean which tells if a view or edit action was chosen.
     *
     * @param editable
     */
    void setEditable(boolean editable);

    /**
     * Sets the first button command name from the page used to add/edit a
     * vocabulary entry.
     *
     * @return the name of the command button
     */
    String getCommandName1();

    /**
     * Sets the first command button name.
     *
     * @param commandName the name of the command button
     */
    void setCommandName1(String commandName);

    /**
     * Sets the second button command name from the page used to add/edit a
     * vocabulary entry.
     *
     * @return the name of the command button
     */
    String getCommandName2();

    /**
     * Sets the second command button name.
     *
     * @param commandName the name of the command button
     */
    void setCommandName2(String commandName);

    /**
     * Cancels the process of adding or editing a vocabulary entry.
     *
     * @return where to forward to after canceling
     */
    String cancel();

    /**
     * Sets the style of the page depending on the action that was chosen, view
     * or edit.
     *
     * @param style
     */
    void setStyle(String style);

    /**
     * Returns the style depending on the action that was chosen,view or edit.
     *
     * @return
     */
    String getStyle();

    /**
     * Performs the search for a vocabulary entries.
     *
     * @param searchCriteria - the criteria used to meke the search.
     */
    void setSearchCriteria(VocabularyEntry searchCriteria);

    /**
     * Returns the criteria which is used to perform the search for a vocabulary
     * entries.
     *
     * @return - the criteria used to perform the search.
     */
    VocabularyEntry getSearchCriteria();

    /**
     * Resets the search criteria.
     *
     * @throws ClientException
     */
    String clearSearchCriteria() throws ClientException;

    /**
     * Gets the vocabulary entries searched by the entered criteria.It returns
     * the way to a page, depending on what the search process returns.
     *
     * @return the corresponding map of the page
     */
    String searchVocabularyEntries() throws ClientException;

    /**
     * Returns the title that will appear above the list with the entries of a
     * vocabulary.
     *
     * @return the title
     */
    String getTitle();

    /**
     * This method is used to populated the select for parent ids.
     */
    List<SelectItem> getParentIds();

    /**
     * Checks if the vocabulary is hierarchical (has parents).
     *
     * @return true if the vocabulary is hierarchical.
     * @throws DirectoryException
     */
    boolean isHierarchical() throws DirectoryException;

    /**
     * Checks if null parents are allowed.
     *
     * @return true if this is a recursive hierarchical vocabulary.
     * @throws DirectoryException
     */
    boolean isNullParentAllowed() throws DirectoryException;

    @Destroy
    @Remove
    void destroy();

}
