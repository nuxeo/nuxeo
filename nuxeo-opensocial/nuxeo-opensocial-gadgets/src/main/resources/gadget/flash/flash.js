var firstTime = true;
var action = "";
var perm = gadgets.util.getUrlParameters().permission;
var url = "/nuxeo/site/gadgetDocumentAPI/getFile/";

function launchGadget() {
jQuery(document).ready(function(){
	if(firstTime){
		action = jQuery("#formUpload").attr("action");
		firstTime =false;
	}
	
	setTitle(prefs.getString("title"));
	
	if(perm != 'true') jQuery("#perm").remove();
    	
    var options = { 
    	beforeSubmit: control,
    	success:function(){
    		launchGadget();
    	}
	}; 
	
	jQuery('#upload').click(function(){
		jQuery('#formUpload').ajaxSubmit(options);
		savePrefs();
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
	
	if(prefs.getString("id")!=""){
		loadFlash(prefs.getString("id"));
	} else {
		gadgets.nuxeo.getGadgetId(function(id) {
			prefs.set("id",id);			
		});
	} 
	
	});
}	

function setTitle(title){
  if(_isSet(title)){
    jQuery("#title-field").val(gadgets.util.unescapeString(title));
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
	prefs.set("title",gadgets.util.escapeString(jQuery("#title-field").val()));
};

function loadFlash(id){
	var actionUrl = [action,id].join("");
	var flashUrl = [url,id].join("");
	jQuery("#formUpload").attr("action", actionUrl);
	jQuery.ajax({
		type : "GET",
		url : flashUrl,
		error : function(){
			console.log("error");
			jQuery("#flash").html("");
		    showFlash();
		},
		success : function(data, textStatus) {
		    gadgets.flash.embedFlash("http://swf.flibus.com/files/mario-racing-tournament.swf", "flash", {
      			swf_version: 6,
      			id: "flashid",
     			width: 300,
      			height: 250
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