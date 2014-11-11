var loading_pathToImage = nxContextPath  + "/img/themes/common/map_loading.gif";
var loadOn = false;

//on page load call tb_init
$(document).ready(function(){
  imgLoader = new Image();// preload image
  imgLoader.src = loading_pathToImage;
  loading_show();
});

function loading_show() {//function called when the user clicks on a thickbox link

  if (loadOn == false)
  {
      loadOn = true;
      if (typeof document.body.style.maxHeight === "undefined") {//if IE 6
        $("body","html").css({height: "100%", width: "100%"});
        $("html").css("overflow","hidden");
        if (document.getElementById("LOADING_HideSelect") === null) {//iframe to hide select elements in ie6
          $("body").append("<iframe id='LOADING_HideSelect'></iframe><div id='LOADING_overlay'></div><div id='LOADING_window'></div>");

        }
      }else{//all others
        if(document.getElementById("LOADING_overlay") === null){
          $("body").append("<div id='LOADING_overlay'></div><div id='LOADING_window'></div>");
        }
      }

      if(loading_detectMacXFF()){
        $("#LOADING_overlay").addClass("LOADING_overlayMacFFBGHack");//use png overlay so hide flash
      }else{
        $("#LOADING_overlay").addClass("LOADING_overlayBG");//use background and opacity
      }

      imgPreloader = new Image();
      imgPreloader.onload = function(){
        imgPreloader.onload = null;

        var imageWidth = imgPreloader.width;
        var imageHeight = imgPreloader.height;

        $("#LOADING_window").append("<img id='LOADING_Image' src='"+loading_pathToImage+"' width='"+imageWidth+"' height='"+imageHeight+"' alt=''/>");
        $("#LOADING_window").css({display:"block"}); //for safari using css instead of show
      };

      imgPreloader.src = loading_pathToImage;
  }
  
  if($("#contentContainer").length>0){
  	var pos = $("#contentContainer").position();
    var top = pos.top - $(document).scrollTop() ;
	var left = pos.left - $(document).scrollLeft();
  	var w = $("#contentContainer").width();
  	$("#LOADING_overlay").css({"left":left,"top":top});
  	$("#LOADING_overlay").width(w);
  }
  
  $(document).bind("scroll", function(){
	$(document).scrollTop(0);
	$(document).scrollLeft(0);
  });
  
}

function loading_remove() {
  if (loadOn == true) {
    loadOn = false;
    $("#LOADING_window").fadeOut("fast",function(){$('#LOADING_window,#LOADING_overlay,#LOADING_HideSelect').trigger("unload").unbind().remove();});
    if (typeof document.body.style.maxHeight == "undefined") {//if IE 6
      $("body","html").css({height: "auto", width: "auto"});
      $("html").css("overflow","");
    }
    $(document).unbind("scroll");
    return false;
    }
  }

function loading_detectMacXFF() {
  var userAgent = navigator.userAgent.toLowerCase();
  if (userAgent.indexOf('mac') != -1 && userAgent.indexOf('firefox')!=-1) {
    return true;
  }
}