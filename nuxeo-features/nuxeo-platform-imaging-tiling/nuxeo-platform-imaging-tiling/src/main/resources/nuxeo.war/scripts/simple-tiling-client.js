var tileWidth = 200;
var tileHeight = 200;
var startX = 0;
var startY = 0;
var nbX = 2;
var nbY = 2;
var tilingInfo;
var tileBaseUrl;
function getTilingInfo(tileWidth, tileHeight, maxTiles) {
  var url = '/nuxeo/restAPI/getTiles/';
  url = url + repoId + "/" + docId + "/" + tileWidth + "/" + tileHeight + "/"
      + maxTiles;
  tileBaseUrl = url;
  url = url + "?" + "format=JSON";

  var request = new XMLHttpRequest();
  request.open('GET', url, true);
  request.onreadystatechange = function() {
    callbackHandler(request, callback);
  }
  request.send(null);
}

function callback(data) {
  tilingInfo = eval('(' + data + ')');
  displayTiles();
}

function scroll(x, y) {
  startX = x;
  startY = y;
  displayTiles();
}

function resize(x, y) {
  nbX = x;
  nbY = y;
  //displayTiles();
}

function callbackHandler(req, callback) {
  if (req.readyState == 4) {
    if (req.status == 200) {
      if (callback)
        callback(req.responseText);
    } else
      alert("There was an error processing your request.  Error code: "
          + req.status);
  }

}
function displayTiles() {
  var maxX = tilingInfo.tileInfo.xtiles;
  var maxY = tilingInfo.tileInfo.ytiles;
  var tileWidth = tilingInfo.tileInfo.tileWidth;
  var tileHeight = tilingInfo.tileInfo.tileHeight;

  var endX = startX + nbX;
  var endY = startY + nbY;

  if (endX > maxX)
    endX = maxX;
  if (endY > maxY)
    endY = maxY;

  zoomDiv = document.getElementById("zoom");
  zoomDiv.innerHTML = tilingInfo.tileInfo.zoom;

  formatDiv = document.getElementById("imgFormat");
  formatDiv.innerHTML = tilingInfo.originalImage.width + "x" + tilingInfo.originalImage.height + " (" + tilingInfo.originalImage.format + ")";

  dispDiv = document.getElementById("dispDiv");
  dispDiv.innerHTML = "";
  tableElem = document.createElement("table");
  tableElem.id = "displayTable";
  tableElem.setAttribute("cellspacing", "0");
  tableElem.setAttribute("cellpadding", "0");

  dispDiv.appendChild(tableElem);
  for (y = startY; y < endY; y++) {
    rowElem = document.createElement("tr");
    tableElem.appendChild(rowElem);
    for (x = startX; x < endX; x++) {
      tdElem = document.createElement("td");
      rowElem.appendChild(tdElem);
      // tdElem.innerHTML=x + "-" + y;
      img = document.createElement("img");
      img.src = tileBaseUrl + "?x=" + x + "&y=" + y;
      if ((x<endX-1) && (y<endY-1))
      {
        img.width = tileWidth;
        img.height = tileHeight;
      }
      tdElem.appendChild(img);
    }
  }
}
