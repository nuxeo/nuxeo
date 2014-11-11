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

  if(perm) jQuery("#perm").show();


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

var reg = new RegExp("^http://");

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
  var link = jQuery.trim(jQuery("#link-field").val());
  prefs.set("picTitle",gadgets.util.escapeString(jQuery("#title-field").val()),
  "link",gadgets.util.escapeString(reg.test(link) ? link : (link.length>0 ? ["http://",link].join("") : "")),
  "legend",gadgets.util.escapeString(jQuery("#legend-field").val()));
};




function loadImage(id){
  var photoUrl = gadgets.nuxeo.getFileActionUrl(id);
  jQuery("#photo").error(function() {
    jQuery(this).attr("src",pUrl);
    jQuery(this).width("30%");
    gadgets.window.adjustHeight();
  });
  
  
  jQuery("#photo").load(function() {
    if (_isSet(prefs.getString("link"))){
      jQuery("#photolink").attr("href",prefs.getString("link")).attr("target","_tab");
    }
  
    jQuery("#pictureLegend").text(gadgets.util.unescapeString(prefs.getString("legend")));
    gadgets.window.adjustHeight();
  });
  
  jQuery("#photo").attr("src",photoUrl);    
};

