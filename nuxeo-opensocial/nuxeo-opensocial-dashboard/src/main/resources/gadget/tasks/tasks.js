var testMode = false;
var errors=0;

function requestTasks() {

    var prefs = new gadgets.Prefs();
    var hostname = prefs.getString("nuxeo_host");
    //console.log("my nuxeo host is "+hostname);

    var params = {};
    var headers = {};

    //var cookie = readCookie('JSESSIONID');
    //console.log("we are using cookie:"+cookie);

    params[gadgets.io.RequestParameters.AUTHORIZATION] = gadgets.io.AuthorizationType.SIGNED;
    params[gadgets.io.RequestParameters.CONTENT_TYPE] = gadgets.io.ContentType.JSON;

    var now = new Date().toUTCString();
    headers["Date", now];

    headers["Expires", "Fri, 01 Jan 1990 00:00:00 GMT"];
    headers["Pragma", "no-cache"];
    headers["Cache-control"] = "no-cache, must-revalidate";
    //headers["X-NUXEO-INTEGRATED-AUTH"] = readCookie("JSESSIONID");

    params[gadgets.io.RequestParameters.HEADERS] = headers
    params[gadgets.io.RequestParameters.REFRESH_INTERVAL] = 0;

    var url = getRestletUrl();
    gadgets.io.makeRequest(url, response, params);
}

