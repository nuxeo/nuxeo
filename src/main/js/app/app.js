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
import {Connection} from './nuxeo/connection';
import {Log} from './ui/log';
import {Spreadsheet} from './ui/spreadsheet';
import {parseNXQL} from './nuxeo/util/nxql';

// Extract the parameters (content view state and page provider)
var {cv, pp} = parseParams();

// Parse the content view state
cv = cv && JSON.parse(atob(cv));

// Check if we're in standalone mode
var isStandalone = !cv;

// Our Spreadsheet instance
var sheet;

var log;

function setupUI() {

  log = new Log($('#console'));

  // Setup popup UI
  if (!isStandalone) {
    $('#close').click(function() {
      parent.jQuery.fancybox.close();
    });
    $('#close').toggle(true);

  // Setup standalone UI
  } else {
    $('#queryArea').toggle(true);
    $('#execute').click(doQuery);
  }

  $('#save').click(() => {
    log.info('Saving...');
    sheet.save().then((results) => {
      if (!results) {
        log.error('Failed to save changes.');
        return;
      }
      var msg;
      if (results.length === 0) {
        msg = 'Everything up to date.';
      } else {
        msg = `Saved ${results.length} rows.`;
      }
      log.info(msg);
    });
  });

  $('input[name=autosave]').click(function() {
    sheet.autosave = $(this).is(':checked');
    if (sheet.autosave) {
      log.default('Each change is automatically saved.');
    } else {
      log.default('');
    }
  });

  $(document).ajaxStart(() => $('#loading').show());
  $(document).ajaxStop(() => $('#loading').hide());
}

function doQuery() {
  // Only parse queries in standalone mode
  if (isStandalone) {
    var q = $('#query').val();
    sheet.nxql = parseNXQL(q);
  }
  sheet.update().catch(function(err) {
    log.error(err.message);
  });
}

function run() {

  // Setup our connection
  var nx = new Connection();
  nx.schemas(['*']);

  $(() => {

    setupUI();

    nx.connect().then(() => {
      // Extract content view configuration
      var layout = (cv && cv.resultLayout && cv.resultLayout.name) || 'spreadsheet_listing',
          resultColumns = cv && cv.resultColumns;

      var pageProvider = pp || 'spreadsheet_query';

      // Setup the SpreadSheet
      sheet = new Spreadsheet($('#grid'), nx, layout, resultColumns, pageProvider);

      // Add query parameters
      if (cv && cv.queryParameters) {
        sheet.queryParameters = cv.queryParameters;
      }

      // Add the search document
      if (cv && cv.searchDocument) {
        var namedParameters = {};
        for (var k in cv.searchDocument.properties) {
          var v = cv.searchDocument.properties[k];
          // skip empty values
          if ((typeof(v.length) !== 'undefined') && (v.length === 0)) {
            continue;
          }
          namedParameters[k] = JSON.stringify(v);
        }
        sheet.namedParameters = namedParameters;
      }

      if (!isStandalone) {
        doQuery();
      }
    });
  });
}

// Utils
function parseParams() {
  var parameters = {};
  var query = window.location.search.replace('?', '');
  if (query.length === 0) {
    return parameters;
  }
  var params = query.split('&');
  for (var param of params) {
    var [k, v] = param.split('=');
    v = v.replace(/\+/g, ' ');
    parameters[k] = decodeURIComponent(v);
  }
  return parameters;
}

export {run};
