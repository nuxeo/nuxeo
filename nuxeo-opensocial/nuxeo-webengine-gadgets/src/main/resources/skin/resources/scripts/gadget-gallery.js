
function getDetails(name) {
  var targetUrl = galleryBaseUrl + "/" + name + "/getDetails";
  jQuery.get(targetUrl, function(data) {
    jQuery('#gadgetDetails').html(data);
  });
}

function refreshList(name) {
  var targetUrl = galleryBaseUrl + "/listGadgets";
  if (name!='all') {
     targetUrl+="?cat=" + name;
  }
  jQuery.get(targetUrl, function(data) {
    jQuery('#gadgetListContainer').html(data);
  });
}

function selectGadget(idx, name) {
  jQuery(".currentGadget").toggleClass('currentGadget');
  jQuery("#gadget"+idx).toggleClass('currentGadget');
  getDetails(name);
}

function selectCategory(idx, name) {
  jQuery(".currentCategory").toggleClass('currentCategory');
  jQuery("#cat"+idx).toggleClass('currentCategory');
  refreshList(name);
}

function doAddGadget(name,url) {

  if (typeof(addGadgetHook)=='function') {
    addGadgetHook(name,url);
  }

  if (typeof(addGadget)=='function') {
    addGadget(name,url);
  }
  else {
    alert("adding Gadget with name=" + name + " and url=" + url + "\n you should define the addGadget(name,url) function !");
  }
}
