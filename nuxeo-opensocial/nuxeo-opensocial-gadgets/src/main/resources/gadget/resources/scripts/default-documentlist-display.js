var currentPage = 0;
var maxPage = 0;

// insert the whole table, as stupid IE can't do a tbody.innerHtml
function tableStart(jsonObject,nxParams) {
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

  _gel('navFirstPage').onclick = function(e) {firstPage(nxParams)};
  _gel('navPrevPage').onclick = function(e) {prevPage(nxParams)};
  _gel('navNextPage').onclick = function(e) {nextPage(nxParams)};
  _gel('navLastPage').onclick = function(e) {lastPage(nxParams)};

  return html;
}

function mkColHeader(colDef) {
   var html="";
   if (colDef instanceof Array)  {
     html="<th>" + colDef[0] + "</th>";
   } else {
     if (colDef=="icon") {
       html="<th/>";
     }
     if (colDef=="titleWithLink") {
       html="<th> Title </th>";
     }
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

  var htmlContent = tableStart(entries,nxParams);

  for ( var i = 0; i < entries.length; i++) {
    htmlContent += mkRow(entries[i], i, nxParams);
  }
  htmlContent += tableEnd();
  _gel("nxDocumentListData").innerHTML = htmlContent + "<br/>";

  gadgets.window.adjustHeight();
}

function getDateForDisplay(datestr) {
  try {
    var d = new Date(datestr);
    var result = d.toLocaleDateString() + " "
        + d.toLocaleTimeString().substring(0, 5);
    return result;
  } catch (e) {
    return datestr;
  }
}

function mkRow(dashBoardItem, i,nxParams) {
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
   var html="";
   if (colDef instanceof Array)  {
      html="<td>";
      if (colDef.length==3 && colDef[2]=='date') {
        html += getDateForDisplay(dashBoardItem.properties[colDef[1]]);
      } else {
        html += dashBoardItem.properties[colDef[1]];
      }
      html+="</td>";
   } else {
     if (colDef=="icon") {
      html += "<td class=\"iconColumn\">"
      html += "<img alt=\"File\" src=\""
      html += NXGadgetContext.clientSideBaseUrl;
      html += dashBoardItem.properties["common:icon"];
      html += "\"/>";
     }
     if (colDef=="titleWithLink") {
       html += "<td><a target = \"_top\" title=\"";
       html += dashBoardItem.title;
       html += "\" href=\"";
       html += NXGadgetContext.clientSideBaseUrl;
       html += "nxpath/default";
       html += dashBoardItem.path;
       html += "@view_documents";
       html += "\" />";
       html += dashBoardItem.title;
       html += "</a></td>";
     }

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
  doAutomationRequest(nxParams);
}
