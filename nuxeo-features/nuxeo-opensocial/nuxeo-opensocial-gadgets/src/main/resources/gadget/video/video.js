var perm = gadgets.util.getUrlParameters().permission;


function launchVideoWidget(balise) {
  setTitle(prefs.getString("vidTitle"));
  setVideo(balise);

  if(perm != 'true')
  	jQuery("#perm").remove();
  	
  gadgets.window.adjustHeight();
  
  jQuery('#show').click(function(){
    jQuery('#show').hide();
    jQuery('#form').show();
    gadgets.window.adjustHeight();
  });

  jQuery('#hide').click(function(){
    jQuery('#form').hide();
    jQuery('#show').show();
    gadgets.window.adjustHeight();
  });
  
  jQuery('#valid').click(function(){
	var base = jQuery("#baliseVideo").val();
	var ti = jQuery("#title-field").val();
	gadgets.nuxeo.setAjaxPref("vidTitle",ti);
	gadgets.nuxeo.setHtmlContent(base, function(content){
		setTitle(ti);
		setVideo(base);
		gadgets.window.adjustHeight();
	});
	
  });
};

function setTitle(title){
  var t = "";
  if(_isSet(title))
  	t = gadgets.util.unescapeString(title);
  jQuery("#title-field").val(t);
  jQuery("#title").text(t);
};

function _isSet(val) {
  return (jQuery.trim(val) != "" && val != null);
};

function setVideo(balise){
  if(_isSet(balise)){
    jQuery("#video").html(balise);
    jQuery("#baliseVideo").text(balise);
    var dim = gadgets.window.getViewportDimensions();
  	var h = (dim.width * jQuery("embed").height())/jQuery("object").width();
  	jQuery("embed").width(dim.width);
  	jQuery("embed").height(h);
  } else {
  	jQuery("#video").html("");
    jQuery("#baliseVideo").text("");
  }
};
