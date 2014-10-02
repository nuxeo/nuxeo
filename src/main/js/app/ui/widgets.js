/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nelson Silva <nelson.silva@inevo.pt>
 */
import {DirectoryEditor} from './editors/directory';
import {DocumentEditor} from './editors/document';
import {UserEditor} from './editors/user';

Handsontable.editors.registerEditor('directory', DirectoryEditor);
Handsontable.editors.registerEditor('document', DocumentEditor);
Handsontable.editors.registerEditor('user', UserEditor);

export const WIDGETS = {
  text: {
    type: 'text'
  },
  datetime: {
    type: 'date',
    dateFormat: 'yy-mm-ddT00:00:00.000'
  },

  // SELECT
  selectOneDirectory: {
    editor: 'directory'
  },
  selectManyDirectory: {
    editor: 'directory',
    multiple: true
  },

  // SUGGESTION
  suggestOneDirectory: {
    editor: 'directory'
  },
  suggestManyDirectory: {
    editor: 'directory',
    multiple: true
  },
  singleUserSuggestion: {
    editor: 'user'
  },
  multipleUsersSuggestion: {
    editor: 'user',
    multiple: true
  },
  singleDocumentSuggestion: {
    editor: 'document'
  },
  multipleDocumentsSuggestion: {
    editor: 'document',
    multiple: true
  },

  // LISTING
  listing_title_link: {
    type: 'text'
  },
  listing_last_contributor: {
    type: 'text'
  }
};
