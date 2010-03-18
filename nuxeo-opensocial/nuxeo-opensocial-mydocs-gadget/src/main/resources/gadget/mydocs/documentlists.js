var gadgetId="";
var path=new Array();
var currentPage=0;
var maxPage = 0;
var errors = 0;
var prefs;
var perm = gadgets.nuxeo.isEditable();

function getNuxeoClientSideUrl() {
  return top.nxBaseUrl;
}

function getUserLang() {
  return top.nxUserLang;
}

function getResourceUrl() {
  return [getNuxeoClientSideUrl(),"site/myDocsRestAPI/",gadgetId,"/",path.join("/"),((path.length > 0) ? "/" : "")].join("");
}

function getRestletUrl() {
  return [getResourceUrl(),"?page=",currentPage,"&ts=",new Date().getTime(),Math.random() * 11].join("");
}

function getDLUrl(name) {
  return [getResourceUrl(), name, "/@file", "?ts=", new Date().getTime(), Math.random() * 11].join("");
}

function getImageBaseUrl() {
  return "/nuxeo";
}

function getBaseUrl() {
  return getNuxeoClientSideUrl();
}

function nextPage() {
  if (currentPage < maxPage - 1) {
    currentPage += 1;
    refresh();
  }
}

function prevPage() {
  if (currentPage > 0) {
    currentPage = currentPage - 1;
    refresh();
  }
}

function firstPage() {
  if(currentPage != 0) {
    currentPage = 0;
    refresh();
  }
}

function lastPage() {
  if (maxPage > 1) {
    currentPage = maxPage - 1;
    refresh();
  }
}

function makeRequest(url, callback, method) {
    var params = {};
    params[gadgets.io.RequestParameters.METHOD] = method || gadgets.io.MethodType.GET;
    params[gadgets.io.RequestParameters.AUTHORIZATION] = gadgets.io.AuthorizationType.NONE;
    params[gadgets.io.RequestParameters.CONTENT_TYPE] = gadgets.io.ContentType.JSON;
    var headers = {};
    headers["Date", new Date().toUTCString()];
    headers["Expires", "Fri, 01 Jan 1990 00:00:00 GMT"];
    headers["Pragma", "no-cache"];
    headers["Cache-control"] = "no-cache, must-revalidate";
    headers["X-NUXEO-INTEGRATED-AUTH"] = readCookie("JSESSIONID");
    params[gadgets.io.RequestParameters.HEADERS] = headers;
    gadgets.io.makeRequest(url, callback, params);
}

function getDocumentLists() {
  makeRequest(getRestletUrl(), handleJSONResponse);
};

function handleJSONResponse(obj) {
  var jsonObject = obj.data;
  if (jsonObject == null) {
    if (errors > 0) {
      displayNoWorkspaceFound();
    } else {
      errors++;
      getDocumentLists();
    }
    return;
  } else {
    errors = 0;
  }
  displayDocumentList(jsonObject);
}

function displayNoWorkspaceFound() {
  jQuery("#pager").remove();
  var html = prefs.getMsg("displayNoWorkspaceFound");
  _gel("nxDocumentListData").innerHTML = html;
  gadgets.window.adjustHeight();
}

// insert the whole table, as stupid IE can't do a tbody.innerHtml
function tableStart(jsonObject) {
  var title = "Document";
  var modified = prefs.getMsg("modified");
  var creator = prefs.getMsg("author");
  var labelInfo = jsonObject.translations;
  if (labelInfo != null && labelInfo != 'undefined') {
    title = labelInfo['label.dublincore.title'];
    modified = labelInfo['label.dublincore.modified'];
    creator = labelInfo['label.dublincore.creator'];
  }
  var html = "<table class='dataList'>";
  html += "  <thead>";
  html += "    <tr>";
  html += "      <th>";
  if (path.length > 0)
    html += "<a href=\"#\" onclick=\"upFolder();return false;\"<img border=\"0\" src=\"" + top.nxContextPath + "/icons/UpFolder_icon.gif\"></a>"
  html += "      </th>";
  html += "      <th>" + title + "</th>";
  html += "      <th/>";
  html += "      <th>" + modified + "</th>";
  html += "      <th>&nbsp;</th>";
  html += "    </tr>";
  html += "  </thead>";
  html += "  <tbody>";
  return html;
}

function tableEnd() {
  return "</tbody></table>";
}

