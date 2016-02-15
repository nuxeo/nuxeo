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

  // Let's override prepare and just pass set the select2 options ourselves
  prepare(row, col, prop, td, originalValue, cellProperties) {
    // setup the label cache
    this._labels = {};

    // flatten our values to a list of ids
    var value = (Array.isArray(originalValue)) ? originalValue.map(this.prepareEntity.bind(this)) : this.prepareEntity(originalValue);

    super.prepare(row, col, prop, td, value, cellProperties);
  }

  // flatten entities to plain ids and cache the labels
  prepareEntity(entity) {
    if (!entity) {
      return;
    }

    // remember if we are handling directoryEntries or just strings
    this._isDirectoryEntry = entity['entity-type'] === 'directoryEntry';
    if (!this._isDirectoryEntry) {
      return entity;
    }

    var id;
    if (entity.properties.parent) {
      id = `${entity.properties.parent.properties.id}/${entity.properties.id}`;
    } else {
      id = entity.properties.id;
    }
    this.cellLabels[id] = this.cellLabels[id] || getEntryLabel(entity, this.language);
    return id;
  }

  // create directory entries again on save
  saveValue(val, ctrlDown) {
    var value = val[0][0];

    if (value) {
      // if we are working with directoryEntries lets build them for saving
      if (this._isDirectoryEntry) {
        value = value.split(',').map(function (id) {
          return {
            'entity-type': 'directoryEntry',
            directoryName: this.directoryName,
            properties: {
              id: id
            }
          };
        }.bind(this));
        // unwrap the map result if not multiple
        if (!this.column.multiple) {
          value = value[0];
        }
      }
    } else {
      value = this.column.multiple ? [] : null;
    }

    super.saveValue([[value]], ctrlDown);
  }

  query(connection, properties, term) {
    var directory = new Directory(connection); // Directory name is a widget property
    // Set the properties
    Object.assign(directory, properties);
    // Set the language
    directory.language = this.language || 'en';
    // Perform the search
    return directory.search(term);
  }

  // When a dbl10n entry is selected we'll cache the labels to be used
  // by our renderer
  onSelected(evt) {
    this.cellLabels[evt.choice.computedId] = evt.choice.absoluteLabel;
  }

  get cellMeta() {
    return this.instance.getCellMeta(this.row, this.col);
  }

  get cellLabels() {
    return this.cellMeta._labels = this.cellMeta._labels || {};
  }

  get language() {
    return this.instance.getSettings().language || 'en';
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

  resultFormatter(entry) {
    return entry.displayLabel;
  }

  formatter(entry) {
    var label = this.cellLabels[entry.id] || entry.absoluteLabel;
    // This is used in initSelection and in this case we don't have 'displayLabel'
    if (!label && this.isDbl10n) {
      label = getEntryLabel(entry, this.language);
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

// l10n Label helpers
function getEntryLabel(entry, lang = 'en') {
  if (entry.properties) {
    var label = '';
    if (entry.properties.parent) {
      label = getEntryLabel(entry.properties.parent, lang) + '/';
    }
    label += (entry.properties['label_' + lang] || entry.properties.label || entry.properties.id);
    return label;
  }
  return entry;
}

function DirectoryRenderer(instance, td, row, col, prop, value, cellProperties) {
  if (value) {
    var lang = instance.getSettings().language || 'en';
    if (!Array.isArray(value)) {
      value = (typeof value === 'string') ? value.split(',') : [value];
    }
    var labels = instance.getCellMeta(row, col)._labels;
    arguments[5] = value.map((v) => {
      var key = v.properties ? v.properties.id : v;
      return (labels && labels[key]) ? labels[key] : getEntryLabel(v, lang);
    }).join(','); // jshint ignore:line
  }
  cellProperties.defaultRenderer.apply(this, arguments);
}

export {DirectoryEditor, DirectoryRenderer};
