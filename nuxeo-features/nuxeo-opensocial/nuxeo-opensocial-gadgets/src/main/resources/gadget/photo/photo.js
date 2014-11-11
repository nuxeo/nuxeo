var firstTime = true;
var pUrl= [top.nxContextPath,"/site/gadgets/photo/thumbnail.png"].join("");
var perm = gadgets.nuxeo.isEditable();
var formUrl;


function launchGadget() {
jQuery(document).ready(function(){
  if(firstTime){
    formUrl = gadgets.nuxeo.getFormActionUrl(gadgets.nuxeo.getGadgetId());
    jQuery("#formUpload").attr("action", formUrl);
    firstTime =false;
  }

  setTitle(prefs.getString("picTitle"));
  setLink(prefs.getString("link"));
  setLegend(prefs.getString("legend"));

  if(!perm) jQuery("#perm").remove();


  jQuery('#upload').click(function(){
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
    } else {
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
  	var _title = gadgets.util.unescapeString(title);
    jQuery("#title-field").val(_title);
    jQuery("#title").text(_title);
  }
};

function setLink(link){
  jQuery("#link-field").val(link);
};

function setLegend(legend){
  if(_isSet(legend)){
  	var _legend = gadgets.util.unescapeString(legend);
    jQuery("#legend-field").val(_legend);
    jQuery("#legend").text(_legend);
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
  jQuery("#formUpload").attr("action", formUrl);
  jQuery.ajax({
    type : "GET",
    url : photoUrl,
    error : function(){
        jQuery("#photo").attr("src", pUrl);
        jQuery("#photo").width("30%");
        showImage();
    },
    success : function(data, textStatus) {
    	jQuery("#photo").remove();
    	jQuery("#link").remove();
    	jQuery("#legend").remove();
    	var imgContainer = jQuery("<img onload=\"gadgets.window.adjustHeight()\"></img>").attr("id","photo").attr("src",photoUrl).attr("style","border:0;").width("100%");
        if (_isSet(prefs.getString("link"))){
          var link = jQuery("<a onload=\"gadgets.window.adjustHeight()\"></a>").attr("id","link").attr("href",prefs.getString("link")).attr("target","_tab");
          link.append(imgContainer);
          jQuery("#pictureContainer").append(link);
        } else {
          jQuery("#pictureContainer").append(imgContainer);
        }        
        jQuery("#pictureContainer").append(jQuery("<span></span>").attr("id","legend").text(gadgets.util.unescapeString(prefs.getString("legend"))));
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