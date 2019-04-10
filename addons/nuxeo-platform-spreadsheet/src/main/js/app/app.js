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

var {layout, query, columns} = parseParams();

// Check if we're in standalone mode
var isStandalone = !query;

// Our Spreadsheet instance
var sheet;

var log;

function setupUI() {

  log = new Log($('#console'));

  // Only display close button in popup
  if (!isStandalone) {
    $('#close').click(function() {
      parent.jQuery.fancybox.close();
    });
    $('#close').toggle(true);
  }

  // Only display query area in standalone
  $('#queryArea').toggle(isStandalone);

  $('#query').val(query);

  $('#execute').click(doQuery);

  $('#save').click(() => {
    log.info('Saving...');
    sheet.save().then((results) => {
      if (!results) {
        log.error("Failed to save changes.");
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
  var q = $('#query').val();
  // Only parse queries in standalone mode
  sheet.nxql = (isStandalone) ? parseNXQL(q) : q;
  sheet.update().catch(function(err) {
    log.error(err.message);
  })
}

function run() {

  // Setup our connection
  var nx = new Connection();
  nx.schemas(['*']);

  $(() => {

    setupUI();

    nx.connect().then(() => {

      // Setup the SpreadSheet
      sheet = new Spreadsheet($('#grid'), nx, layout, (columns) ? columns.split(',') : null);
      sheet.nxql = query;

      if (query) {
        doQuery();
      }

    })
  });
}

// Utils
function parseParams() {
  var parameters = {};
  var query = window.location.search;
  query = query.replace('?', '');
  var params = query.split('&');
  for(var param of params) {
    var [k, v] = param.split('=');
    parameters[k] = decodeURIComponent(v);
  }
  return parameters;
}

export {run};
