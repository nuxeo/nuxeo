var currentPage = 0;
var maxPage=0;
var errors=0;

function getNuxeoServerSideUrl() {
    return top.nxServerSideUrl;
}

function getNuxeoClientSideUrl() {
    return top.nxBaseUrl;
}

function getCurrentDomain() {
    return top.nxDomain;
}

function getUserLang() {
    return top.nxUserLang;
}

function getWebappName() {
    return top.nxContextPath.substring(1);
}


function getRestletUrl() {
    var ts = new Date().getTime() + "" + Math.random()*11
    var url ="";
    if (testMode) {
        url= "http://127.0.0.1:8080/nuxeo/restAPI/dashboard/";
    } else {
        url= requestBaseUrl + "restAPI/dashboard/";
    }
    url+=QM_Name + "?format=JSON&page="+ currentPage;
    if (getCurrentDomain()!=null && getCurrentDomain()!="") {
        url+="&domain=" + getCurrentDomain();
    }
    if (getUserLang()!=null && getUserLang()!="") {
        url+="&lang=" + getUserLang();
    }
    url+="&ts=" + ts;
    return url;
}

function getImageBaseUrl() {
    return getNuxeoClientSideUrl();
}

function getBaseUrl() {
    return getNuxeoClientSideUrl();
}

function nextPage() {
    if (currentPage < maxPage-1) {
        currentPage+=1;
    }
    refresh();
}

function prevPage() {
    if (currentPage > 0) {
        currentPage=currentPage-1;
    }
    refresh();
}

function firstPage() {
    currentPage=0;
    refresh();
}

function lastPage() {
    currentPage=maxPage-1;
    if (currentPage < 0) {
        currentPage=0;
    }
    refresh();
}

function getDocumentLists() {
    //console.log("calling REST API go to QM " + QM_Name);
    var params = {};
    var headers = {};

    params[gadgets.io.RequestParameters.AUTHORIZATION] = gadgets.io.AuthorizationType.SIGNED;
    params[gadgets.io.RequestParameters.CONTENT_TYPE] = gadgets.io.ContentType.JSON;

    var now = new Date().toUTCString();
    headers["Date", now];

    headers["Expires", "Fri, 01 Jan 1990 00:00:00 GMT"];
    headers["Pragma", "no-cache"];
    headers["Cache-control"] = "no-cache, must-revalidate";
    //headers["X-NUXEO-INTEGRATED-AUTH"] = readCookie("JSESSIONID");

    params[gadgets.io.RequestParameters.HEADERS] = headers;

    var url = getRestletUrl();
    gadgets.io.makeRequest(url, handleJSONResponse, params);
};

function testGetDocumentLists() {
    var req = new XMLHttpRequest();
    req.open("GET", getRestletUrl(), true);
    req.onreadystatechange = function(){testHandleJSONResponse(req)};
    req.send(null);
}

function testHandleJSONResponse(req) {
   if (req.readyState == 4) {
     if (req.status==200) {
         var jsonObject = eval('(' + req.responseText + ')');
         displayDocumentList(jsonObject);
     } else {
       alert("Received " + req.status + " from server");
       alert(req.responseText);
     }
   }
}

function handleJSONResponse(obj) {
    var jsonObject = obj.data;
    if (jsonObject==null) {
        if (errors==0) {
            errors=1;
            getDocumentLists();
        } else {
            alert("Error, no result from server : " + obj.errors);
        }
        return;
    } else {
        errors=0;
    }
    displayDocumentList(jsonObject);
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
    html += "      <th/>";
    html += "      <th>" + title + "</th>";
    html += "      <th/>";
    html += "      <th>" + modified + "</th>";
    html += "      <th>" + creator + "</th>";
    html += "      <th/>";
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
    var data = jsonObject.data;
    for (var i=0; i< data.length; i++) {
        htmlContent+=mkRow(data[i], i);
    }
    htmlContent += tableEnd();
    _gel("nxDocumentListData").innerHTML = htmlContent + "<br/>";

    // page info
    var pageInfo = jsonObject.summary;
    var pageInfoLabel = pageInfo.pageNumber+1;
    pageInfoLabel+= "/";
    pageInfoLabel+= pageInfo.pages;
    maxPage = pageInfo.pages;
    _gel("nxDocumentListPage").innerHTML = pageInfoLabel;

    gadgets.window.adjustHeight();
}

function getDateForDisplay(datestr) {
    try {
        datestr = datestr.replace("-", "/").replace("-", "/");
        var d = new Date(datestr);
        var result = d.toLocaleDateString() + " " + d.toLocaleTimeString().substring(0,5);
        return result;
    }
    catch(e) {
        return datestr;
    }
}

function mkRow(dashBoardItem, i) {
    var htmlRow = "<tr class=\"";
    if (i%2==0){
    htmlRow+="dataRowEven";
    } else {
    htmlRow+="dataRowOdd";
    }
    htmlRow+="\">";
    htmlRow+="<td class=\"iconColumn\">"
    htmlRow+="<img alt=\"File\" src=\""
    htmlRow+=getImageBaseUrl();
    htmlRow+=dashBoardItem.icon;
    htmlRow+="\"/>";
    htmlRow+="</td><td><a target = \"_top\" title=\"";
    htmlRow+=dashBoardItem.title;
    htmlRow+="\" href=\"";
    htmlRow+=getBaseUrl();
    htmlRow+=dashBoardItem.url;
    htmlRow+="\" />";
    htmlRow+=dashBoardItem.title;
    htmlRow+="</a></td><td class=\"iconColumn\"/>";
    htmlRow+="<td>";
    htmlRow+=getDateForDisplay(dashBoardItem.modified);
    htmlRow+="</td>";
    htmlRow+="<td>";
    htmlRow+=dashBoardItem.creator;
    htmlRow+="</td>";
    htmlRow+="<td class=\"iconColumn\"/>";
    htmlRow+="</tr>";
    return htmlRow;
}

function refresh() {
    if (testMode) {
        testGetDocumentLists();
    }
    else {
        getDocumentLists();
    }
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

if (testMode) {
    refresh();
}
else {
    gadgets.util.registerOnLoadHandler(refresh);
}
