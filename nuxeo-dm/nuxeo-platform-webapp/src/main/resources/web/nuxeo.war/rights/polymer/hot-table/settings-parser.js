(function(w) {

  var
    publicMethods = [
      'addHook',
      'addHookOnce',
      'alter',
      'clear',
      'clearUndo',
      'colOffset',
      'colToProp',
      'countCols',
      'countEmptyCols',
      'countEmptyRows',
      'countRenderedCols',
      'countRenderedRows',
      'countRows',
      'countVisibleCols',
      'countVisibleRows',
      'deselectCell',
      'destroy',
      'destroyEditor',
      'determineColumnWidth',
      'getCell',
      'getCellEditor',
      'getCellMeta',
      'getCellRenderer',
      'getCellValidator',
      'getColHeader',
      'getColWidth',
      'getCopyableData',
      'getData',
      'getDataAtCell',
      'getDataAtCol',
      'getDataAtProp',
      'getDataAtRow',
      'getDataAtRowProp',
      'getInstance',
      'getRowHeader',
      'getRowHeight',
      'getSchema',
      'getSelected',
      'getSelectedRange',
      'getSettings',
      'getSourceDataAtCol',
      'getSourceDataAtRow',
      'getValue',
      'hasColHeaders',
      'hasRowHeaders',
      'init',
      //'isEmptyCol',
      //'isEmptyRow',
      'isListening',
      'isRedoAvailable',
      'isUndoAvailable',
      'listen',
      'loadData',
      'populateFromArray',
      'propToCol',
      'redo',
      'removeCellMeta',
      'removeHook',
      'render',
      'rowOffset',
      'runHooks',
      'selectCell',
      'selectCellByProp',
      'setCellMeta',
      'setCellMetaObject',
      'setDataAtCell',
      'setDataAtRowProp',
      'spliceCol',
      'spliceRow',
      'undo',
      'unlisten',
      'updateSettings',
      'validateCell',
      'validateCells'
    ],
    publicHooks = Object.keys(Handsontable.hooks.hooks),
    publicProperties = Object.keys(Handsontable.DefaultSettings.prototype),
    wcDefaults = webComponentDefaults()
  ;

  publicProperties = publicProperties.concat(publicHooks);

  function webComponentDefaults() {
    return {
      observeChanges: true
    };
  }

  /**
   * @constructor
   */
  function SettingsParser() {

  }

  /**
   * Get handsontable public methods and properties
   *
   * @returns {Object}
   */
  SettingsParser.prototype.getPublishMethodsAndProps = function() {
    var _this = this,
      publish = {};

    publicMethods.forEach(function(hotMethod) {
      publish[hotMethod] = function() {
        return this.instance[hotMethod].apply(this.instance, arguments);
      };
    });

    publicProperties.forEach(function(hotProp) {
      var wcProp, val;

      if (publish[hotProp]) {
        return;
      }
      wcProp = hotProp;

      if (hotProp === 'data') {
        wcProp = 'datarows';

      } else if (hotProp === 'title') {
        // rename 'title' attribute to 'header' because 'title' was causing
        // problems (https://groups.google.com/forum/#!topic/polymer-dev/RMMsV-D4HVw)
        wcProp = 'header';
      }
      val = wcDefaults[hotProp] === void 0 ? Handsontable.DefaultSettings.prototype[hotProp] : wcDefaults[hotProp];

      if (val === void 0) {
        // Polymer does not like undefined
        publish[wcProp] = null;

      } else if (hotProp === 'observeChanges') {
        // on by default
        publish[wcProp] = true;

      } else {
        publish[wcProp] = val;
      }

      publish[wcProp + 'Changed'] = function() {
        var settings = {};

        // attribute changed callback called before attached
        if (!this.instance || this.destroyed) {
          return;
        }
        if (wcProp === 'settings') {
          settings = _this.getModelPath(this, this[wcProp]);

        } else {
          settings[hotProp] = _this.readOption(this, wcProp, this[wcProp]);
        }

        if (wcProp === 'datarows') {
          if (settings[hotProp] !== this.instance.getSettings()[hotProp]) {
            this.updateSettings(settings);
          }
        } else {
          // TODO (performance) On Chrome (natively supported web components) every single attribute fired updateSettings
          this.updateSettings(settings);
        }
      };
    });
    publish.highlightedRow = -1;
    publish.highlightedColumn = -1;

    return publish;
  };

  /**
   * Parse hot-table to build handsontable settings object
   *
   * @param {HTMLElement} hotTable
   * @returns {Object}
   */
  SettingsParser.prototype.parse = function(hotTable) {
    var columns = this.parseColumns(hotTable),
      options = webComponentDefaults(),
      attrName, settingsAttr, i, iLen;

    for (i = 0, iLen = publicProperties.length; i < iLen; i ++) {
      attrName = publicProperties[i];

      if (attrName === 'data') {
        attrName = 'datarows';
      }
      options[publicProperties[i]] = this.readOption(hotTable, attrName, hotTable[attrName]);
    }

    if (hotTable.settings) {
      settingsAttr = this.getModelPath(hotTable, hotTable.settings);

      for (i in settingsAttr) {
        if (settingsAttr.hasOwnProperty(i)) {
          options[i] = settingsAttr[i];
        }
      }
    }

    if (columns.length) {
      options.columns = columns;
    }
    // Polymer reports null default values for all declared custom element properties.
    // We don't want them to override Handsontable defaults
    options = this.filterNonNull(options);

    return options;
  };

  /**
   * Parse hot-table columns (hot-column) to build handsontable columns settings object
   *
   * @param {HTMLElement} hotTable
   * @returns {Array}
   */
  SettingsParser.prototype.parseColumns = function(hotTable) {
    var columns = [],
      i, iLen;

    for (i = 0, iLen = hotTable.childNodes.length; i < iLen; i++) {
      if (hotTable.childNodes[i].nodeName === 'HOT-COLUMN') {
        columns.push(this.parseColumn(hotTable, hotTable.childNodes[i]));
      }
    }

    return columns;
  };

  /**
   * Parse hot-column to build handsontable column settings object
   *
   * @param {HTMLElement} hotTable
   * @param {HTMLElement} hotColumn
   * @returns {Object}
   */
  SettingsParser.prototype.parseColumn = function(hotTable, hotColumn) {
    var object = {},
      innerHotTable,
      attrName,
      len,
      val,
      i;

    for (i = 0, len = publicProperties.length; i < len; i++) {
      attrName = publicProperties[i];

      if (attrName === 'data') {
        attrName = 'value';

      } else if (attrName === 'title') {
        attrName = 'header';

      } else if (attrName === 'className') {
        attrName = 'class';
      }

      if (hotColumn[attrName] === null) {
        continue; // default value

      } else if (hotColumn[attrName] !== void 0 && hotColumn[attrName] !== '') {
        val = hotColumn[attrName];

      } else {
        // Dec 3, 2013 - Polymer returns empty string for node properties such as hotcolumn.width
        val = hotColumn.getAttribute(attrName);
      }

      if (val !== void 0 && val !== hotTable[attrName]) {
        object[publicProperties[i]] = this.readOption(hotColumn, attrName, val);
      }
    }
    innerHotTable = hotColumn.getElementsByTagName('hot-table');

    if (innerHotTable.length) {
      object.handsontable = new Settings(innerHotTable[0]).parse();
    }

    return object;
  };

  /**
   * Read hot-table single option (attribute)
   *
   * @param {HTMLElement} hotTable
   * @param {String} key
   * @param {*} value
   * @returns {*}
   */
  SettingsParser.prototype.readOption = function(hotTable, key, value) {
    if (key === 'datarows' || key === 'renderer' || key === 'source' || key === 'dataSchema') {
      return this.getModelPath(hotTable, value);
    }
    if (key === 'className') {
      return value;
    }

    return this.readBool(value);
  };

  /**
   * Try to read value as boolean if not return untouched value
   *
   * @param {*} value
   * @returns {*}
   */
  SettingsParser.prototype.readBool = function(value) {
    if (value === void 0 || value === 'false') {
      return false;

    } else if (value === '' || value === 'true') {
      return true;
    }

    return value;
  };

  /**
   * @param {Object} object
   * @returns {Object}
   */
  SettingsParser.prototype.filterNonNull = function(object) {
    var result = {};

    for (var i in object) {
      if (object.hasOwnProperty(i) && object[i] !== null) {
        result[i] = object[i];
      }
    }

    return result;
  };

  /**
   * @param {HTMLElement} hotTable
   * @returns {*}
   */
  SettingsParser.prototype.getModel = function(hotTable) {
    var model;

    if (hotTable.templateInstance) {
      model = hotTable.templateInstance.model;
    } else {
      model = window;
    }

    return model;
  };

  /**
   * @param {HTMLElement} hotTable
   * @param {*} path
   * @returns {*}
   */
  SettingsParser.prototype.getModelPath = function(hotTable, path) {
    var model, expression, object;

    // happens in Polymer when assigning
    // as datarows="{{ model.subpage.people }}" or settings="{{ model.subpage.settings }}
    if (typeof path === 'object' || typeof path === 'function') {
      return path;
    }
    model = this.getModel(hotTable);
    expression = 'with(model) { ' + path + ';}';
    /* jshint -W061 */
    object = eval(expression);

    return object;
  };


  w.HotTableUtils = w.HotTableUtils || {};
  w.HotTableUtils.SettingsParser = SettingsParser;

}(window));
