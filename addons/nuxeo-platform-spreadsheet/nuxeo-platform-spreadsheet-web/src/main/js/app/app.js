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
import {i18n} from './ui/i18n';

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
      if (parent.jQuery.fancybox) {
        parent.jQuery.fancybox.close();
      }
    });
    $('#close').toggle(true);

  // Setup standalone UI
  } else {
    $('#queryArea').toggle(true);
    $('#execute').click(doQuery);
  }

  $('#save').click(() => {
    log.info(i18n('saving'));
    sheet.save().then((results) => {
      if (!results) {
        log.error(i18n('failedSave'));
        return;
      }
      var msg;
      if (results.length === 0) {
        msg = i18n('upToDate');
      } else {
        msg = `${results.length} ${i18n('rowsSaved')}`;
      }
      log.info(msg);
    });
  });

  $('input[name=autosave]').click(function() {
    sheet.autosave = $(this).is(':checked');
    if (sheet.autosave) {
      log.default(i18n('autoSave'));
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
  return sheet.update().catch(function(err) {
    log.error(err.message);
  });
}

function run(baseURL = '/nuxeo', username = null, password = null) {

  // Setup our connection
  var nx = new Connection(baseURL, username, password);
  nx.schemas(['*']);

  setupUI();

  return nx.connect().then(() => {

    // Setup the language
    let language = (nuxeo && nuxeo.spreadsheet && nuxeo.spreadsheet.language) ? nuxeo.spreadsheet.language.split('_')[0] : 'en';

    // Extract content view configuration
    let resultLayoutName = cv && cv.resultLayout && cv.resultLayout.name;
    let resultColumns = cv && cv.resultColumns;
    let pageProviderName = cv ? cv.pageProviderName : (pp || 'spreadsheet_query');

    // default columns
    if (!resultLayoutName && (!resultColumns || resultColumns.length === 0)) {
      resultColumns = [
        { label: 'Title', field: 'dc:title' },
        { label: 'Modified', field: 'dc:modified'},
        { label: 'Last Contributor', field: 'dc:lastContributor'},
        { label: 'State', field: 'currentLifeCycleState'}
      ];
    }

    // Setup the SpreadSheet
    sheet = new Spreadsheet($('#grid'), nx, resultLayoutName, resultColumns, pageProviderName, language);

    // If we don't have a content view we're done...
    if (isStandalone) {
      return;
    }
    // ... otherwise let's set it up

    // Add query parameters
    if (cv.queryParameters) {
      sheet.queryParameters = cv.queryParameters;
    }

    // Add the search document
    if (cv.searchDocument) {
      var namedParameters = {};
      for (var k in cv.searchDocument.properties) {
        var v = cv.searchDocument.properties[k];
        // skip empty values
        if ((typeof(v.length) !== 'undefined') && (v.length === 0)) {
          continue;
        }
        namedParameters[k] = (typeof v === 'string') ? v : JSON.stringify(v);
      }
      sheet.namedParameters = namedParameters;
    }

    // Add sort infos
    if (cv.sortInfos) {
      sheet.sortInfos = cv.sortInfos;
    }

    // Run the query
    return doQuery();
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
