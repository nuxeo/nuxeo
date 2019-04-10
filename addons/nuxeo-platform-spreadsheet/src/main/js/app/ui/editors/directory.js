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
    if (entity['entity-type'] !== 'directoryEntry') {
      return entity;
    }
    var id;
    if (entity.properties.parent) {
      id = `${entity.properties.parent.properties.id}/${entity.properties.id}`;
    } else {
      id = entity.properties.id;
    }
    this._labels[id] = getEntryLabel(entity);
    return id;
  }

  // create directory entries again on save
  saveValue(val, ctrlDown) {
    var value = val[0][0];

    if (value) {
      value = value.split(',').map(function (id) {
        return {
          'entity-type': 'directoryEntry',
          directoryName: this.directoryName,
          properties: {
            id: id
            // TOOD: store label to use in renderer
            // label: this._labels[id]
          }
        };
      }.bind(this));
      // unwrap the map result if not multiple
      if (!this.column.multiple) {
        value = value[0];
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
    // Perform the search
    return directory.search(term);
  }

  // When a dbl10n entry is selected we'll cache the labels to be used
  // by our renderer
  onSelected(evt) {
    this._labels[evt.choice.computedId] = evt.choice.absoluteLabel;
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

  selectionFormatter(entry) {
    var id = this.getEntryId(entry);
    return this._labels[id] || entry.absoluteLabel;
  }

  formatter(entry) {
    var label = this._labels[entry.id] || entry.absoluteLabel;
    // This is used in initSelection and in this case we don't have 'displayLabel'
    if (!label && this.isDbl10n) {
      label = getEntryLabel(entry);
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
function getEntryLabel(entry) {
  if (entry.properties) {
    var label = '';
    if (entry.properties.parent) {
      label = getEntryLabel(entry.properties.parent) + '/';
    }
    label += (entry.properties.label_en || entry.properties.label || entry.properties.id);
    return label;
  }
  return entry;
}

function DirectoryRenderer(instance, td, row, col, prop, value, cellProperties) {
  if (value) {
    if (!Array.isArray(value)) {
      value = [value];
    }
    arguments[5] = value.map((v) => getEntryLabel(v)).join(','); // jshint ignore:line
  }
  cellProperties.defaultRenderer.apply(this, arguments);
}

export {DirectoryEditor, DirectoryRenderer};
