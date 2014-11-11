var url = "/nuxeo/site/gadgetDocumentAPI/getFile/";
var perm = gadgets.util.getUrlParameters().permission;
var action = "";
var firstTime = true;

function launchGadget(){
jQuery(document).ready(function(){

  setTitle(prefs.getString("richTitle"));

    setPhoto();
    setLink(prefs.getString("link"));
    setLegend(prefs.getString("legend"));

    setHtml();
    setPlace(prefs.getString("place"));


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

  if(perm != 'true') jQuery("#perm").remove();

    var options = {
      success:function(){
        saveHtml();
      }
    };

  jQuery('#upload').click(function(){
    if(control()) {
      jQuery('#formUpload').ajaxSubmit(options);
    } else {
      saveHtml();
    }
  });

  new nicEditor({iconsPath : '/nuxeo/site/gadgets/richtext/nicEditorIcons.gif'}).panelInstance('richtext');

  setWidthAndBindEvents();
  jQuery('#loader').remove();
  gadgets.window.adjustHeight();


});
};

function saveHtml(){
  var html = jQuery('.nicEdit-main').html();
  gadgets.nuxeo.setHtmlContent(html, function(content){
  });
};

function savePrefs(){
  prefs.set("richTitle",val("title-field"),
  "link",val("link-field"),
  "legend",val("legend-field"),
  "place",jQuery("input[name='place']:checked").val());
};

function val(id){
  return gadgets.util.escapeString(jQuery("#"+id).val());
};

function control(){
  if(jQuery.trim(jQuery("#file").val()) != "")
    return true;
  return false;
};

function setWidthAndBindEvents(){
  if(firstTime){
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
    action = jQuery("#formUpload").attr("action");
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
  if(_isSet(link)){
    jQuery("#link-field").val(link);
  }
};

function setLegend(legend) {
  if(_isSet(legend)){
    jQuery("#legend-field").val(legend);
  }
};

function setPlace(place) {
    jQuery('link[@rel*=style][title]').each(function(i)
    {
      this.disabled = true;
      if (this.getAttribute('title') == prefs.getString("templates")) this.disabled = false;
    });
};

function setHtml() {
   gadgets.nuxeo.getHtmlContent(function(content) {
     if(_isSet(content))
       jQuery(".nicEdit-main").html(content);
       jQuery("#text").html(content);
   });
};

function setPhoto() {
   gadgets.nuxeo.getGadgetId(function(id) {
      loadImage(id);
    });
};

function loadImage(id){
  var actionUrl = [action,id].join("");
  var imgContainer = "";
  var photoUrl = [url,id,'?junk=',Math.random()].join("");
  jQuery("#formUpload").attr("action", actionUrl);
  jQuery.ajax({
    type : "GET",
    url : photoUrl,
    error : function(){
    imgContainer="<div>Pas de Photo</div>";
    },
    success : function(data, textStatus) {
       if (_isSet(prefs.getString("link")))
          imgContainer = jQuery("<a id=\"link\" href=\""+prefs.getString("link")+"\" target=\"_tab\" ><img style=\"border:0;\" id=\"picture\" src=\"\"></a>");
        else
          imgContainer = jQuery("<img style=\"border:0;\" id=\"picture\" src=\"\">");


       jQuery("#pictureContainer").append(imgContainer);
       jQuery("#picture").attr("src", photoUrl);
       jQuery("#pictureContainer").append("<span id=\"legend\"></span>");
       jQuery("#legend").text(gadgets.util.unescapeString(prefs.getString("legend")));
       gadgets.window.adjustHeight();
      }
    });


};

function _isSet(val) {
  return (jQuery.trim(val) != "" && val != null);
};
