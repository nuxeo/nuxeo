var perm = gadgets.util.getUrlParameters().permission;
var modifyLink = "<div style=\"float:left;\" id=\"modifyLink\"><a href=\"javascript:modifyVideo();\" id=\"modifyVideo\">Modifier</a></div>";

function validateVideo() {
  prefs.set("vidTitle",gadgets.util.escapeString(jQuery("#title-field").val()));
  gadgets.nuxeo.setHtmlContent(jQuery("#baliseVideo").val());
  if(perm == 'true')
    html = modifyLink + jQuery("#baliseVideo").val();

  showVideo(html);
  jQuery("#cancelVideo").show();

  return false;
}

function modifyVideo() {
  jQuery("#modifyLink").hide();
  showForm();
}

function showForm() {
  jQuery("#addVideo").show();
  //jQuery("#showVideo").hide();
  gadgets.window.adjustHeight();
}

function cancelVideo(){
  jQuery("#modifyLink").show();
  showVideo(html)
}

function showVideo(html) {
  html = "<div id=\"title\">"+prefs.getString("vidTitle")+"</div>"+html;
  jQuery("#showVideo").html(html);
  jQuery("#addVideo").hide();
  jQuery("#showVideo").fadeIn();
  var dim = gadgets.window.getViewportDimensions();
  var h = (dim.width * jQuery("embed").height())/jQuery("object").width();
  jQuery("embed").width(dim.width);
  jQuery("embed").height(h);
  gadgets.window.adjustHeight();
}

function launchVideoWidget(balise) {
  jQuery("#baliseVideo").val(balise);
  jQuery("#title-field").val(prefs.getString("vidTitle"));
  if (balise == "") {
    // Pas de balise vidéo saisie
  jQuery("#cancelVideo").hide();
    showForm();
  } else {
    // Vidéo présente
    html = balise+modifyLink;
    showVideo(html)
  }
}