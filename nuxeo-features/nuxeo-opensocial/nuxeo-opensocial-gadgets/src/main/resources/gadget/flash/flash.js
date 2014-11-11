var firstTime = true;
var action = "";
var perm = gadgets.nuxeo.isEditable();
var url = top.nxContextPath + "/site/gadgetDocumentAPI/getFile/";

function launchGadget() {
jQuery(document).ready(function(){
  if(firstTime){
    var idGadget = gadgets.nuxeo.getGadgetId();
    jQuery("#formUpload").attr("action", gadgets.nuxeo.getFormActionUrl(idGadget));
    firstTime =false;
  }

  setTitle(prefs.getString("title-flash"));

  if(!perm) jQuery("#perm").remove();

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

  loadFlash(gadgets.nuxeo.getGadgetId());

  });
}

function setTitle(title){
  if(_isSet(title)){
    jQuery("#title-flash").html(gadgets.util.unescapeString(title));
    jQuery("#title-field").val(gadgets.util.unescapeString(title));
  }
};

function control(){
  var filename  =jQuery.trim(jQuery("#file").val())  ;
  if( filename == "")
    return false;
  if(filename.match(".swf$" == ".swf")) {
    alert("Fichier non pris en charge (pas .swf)");
    return false;
  }

  return true;
};

function _isSet(val){
  return (jQuery.trim(val) != "" && val != null);
};

function savePrefs(){
  prefs.set("title-flash",gadgets.util.escapeString(jQuery("#title-field").val()));
};

function loadFlash(id){
  var flashUrl = [gadgets.nuxeo.getFileActionUrl(id),'?junk=',Math.random()].join("");
  jQuery("#formUpload").attr("action", gadgets.nuxeo.getFormActionUrl(id));
  jQuery.ajax({
    type : "GET",
    url : flashUrl,
    error : function(){
      jQuery("#flash").html("");
      showFlash();
    },
    success : function(data, textStatus) {
      gadgets.flash.embedFlash(flashUrl, "flash", 10,{
            id: "flashid",
            height: prefs.getString("height") + "px",
            onload: "gadgets.window.adjustHeight()"
        });

       showFlash();
      }
    });
  };

function showFlash(){
  jQuery("#loader").hide();
  jQuery("#flash").fadeIn("slow");
  setTimeout(function(){
    gadgets.window.adjustHeight();
  },150);
};