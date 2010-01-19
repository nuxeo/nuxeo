var firstTime = true;
var action = "";
var pUrl="/nuxeo/site/gadgets/photo/thumbnail.png";
var perm = gadgets.util.getUrlParameters().permission;
var url = "/nuxeo/site/gadgetDocumentAPI/getFile/";

function launchGadget() {
jQuery(document).ready(function(){
  if(firstTime){
    action = jQuery("#formUpload").attr("action");
    firstTime =false;
  }

  setTitle(prefs.getString("picTitle"));
  setLink(prefs.getString("link"));
  setLegend(prefs.getString("legend"));

  if(perm != 'true') jQuery("#perm").remove();

    var options = {
      beforeSubmit: control,
      success:function(){
        launchGadget();
      }
  };

  jQuery('#upload').click(function(){
    savePrefs();
    jQuery('#formUpload').ajaxSubmit(options);
  });

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

    gadgets.nuxeo.getGadgetId(function(id) {
    	loadImage(id);
    });
  });
}

function setTitle(title){
  if(_isSet(title)){
    jQuery("#title-field").val(gadgets.util.unescapeString(title));
    jQuery("#title").text(gadgets.util.unescapeString(title));
  }
};

function setLink(link){
  jQuery("#link-field").val(link);
};

function setLegend(legend){
  if(_isSet(legend)){
    jQuery("#legend-field").val(gadgets.util.unescapeString(legend));
    jQuery("#legend").text(gadgets.util.unescapeString(legend));
  }
};

function control(){
  if(jQuery.trim(jQuery("#file").val()) != "")
    return true;
  return false;
};

function _isSet(val){
  return (jQuery.trim(val) != "" && val != null);
};

function savePrefs(){
  prefs.set("picTitle",gadgets.util.escapeString(jQuery("#title-field").val()),
  "link",gadgets.util.escapeString(jQuery("#link-field").val()),
  "legend",gadgets.util.escapeString(jQuery("#legend-field").val()));
};

function loadImage(id){
  var actionUrl = [action,id].join("");
  var photoUrl = [url,id,'?junk=',Math.random()].join("");
  jQuery("#formUpload").attr("action", actionUrl);
  jQuery.ajax({
    type : "GET",
    url : photoUrl,
    error : function(){
        jQuery("#photo").attr("src", pUrl);
        jQuery("#photo").width("30%");
        showImage();
    },
    success : function(data, textStatus) {
        if (_isSet(prefs.getString("link")))
          var imgContainer = jQuery("<a id=\"link\" href=\""+prefs.getString("link")+"\" target=\"_tab\" ><img style=\"border:0;\" id=\"photo\" src=\"\"></a>");
        else
          var imgContainer = jQuery("<img style=\"border:0;\" id=\"photo\" src=\"\">");

        jQuery("#pictureContainer").append(imgContainer);
        jQuery("#photo").attr("src", photoUrl);
        jQuery("#pictureContainer").append("<span id=\"legend\"></span>");
        jQuery("#legend").text(gadgets.util.unescapeString(prefs.getString("legend")));
        jQuery("#photo").width("100%");
        showImage();
      }
    });
  };

function showImage(){
  jQuery("#loader").hide();
  jQuery("#link").fadeIn("slow");
  setTimeout(function(){
    gadgets.window.adjustHeight();
  },150);
};