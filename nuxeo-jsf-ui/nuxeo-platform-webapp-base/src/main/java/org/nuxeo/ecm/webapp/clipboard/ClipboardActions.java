/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id$
 */

package org.nuxeo.ecm.webapp.clipboard;

import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.webapp.documentsLists.DocumentsListDescriptor;

/**
 * Interface for clipboard template page action listener. Exposes methods for handling user actions related to the
 * copy/paste buttons from clipboard.xhtml template.
 *
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 */
public interface ClipboardActions {

    /**
     * Called when the drag and drop is launched in the clipboard fragment. Copies the documents passed to the
     * clipboard.
     * <p>
     * The selection is added to the clipboard and to the WorkingList.
     *
     * @param docCopied the list of documents we want to copy
     */
    void copySelection(List<DocumentModel> docCopied);

    /**
     * Called when the delete button is clicked on the clipboard.
     */
    String removeWorkListItem(DocumentRef ref);

    /**
     * Called when the "delete all" button is clicked on the clipboard.
     */
    String clearWorkingList();

    /**
     * Called when the "paste all" button is clicked on the clipboard.
     */
    String pasteWorkingList();

    /**
     * Called when the "move all" button is clicked on the clipboard/selection
     */
    String moveWorkingList();

    /**
     * Called when the drag and drop is launched in the body fragment. Pastes the documents passed to the clipboard.
     *
     * @param docPaste the list of doc we want to paste
     */
    String pasteDocumentList(List<DocumentModel> docPaste);

    String pasteDocumentListInside(List<DocumentModel> docPaste, String docId);

    /**
     * Pastes the content of the list listName into the current context document.
     */
    String pasteDocumentList(String listName);

    String pasteDocumentListInside(String listName, String docId);

    String pasteClipboard();

    String pasteClipboardInside(String docId);

    String moveClipboardInside(String docId);

    String exportWorklistAsZip();

    String exportAllBlobsFromWorkingListAsZip();

    String exportMainBlobFromWorkingListAsZip();

    String exportWorklistAsZip(List<DocumentModel> documents);

    String exportWorklistAsZip(List<DocumentModel> documents, boolean exportAllBlobs);

    void releaseClipboardableDocuments();

    boolean isInitialized();

    /**
     * Checks if the currently selected WorkList is empty.
     */
    boolean isWorkListEmpty();

    /**
     * Shortcut for getCanPaste on the currently selected workList.
     */
    boolean getCanPasteWorkList();

    /**
     * Shortcut for getCanPaste on the clipboard.
     */
    boolean getCanPasteFromClipboard();

    boolean getCanPasteFromClipboardInside(DocumentModel document);

    /**
     * Checks if the documents from the clipboard can be moved into the given document
     */
    boolean getCanMoveFromClipboardInside(DocumentModel document);

    /**
     * Checks if the content of a given workList can be pasted.
     * <p>
     * - checks if the list is empty<br>
     * - checks if the user has the needed rights in the current context
     */
    boolean getCanPaste(String listName);

    boolean getCanPasteInside(String listName, DocumentModel document);

    /**
     * Checks if there are selected items that can be copied into the current worklist.
     */
    boolean getCanCopy();

    /**
     * Checks if there are documents in current worklist can be moved into the current folder.
     */
    boolean getCanMoveWorkingList();

    /**
     * Checks if the documents in a given worklist can be moved into the given document
     */
    boolean getCanMoveInside(String listName, DocumentModel document);

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

    void putInClipboard(String docId);

    /**
     * Retries contents of current WorkList.
     */
    List<DocumentModel> getCurrentSelectedList();

    /*
     * List<DocumentModel> getWorkingList();
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
     * Returns the list of available lists (ie: the lists from the CLIPBOARD Category).
     */
    List<String> getAvailableLists();

    /**
     * Returns the list of Descriptors for available lists.
     */
    List<DocumentsListDescriptor> getDescriptorsForAvailableLists();

    /**
     * Returns the list of available web actions for the currently selected DocumentList.
     *
     * @return the WebAction list
     */
    List<Action> getActionsForCurrentList();

    /**
     * Returns the list of available web actions for the currently selected Documents.
     *
     * @return the WebAction list
     */
    List<Action> getActionsForSelection();

    void selectList();

    /**
     * Checks for documents bulk editing action.
     *
     * @return <code>true</code> if the current selected docs (from clipboard) are editable by the current user
     */
    boolean getCanEditSelectedDocs();

    /**
     * Checks if documents in the specified list are editable so the bulk editing action can be invoked later.
     *
     * @return <code>true</code> if the docs from the specified working list are editable by the current user
     */
    boolean getCanEditListDocs(String listName);

    boolean factoryForIsCurrentWorkListEmpty();

    boolean isCacheEnabled();

    String getCacheKey();

    boolean isCacheEnabledForSelection();

}
