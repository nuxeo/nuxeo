function requestTasks() {
    //console.log("hi! i'm a log message!");

    var prefs = new gadgets.Prefs();
    var hostname = prefs.getString("nuxeo_host");
    //console.log("my nuxeo host is "+hostname);

    var ts = new Date().getTime() + "" + Math.random()*11
    var params = {};
    var headers = {};

    //var cookie = readCookie('JSESSIONID');
    //console.log("we are using cookie:"+cookie);

    params[gadgets.io.RequestParameters.CONTENT_TYPE] = gadgets.io.ContentType.DOM;

    var now = new Date().toUTCString();
    headers["Date", now];

    headers["Expires", "Fri, 01 Jan 1990 00:00:00 GMT"];
    headers["Pragma", "no-cache"];
    headers["Cache-control"] = "no-cache, must-revalidate";
    headers["X-NUXEO-INTEGRATED-AUTH"] = readCookie("JSESSIONID");

    params[gadgets.io.RequestParameters.HEADERS] = headers
    params[gadgets.io.RequestParameters.REFRESH_INTERVAL]=0;

    var url = "http://localhost:8080/nuxeo/restAPI/workflowTasks/default?ts="+ts;
    gadgets.io.makeRequest(url, response, params);
};

function getNXTElement(node, localName) {
    if (node.getElementsByTagNameNS) {
        return node.getElementsByTagNameNS("http://www.nuxeo.org/tasks", localName)
    } else {
        return node.getElementsByTagName("nxt:" + localName);
    }
}

function response(obj) {
    var dom = obj.data;
    var resultHTML = "";
    //console.log("starting to parse "+obj);
    var categories = getNXTElement(dom, "category");
    var i= 0;
    //console.log("number of categories "+categories.length);

    while (i < categories.length) {
        categoryNode = categories[i];
        var categoryName = categoryNode.getAttribute("category");
        //console.log("entered category "+categoryName);

        if (categoryName!=null) {
            //console.log("we have a category name:"+categoryName);
            //17 is the length of workflowDirective...startsWith doesn't work
            if (categoryName.substring(17) == "workflowDirective") {
                //console.log("category name is clunky...")
                categoryName=categoryName.substring("workflowDirective".length);
                //console.log("shorted category name to "+categoryName)
            }
        }

        var tasks = getNXTElement(categoryNode, "task")
        //console.log("parsing category:"+categoryName+" with "+ tasks.length +" tasks");

        resultHTML += openDiv(categoryName);

        var j =0 ;
        while (j < tasks.length) {
            var taskNode = tasks[j];
            var value="<nothing>";
            var link=null;

            //should we try backup plan?
            if (taskNode.childNodes.length==0) {
                //console.log("no node value, using name");
                value = taskNode.getAttribute("name");
            } else {
                value=taskNode.childNodes[0].nodeValue;
                link = taskNode.getAttribute("link");
                //console.log("task link:"+link);
            }
            //console.log(categoryName + " -> "+value);
            resultHTML += createItem(value,link);
            ++j;
        }

        resultHTML += closeDiv(categoryName);
        ++i;
    }
    //console.log("done parsing!");
    var taskListDiv = _gel("nxTaskList");
    if (taskListDiv==null) {
        //console.log("can't find nxTaskList!")
    } else {
        taskListDiv.innerHTML = resultHTML;
    }
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

function openDiv(name) {
    var result =  '<div class="nxcategory-wrapper">';
    result += '<span class="nxcategory-title">'+name+' ';
    result +='<a class="nxcategory-toggle" href="javascript:openCategory('+"'nxcat-"+name+"')"+'">';
    result +='<span class="takeupspace"></span></a>';
    result +="</span>";
    result +='<div class="nxcategory" id="nxcat-'+name+'">';
    return result;
}

function closeDiv(name) {
    var result = '</div> <!--'+name+' -->';
    result += '</div> <!--'+name+'wrapper -->';
    return result;
}

function getNuxeoClientSideUrl() {
    return top.nxBaseUrl;
}

function openCategory(divname) {
    //console.log(divname);
}
function createItem(name,link) {
    if (link==null) {
        return '<span class="nxcategory-item">'+name+'</span>';
    }
    var baseUrl = getNuxeoClientSideUrl();
    baseUrl = baseUrl.substring(0,baseUrl.length-1);
    return '<span class="nxcategory-item">'+'<A target="_top" HREF="' + baseUrl +link+'">'+name+'</A></span>';
}

gadgets.util.registerOnLoadHandler(requestTasks);
