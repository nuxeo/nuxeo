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
import {Column} from './column';
import {Layout} from '../nuxeo/layout';
import {Schemas} from '../nuxeo/rest/schemas';
import {Directory} from '../nuxeo/rpc/directory';
import {Query} from '../nuxeo/rpc/query';
import {DirectoryEditor} from './editors/directory';
import { Select2Editor } from './editors/select2';

/**
 * Spreadsheet backed by Hansontable
 */
class Spreadsheet {

  constructor(container, connection, layout, columns, pageProvider, language) {
    this.container = container;
    this.connection = connection;
    this._data = [];
    this.options = {
      data: this._data,
      autoColumnSize: false,
      colWidths: 200,
      stretchH: 'all',
      rowHeaders: true,
      manualColumnResize: true,
      startRows: 0,
      currentRowClassName: 'currentRow',
      currentColClassName: 'currentCol',
      contextMenu: ['undo', 'redo'],
      afterChange: this.onChange.bind(this),
      beforeAutofill: this.beforeAutofill.bind(this),
      beforeCopy: this.beforeCopy.bind(this),
      search: true,
      cells: this.createCell.bind(this),
      language: language
    };

    this.query = new Query(connection);
    // include the user's permission on each document
    this.query.enrich('document', 'permissions');
    // fetch every property and versioning information
    this.query.fetch('document', 'properties', 'versionLabel');
    // fetch parent for directory entries
    this.query.fetch('directoryEntry', 'parent');
    // request root depth
    this.query.depth = 'root';
    // translate directory labels
    this.query.translate('directoryEntry', 'label');
    this.query.pageProvider = pageProvider;

    // set columns based on result layout
    if (layout) {
      new Layout(connection, layout, language).fetch().then((l) => {
        // Check which columns to display
        let cols = (columns) ? columns.map((name) => l.columns.filter((c) => c.name === name)[0])
          : l.columns.filter((c) => c.selectedByDefault !== false);
        this.columns = cols
          // Exclude columns without widgets
          .filter((c) => c.widgets)
          // Create our columns wrapper
          .map((c) => new Column(connection, c, l.widgets[c.widgets[0].name], this.dirtyRenderer.bind(this)))
          // Only show columns with a known widget type and with a field
          .filter((c) => c.hasSupportedWidgetType && c.field);
      });

    // or based on result columns only
    } else {

      // get schemas prefixes from columns
      let schemasPrefixes = [];
      for (let c of columns) {
        let schema = c.field.indexOf(':') > -1 ? c.field.split(':')[0] : undefined;
        if (schema && schemasPrefixes.indexOf(schema) === -1) {
          schemasPrefixes.push(schema);
        }
      }

      // fetch schemas (based on prefixes)
      new Schemas(connection).fetch(schemasPrefixes).then((schemas) => {

        let cols = columns.map((c) => {

          let column = {
            def: {properties: {any: {sortPropertyName: c.field, label: c.label}}},
            widget: {field: c.field}
          };

          // get field definition from schemas map
          let field = undefined; // <- explicitly set field as undefined in each iteration
          if (c.field.indexOf(':') > -1) {
            let [s, f] = c.field.split(':');
            field = schemas[s].fields[f] || undefined;
            field = (typeof field === 'string') ? {type: field} : field;
          }

          // set column widget type and properties based on field constraints
          if (field) {
            if (field.type.endsWith('[]')) {
              column.widget.multiple = true;
            }
            let constraints = field.itemConstraints || field.constraints;
            if (constraints) {
              for (let constraint of constraints) {
                switch (constraint.name) {
                  case 'documentResolver':
                    column.widget.type = (field.type === 'string[]') ? 'multipleDocumentsSuggestion' : 'singleDocumentSuggestion';
                    break;
                  case 'directoryResolver':
                    column.widget.type = (field.type === 'string[]') ? 'suggestManyDirectory' : 'suggestOneDirectory';
                    column.widget.properties = {dbl10n: true, directoryName: constraint.parameters.directory};
                    break;
                  case 'userManagerResolver':
                    column.widget.type = (field.type === 'string[]') ? 'multipleUsersSuggestion' : 'singleUserSuggestion';
                    let searchType;
                    if (constraint.parameters.includeGroups === 'true' && constraint.parameters.includeUsers === 'true') {
                      searchType = 'USER_GROUP_TYPE';
                    } else if (constraint.parameters.includeUsers === 'true') {
                      searchType = 'USER_TYPE';
                    } else if (constraint.parameters.includeGroups === 'true') {
                      searchType = 'GROUP_TYPE';
                    }
                    column.widget.properties = {
                      any: {
                        userSuggestionSearchType: searchType
                      }
                    };
                    break;
                }
              }
            }
          }

          return column;
        });

        this.columns = cols.map((c) => new Column(connection, c.def, c.widget, this.dirtyRenderer.bind(this)));
      });
    }

    this.container.handsontable(this.options);
    this.ht = this.container.data('handsontable');
  }

