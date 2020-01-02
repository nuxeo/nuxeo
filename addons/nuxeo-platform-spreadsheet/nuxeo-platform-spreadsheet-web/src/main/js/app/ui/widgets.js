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
 *     Jackie Aldama <jaldama@nuxeo.com>
 */
import {DirectoryEditor, DirectoryRenderer} from './editors/directory';
import {DocumentEditor, DocumentRenderer} from './editors/document';
import {UserEditor, UserRenderer} from './editors/user';
import {ImageRenderer} from './editors/image';

Handsontable.editors.registerEditor('directory', DirectoryEditor);
Handsontable.editors.registerEditor('document', DocumentEditor);
Handsontable.editors.registerEditor('user', UserEditor);

/**
 * Mixin for widgets based on their name.
 * Used to override widget definitions (ex: template widgets).
 */
export const WIDGETS = {
  listing_coverage: {
    type: 'suggestOneDirectory',
    properties: {
      any: {
        dbl10n: true,
        directoryName: 'l10ncoverage'
      }
    }
  },
  listing_subjects: {
    type: 'suggestManyDirectory',
    properties: {
      any: {
        dbl10n: true,
        directoryName: 'l10nsubjects'
      }
    }
  },
  listing_version: {
    type: 'text',
    field: 'versionLabel'
  },
  listing_last_contributor: {
    type: 'singleUserSuggestion',
    renderer: UserRenderer
  },
  listing_title_link: {
    type: 'text'
  },
  listing_thumbnail: {
    field: 'thumb:thumbnail'
  }
};

/**
 * Mapping between widget types and handsontable's editors and renderers
 */
export const WIDGET_TYPES = {
  text: {
    type: 'text'
  },
  textarea: {
    type: 'text'
  },
  date: {
    type: 'date'
  },
  datetime: {
    type: 'date',
    dateFormat: 'yy-mm-ddT00:00:00.000'
  },
  numeric: {
    type: 'numeric'
  },
  int: {
    type: 'numeric'
  },
  // SELECT
  selectOneDirectory: {
    renderer: DirectoryRenderer,
    editor: 'directory'
  },
  selectManyDirectory: {
    renderer: DirectoryRenderer,
    editor: 'directory',
    multiple: true
  },

  // SUGGESTION
  suggestOneDirectory: {
    renderer: DirectoryRenderer,
    editor: 'directory'
  },
  suggestManyDirectory: {
    renderer: DirectoryRenderer,
    editor: 'directory',
    multiple: true
  },
  singleUserSuggestion: {
    renderer: UserRenderer,
    editor: 'user'
  },
  multipleUsersSuggestion: {
    renderer: UserRenderer,
    editor: 'user',
    multiple: true
  },
  singleDocumentSuggestion: {
    renderer: DocumentRenderer,
    editor: 'document',
    minimumInputLength: 3
  },
  multipleDocumentsSuggestion: {
    renderer: DocumentRenderer,
    editor: 'document',
    multiple: true,
    minimumInputLength: 3
  },

  // IMAGE
  image: {
    renderer: ImageRenderer,
    height: '150px',
    readOnly: true
  }
};