function displayDocumentList(jsonObject) {
  var pageInfo = jsonObject.summary;
  maxPage = pageInfo.pages;
  if(maxPage < currentPage+1){
    currentPage--;
    refresh();
    return;
  }
  var htmlContent = tableStart(jsonObject);
  var document = jsonObject.document;
  for ( var i = 0; i < document.length; i++) {
    htmlContent += mkRow(document[i], i);
  }
  htmlContent += tableEnd();
  jQuery("#nxDocumentListData").html(htmlContent);
  currentPage = pageInfo.pageNumber;
  jQuery("#nxDocumentListPage").text([pageInfo.pageNumber + 1,"/",pageInfo.pages].join(""));
  gadgets.window.adjustHeight();

  if(perm){
    jQuery(".deleteaction").click(function() {
      deleteDoc(jQuery(this));
      return false;
    });

  jQuery("#uploadBtn").click(function() {
      jQuery('#formUpload').ajaxSubmit({
        beforeSubmit: control,
        success:function(){
          //refresh();
          gadgets.nuxeo.refreshGadget();
        },
        error: function(xhr,rs) {
          alert(xhr.responseText);
        },
        url: getResourceUrl(),
        resetForm: true,
        clearForm: true,
        type: 'POST'
      });
      return false;
    });
  }
}

function getDateForDisplay(datestr) {
  try {
    datestr = datestr.replace("-", "/").replace("-", "/");
    var d = new Date(datestr);
    var result = d.toLocaleDateString() + " "
        + d.toLocaleTimeString().substring(0, 5);
    return result;
  } catch (e) {
    return datestr;
  }
}

function upFolder() {
  path.pop();
  refresh();
}

function followPath(pathToFollow) {
  path.push(pathToFollow);
  refresh();
}

function mkRow(document, i) {
  var htmlRow = "<tr class=\"";
  if (i % 2 == 0)
    htmlRow += "dataRowEven";
  else
    htmlRow += "dataRowOdd";
  htmlRow += "\">";
  htmlRow += "<td class=\"iconColumn\">"
  htmlRow += "<img alt=\"File\" src=\""
  htmlRow += getImageBaseUrl();
  htmlRow += document.icon;
  htmlRow += "\"/>";
  htmlRow += "</td>";
  if (document.folderish == 1) {
    htmlRow += "<td><a href=\"#\" title=\"" + document.title
        + "\" onclick=\"followPath('" + document.name
        + "');return false;\">";
    htmlRow += document.title + "</a></td>";
  } else if (document.type == "File"){
    var DLUrl = getDLUrl(document.name);
    htmlRow += "<td><a title=\"" + document.title
        + "\" href=\"" + DLUrl + "\">";
    htmlRow += document.title + "</a></td>";
  } else {
    htmlRow +="<td>" + document.title + "</td>";
  }

  /*
   * if (document.folderish == 0) { var DLUrl = getDLUrl(document.name);
   * htmlRow += "<a href=\"" + DLUrl + "\"><img
   * src=\"/nuxeo/icons/download.png\" alt=\"Download\"></a>"; }
   */
  htmlRow += "<td class=\"iconColumn\"/>";
  htmlRow += "<td>";
  htmlRow += getDateForDisplay(document.modified);
  htmlRow += "</td>";
  htmlRow += "<td class=\"iconColumn\">";
  if(perm) {
    htmlRow += "<a class=\"deleteaction perm\" href=\"" + getResourceUrl() + document.name +"\" onclick=\"delete(this);\">";
    htmlRow += "<img src=\"/nuxeo/icons/action_delete_mini.gif\"></a>&nbsp;";
  }
  htmlRow +="</td>";
  htmlRow += "</tr>";
  return htmlRow;
}


function refresh() {
  getDocumentLists();
}

function readCookie(name) {
  var nameEQ = name + "=";
  var ca = document.cookie.split(';');
  for ( var i = 0; i < ca.length; i++) {
    var c = ca[i];
    while (c.charAt(0) == ' ')
      c = c.substring(1, c.length);
    if (c.indexOf(nameEQ) == 0)
      return c.substring(nameEQ.length, c.length);
  }
  return null;
}

function deleteDoc(obj) {
  if(confirm(prefs.getMsg("confirmDelete")))
    makeRequest(obj.attr('href'), function() { refresh();}, gadgets.io.MethodType.DELETE);
  return false;
}

jQuery(document).ready(function(){

});

function control(){
  return (jQuery.trim(jQuery("#uploadFile").val()) != "");
}