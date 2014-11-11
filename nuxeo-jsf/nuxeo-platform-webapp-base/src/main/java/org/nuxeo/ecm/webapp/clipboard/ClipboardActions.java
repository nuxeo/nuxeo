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

package org.nuxeo.ecm.webapp.clipboard;

import java.util.List;

import javax.ejb.Remove;

import org.jboss.seam.annotations.Destroy;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.webapp.documentsLists.DocumentsListDescriptor;

/**
 * Interface for clipboard template page action listener. Exposes methods for
 * handling user actions related to the copy/paste buttons from clipboard.xhtml
 * template.
 *
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 */
public interface ClipboardActions {

    /**
     * Called when the drag and drop is launched in the clipboard fragment.
     * Copies the documents passed to the clipboard.
     * <p>
     * The selection is added to the clipboard and to the WorkingList.
     *
     * @param docCopied the list of documents we want to copy
     */
    void copySelection(List<DocumentModel> docCopied);

    /**
     * Called when the delete button is clicked on the clipboard.
     */
    String removeWorkListItem(DocumentRef ref) throws ClientException;

    /**
     * Called when the "delete all" button is clicked on the clipboard.
     */
    String clearWorkingList();

    /**
     * Called when the "paste all" button is clicked on the clipboard.
     */
    String pasteWorkingList() throws ClientException;

    /**
     * Called when the "move all" button is clicked on the clipboard/selection
     */
    String moveWorkingList() throws ClientException;

    /**
     * Called when the drag and drop is launched in the body fragment.
     * Pastes the documents passed to the clipboard.
     *
     * @param docPaste the list of doc we want to paste
     */
    String pasteDocumentList(List<DocumentModel> docPaste) throws ClientException;

    String pasteDocumentListInside(List<DocumentModel> docPaste, String docId) throws ClientException;

    /**
     * Pastes the content of the list listName into the current context document.
     */
    String pasteDocumentList(String listName) throws ClientException;

    String pasteDocumentListInside(String listName, String docId) throws ClientException;


    String pasteClipboard() throws ClientException;

    String pasteClipboardInside(String docId) throws ClientException;

    String moveClipboardInside(String docId) throws ClientException;

    /**
     * Removes the component.
     */
    @Destroy
    @Remove
    void destroy();

    String exportWorklistAsZip() throws ClientException;

    String exportAllBlobsFromWorkingListAsZip() throws ClientException;

    String exportMainBlobFromWorkingListAsZip() throws ClientException;

    String exportWorklistAsZip(List<DocumentModel> documents) throws ClientException;

    String exportWorklistAsZip(List<DocumentModel> documents, boolean exportAllBlobs) throws ClientException;

    void releaseClipboardableDocuments();

    boolean isInitialized();

    /**
     * Checks if the currently selected WorkList is empty.
     */
    boolean isWorkListEmpty();

    /**
     * Shortcut for getCanPaste on the currently selected workList.
     */
    boolean getCanPasteWorkList() throws ClientException;

    /**
     * Shortcut for getCanPaste on the clipboard.
     */
    boolean getCanPasteFromClipboard() throws ClientException;

    boolean getCanPasteFromClipboardInside(DocumentModel document) throws ClientException;

    /**
     * Checks if the documents from the clipboard can be moved into the given document
     */
    boolean getCanMoveFromClipboardInside(DocumentModel document)throws ClientException;

    /**
     * Checks if the content of a given workList can be pasted.
     * <p>
     * - checks if the list is empty<br>
     * - checks if the user has the needed rights in the current context
     */
    boolean getCanPaste(String listName) throws ClientException;

    boolean getCanPasteInside(String listName, DocumentModel document) throws ClientException;

    /**
     * Checks if there are selected items that can be copied into the current worklist.
     */
    boolean getCanCopy();

    /**
     * Checks if there are documents in current worklist can be moved into the current folder.
     */
    boolean getCanMoveWorkingList() throws ClientException;

    /**
     * Checks if the documents in a given worklist can be moved into the given document
     */
    boolean getCanMoveInside(String listName, DocumentModel document) throws ClientException;

    /**
     * Copies docsList into the current WorkList.
     */
    void putSelectionInWorkList(List<DocumentModel> docsList);

    void putSelectionInWorkList(List<DocumentModel> docsList, Boolean forceAppend);

    /**
     * Copies the lists of selected documents into the current WorkList.
     */
    void putSelectionInWorkList();

    void putSelectionInWorkList(Boolean forceAppend);

    void putSelectionInClipboard();

    void putSelectionInDefaultWorkList();

    void putInClipboard(String docId) throws ClientException;

    /**
     * Retries contents of current WorkList.
     */
    List<DocumentModel> getCurrentSelectedList();

    /*
    List<DocumentModel> getWorkingList();
    */

    /**
     * Returns the name of the current selected WorkList.
     */
    String getCurrentSelectedListName();

    String getCurrentSelectedListTitle();

    /**
     * Sets the current selected WorkList.
     */
    void setCurrentSelectedList(String listId);

    /**
     *  Returns the list of available lists
     *  (ie: the lists from the CLIPBOARD Category).
     */
    List<String> getAvailableLists();

    /**
     * Returns the list of Descriptors for available lists.
     */
    List<DocumentsListDescriptor> getDescriptorsForAvailableLists();

    /**
     * Returns the list of available web actions for the currently selected
     * DocumentList.
     *
     * @return the WebAction list
     */
    List<Action> getActionsForCurrentList();

    /**
     * Returns the list of available web actions for the currently selected
     * Documents.
     *
     * @return the WebAction list
     */
    List<Action> getActionsForSelection();

    void selectList();

    /**
     * Checks for documents bulk editing action.
     *
     * @return <code>true</code> if the current selected docs (from clipboard) are editable by
     *         the current user
     */
    boolean getCanEditSelectedDocs() throws ClientException;

    /**
     * Checks if documents in the specified list are editable so the bulk
     * editing action can be invoked later.
     *
     * @return <code>true</code> if the docs from the specified working list
     *         are editable by the current user
     */
    boolean getCanEditListDocs(String listName) throws ClientException;

    boolean factoryForIsCurrentWorkListEmpty();

    boolean isCacheEnabled();

    String getCacheKey();

    boolean isCacheEnabledForSelection();

}
