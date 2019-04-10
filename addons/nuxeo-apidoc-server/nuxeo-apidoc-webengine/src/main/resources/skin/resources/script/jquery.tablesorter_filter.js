/*
 * Copyright (c) 2008 Justin Britten justinbritten at gmail.com
 *
 * Some code was borrowed from:
 * 1. Greg Weber's uiTableFilter project (http://gregweber.info/projects/uitablefilter)
 * 2. Denny Ferrassoli & Charles Christolini's TypeWatch project (www.dennydotnet.com)
 *
 * Contributions have been made by:
 * René Leonhardt (github.com/rleonhardt)
 * Thomas Kappler (github.com/thomas11)
 *
 * Dual licensed under the MIT and GPL licenses:
 * http://www.opensource.org/licenses/mit-license.php
 * http://www.gnu.org/licenses/gpl.html
 *
 */


(function($) {
  $.extend({
    tablesorterFilter: new function() {

      // Default filterFunction implementation (element text, search words, case-sensitive flag)
      function has_words(str, words, caseSensitive) {
        var text = caseSensitive ? str : str.toLowerCase();

        for (var i=0; i < words.length; i++) {
          if (words[i].charAt(0) == '-') {
            if (text.indexOf(words[i].substr(1)) != -1) return false; // Negated word must not be in text
          } else if (text.indexOf(words[i]) == -1) return false; // Normal word must be in text
        }

        return true;
      }


      function doFilter(table) {
        if(table.config.debug) { var cacheTime = new Date(); }

        // Build multiple filters from input boxes
        // TODO: enable incremental filtering by caching result and applying only single filter action
        var filters = [];

      	// Implement dynamic selection of a filter column
      	// by setting filter.filterColumns based on the user's
      	// query. That means we have to restore the base configuration
      	// of filterColumns after processing the query.
	      var defaultFilterColumns = [];

        for(var i=0; i < table.config.filter.length; i++) {
          var filter = table.config.filter[i];
          var container = $(filter.filterContainer);

	        // Record the base setting of filtered columns to
	        // be able to restore it later, see above.
          defaultFilterColumns[i] = filter.filterColumns;

          // Trim and unify whitespace before splitting
          var phrase = jQuery.trim(container.val()).replace(/\s+/g, ' ');
          if(phrase.length != 0) {

            // Check for a 'col:' prefix.
            var field_prefix = /^([a-z]+):(.+)/;
            var match = field_prefix.exec(phrase);
            if (match !== null) {
              // The user wants to filter based on a
              // certain column. Find the index of that column and
              // set filterColumns accordingly.
              var field = match[1];
              phrase = match[2];
              for (var k=0; k < filter.columns.length; k++) {
                if (filter.columns[k].indexOf(field) === 0) {
                  filter.filterColumns = [k];
                  break;
                }
              }
	          }

            var caseSensitive = filter.filterCaseSensitive;
            filters.push({
              caseSensitive: caseSensitive,
              words: caseSensitive ? phrase.split(" ") : phrase.toLowerCase().split(" "),
              findStr: filter.filterColumns ? "td:eq(" + filter.filterColumns.join("),td:eq(") + ")" : "",
              filterFunction: filter.filterFunction
            });
          }

	        // Restore the base setting of filtered columns
          filter.filterColumns = defaultFilterColumns[i];
        }
        var filterCount = filters.length;

        // Filter cleared?
        if(filterCount == 0) {
          var search_text = function() {
            var elem = jQuery(this);
            resultRows[resultRows.length] = elem;
          }
        } else {
          var search_text = function() {
            var elem = jQuery(this);
            for(var i=0; i < filterCount; i++) {
              if(! filters[i].filterFunction(
		     (filters[i].findStr ? elem.find(filters[i].findStr) : elem).text(),
		     filters[i].words,
		     filters[i].caseSensitive) ) {
                return true; // Skip elem and continue to next element
              }
            }
            resultRows[resultRows.length] = elem;
          }
        }

        // Walk through all of the table's rows and search.
        // Rows which match the string will be pushed into the resultRows array.
        var allRows = table.config.cache.row;
        var resultRows = [];

        var allRowsCount = allRows.length;
        for (var i=0; i < allRowsCount; i++) {
          allRows[i].each ( search_text );
        }

        // Clear the table
        $.tablesorter.clearTableBody(table);

        // Push all rows which matched the search string onto the table for display.
        var resultRowsCount = resultRows.length;
        for (var i=0; i < resultRowsCount; i++) {
          $(table.tBodies[0]).append(resultRows[i]);
        }

        // Update the table by executing some of tablesorter's triggers
        // This will apply any widgets or pagination, if used.
        $(table).trigger("update");
        if (resultRows.length) {
          $(table).trigger("appendCache");
          // Apply current sorting after restoring rows
          $(table).trigger("sorton", [table.config.sortList]);
        }

        if(table.config.debug) { $.tablesorter.benchmark("Apply filter:", cacheTime); }

        // Inform subscribers that filtering finished
        $(table).trigger("filterEnd");

        return table;
      };

      function clearFilter(table) {
        if(table.config.debug) { var cacheTime = new Date(); }

        // Reset all filter values
        for(var i=0; i < table.config.filter.length; i++)
          $(table.config.filter[i].filterContainer).val('').get(0).lastValue = '';

        var allRows = table.config.cache.row;

        $.tablesorter.clearTableBody(table);

        for (var i=0; i < allRows.length; i++) {
          $(table.tBodies[0]).append(allRows[i]);
        }

        $(table).trigger("update");
        $(table).trigger("appendCache");
        // Apply current sorting after restoring all rows
        $(table).trigger("sorton", [table.config.sortList]);

        if(table.config.debug) { $.tablesorter.benchmark("Clear filter:", cacheTime); }

        $(table).trigger("filterCleared");

        return table;
      };

      this.defaults = {
        filterContainer: '#filter-box',
        filterClearContainer: '#filter-clear-button',
        filterColumns: null,
        filterCaseSensitive: false,
        filterWaitTime: 500,
        filterFunction: has_words,
	      columns: []
      };


      this.construct = function() {
        var settings = arguments; // Allow multiple config objects in constructor call

        return this.each(function() {
          this.config.filter = new Array(settings.length);
          var config = this.config;
          config.filter = new Array(settings.length);

          for (var i = 0; i < settings.length; i++)
            config.filter[i] = $.extend(this.config.filter[i], $.tablesorterFilter.defaults, settings[i]);

          var table = this;

          // Create a timer which gets reset upon every keyup event.
          //
          // Perform filter only when the timer's wait is reached (user finished typing or paused long enough to elapse the timer).
          //
          // Do not perform the filter is the query has not changed.
          //
          // Immediately perform the filter if the ENTER key is pressed.

          function checkInputBox(inputBox, override) {
            var value = inputBox.value;

            if ((value != inputBox.lastValue) || (override)) {
              inputBox.lastValue = value;
              doFilter( table );
            }
          };

          var timer = new Array(settings.length);

          for (var i = 0; i < settings.length; i++) {
            var container = $(config.filter[i].filterContainer);
            // TODO: throw error for non-existing filter container?
            if(container.length)
              container[0].filterIndex = i;
            container.keyup(function(e, phrase) {
              var index = this.filterIndex;
              if(undefined !== phrase)
                $(this).val(phrase);
              var inputBox = this;

              // Was ENTER pushed?
              if (inputBox.keyCode == 13 || undefined !== phrase) {
                var timerWait = 1;
                var overrideBool = true;
              } else {
                var timerWait = config.filter[index].filterWaitTime || 500;
                var overrideBool = false;
              }

              var timerCallback = function() {
                checkInputBox(inputBox, overrideBool);
              }

              // Reset the timer
              clearTimeout(timer[index]);
              timer[index] = setTimeout(timerCallback, timerWait);

              return false;
            });

            // Avoid binding click event to whole document if no clearContainer has been defined
            if(config.filter[i].filterClearContainer) {
              var container = $(config.filter[i].filterClearContainer);
              if(container.length) {
                container[0].filterIndex = i;
                container.click(function() {
                  var index = this.filterIndex;
                  var container = $(config.filter[index].filterContainer);
                  container.val("");
                  // Support entering the same filter text after clearing
                  container[0].lastValue = "";
                  // TODO: Clear single filter only
                  doFilter(table);
                  if(container[0].type != 'hidden')
                    container.focus();
                });
              }
            }
          }

          $(table).bind("doFilter",function() {
            doFilter(table);
          });
          $(table).bind("clearFilter",function() {
            clearFilter(table);
          });
        });
      };

    }
  });

  // extend plugin scope
  $.fn.extend({
    tablesorterFilter: $.tablesorterFilter.construct
  });

})(jQuery);