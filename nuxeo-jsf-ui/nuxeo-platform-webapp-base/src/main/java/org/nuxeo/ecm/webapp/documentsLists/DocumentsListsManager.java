/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.webapp.documentsLists;

import java.util.List;

import org.jboss.seam.annotations.Observer;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.webapp.helpers.EventNames;

/**
 * Seam component used to manage named lists of documents.
 * <p>
 * Managing the DM lists into this component insteed of directly inside the Seam context offers the following
 * advantages:
 * <ul>
 * <li>DM Lists life cycle management can be done transparently, the DocumentsListsManager can use internal fields or
 * differently scoped variables (Conversation, Process ...)
 * <li>DocumentsListsManager provides (will) an Extension Point mechanisme to register new names lists
 * <li>DocumentsListsManager provides add configurations to each lists
 * <ul>
 * <li>List Name
 * <li>List Icone
 * <li>List append behavior
 * <li>Category of the list
 * <li>...
 * </ul>
 * <li>DocumentsListsManager provides helpers features for merging and resetting lists
 * </ul>
 *
 * @author tiry
 */
public interface DocumentsListsManager {

    /**
     * List identifier: Default working list.
     */
    String DEFAULT_WORKING_LIST = "DEFAULT";

    /**
     * List identifier: Clipboard list.
     */
    String CLIPBOARD = "CLIPBOARD";

    /**
     * List identifier: Stores the current selection of documents.
     */
    String CURRENT_DOCUMENT_SELECTION = "CURRENT_SELECTION";

    /**
     * List identifier: Stores the current selection of deleted documents.
     */
    String CURRENT_DOCUMENT_TRASH_SELECTION = "CURRENT_SELECTION_TRASH";

    /**
     * List identifier: Stores the current selection of published documents.
     */
    String CURRENT_DOCUMENT_SECTION_SELECTION = "CURRENT_SELECTION_SECTIONS";

    /**
     * List identifier: Stores the current selection of versions.
     *
     * @since 5.6
     */
    String CURRENT_VERSION_SELECTION = "CURRENT_SELECTION_VERSIONS";

    /**
     * Creates (declares) a new named list of documents.
     *
     * @param listName Name of the list
     */
    void createWorkingList(String listName, DocumentsListDescriptor descriptor);

    /**
     * Returns the list listName.
     *
     * @param listName Name of the list
     * @return
     */
    List<DocumentModel> getWorkingList(String listName);

    /**
     * Returns the default list.
     *
     * @return
     */
    List<DocumentModel> getWorkingList();

    /**
     * Returns the list of document types contained into the list ListName.
     *
     * @param listName Name of the list to retrieve
     * @return the DocumentModel List or null if the ListName is unknown
     */
    List<String> getWorkingListTypes(String listName);

    /**
     * Returns the list of document types contained in the default list.
     *
     * @return the DocumentModel List
     */
    List<String> getWorkingListTypes();

    /**
     * Updates the list listName.
     *
     * @param listName Name of the list to update
     * @param docList the DocumentModel list to store in the list ListName
     */
    void setWorkingList(String listName, List<DocumentModel> docList);

    /**
     * Updates the default list.
     *
     * @param docList the DocumentModel list to store in the default list
     */
    void setWorkingList(List<DocumentModel> docList);

    /**
     * Adds one document to the list listName.
     *
     * @param listName the name of the list to update
     * @param doc the doc to append
     * @return the updated list of DocumentModels
     */
    List<DocumentModel> addToWorkingList(String listName, DocumentModel doc);

    /**
     * Adds one document to the default list.
     *
     * @param doc
     * @return the updated list of DocumentModels
     */
    List<DocumentModel> addToWorkingList(DocumentModel doc);

    /**
     * Adds a list of DocumentModels to the list ListName.
     *
     * @param listName the name of the list to update
     * @param docList the DocumentModels list to append
     * @return the updated list of DocumentModels
     */
    List<DocumentModel> addToWorkingList(String listName, List<DocumentModel> docList);

    /**
     * Adds a list of DocumentModels to the list ListName.
     *
     * @param listName the name of the list to update
     * @param docList the DocumentModels list to append
     * @param forceAppend force the new elements to be appened even if the list default behaviour is reset
     * @return the updated list of DocumentModels
     */
    List<DocumentModel> addToWorkingList(String listName, List<DocumentModel> docList, Boolean forceAppend);

    /**
     * Adds a list of DocumentModels to the default list.
     *
     * @param docList
     * @return the updated list of DocumentModels
     */
    List<DocumentModel> addToWorkingList(List<DocumentModel> docList);

    /**
     * Removes one DocumentModel from the list ListName.
     *
     * @param listName
     * @param doc
     * @return the updated list of DocumentModels
     */
    List<DocumentModel> removeFromWorkingList(String listName, DocumentModel doc);

    List<DocumentModel> removeFromWorkingList(String listName, List<DocumentModel> lst);

    /**
     * Removes one DocumentModel from the default list.
     *
     * @param doc
     * @return the updated list of DocumentModels
     */
    List<DocumentModel> removeFromWorkingList(DocumentModel doc);

    /**
     * Removes DocumentModels from the list ListName.
     *
     * @param listName
     * @return the updated list of DocumentModels
     */
    List<DocumentModel> resetWorkingList(String listName);

    /**
     * Removes DocumentModels from the default list.
     *
     * @return the updated list of DocumentModels
     */
    List<DocumentModel> resetWorkingList();

    /**
     * Resets list listName and fill it with newDocList.
     *
     * @param listName
     * @param newDocList
     * @return
     */
    List<DocumentModel> resetWorkingList(String listName, List<DocumentModel> newDocList);

    /**
     * Resets default list and fills it with newDocList.
     *
     * @param newDocList
     * @return the updated list of DocumentModels
     */
    List<DocumentModel> resetWorkingList(List<DocumentModel> newDocList);

    /**
     * Check is list listName is empty.
     *
     * @param listName
     * @return true if the list is Empty
     */
    boolean isWorkingListEmpty(String listName);

    /**
     * Checks if default list is empty.
     *
     * @return true if the list is Empty
     */
    boolean isWorkingListEmpty();

    /**
     * Method called by Seam event service to reset lists.
     */
    @Observer(value = { EventNames.DOCUMENT_SELECTION_CHANGED }, create = false)
    void refreshLists(DocumentModel currentDocument);

    /**
     * Removes documentsToRemove from all lists.
     *
     * @param documentsToRemove
     */
    void removeFromAllLists(List<DocumentModel> documentsToRemove);

    /**
     * Init Method (replaces for now Registry initialization that will be done by the extension point and the Runtime).
     */
    void initListManager();

    /**
     * Returns the availables lists names for a given category.
     *
     * @param categoryName
     * @return the names of the available lists
     */
    List<String> getWorkingListNamesForCategory(String categoryName);

    /**
     * Gets the descriptor (meta-data) of a given list.
     *
     * @param listName
     * @return the Descriptor of the DocumentModel list
     */
    DocumentsListDescriptor getWorkingListDescriptor(String listName);

    /**
     * Gets the descriptor (meta-data) of a default list.
     *
     * @return the Descriptor of the DocumentModel list
     */
    DocumentsListDescriptor getWorkingListDescriptor();

}
