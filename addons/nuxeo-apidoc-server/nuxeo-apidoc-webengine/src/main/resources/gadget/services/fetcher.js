
function getResourceUrl() {
  return "http://explorer.nuxeo.org/nuxeo/site/distribution/current/feedServices";
}

var dbgResponse;

function getData() {
    var params = {};
    var headers = {};

    params[gadgets.io.RequestParameters.AUTHORIZATION] = gadgets.io.AuthorizationType.NONE;
    params[gadgets.io.RequestParameters.CONTENT_TYPE] = gadgets.io.ContentType.JSON;

    var now = new Date().toUTCString();
    headers["Date", now];

    headers["Expires", "Fri, 01 Jan 1990 00:00:00 GMT"];
    headers["Pragma", "no-cache"];
    headers["Cache-control"] = "no-cache, must-revalidate";

    params[gadgets.io.RequestParameters.HEADERS] = headers;

    var url = getResourceUrl();
    gadgets.io.makeRequest(url, handleJSONResponse, params);
};


function handleJSONResponse(ob, errors, rc, text) {
  dbgResponse=ob;
  var data = ob.data;
    if (data==null) {
        if (errors==0) {
            errors=1;
            getData();
        } else {
            alert("Error, no result from server : " + obj.errors);
        }
        return;
    } else {
        errors=0;
    }
    displayList(data);
}

function mkRow(dataObject, idx) {
  var html = "<div class='dataRow";
  if (idx%2) {
    html+="even";
  }
  html+="'>";
  html+="<B>" + dataObject.label + "</B><br/>";
  if (dataObject.desc) {
    html+="<span>" + dataObject.desc +"</span>";
  }
  html+="</div>";
  return html;
}

function displayList(data) {
    var htmlContent = "";
    for (var i=0; i< data.length; i++) {
        htmlContent+=mkRow(data[i], i);
    }
    _gel("serviceList").innerHTML = htmlContent + "<br/>";

    gadgets.window.adjustHeight();
}

gadgets.util.registerOnLoadHandler(getData);
