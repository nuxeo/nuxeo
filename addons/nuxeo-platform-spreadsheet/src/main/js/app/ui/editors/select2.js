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

class Select2Editor extends Handsontable.editors.Select2Editor {
  constructor(instance) {
    super(instance);
  }

  // Let's override prepare and just pass set the select2 options ourselves
  prepare(row, col, prop, td, originalValue, cellProperties) {

    // cellProperties is an instance our our Column
    var widget = cellProperties.widget,
        connection = cellProperties.connection;

    Handsontable.editors.TextEditor.prototype.prepare.apply(this, arguments);

    var isMultiple = cellProperties.multiple;

    // See :
    // https://github.com/nuxeo/nuxeo-features/blob/master/nuxeo-platform-ui-select2/src/main/resources/web/nuxeo.war/scripts/select2/nuxeo-select2.js
    // https://github.com/nuxeo/nuxeo-features/blob/master/nuxeo-platform-ui-select2/src/main/java/org/nuxeo/ecm/platform/ui/select2/Select2ActionsBean.java
    this.options = {
      query: (q) => {
        this.query(connection, widget.properties, q.term)
        .then((results) => { q.callback({results: results}); });
      },
      dropdownAutoWidth: true,
      allowClear: true,
      width: 'resolve',
      minimumInputLength: 0,
      formatResult: this.resultFormatter.bind(this),
      formatSelection: this.selectionFormatter.bind(this),
      multiple: isMultiple,
      placeholder: 'Select a value',
      initSelection: this.initSelection(isMultiple).bind(this),
      id: this.getEntryId
    };

  }

  open() {
    super.open();
    this.$textarea.on('selected', this.onSelected.bind(this));
    this.$textarea.on('select2-selected', this.onSelected.bind(this));
    this.$textarea.on('select2-removed', this.onRemoved.bind(this));
  }

  onSelected(evt) {
    //
  }

  onRemoved(evt) {
    //
  }

  // use same formatter by default
  resultFormatter(entry) { return this.formatter(entry); }
  selectionFormatter(entry) { return this.formatter(entry); }

  initSelection(isMultiple) {
    if (!isMultiple) {
      return (element, callback) => callback(
        {id: element.val(), text: element.val()}
      );
    }
    return (element, callback) => callback(
      element.val().split(',').map((v) => ({id: v, text: v}))
    );
  }

  getEntryId(item) {
    return item.id;
  }

  // Must return a promise
  query(connection, properties, term) {
    //
  }
}

export {Select2Editor};
