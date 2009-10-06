var currentPage = 0;
var maxPage=0;

function getRestletUrl() {
    return "http://127.0.0.1:8080/nuxeo/restAPI/dashboard/" + QM_Name + "?format=JSON&page="+ currentPage;
}
function getImageBaseUrl() {
    return "/nuxeo";
}
function getBaseUrl() {
    return "/nuxeo/";
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
    console.log("calling REST API go to QM " + QM_Name);
    var params = {};
    var headers = {};

    params[gadgets.io.RequestParameters.CONTENT_TYPE] = gadgets.io.ContentType.JSON;

    var now = new Date().toUTCString();
    headers["Date", now];

    headers["Expires", "Fri, 01 Jan 1990 00:00:00 GMT"];
    headers["Pragma", "no-cache"];
    headers["Cache-control"] = "no-cache, must-revalidate";

    var encoded = encode64("Administrator:Administrator");
    console.log("encoded in base64 credentials:"+encoded);
    headers["authentication"] = "basic "+encoded;

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
       var jsonObject = eval('(' + req.responseText + ')');
       displayDocumentList(jsonObject);
   }
}

function handleJSONResponse(obj) {
    var jsonObject = obj.data;
    displayDocumentList(jsonObject);
}

function displayDocumentList(jsonObject) {
    var htmlContent = "";
    var data = jsonObject.data;
    for (var i=0; i< data.length; i++) {
        htmlContent+=mkRow(data[i], i);
    }
    document.getElementById("nxDocumentListData").innerHTML = htmlContent;

    var pageInfo = jsonObject.summary;
    var pageInfoLabel = pageInfo.pageNumber+1;
    pageInfoLabel+= "/";
    pageInfoLabel+= pageInfo.pages;
    maxPage = pageInfo.pages;
    document.getElementById("nxDocumentListPage").innerHTML = pageInfoLabel;
}

function mkRow(dashBoardItem, i) {
    var htmlRow = "<tr class=\"dataRowEven\">";
    htmlRow+="<td class=\"iconColumn\">"
    htmlRow+="<img alt=\"File\" src=\""
    htmlRow+=getImageBaseUrl();
    htmlRow+=dashBoardItem.icon;
    htmlRow+="\"/>";
    htmlRow+="</td><td><a title=\"";
    htmlRow+=dashBoardItem.title;
    htmlRow+="\" href=\"";
    htmlRow+=getBaseUrl();
    htmlRow+=dashBoardItem.url;
    htmlRow+="\" />";
    htmlRow+=dashBoardItem.title;
    htmlRow+="</a></td><td class=\"iconColumn\"/>";
    htmlRow+="<td>";
    htmlRow+=dashBoardItem.modified;
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

if (testMode) {
    refresh();
}
else {
    gadgets.util.registerOnLoadHandler(refresh);
}
