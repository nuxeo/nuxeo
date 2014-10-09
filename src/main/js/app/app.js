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
import {Spreadsheet} from './ui/spreadsheet';

var {layout, query, columns} = parseParams();

function run() {
  var nx = new Connection();
  nx.schemas(['*']);

  $(() => nx.connect().then(() => {

    $('#queryArea').toggle(!query);
    $('#query').val(query);

    var console = $('#console');

    // Setup the SpreadSheet
    var sheet = new Spreadsheet($('#grid'), nx, layout, (columns) ? columns.split(',') : null);
    sheet.query.nxql = query;

    var doQuery = () => {
      sheet.query.nxql = $('#query').val();
      sheet.update();
    };

    if (query) {
      doQuery();
    }

    $('#execute').click(doQuery);

    $('#save').click(() => {
      console.text('Saving...');
      sheet.save().then(() => console.text(''));
    });

    $('input[name=autosave]').click(function() {
      sheet.autosave = $(this).is(':checked');
      if (sheet.autosave) {
        console.text('Changes will be automatically saved!');
      } else {
        console.text('');
      }
    });

  }));
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
