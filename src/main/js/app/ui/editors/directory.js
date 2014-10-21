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
import {Select2Editor} from './select2';
import {Directory} from '../../nuxeo/rpc/directory';

class DirectoryEditor extends Select2Editor {

  query(connection, properties, term) {
    var directory = new Directory(connection); // Directory name is a widget property
    // Set the properties
    Object.assign(directory, properties);
    // Perform the search
    return directory.search(term);
  }

  // When a dbl10n entry is selected we'll cache the labels to be used
  // by our renderer
  onSelected(evt) {

    // skip if this is not dbl10n
    if (!this.isDbl10n) {
      return;
    }

    var choice = evt.choice;
    var ctx = this.sourceData.contextParameters;
    var labels = ctx[this.directoryName][this.field];
    // Check if we already have this label in context parameters
    for (var e of labels) {
      if (choice.id === e.id) {
        return;
      }
    }
    // If not add then add it
    labels.push({id: choice.computedId, label_en: choice.absoluteLabel});
  }

  get column() {
    return this.cellProperties;
  }

  get widget() {
    return this.column.widget;
  }

  get field() {
    return this.widget.field;
  }

  get directoryName() {
    return this.widget.properties.directoryName;
  }

  get isDbl10n() {
    return !!this.widget.properties.dbl10n;
  }

  get sourceData() {
    return this.instance.getSourceDataAtRow(this.row);
  }

  formatter(entry) {
    var label = entry.displayLabel;
    // This is used in initSelection and in this case we don't have 'displayLabel'
    if (!label && this.isDbl10n) {
      var ctx = this.sourceData.contextParameters;
      label = getLabel(ctx, this.column, entry.id);
    }
    return label || entry.text;
  }

  getEntryId(item) {
    if (item.computedId) {
      return item.computedId;
    } else {
      return item.id;
    }
  }
}

function DirectoryRenderer(instance, td, row, col, prop, value, cellProperties) {
  var ctx = instance.getSourceDataAtRow(row).contextParameters;
  if (value) {
    if (!Array.isArray(value)) {
      value = value.split(',');
    }
    arguments[5] = getLabels(ctx, cellProperties, value).join(',');
  }
  cellProperties.defaultRenderer.apply(this, arguments);
}

// l10n Label helpers
function findLabel(id, entries) {
  for (var e of entries) {
    if (id === e.id) {
      return e.label_en;
    }
  }
  return id;
}

function getLabels(ctx, column, ids) {
  var field = column.widget.field,
      directoryName = column.widget.properties.directoryName;

  if(!ctx[directoryName]) {
    return ids;
  }
  var directoryEntries = ctx[directoryName][field];
  return ids.map((id) => findLabel(id, directoryEntries));

}

function getLabel(ctx, column, id) {
  return getLabels(ctx, column, [id])[0];
}

export {DirectoryEditor, DirectoryRenderer};
