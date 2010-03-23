var perm = gadgets.nuxeo.isEditable();
var url = [top.nxContextPath,"/site/gadgetDocumentAPI/getHtmlContent/"].join();

function launchVideoWidget() {
   var idGadget = gadgets.nuxeo.getGadgetId();
   jQuery("#formUpload").attr("action", gadgets.nuxeo.getFormActionUrl(idGadget));

   setTitle(prefs.getString("vidTitle"));

  loadVideo(idGadget);

  if(!perm)
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
    prefs.set("vidTitle", gadgets.util.escapeString(jQuery("#title-field").val()),"tmp", ["",Math.random()].join(""));
    jQuery('#formUpload').ajaxSubmit();
  });
};


function loadVideo(id){
  jQuery.ajax({
    type : "GET",
    url :  gadgets.nuxeo.getHtmlActionUrl(id),
    success : function(html) {
      setVideo(html);
    }
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
    var embed = jQuery("embed");
    if(embed.length == 0) embed = jQuery("object");
    var h = (dim.width * embed.height())/jQuery("object").width();
    embed.width(dim.width);
    if(h!=0) embed.height(h);
    embed.attr("wmode","transparent");
    embed.attr("type","application/x-shockwave-flash");
    //Hack, if open popup in container
    jQuery("#video").fadeOut("fast",function(){
    	jQuery("#video").fadeIn();
    });
    
  } else {
    jQuery("#video").html("");
    jQuery("#baliseVideo").text("");
  }
  setTimeout(function(){
    gadgets.window.adjustHeight();
  }, 150);
};
