(function() {

  var
    settingsParser = new HotTableUtils.SettingsParser(),
    lastSelectedCellMeta;


  Polymer('hot-table', {
    publish: settingsParser.getPublishMethodsAndProps(),

    /**
     * @property instance
     * @type Handsontable
     * @default null
     */
    instance: null,


    /**
     * On create element but not attached to DOM
     */
    created: function() {
      this.activeNestedTable = null;
      this.nestedTables = null;
      this.destroyed = false;
    },

    /**
     * On attached element to DOM
     */
    attached: function() {
      this.instance = new Handsontable(this.$.htContainer, settingsParser.parse(this));

      this.collectNestedTables();
      this.registerHooks();

      if (Array.isArray(this.datarows) && this.datarows.length && this.colHeaders !== null) {
        if (typeof this.datarows[0] === 'object' && !Array.isArray(this.datarows[0])) {
          this.colHeaders = Object.keys(this.datarows[0]);
        }
      }
    },

    /**
     * Try to destroy handsontable instance if hadn't been destroyed
     */
    detached: function() {
      if (this.instance && !this.destroyed) {
        this.instance.destroy();
      }
    },

    /**
     * Register hooks
     */
    registerHooks: function() {
      var _this = this;

      if (!Handsontable.Dom.isChildOfWebComponentTable(this.parentNode)) {
        Handsontable.hooks.add('beforeOnCellMouseDown', function() {
          _this.onBeforeOnCellMouseDown.apply(_this, [this].concat(Array.prototype.slice.call(arguments)));
        });
        Handsontable.hooks.add('afterOnCellMouseDown', function() {
          _this.onAfterOnCellMouseDown.apply(_this, [this].concat(Array.prototype.slice.call(arguments)));
        });
      }
      this.addHook('afterModifyTransformStart', this.onAfterModifyTransformStart.bind(this));
      this.addHook('beforeKeyDown', this.onBeforeKeyDown.bind(this));
      this.addHook('afterDeselect', function() {
        _this.highlightedRow = -1;
        _this.highlightedColumn = -1;
      });
      this.addHook('afterSelectionEnd', function() {
        var range = _this.getSelectedRange();

        _this.highlightedRow = range.highlight.row;
        _this.highlightedColumn = range.highlight.col;
      });
      this.addHook('afterDestroy', function() {
        _this.destroyed = true;
      });
    },

    /**
     * Detect and collect all founded nested hot-table's
     */
    collectNestedTables: function() {
      var parentTable = null,
        isNative = Handsontable.Dom.isWebComponentSupportedNatively();

      if (!Handsontable.Dom.isChildOfWebComponentTable(this.parentNode)) {
        parentTable = this;
      }
      this.nestedTables = new HotTableUtils.NestedTable(this);
      this.nestedTables.setStrategy(isNative ? 'native' : 'emulation', parentTable);
      this.nestedTables.update();
    },

    /**
     * @param {Handsontable} hotInstance
     * @param {DOMEvent} event
     * @param {Object} coords
     * @param {HTMLElement} TD
     */
    onBeforeOnCellMouseDown: function(hotInstance, event, coords, TD) {
      var cellMeta;

      if (!this.nestedTables.isNested(hotInstance) && hotInstance !== this.instance) {
        return;
      }
      cellMeta = hotInstance.getCellMeta(coords.row, coords.col);

      if (this.activeNestedTable) {
        cellMeta.disableVisualSelection = true;

      } else {
        cellMeta.disableVisualSelection = false;
        this.activeNestedTable = hotInstance;
      }
      // on last event set first table as listening
      if (hotInstance === this.instance) {
        this.activeNestedTable.listen();
        this.activeNestedTable = null;
      }
    },

    /**
     * @param {Handsontable} hotInstance
     * @param {DOMEvent} event
     * @param {Object} coords
     */
    onAfterOnCellMouseDown: function(hotInstance, event, coords) {
      var cellMeta;

      if (!this.nestedTables.isNested(hotInstance) && hotInstance !== this.instance) {
        return;
      }
      cellMeta = hotInstance.getCellMeta(coords.row, coords.col);
      cellMeta.disableVisualSelection = false;
    },

    /**
     * @param {WalkontableCellCoords} coords
     * @param {Number} rowTransform
     * @param {Number} colTransform
     */
    onAfterModifyTransformStart: function(coords, rowTransform, colTransform) {
      var parent = this.nestedTables.getParent(),
        cellMeta,
        newCoords,
        selected;

      cellMeta = this.getCellMeta(coords.row, coords.col);
      cellMeta.disableVisualSelection = false;
      lastSelectedCellMeta = cellMeta;

      if (parent && (rowTransform !== 0 || colTransform !== 0)) {
        selected = parent.getSelected();
        cellMeta.disableVisualSelection = true;
        newCoords = {
          row: selected[0] + rowTransform,
          col: selected[1] + colTransform
        };

        if (newCoords.row < 0 || newCoords.row >= parent.countRows()) {
          newCoords.row -= rowTransform;
        }
        if (newCoords.col < 0 || newCoords.col >= parent.countCols()) {
          newCoords.col -= colTransform;
        }
        cellMeta = parent.getCellMeta(newCoords.row, newCoords.col);
        cellMeta.disableVisualSelection = false;
        lastSelectedCellMeta = cellMeta;

        parent.selectCell(newCoords.row, newCoords.col, undefined, undefined, true, false);
        parent.listen();
      }
    },

    /**
     * @param {DOMEvent} event
     */
    onBeforeKeyDown: function(event) {
      var td, childTable, cellMeta;

      if (!this.isListening() || event.keyCode !== Handsontable.helper.keyCode.ENTER) {
        return;
      }
      if (!lastSelectedCellMeta || lastSelectedCellMeta.editor !== false) {
        return;
      }
      td = this.getCell(lastSelectedCellMeta.row, lastSelectedCellMeta.col);

      if (td) {
        cellMeta = this.getCellMeta(lastSelectedCellMeta.row, lastSelectedCellMeta.col);
        cellMeta.disableVisualSelection = true;
        // Refresh cell border according to disableVisualSelection setting
        this.selectCell(lastSelectedCellMeta.row, lastSelectedCellMeta.col);

        childTable = td.querySelector(this.nodeName);
        cellMeta = childTable.getCellMeta(0, 0);
        cellMeta.disableVisualSelection = false;
        lastSelectedCellMeta = cellMeta;
        childTable.selectCell(0, 0, undefined, undefined, true, false);

        setTimeout(function() {
          childTable.listen();
        }, 0);
      }
    },

    onMutation: function() {
      var columns;

      if (this === window) {
        // it is a bug in Polymer or Chrome as of Nov 29, 2013
        return;
      }
      if (!this.instance) {
        // happens in Handsontable WC demo page in Chrome 33-dev
        return;
      }
      columns = settingsParser.parseColumns(this);

      if (columns.length) {
        this.updateSettings({columns: columns});
      }
    }
  });
}());
