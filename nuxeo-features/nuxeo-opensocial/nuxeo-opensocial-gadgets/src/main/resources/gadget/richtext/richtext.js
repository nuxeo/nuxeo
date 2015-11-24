var perm = gadgets.nuxeo.isEditable();
var firstTime = true;
var formUrl;

function launchGadget(){
jQuery(document).ready(function(){
  var idGadget = gadgets.nuxeo.getGadgetId();
  formUrl = gadgets.nuxeo.getFormActionUrl(idGadget);
  jQuery("#formUpload").attr("action", formUrl);
  jQuery("#fileUploadForm").attr("action", formUrl);

  loadHtml(idGadget);
  loadImage(idGadget);

  setTitle(gadgets.util.unescapeString(prefs.getString("richTitle")));
  setLink(gadgets.util.unescapeString(prefs.getString("link")));
  setLegend(gadgets.util.unescapeString(prefs.getString("legend")));
  setPlace(gadgets.util.unescapeString(prefs.getString("templates")));

  jQuery('#show').click(function(){
    jQuery("#mainContainer").hide();
    jQuery('#show').hide();
    jQuery('#form').show();
    gadgets.window.adjustHeight();
  });

  jQuery('#hide').click(function(){
    jQuery("#mainContainer").show();
    jQuery('#form').hide();
    jQuery('#show').show();
    gadgets.window.adjustHeight();
  });

  jQuery('#refresh').click(function(){
    gadgets.window.adjustHeight();
  });

  if(perm) jQuery("#perm").show();

  jQuery('#upload').click(function(){
    jQuery('#richtext').val(jQuery('.nicEdit-main').html());
    jQuery('#formUpload').ajaxSubmit({
      success:function(){
        savePrefs();
      }
    });
    return false;
  });

  jQuery('#fileuploadBtn').click(function(){
    jQuery('#fileUploadForm').ajaxSubmit({
      beforeSubmit: control,
      success:function(){
        loadImage(gadgets.nuxeo.getGadgetId());
      }
    });
    return false;
  });

  jQuery("#deletePhoto").click(function(){
    jQuery.ajax({
      async:false,
      type : "POST",
      url : [formUrl,"/deletePicture"].join(""),
      success:function(data){
        jQuery('#deletePhoto').hide();
        jQuery("#imgPreview").hide();
        jQuery("#pictureContainer").hide();
      }
    });
    return false;
  });

  new nicEditor({iconsPath : '/nuxeo/site/gadgets/richtext/nicEditorIcons.gif', buttonList : ['bold','italic','underline','left','center','right','justify','ul','ol','link','unlink','forecolor','bgcolor','fontSize','fontFamily']}).panelInstance('richtext');

  setWidthAndBindEvents();
  jQuery('#loader').remove();
  jQuery('.nicEdit-main').attr("style","min-height:60px;margin:4px;");
  gadgets.window.adjustHeight();
});
};

function control(){
  return (jQuery.trim(jQuery("#file").val()) != "") ? true : false;
};

function savePrefs(){
  prefs.set("richTitle",val("title-field"),
  "link",formatUrl(val("link-field")),
  "legend",val("legend-field"),
  "tmp",["",Math.random()].join(""));
};

function formatUrl(url)
{
  url = jQuery.trim(url);
  if (url.toLowerCase().indexOf("http://") == 0) {
    return(url);
  } else if (url.length > 0) {
    return("http://" + url);
  } else {
    return "";
  }
}

function val(id){
  return gadgets.util.escapeString(jQuery(["#",id].join("")).val());
};

function setWidthAndBindEvents(){
  if(firstTime) {
    var width = gadgets.window.getViewportDimensions().width;
    var area = jQuery('#richtext').prev();
    area.css("background-color","white");
    area.width(width);
    jQuery(area).keydown(function(e){
      var keycode = (e.keyCode ? e.keyCode : (e.which ? e.which : e.charCode));
      if (keycode == 13 || keycode == 8)
        gadgets.window.adjustHeight();
      return true;
    });
    jQuery('.nicEdit-main').width("100%");
    var prev = jQuery(area).prev();
    prev.width(width);
    firstTime = false;
  }
};

function setTitle(title) {
  if(_isSet(title)){
    jQuery("#title-field").val(title);
    jQuery("#title").text(title);
  }
};

function setLink(link) {
  if(_isSet(link)) jQuery("#link-field").val(link);
};

function setLegend(legend) {
  if(_isSet(legend)) jQuery("#legend-field").val(legend);
};

function setPlace(templates) {
  jQuery("#mainContainer").attr("class",templates);
};

function loadHtml(id){
  jQuery.ajax({
    type : "GET",
    url :  gadgets.nuxeo.getHtmlActionUrl(id),
    success : function(html) {
      setHtml(html);
    }
    });
};

function setHtml(content) {
  if(_isSet(content)){
    jQuery(".nicEdit-main").html(content);
    jQuery("#pictureContainer").append(content);
    gadgets.window.adjustHeight();
  }
};

function loadImage(id){
  var photoUrl = gadgets.nuxeo.getFileActionUrl(id);

  jQuery("#imgPreview").error(function() { 
    jQuery(this).hide();
    jQuery("#deletePhoto").hide();
  });

  jQuery("#picture").error(function() {
    jQuery("#pictureContainer").hide();
    jQuery("#imgPreview").hide();
    gadgets.window.adjustHeight();
  });

  jQuery("#picture").load(function() {

    jQuery("#imgPreview").show();
    jQuery("#deletePhoto").show();

    if (_isSet(prefs.getString("link"))){
      jQuery("#pictureLink").attr("href",prefs.getString("link")).attr("target","_tab");
    }

    if (_isSet(prefs.getString("legend"))){
      jQuery("#pictureLegend").text(gadgets.util.unescapeString(prefs.getString("legend")));
    }

    gadgets.window.adjustHeight();
  });

  jQuery("#picture").removeAttr("src").attr("src",photoUrl);
  jQuery("#imgPreview").removeAttr("src").attr("src",photoUrl);

  
};

function _isSet(val) {
  return (jQuery.trim(val) != "" && val != null);
};
