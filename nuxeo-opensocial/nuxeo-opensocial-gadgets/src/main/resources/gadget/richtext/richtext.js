var perm = gadgets.util.getUrlParameters().permission;
var firstTime = true;

function launchGadget(){
jQuery(document).ready(function(){
    var idGadget = gadgets.nuxeo.getGadgetId();
    jQuery("#formUpload").attr("action", gadgets.nuxeo.getFormActionUrl(idGadget));
    loadHtml(idGadget);
    loadImage(idGadget);

    setTitle(prefs.getString("richTitle"));
    setLink(prefs.getString("link"));
    setLegend(prefs.getString("legend"));
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

  jQuery('#refresh').click(function(){
    gadgets.window.adjustHeight();
  });

  if(perm != 'true') jQuery("#perm").remove();


  jQuery('#upload').click(function(){
    jQuery('#richtext').val(jQuery('.nicEdit-main').html());
    savePrefs();

    jQuery('#formUpload').ajaxSubmit();
  });

  new nicEditor({iconsPath : '/nuxeo/site/gadgets/richtext/nicEditorIcons.gif'}).panelInstance('richtext');

  setWidthAndBindEvents();
  jQuery('#loader').remove();
  gadgets.window.adjustHeight();


});
};

function savePrefs(){
  prefs.set("richTitle",val("title-field"),
  "link",val("link-field"),
  "legend",val("legend-field"));
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
       jQuery("#text").html(content);
    }
};

function loadImage(id){
  var imgContainer = "";
  var photoUrl = [gadgets.nuxeo.getFileActionUrl(id),'?junk=',Math.random()].join("");
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