  get data() {
    return this._data;
  }

  set data(d) {
    this._data = d;
    this.ht.loadData(this._data);
  }

  // Returns source data
  getDataAtRow(row) {
    return (this.ht) ? this.ht.getSourceDataAtRow(row) : null;
  }

  get columns() { return this._columns; }
  set columns(columns) {
    this._columns = columns;
    this._columnsByField = {};
    for (var c of columns) {
      this._columnsByField[c.data] = c;
    }
    this._update();
  }

  createCell(row, col, prop) {
    var cell = {};
    var doc = this.getDataAtRow(row);
    var permissions = doc && doc.contextParameters && doc.contextParameters.permissions;
    if (permissions && (permissions.indexOf('Write') === -1)) {
      cell.readOnly = true;
    }
    return cell;
  }

  set nxql(q) {
    this.queryParameters = [q];
  }

  set queryParameters(p) {
    this.query.queryParameters = p;
  }

  set namedParameters(p) {
    this.query.namedParameters = p;
  }

  set sortInfos(sortInfos) {
    this.query.sortBy = sortInfos.map((s) => s.sortColumn);
    this.query.sortOrder = sortInfos.map((s) => s.sortAscending ? 'asc' : 'desc');
  }

  _fetch() {
    return this.query.run().then((result) => {
      Array.prototype.push.apply(this._data, result.entries);
      // prevent adding new rows
      this.ht.updateSettings({maxRows: this._data.length});
      this.ht.render();
      if (result.isNextPageAvailable) {
        this.query.page++;
        return this._fetch();
      }
    });
  }

  update() {
    this._data.length = 0;
    this._dirty = {};
    this.query.page = 0;
    this.ht.clearUndo();
    return this._fetch();
  }

  save() {
    return Promise.all(
      Object.keys(this._dirty).map((uid) => {
        return new Promise((resolve, reject) => {
          try {
            // TODO(nfgs) - Move request execution to the connection
            this.connection.request('/id/' + uid)
              .put(
              {data: this._dirty[uid]},
              (error) => {
                if (error !== null) {
                  this._dirty[uid]._error = error;
                  reject(Error(error));
                  return;
                }
                delete this._dirty[uid];
                resolve(uid);
              });
          } catch (e) {
            this._dirty[uid]._error = e;
            reject(Error(e));
          }
        });
      })
    ).catch((err) => {
      console.error(err);
    }).then((result) => {
      this.ht.clearUndo();
      this.ht.render();
      return result;
    });
  }

  onChange(change, source) {
    if (source === 'loadData') {
      this._dirty = {};
      return;
    }
    if (change !== null) {
      for (var i = 0; i < change.length; i++) {
        var [idx, field, oldV, newV] = change[i];
        if (oldV === newV) {
          continue;
        }
        var uid = this.data[idx].uid;
        var doc = this._dirty[uid] = this._dirty[uid] || {'entity-type': 'document', uid: uid};

        // Split csv values into array
        var column = this._columnsByField[field];
        if (column.multiple && !Array.isArray(newV)) {
          newV = newV ? newV.split(',') : [];
        }

        // Fix empty object values
        if (!newV && typeof oldV === 'object') {
          newV = null;
        }

        assign(doc, field, newV);
      }
      if (this.autosave) {
        this.save();
      }
      this.ht.render();
    }
  }

