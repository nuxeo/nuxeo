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
import {Directory} from '../nuxeo/rpc/directory';
import {Query} from '../nuxeo/rpc/query';

/**
 * Spreadsheet backed by Hansontable
 */
class Spreadsheet {

  constructor(container, connection, layout = 'spreadsheet_listing', columns = null) {
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
      contextMenu: ['undo', 'redo', 'sep1', 'sep2', 'sep3'],
      afterChange: this.onChange.bind(this),
      search: true,
      cells: this.createCell.bind(this)
    };

    this.query = new Query(connection);
    this.query.enrichers = ['permissions', 'vocabularies'];
    this.query.pageSize = 1000;

    new Layout(connection, layout).fetch().then((layout) => {
      // Check which columns to display
      var cols = (!columns) ?
          layout.columns.filter((c) =>  c.selectedByDefault !== false)
          :
          layout.columns.filter((c) => columns.indexOf(c.name) !== -1);
      this.columns = cols
          // Exclude columns without widgets
          .filter((c) => c.widgets)
          // Create our columns wrapper
          .map((c) => new Column(connection, c, layout.widgets[c.widgets[0].name], this.dirtyRenderer.bind(this)))
          // Only show columns with a known widget type and with a field
          .filter((c) => c.hasSupportedWidgetType && c.field);
    });

    this._dirty = {};
    this.container.handsontable(this.options);
    this.ht = this.container.data('handsontable');
  }

  get data() { return this._data; }
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

  _fetch() {
    return this.query.run()
        .then((result) => {
          Array.prototype.push.apply(this._data, result.entries);
          this.ht.render();
          if (result.isNextPageAvailable) {
            this.query.page++;
            return this._fetch();
          }
        });
  }

  update() {
    this.query.page = 0;
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
      console.log(err);
    }).then((result) => {
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
        var doc = this._dirty[uid] = this._dirty[uid] || {};

        // Split csv values into array
        var column = this._columnsByField[field];
        if (column.multiple && !Array.isArray(newV)) {
          newV = newV.split(',');
        }

        assign(doc, field, newV);
      }
      if (this.autosave) {
        this.save();
      }
      this.ht.render();
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
      } else  if (hasProp(this._dirty[doc.uid], prop)) {
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
    var options = $.extend({}, this.options);
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
    assign(obj[e] = Object.prototype.toString.call(obj[e]) === '[object Object]'? obj[e] : {},
      prop,
      value);
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
    return hasProp(obj[e] = Object.prototype.toString.call(obj[e]) === '[object Object]'? obj[e] : {},
      prop);
  } else {
    return obj.hasOwnProperty(prop[0]);
  }
}

export {Spreadsheet};
