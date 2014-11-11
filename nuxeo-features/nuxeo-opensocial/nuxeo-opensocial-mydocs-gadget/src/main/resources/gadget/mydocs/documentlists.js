var spaceId="";
var path=new Array();
var currentPage=0;
var maxPage = 0;
var errors = 0;

function getNuxeoServerSideUrl() {
  return top.nxServerSideUrl;
}

function getNuxeoClientSideUrl() {
  return top.nxBaseUrl;
}

function getUserLang() {
  return top.nxUserLang;
}

function getResourceUrl() {
  var url = "";
  url = "http://localhost:8080/nuxeo/site/myDocsRestAPI/";
  url += spaceId + "/";

  strPath = path.join();
  regEx = new RegExp(",", "g");
  strPath = strPath.replace(regEx, "/");
  url += strPath;
  return url;
}

function getRestletUrl() {
  var ts = new Date().getTime() + "" + Math.random() * 11
  var url = getResourceUrl();
  url += "?page=" + currentPage;
  url += "&ts=" + ts;
  return url;
}

function getDLUrl(name) {
  var ts = new Date().getTime() + "" + Math.random() * 11
  var url = getResourceUrl();
  url += name +"/@file";
  url += "?ts=" + ts;
  return url;
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
  }
  refresh();
}

function prevPage() {
  if (currentPage > 0) {
    currentPage = currentPage - 1;
  }
  refresh();
}

function firstPage() {
  currentPage = 0;
  refresh();
}

function lastPage() {
  currentPage = maxPage - 1;
  if (currentPage < 0) {
    currentPage = 0;
  }
  refresh();
}

function makeRequest(url, callback, method) {
    var params = {};
    var headers = {};

    params[gadgets.io.RequestParameters.METHOD] = method || gadgets.io.MethodType.GET;

    params[gadgets.io.RequestParameters.AUTHORIZATION] = gadgets.io.AuthorizationType.NONE;

    params[gadgets.io.RequestParameters.CONTENT_TYPE] = gadgets.io.ContentType.JSON;

    var now = new Date().toUTCString();
    headers["Date", now];

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
    if (errors == 0) {
      errors = 1;
      getDocumentLists();
    } else {
      displayNoWorkspaceFound();
    }
    return;
  } else {
    errors = 0;
  }
  displayDocumentList(jsonObject);
}

function displayNoWorkspaceFound() {
  var html = "";
  html = "Aucun espace de travail n'a été trouvé pour cet espace";
  _gel("nxDocumentListData").innerHTML = html;
  gadgets.window.adjustHeight();
}

// insert the whole table, as stupid IE can't do a tbody.innerHtml
function tableStart(jsonObject) {
  var title = "Document";
  var modified = "Modified";
  var creator = "Author";
  var labelInfo = jsonObject.translations;
  if (labelInfo != null && labelInfo != 'undefined') {
    title = labelInfo['label.dublincore.title'];
    modified = labelInfo['label.dublincore.modified'];
    creator = labelInfo['label.dublincore.creator'];
  }
  var html = "";
  html += "<table class='dataList'>";
  html += "  <thead>";
  html += "    <tr>";
  html += "      <th>";
  if (path.length > 0)
    html += "<a href=\"#\" onclick=\"upFolder();return false;\"<img border=\"0\" src=\"/nuxeo/img/UpFolder_icon.gif\"></a>"
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
  var html = "";
  html += "  </tbody>";
  html += "</table>";
  return html
}

function displayDocumentList(jsonObject) {
  var htmlContent = tableStart(jsonObject);
  var document = jsonObject.document;
  for ( var i = 0; i < document.length; i++) {
    htmlContent += mkRow(document[i], i);
  }
  htmlContent += tableEnd();
  _gel("nxDocumentListData").innerHTML = htmlContent;


  // page info
  var pageInfo = jsonObject.summary;
  var pageInfoLabel = pageInfo.pageNumber + 1;
  pageInfoLabel += "/";
  pageInfoLabel += pageInfo.pages;
  maxPage = pageInfo.pages;
  _gel("nxDocumentListPage").innerHTML = pageInfoLabel;
  gadgets.window.adjustHeight();
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
  path.shift();
  refresh();
}

function followPath(pathToFollow) {
  path.push(pathToFollow);
  refresh();
}

function mkRow(document, i) {
  var htmlRow = "<tr class=\"";
  if (i % 2 == 0) {
    htmlRow += "dataRowEven";
  } else {
    htmlRow += "dataRowOdd";
  }
  htmlRow += "\">";
  htmlRow += "<td class=\"iconColumn\">"
  htmlRow += "<img alt=\"File\" src=\""
  htmlRow += getImageBaseUrl();
  htmlRow += document.icon;
  htmlRow += "\"/>";
  htmlRow += "</td>";
  if (document.folderish == 1) {
    htmlRow += "<td><a title=\"" + document.title
        + "\" onclick=\"followPath('" + document.name
        + "');return false;\" />";
  } else {
  var DLUrl = getDLUrl(document.name);
    htmlRow += "<td><a title=\"" + document.title
        + "\" href=\"" + DLUrl + "\" />";
  }
  htmlRow += document.title;
  /*
   * if (document.folderish == 0) { var DLUrl = getDLUrl(document.name);
   * htmlRow += "<a href=\"" + DLUrl + "\"><img
   * src=\"/nuxeo/icons/download.png\" alt=\"Download\"></a>"; }
   */
  htmlRow += "</a></td><td class=\"iconColumn\"/>";
  htmlRow += "<td>";
  htmlRow += getDateForDisplay(document.modified);
  htmlRow += "</td>";

  htmlRow += "<td class=\"iconColumn\">";
  //htmlRow += "<a class=\"deleteaction\" href=\"" + getResourceUrl() + document.name +"\" onclick=\"return delete(this)\"><img src=\"/nuxeo/icons/action_delete_mini.gif\"></a>&nbsp;";
  htmlRow += "<a target=\"_tab\" href=\"/nuxeo/"
      + document.url
      + "\"><img src=\"/nuxeo/img/external.gif\" alt=\"Download\"></a>";
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

function delete(obj) {
  if(confirm("Voulez vous réellement supprimer le document ?")) {
    url = this.attr('href');
    makeRequest(url, function() { refresh();}, gadgets.io.MethodType.DELETE);
  }
  return false;

}



jQuery(document).ready(function(){
    jQuery('#formUpload').submit(function(){
      jQuery(this).ajaxSubmit({ beforeSubmit: control,
                                success:function(){
                                  refresh();
                                   },
                                url: getResourceUrl(),
                                method: 'put'
                              });
      return false;
    });

  });

  function control(){
    if(jQuery.trim(jQuery("#uploadFile").val()) != "")
      return true;
    return false;
  };