var firstTime = true;
var pUrl=top.nxContextPath + "/site/gadgets/photo/thumbnail.png";
var perm = gadgets.nuxeo.isEditable();


function launchGadget() {
jQuery(document).ready(function(){
  if(firstTime){
    var idGadget = gadgets.nuxeo.getGadgetId();
    jQuery("#formUpload").attr("action", gadgets.nuxeo.getFormActionUrl(idGadget));
    firstTime =false;
  }

  setTitle(prefs.getString("picTitle"));
  setLink(prefs.getString("link"));
  setLegend(prefs.getString("legend"));

  if(!perm) jQuery("#perm").remove();


  jQuery('#upload').click(function(){
    jQuery("#resize_width").val(gadgets.window.getViewportDimensions().width);
    if (jQuery("#file").val() != ""){
      jQuery('#formUpload').ajaxSubmit({
          success:function(data){
        savePrefs();
          launchGadget();

          },
          error: function(xhr,rs) {
            alert(xhr.responseText);
          }
      });
    }
    else{
    savePrefs();
    launchGadget();
    }
    return false;
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


  loadImage(gadgets.nuxeo.getGadgetId());
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

function _isSet(val){
  return (jQuery.trim(val) != "" && val != null);
};

function savePrefs(){
  prefs.set("picTitle",gadgets.util.escapeString(jQuery("#title-field").val()),
  "link",gadgets.util.escapeString(jQuery("#link-field").val()),
  "legend",gadgets.util.escapeString(jQuery("#legend-field").val()));
};




function loadImage(id){
  var photoUrl = [gadgets.nuxeo.getFileActionUrl(id),'?junk=',Math.random()].join("");
  jQuery("#formUpload").attr("action", gadgets.nuxeo.getFormActionUrl(id));
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
          var imgContainer = jQuery("<a id=\"link\" href=\""+prefs.getString("link")+"\" target=\"_tab\" ><img style=\"border:0;\" id=\"photo\" src=\"\" onload=\"gadgets.window.adjustHeight()\"></a>");
        else
          var imgContainer = jQuery("<img style=\"border:0;\" id=\"photo\" src=\"\" onload=\"gadgets.window.adjustHeight()\">");

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