// insert the whole table, as stupid IE can't do a tbody.innerHtml
function tableStart(jsonObject) {
    var name = "Name";
    var title = "Title";
    var directive = "Directive";
    var comment = "Comment";
    var duedate = "Due Date";
    var startdate = "Start Date";
    var labelInfo = jsonObject.translations;
    if (labelInfo != null && labelInfo != 'undefined') {
        name = labelInfo['label.workflow.task.name'];
        title = labelInfo['label.title'];
        directive = labelInfo['label.workflow.task.directive'];
        comment = labelInfo['label.review.user.comment'];
        duedate = labelInfo['label.workflow.task.duedate'];
        startdate = labelInfo['label.workflow.task.startdate'];
    }
    var html = "";
    html += "<table class='dataList'>";
    html += "  <thead>";
    html += "    <tr>";
    html += "      <th>" + name + "</th>";
    html += "      <th/>";
    html += "      <th>" + title + "</th>";
    html += "      <th>" + directive + "</th>";
    html += "      <th>" + comment + "</th>";
    html += "      <th>" + duedate + "</th>";
    html += "      <th>" + startdate + "</th>";
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

function displayTaskList(data) {
    var htmlContent = tableStart(data);

    for (var directive in data.data) {
        for (i = 0; i <  data.data[directive].length; i++) {
            htmlContent += mkRow(data.data[directive][i], i);
        }
    }

    htmlContent += tableEnd();

    document.getElementById("nxDocumentListData").innerHTML = htmlContent + "<br/>";
    // no pagination

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

function getNuxeoClientSideUrl() {
    return top.nxBaseUrl;
}
function getImageBaseUrl() {
    return getNuxeoClientSideUrl();
}

function getBaseUrl() {
    return getNuxeoClientSideUrl();
}

function mkRow(dashBoardItem, i) {
    var htmlRow = "<tr class=\"";
    if (i % 2 == 0) {
        htmlRow += "dataRowEven";
    } else {
        htmlRow += "dataRowOdd";
    }
    htmlRow += "\">";
    htmlRow += "<td>" + dashBoardItem.nameI18n + "</td>";
    htmlRow += "<td class=\"iconColumn\">"
    htmlRow += "<img alt=\"File\" src=\""
    htmlRow += getImageBaseUrl();
    htmlRow += "icons/file.gif";
    htmlRow += "\"/>";
    htmlRow += "</td>";
    htmlRow += "</td><td><a target = \"_top\" title=\"";
    var docTitle = dashBoardItem.id;
    if ((dashBoardItem.title != null) && (dashBoardItem.title != "")) {
      docTitle = dashBoardItem.title;
    }
    htmlRow += docTitle;
    htmlRow += "\" href=\"";
    htmlRow += getBaseUrl();
    htmlRow += dashBoardItem.link.substring(1);
    htmlRow += "\" />";
    htmlRow += docTitle;
    htmlRow += "</a></td>";
    htmlRow += "<td>" + dashBoardItem.directiveI18n + "</td>";
    htmlRow += "<td>" + dashBoardItem.comment + "</td>";
    htmlRow += "<td>";
    var dateToDisplay = null;
    if ((dashBoardItem.dueDate != null) && (dashBoardItem.dueDate != "")) {
        dateToDisplay = dashBoardItem.dueDate;
    }
    if (dateToDisplay != null) {
        htmlRow += getDateForDisplay(dateToDisplay);
    }
    htmlRow += "</td>";
    htmlRow += "<td>";
    if ((dashBoardItem.startDate != null) && (dashBoardItem.startDate != "")) {
        dateToDisplay = dashBoardItem.startDate;
    }
    if (dateToDisplay != null) {
        htmlRow += getDateForDisplay(dateToDisplay);
    }
    htmlRow += "</td>";
    htmlRow += "<td class=\"iconColumn\"/>";
    htmlRow += "</tr>";
    return htmlRow;
}

function response(obj) {
    var jsonObject = obj.data;
    if (jsonObject==null) {
        if (errors==0) {
            errors=1;
            requestTasks();
        } else {
            alert("Error, no result from server : " + obj.errors);
        }
        return;
    } else {
        errors=0;
    }

    displayTaskList(jsonObject);
};

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

function closeDiv(name) {
    return "</div></div>";
}

function getNuxeoServerSideUrl() {
    return top.nxServerSideUrl;
}

function getNuxeoClientSideUrl() {
    return top.nxBaseUrl;
}

function getUserLang() {
    return top.nxUserLang;
}

function getWebappName() {
    return top.nxContextPath.substring(1);
}

function getRestletUrl() {
    var url ; //= getNuxeoServerSideUrl();
    if (testMode) {
        url = 'http://localhost:8080/nuxeo/';
    } else {
        url = requestBaseUrl;
    }
    url += "restAPI/workflowTasks/default?mytasks=true&format=JSON";
    var ts = new Date().getTime() + "" + Math.random() * 11
    url += "&ts=" + ts;

    var lang = getUserLang();
    if (lang != null && lang != "") {
        url += "&lang=" + lang;
    }
    if (testMode) {
        url += "&lang=en";
    }
    url += "&labels=";
    //labels
    url += "workflowDirectiveValidation,";
    url += "workflowDirectiveOpinion,";
    url += "workflowDirectiveVerification,";
    url += "workflowDirectiveCheck,";
    url += "workflowDirectiveDiffusion,";
    url += "label.workflow.task.name,";
    url += "label.workflow.task.duedate,";
    url += "label.workflow.task.directive,";
    url += "label.title,";
    url += "label.review.user.comment,";
    url += "label.workflow.task.startdate";

    return url;
}

function testHandleJSONResponse(req) {
    if (req.readyState == 4) {
       if (req.status==200) {
             var jsonObject = eval('(' + req.responseText + ')');
             displayTaskList(jsonObject);
         } else {
           alert("Received " + req.status + " from server");
           alert(req.responseText);
         }
    }
}

function testGetTaskList() {
    var req = new XMLHttpRequest();
    req.open("GET", getRestletUrl(), true);
    req.onreadystatechange = function() {
        testHandleJSONResponse(req)
    };
    req.send(null);
}

function refresh() {
    if (testMode) {
        testGetTaskList();
    } else {
        requestTasks();
    }
}

if (testMode) {
    refresh();
} else {
    gadgets.util.registerOnLoadHandler(refresh);
}
