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

function displayProcesses(response, nxParams) {
  if (nxParams.usePagination) {
      maxPage = response.data['pageCount'];
  }
  // set callback
  nxParams.refreshCB = doAutomationRequest;

  var htmlContent = '';
  var entries = response.data.entries;
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
    _gel('nxDocumentListPage').innerHTML = (currentPage + 1) + "/" + maxPage;
    _gel('navFirstPage').onclick = function(e) {
      firstPage(nxParams)
    };
    _gel('navPrevPage').onclick = function(e) {
        prevPage(nxParams)
    };
    _gel('navNextPage').onclick = function(e) {
        nextPage(nxParams)
    };
    _gel('navLastPage').onclick = function(e) {
        lastPage(nxParams)
    };
  } else {
    _gel('pageNavigationControls').style.display = 'none';
  }
}

function getDateForDisplay(datestr) {
    try {
        var d = new Date(datestr);
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

function mkCell(colDef, entry) {
    var html = "";
    if (colDef.type == 'builtin') {
        if (colDef.field == "titleWithLink") {
            html += "<td><a target = \"_top\" title=\"";
            html += entry.documentTitle;
            html += "\" href=\"";
            html += NXGadgetContext.clientSideBaseUrl;
            html += entry.documentLink;
            html += "\">";
            html += entry.documentTitle;
            html += "</a></td>";
        }
    } else {
        html += "<td>";
        if (colDef.type == 'date') {
            html += getDateForDisplay(entry[colDef.field]);
        } else {
            html += entry[colDef.field];
        }
        html += "</td>";
    }
    return html;
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
}
