// insert the whole table, as stupid IE can't do a tbody.innerHtml
function tableStart(jsonObject, nxParams) {
  var html = "";
  html += "<table class='dataList'>";
  html += "  <thead>";
  html += "    <tr>";
  for (idx in nxParams.displayColumns) {
      html += mkColHeader(nxParams.displayColumns[idx]);
  }
  html += "    </tr>";
  html += "  </thead>";
  html += "  <tbody>";
  return html;
}

function mkColHeader(colDef) {
    var html = "";
    if (colDef.type == 'builtin') {
        if (colDef.field == "icon") {
            html = "<th/>";
        }
        else if (colDef.field == "titleWithLink") {
            html = "<th> " + colDef.label + " </th>";
        }
    }
    else {
        html = "<th>" + colDef.label + "</th>";
    }
    return html;
}

function tableEnd() {
    var html = "";
    html += "  </tbody>";
    html += "</table>";
    return html
}

function displayDocumentList(entries, nxParams) {
  var htmlContent = '';
  if (entries.length == 0) {
    nxParams.noEntryLabel = nxParams.noEntryLabel || 'Nothing to show.';
    htmlContent = '<p>' + nxParams.noEntryLabel + '</p>';
  } else {
    htmlContent = tableStart(entries, nxParams);
    for (var i = 0; i < entries.length; i++) {
        htmlContent += mkRow(entries[i], i, nxParams);
    }
    htmlContent += tableEnd();
  }

  displayPageNavigationControls(nxParams);
  _gel("nxDocumentListData").innerHTML = htmlContent + "<br/>";
  _gel("nxDocumentList").style.display = 'block';
  gadgets.window.adjustHeight();
}

function displayPageNavigationControls(nxParams) {
  if (nxParams.usePagination && maxPage > 1) {
     _gel('pageNavigationControls').style.display = 'block';
     if (unknownSize) {
        _gel('nxDocumentListPage').innerHTML = (currentPage + 1);
     } else {
	_gel('nxDocumentListPage').innerHTML = (currentPage + 1) + "/" + maxPage;
     }
    _gel('navFirstPage').onclick = function(e) {
      firstPage(nxParams)
    };
    _gel('navPrevPage').onclick = function(e) {
        prevPage(nxParams)
    };
    _gel('navNextPage').onclick = function(e) {
        nextPage(nxParams)
    };
    if (unknownSize) {
      _gel('navLastPage').style.display = 'none';
    } else {
      _gel('navLastPage').onclick = function(e) {
          lastPage(nxParams)
      };
    }
  } else {
    _gel('pageNavigationControls').style.display = 'none';
  }
}

function parseISODate(datestr) {
    var m = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2}(?:\.\d*)?)(?:([\+-])(\d{2})\:(\d{2}))?Z?$/.exec(datestr);
    if (m) {
        // TODO ms
        var t = Date.UTC(+m[1], +m[2] - 1, +m[3], +m[4], +m[5], +m[6]);
        if (m[7]) {
            var tz = m[8] * 3600 + m[9] * 60;
            if (m[7] == '-') {
                tz = -tz;
            }
            t -= tz * 1000;
        }
        return new Date(t);
    }
    return datestr;
}

function getDateForDisplay(datestr) {
    try {
        var d = parseISODate(datestr);
        return d.toLocaleDateString() + " "
                + d.toLocaleTimeString().substring(0, 5);
    } catch (e) {
        return datestr;
    }
}

function mkRow(dashBoardItem, i, nxParams) {
    var htmlRow = "<tr class=\"";
    if (i % 2 == 0) {
        htmlRow += "dataRowEven";
    } else {
        htmlRow += "dataRowOdd";
    }
    htmlRow += "\">";

    for (idx in nxParams.displayColumns) {
        htmlRow += mkCell(nxParams.displayColumns[idx], dashBoardItem);
    }

    htmlRow += "</tr>";
    return htmlRow;
}

function mkCell(colDef, dashBoardItem) {
    var html = "";
    if (colDef.type == 'builtin') {
        if (colDef.field == "icon") {
            html += "<td class=\"iconColumn\">";
            html += "<img alt=\"File\" src=\"";
            html += NXGadgetContext.clientSideBaseUrl;
            html += dashBoardItem.properties["common:icon"];
            html += "\"/>";
        }
        else if (colDef.field == "titleWithLink") {
            var  view = "view_documents";
            if (colDef.view){
                view = colDef.view;
            }
            var  codec = "nxpath";
            if (colDef.codec){
                codec = colDef.codec;
            }
            html += "<td><a target = \"_top\" title=\"";
            html += gadgets.util.escapeString(dashBoardItem.title);
            html += "\" href=\"";
            html += dashBoardItem.contextParameters.documentURL;
            html += "\" />";
            html += gadgets.util.escapeString(dashBoardItem.title);
            html += "</a></td>";
        }
    } else if (colDef.type == 'system') {
        html += "<td>";
        if (!!dashBoardItem[colDef.field]) {
            if (colDef.i18n) {
                var  text = prefs.getMsg(dashBoardItem[colDef.field]);
                if (text == "") {
                    text = dashBoardItem[colDef.field];
                }
                html += gadgets.util.escapeString(text);
            } else {
                html += gadgets.util.escapeString(dashBoardItem[colDef.field]);
            }
        }
        html += "</td>";
    } else {
        html += "<td>";
        if (!!dashBoardItem.properties[colDef.field]) {
            if (colDef.type == 'date') {
                html += getDateForDisplay(dashBoardItem.properties[colDef.field]);
            } else {
                html += gadgets.util.escapeString(dashBoardItem.properties[colDef.field]);
            }
        }
        html += "</td>";
    }
    return html;
}

function encode(path) {
  var segments = path.split('/');
  for (var i = 0; i < segments.length; i++) {
    segments[i] = encodeURIComponent(segments[i]);
  }
  return segments.join('/');
}

function nextPage(nxParams) {
    if (currentPage < maxPage - 1) {
        currentPage += 1;
    }
    refresh(nxParams);
}

function prevPage(nxParams) {
    if (currentPage > 0) {
        currentPage = currentPage - 1;
    }
    refresh(nxParams);
}

function firstPage(nxParams) {
    currentPage = 0;
    refresh(nxParams);
}

function lastPage(nxParams) {
    currentPage = maxPage - 1;
    if (currentPage < 0) {
        currentPage = 0;
    }
    refresh(nxParams);
}

function refresh(nxParams) {
    nxParams.refreshCB(nxParams);
    //doAutomationRequest(nxParams);
}
