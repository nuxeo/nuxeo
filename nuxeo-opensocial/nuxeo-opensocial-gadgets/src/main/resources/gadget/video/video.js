var perm = gadgets.util.getUrlParameters().permission;
var modifyLink = "<div style=\"float:right;\"><a href=\"javascript:modifyVideo();\" id=\"modifyVideo\">Modifier</a></div>"; // javascript:
// toogleForms('create')\

function validateVideo() {
  gadgets.nuxeo.setHtmlContent(jQuery("#baliseVideo").val());
  if(perm == 'true')
  	html = modifyLink + jQuery("#baliseVideo").val();
  showVideo(html);
}

function modifyVideo() {
  showForm();
}

function showForm() {
  jQuery("#addVideo").show();
  jQuery("#showVideo").hide();
  gadgets.window.adjustHeight();
  var dim = gadgets.window.getViewportDimensions();
  console.log("width is "+dim.width);
  console.log("height is "+dim.height);
  
}

function showVideo(html) {
  jQuery("#showVideo").html(html);
  jQuery("#addVideo").hide();
  jQuery("#showVideo").show();
  gadgets.window.adjustHeight();
}

function launchVideoWidget(balise) {
  if (balise == "") {
    // Pas de balise vidéo saisie
    showForm();
  } else {
    // Vidéo présente
    html = modifyLink + balise;
    showVideo(html)
  }
}