  beforeAutofill(start, end, data) {
    var ht = this.ht.getInstance();
    var editor = ht.getActiveEditor();
    if (!editor || !(editor instanceof DirectoryEditor)) {
      return;
    }
    if (!data && (data.length === 0 || data[0].length === 0)) {
      console.warn('It is not expected to have an empty data set.');
      return;
    }

    var draggingDirection = new WalkontableCellRange(undefined, ht.getSelectedRange().highlight, start).getDirection();
    if (draggingDirection.charAt(0) === 'S') {
      data = data.reverse();
    }

    var originalCornerCell = this._getCornerCell(ht.getSelectedRange().from, ht.getSelectedRange().to, draggingDirection);
    for (var i = start.row; i <= end.row; i++) {
      for (var j = start.col; j <= end.col; j++) {
        var dataRowIndex = (i - start.row) % data.length;
        var dataColIndex = (j - start.col) % data[0].length;
        var dataEntry = data[dataRowIndex][dataColIndex];
        var formattedLabel = editor.formatter(dataEntry);
        if (!formattedLabel) {
          // resolved || unresolved (when just filled in)
          var id = (dataEntry.properties && dataEntry.properties.id) || dataEntry;
          var cell = ht.getCellMeta(i, j);
          if (!cell._labels) {
            cell._labels = {};
          }
          var originalCell = ht.getCellMeta(originalCornerCell.row + dataRowIndex, originalCornerCell.col + dataColIndex);
          if (originalCell._labels) {
            cell._labels[id] = originalCell._labels[id];
          }
        }
      }
    }
  }

  beforeCopy(value, row, prop) {
    if (typeof value === 'object') {
      const editorName = this.ht.getCellEditor(row, this.ht.propToCol(prop));
      const editor = Handsontable.editors.getEditor(editorName, this.ht);
      if (editor instanceof Select2Editor) {
        value = Array.isArray(value) ? value.map(editor.getEntryId) : editor.getEntryId(value);
      }
    }
    return value;
  }

  _getCornerCell(start, end, draggingDirection) {
    var range = new WalkontableCellRange(undefined, start, end);
    switch (draggingDirection) {
      case 'NW-SE' :
        return range.getBottomRightCorner();
      case 'NE-SW' :
        return range.getBottomLeftCorner();
      case 'SE-NW' :
        return range.getTopLeftCorner();
      case 'SW-NE' :
        return range.getTopRightCorner();
    }
  }

  dirtyRenderer(instance, td, row, col, prop, value, cellProperties) {
    Handsontable.renderers.TextRenderer.apply(this, arguments);
    var doc = this.getDataAtRow(row);
    if (doc && this._dirty[doc.uid]) {
      // color dirty rows
      var color = '#e2f1ff';
      // check for errors
      if (this._dirty[doc.uid].hasOwnProperty('_error')) {
        color = '#f33';
      // check for dirty property
      } else if (hasProp(this._dirty[doc.uid], prop)) {
        color = '#afd8ff';
      }
      $(td).css({
        background: color
      });
    }
  }

  destroy() {
    this.ht.destroy();
  }

  _update() {
    let options = $.extend({}, this.options);
    options.colHeaders = this.columns.map((c) => c.header);
    options.columns = this.columns;
    this.ht.updateSettings(options);
  }
}

// Renderers
var ReadOnlyRenderer = function (instance, td, row, col, prop, value, cellProperties) {
  Handsontable.renderers.TextRenderer.apply(this, arguments);
  td.style.color = 'green';
  td.style.background = '#CEC';
};
Handsontable.renderers.registerRenderer('readOnly', ReadOnlyRenderer);

// Property Utils

// http://stackoverflow.com/questions/13719593/javascript-how-to-set-object-property-given-its-string-name
function assign(obj, prop, value) {
  if (typeof prop === 'string') {
    prop = prop.split('.');
  }

  if (prop.length > 1) {
    var e = prop.shift();
    assign(obj[e] = Object.prototype.toString.call(obj[e]) === '[object Object]' ? obj[e] : {}, prop, value);
  } else {
    obj[prop[0]] = value;
  }
}

function hasProp(obj, prop) {
  if (typeof prop === 'string') {
    prop = prop.split('.');
  }

  if (prop.length > 1) {
    var e = prop.shift();
    return hasProp(obj[e] = Object.prototype.toString.call(obj[e]) === '[object Object]' ? obj[e] : {}, prop);
  } else {
    return obj.hasOwnProperty(prop[0]);
  }
}

export {Spreadsheet};
