
function getDetails(name) {
  var targetUrl = galleryBaseUrl + "/" + name + "/getDetails";
  jqw.get(targetUrl, function(data) {
    jqw('#gadgetDetails').html(data);
  });
}

function refreshList(name) {
  var targetUrl = galleryBaseUrl + "/listGadgets";
  if (name!='all') {
     targetUrl+="?cat=" + name;
  }
  jqw.get(targetUrl, function(data) {
    jqw('#gadgetListContainer').html(data);
  });
}

function selectGadget(idx, name) {
  jqw(".currentGadget").toggleClass('currentGadget');
  jqw("#gadget"+idx).toggleClass('currentGadget');
  getDetails(name);
}

function selectCategory(idx, name) {
  jqw(".currentCategory").toggleClass('currentCategory');
  jqw("#cat"+idx).toggleClass('currentCategory');
  refreshList(name);
}

//function addGadget(name, url) {
//  alert(name + ":" + url);
//}