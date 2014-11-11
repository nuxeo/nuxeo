var testMode = false;

function requestTasks() {

    var prefs = new gadgets.Prefs();
    var hostname = prefs.getString("nuxeo_host");
    //console.log("my nuxeo host is "+hostname);

    var params = {};
    var headers = {};

    //var cookie = readCookie('JSESSIONID');
    //console.log("we are using cookie:"+cookie);

    params[gadgets.io.RequestParameters.AUTHORIZATION] = gadgets.io.AuthorizationType.NONE;
    params[gadgets.io.RequestParameters.CONTENT_TYPE] = gadgets.io.ContentType.JSON;

    var now = new Date().toUTCString();
    headers["Date", now];

    headers["Expires", "Fri, 01 Jan 1990 00:00:00 GMT"];
    headers["Pragma", "no-cache"];
    headers["Cache-control"] = "no-cache, must-revalidate";
    headers["X-NUXEO-INTEGRATED-AUTH"] = readCookie("JSESSIONID");

    params[gadgets.io.RequestParameters.HEADERS] = headers
    params[gadgets.io.RequestParameters.REFRESH_INTERVAL] = 0;

    var url = getRestletUrl();
    gadgets.io.makeRequest(url, response, params);
}

// insert the whole table, as stupid IE can't do a tbody.innerHtml
function tableStart(jsonObject) {
    var name = "Name";
    var duedate = "Due Date";
    var directive = "Directive";
    var labelInfo = jsonObject.translations;
    if (labelInfo != null && labelInfo != 'undefined') {
        name = labelInfo['label.workflow.task.name'];
        duedate = labelInfo['label.workflow.task.duedate'];
        directive = labelInfo['label.workflow.task.directive'];
    }
    var html = "";
    html += "<table class='dataList'>";
    html += "  <thead>";
    html += "    <tr>";
    html += "      <th/>";
    html += "      <th>" + name + "</th>";
    html += "      <th/>";
    html += "      <th>" + duedate + "</th>";
    html += "      <th>" + directive + "</th>";
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

    //validation
    var validation = data.data['workflowDirectiveValidation'];
    var directive = data.translations['workflowDirectiveValidation'];
    var i;

    if (validation) {
        for (i = 0; i < validation.length; i++) {
            htmlContent += mkRow(validation[i], i, directive);
        }
    }
    validation = data.data['workflowDirectiveCheck'];
    directive = data.translations['workflowDirectiveCheck'];
    if (validation) {
        for (i = 0; i < validation.length; i++) {
            htmlContent += mkRow(validation[i], i, directive);
        }
    }

    validation = data.data['workflowDirectiveDiffusion'];
    directive = data.translations['workflowDirectiveDiffusion'];
    if (validation) {
        for (i = 0; i < validation.length; i++) {
            htmlContent += mkRow(validation[i], i, directive);
        }
    }
    validation = data.data['workflowDirectiveOpinion'];
    directive = data.translations['workflowDirectiveOpinion'];
    if (validation) {
        for (i = 0; i < validation.length; i++) {
            htmlContent += mkRow(validation[i], i, directive);
        }
    }

    validation = data.data['workflowDirectiveVerification'];
    directive = data.translations['workflowDirectiveVerification'];
    if (validation) {
        for (i = 0; i < validation.length; i++) {
            htmlContent += mkRow(validation[i], i, directive);
        }
    }

    htmlContent += tableEnd();

    document.getElementById("nxDocumentListData").innerHTML = htmlContent;
    // page info
    //alert("page info " + data.summary.pageNumber)
    var pageInfoLabel = data.summary.pageNumber + 1;
    pageInfoLabel += "/";
    maxPage = data.summary.pages;
    pageInfoLabel += maxPage + 1;
    document.getElementById("nxDocumentListPage").innerHTML = pageInfoLabel;
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

function mkRow(dashBoardItem, i, directive) {
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
    htmlRow += "icons/file.gif";
    htmlRow += "\"/>";
    htmlRow += "</td><td><a target = \"_top\" title=\"";
    var title = dashBoardItem.name;
    if ((dashBoardItem.title != null) && (dashBoardItem.title != "")) {
        title = dashBoardItem.title;
    }
    htmlRow += title;
    htmlRow += "\" href=\"";
    htmlRow += getBaseUrl();
    htmlRow += dashBoardItem.link.substring(1);
    htmlRow += "\" />";
    htmlRow += title;
    htmlRow += "</a></td><td class=\"iconColumn\"/>";
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
    htmlRow += directive;
    htmlRow += "</td>";
    htmlRow += "<td class=\"iconColumn\"/>";
    htmlRow += "</tr>";
    return htmlRow;
}

function response(obj) {
    //var jsonObject = eval('(' + obj + ')');
    //var data = jsonObject;
    //alert("eval is "+eval(obj));
    var jsonObject = obj.data;
    //alert("howdy"+jsonObject);

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

function getRestletUrl() {
    var url = getNuxeoServerSideUrl();
    if (testMode) {
        url = 'http://localhost:8080/';
    }
    url += "nuxeo/restAPI/workflowTasks/default?mytasks=true&format=JSON";
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
    url += "label.workflow.task.directive";

    return url;
}

function testHandleJSONResponse(req) {
    if (req.readyState == 4) {
        var jsonObject = eval('(' + req.responseText + ')');
        var data = jsonObject;
        displayTaskList(data);
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
