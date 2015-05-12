(function(w) {
  /**
   * @param {HTMLElement} hotTable
   * @constructor
   */
  function NestedTable(hotTable) {
    this.hotTable = hotTable;
  }

  NestedTable.strategies = {};

  /**
   * @param {String} strategyName
   * @param {HTMLElement} rootHotTable
   */
  NestedTable.prototype.setStrategy = function(strategyName, rootHotTable) {
    var strategy;

    if (NestedTable.strategies[strategyName]) {
      strategy = new NestedTable.strategies[strategyName](rootHotTable);

    } else {
      throw new Error('Strategy name (' + strategyName + ') is not supported');
    }
    this.strategy = strategy;
  };

  /**
   * Push nested table to collection
   *
   * @param {HTMLElement} hotTable
   */
  NestedTable.prototype.push = function(hotTable) {
    if (this.strategy.tables.indexOf(hotTable) === -1) {
      this.strategy.tables.push(hotTable);
    }
  };

  /**
   * Get child tables
   *
   * @returns {Array} Array of HTMLElements (hot-table)
   */
  NestedTable.prototype.getChildren = function() {
    return this.strategy.tables;
  };

  /**
   * Get parent table
   *
   * @returns {NestedTable}
   */
  NestedTable.prototype.getParent = function() {
    return this.parent;
  };

  /**
   * Set parent table
   *
   * @returns {NestedTable}
   */
  NestedTable.prototype.setParent = function(parent) {
    this.parent = parent;
  };

  /**
   * Collect tables
   */
  NestedTable.prototype.update = function() {
    this.strategy.update(this.hotTable);
  };

  /**
   * Checks if HOT instance belongs to the nested tables
   *
   * @param {Handsontable} hotInstance
   * @returns {Boolean}
   */
  NestedTable.prototype.isNested = function(hotInstance) {
    function isNestedTable(nestedTable, hotInstance) {
      var result = false,
        tables = nestedTable.getChildren();

      for (var i = 0, len = tables.length; i < len; i++) {
        if (wrap(tables[i]).instance === hotInstance) {
          result = true;
          break;
        }
        if (isNestedTable(wrap(tables[i]).nestedTables, hotInstance)) {
          result = true;
          break;
        }
      }

      return result;
    }

    return isNestedTable(this, hotInstance);
  };

  w.HotTableUtils = w.HotTableUtils || {};
  w.HotTableUtils.NestedTable = NestedTable;

}(window));


(function(strategies) {
  strategies.emulation = EmulationSupport;

  /**
   * Strategy for browsers which not support web components natively (emulation from polymer).
   * Update is called by hot-table from child to parent.
   *
   * @param {HTMLElement} rootHotTable
   * @constructor
   */
  function EmulationSupport(rootHotTable) {
    this.rootHotTable = rootHotTable;
    this.tables = [];
  }

  /**
   * @param {HTMLElement} hotTable
   */
  EmulationSupport.prototype.update = function(hotTable) {
    var latestParent = null;

    if (this.rootHotTable && this.rootHotTable === hotTable) {
      hotTable.addEventListener('initialize', function(event) {
        var target, parent;

        event = unwrap(event);
        target = event.target;

        if (target === unwrap(hotTable)) {
          latestParent = target;

          return;
        }
        parent = Handsontable.Dom.closest(target.parentNode, [target.nodeName]);

        if (parent === latestParent) {
          wrap(latestParent).nestedTables.push(wrap(target));
          wrap(target).nestedTables.setParent(wrap(latestParent));

        } else {
          latestParent = parent;
          wrap(latestParent).nestedTables.push(wrap(target));
          wrap(target).nestedTables.setParent(wrap(latestParent));
        }
      });
    }
    hotTable.fire('initialize');
  };
}(HotTableUtils.NestedTable.strategies));


(function(strategies) {
  strategies.native = NativeSupport;

  /**
   * Strategy for browsers which support web components natively.
   * Update is called by hot-table from parent to child.
   *
   * @param {HTMLElement} rootHotTable
   * @constructor
   */
  function NativeSupport(rootHotTable) {
    this.rootHotTable = rootHotTable;
    this.tables = [];
  }

  /**
   * @param {HTMLElement} hotTable
   */
  NativeSupport.prototype.update = function(hotTable) {
    var childHotTables = hotTable.instance.rootElement.querySelectorAll(hotTable.nodeName),
      index = childHotTables.length,
      parentTable;

    while (index --) {
      childHotTables[index].nestedTables.setParent(hotTable);
      this.tables.unshift(childHotTables[index]);
    }
    // On table new col/row insert update nested tables collection
    parentTable = Handsontable.Dom.closest(hotTable.parentNode, [hotTable.nodeName]);

    if (parentTable && parentTable.nestedTables) {
      parentTable.nestedTables.push(hotTable);
      hotTable.nestedTables.setParent(parentTable);
    }
  };
}(HotTableUtils.NestedTable.strategies));


