/****
  WaitDlg - Simple Wait screen popup
   Copyright Nuxeo 2006
 AUTHOR
  td@nuxeo.com
 VERSION
	 1.0
 LICENSE
  LGPL
****/

var WD_CURRENT = null;
var WD_Singleton = null;
WD_IMG_DIR = null;

function WaitDlg() {
  if(WD_IMG_DIR)
    this.img_dir = WD_IMG_DIR;
  else
    this.img_dir = "waitdialog/";
  this.center_window = true;
  this.g_window = null;
  this.overlay = null;
  this.timeout = null;
  this.defaultSize();
  this.url = "";
  this.callback_fn = null;
  this.reload_on_close = false;
}

WaitDlg.prototype.setDimension = function(width, height) {
  this.height = height;
  this.width = width;
}

WaitDlg.prototype.setFullScreen = function(bool) {
  this.full_screen = bool;
}

/**
  If bool is true the window will be centered vertically also
  **/
WaitDlg.prototype.setCenterWindow = function(bool) {
  this.center_window = bool;
}

/**
  Set the path where images can be found.
  Can be relative: WaitDlg/
  Or absolute: http://yoursite.com/WaitDlg/
  **/
WaitDlg.prototype.setImageDir = function(dir) {
  this.img_dir = dir;
}
/**
  Set a function that will be called when WaitDlg closes
  **/
WaitDlg.prototype.setCallback = function(fn) {
  if(fn)
    this.callback_fn=fn;
}
/**
  Show the WaitDlg with a caption and an url
  **/
WaitDlg.prototype.show = function() {
  WD_CURRENT = this;

  //Be sure that the old loader and dummy_holder are removed
  AJS.map(AJS.$bytc("div", "WD_dummy"), function(elm) { AJS.removeElement(elm) });
  AJS.map(AJS.$bytc("div", "WD_loader"), function(elm) { AJS.removeElement(elm) });

  //If ie, hide select, in others hide flash
  if(AJS.isIe())
    AJS.map(AJS.$bytc("select"), function(elm) {elm.style.visibility = "hidden"});
  AJS.map(AJS.$bytc("object"), function(elm) {elm.style.visibility = "hidden"});

  this.initOverlayIfNeeded();
  this.setOverlayDimension();
  AJS.showElement(this.overlay);
  this.setFullScreenOption();

  this.initIfNeeded();
  AJS.hideElement(this.g_window);
  this.setVerticalPosition();
  this.setWidthNHeight();
  this.setTopNLeft();
  AJS.showElement(this.g_window)

  return false;
}

WaitDlg.prototype.setType = function(type) {
  this.type = type;
}


WaitDlg.prototype.hide = function() {
  if ( this.g_window && this.g_window.style && this.g_window.style.display == 'none') return;
  AJS.hideElement(this.g_window, this.overlay);
  if(AJS.isIe())
    AJS.map(AJS.$bytc("select"), function(elm) {elm.style.visibility = "visible"});
  AJS.map(AJS.$bytc("object"), function(elm) {elm.style.visibility = "visible"});

  var cb = WD_CURRENT.callback_fn;
  if(cb)
  	cb();

  WD_CURRENT = null;

  if(this.reload_on_close)
	if ( window.location.reload)
		window.location.reload();
	else
		window.location = window.location;
}

WD_initSingleton = function() {
  if(!WD_Singleton) {
    WD_Singleton = new WaitDlg();
  }
}

WD_show = function(callback_fn) {
  WD_initSingleton();
  WD_Singleton.defaultSize();
  WD_Singleton.setFullScreen(false);
  WD_Singleton.setType("page");
  WD_Singleton.setCallback(callback_fn);
  WD_Singleton.setDimension(200, 200);
  WD_Singleton.show();
}

WD_setText = function (txt)
{
	if (WD_Singleton)
		WD_Singleton.label.innerHTML=txt;
}

WD_hide = function() {
  WD_CURRENT.hide();
}

/**
  Preload all the images used by WaitDlg. Static function
  **/
WaitDlg.preloadWaitDlgImages = function(img_dir) {
  var pics = [];

  if(!img_dir)
    img_dir = WD_IMG_DIR;

  var fn = function(path) {
    var pic = new Image();
    pic.src = WD_IMG_DIR + path;
    pics.push(pic);
  };
  AJS.map([ 'overlay_light.png', 'NxWaiter.gif'], AJS.$b(fn, this));
}


/**
  Init functions
  **/
WaitDlg.prototype.initOverlayIfNeeded = function() {
  //Create the overlay
  this.overlay = AJS.DIV({'id': 'WD_overlay'});
  if(AJS.isIe()) {
    this.overlay.style.backgroundColor = "#000000";
    this.overlay.style.backgroundColor = "transparent";
    this.overlay.style.backgroundImage = "url("+ this.img_dir +"blank.gif)";
    this.overlay.runtimeStyle.filter = "progid:DXImageTransform.Microsoft.AlphaImageLoader(src='" + this.img_dir + "overlay_light.png" + "',sizingMethod='scale')";
  }
  else
  	{
    this.overlay.style.backgroundImage = "url("+ this.img_dir + "overlay_light.png)";
	}

  AJS.getBody().insertBefore(this.overlay, AJS.getBody().firstChild);
};

WaitDlg.prototype.initIfNeeded = function() {
  this.init();
  this.setWidthNHeight = AJS.$b(this.setWidthNHeight, this);
  this.setTopNLeft = AJS.$b(this.setTopNLeft, this);
  this.setFullScreenOption = AJS.$b(this.setFullScreenOption, this);
  this.setOverlayDimension = AJS.$b(this.setOverlayDimension, this);

  WaitDlg.addOnWinResize(this.setWidthNHeight, this.setTopNLeft, this.setFullScreenOption, this.setOverlayDimension);

  var fn = function() {
    this.setOverlayDimension();
    this.setVerticalPosition();
    this.setTopNLeft();
  };


}

WaitDlg.prototype.init = function() {
  this.g_window = AJS.DIV({'id': 'WD_window'});

  var img_wait = AJS.IMG({'src': this.img_dir + "NXwaiter.gif"});
  var table = AJS.TABLE({'frameborder': 0});

	// IE fix: you cannot insert TRs directly in a TABLE, but in its inner TBODY
  var tbody = AJS.TBODY();
   AJS.ACN(table, tbody);

  var td_link = AJS.TD({'class': 'Wait_Link'});

  td_link.innerHTML="<A class='Wait_Link' href='javascript:WD_hide()'>close</A>";
  AJS.ACN(tbody, AJS.TR(td_link));

  var td_img = AJS.TD({'class': 'Wait_Img'});
  AJS.ACN(tbody, AJS.TR(td_img));
  var td_label = AJS.TD({'class': 'Wait_Label'});
  AJS.ACN(tbody, AJS.TR(td_label));

  AJS.ACN(td_img, img_wait);
  label = AJS.DIV({'id': 'WD_label'});
  label.innerHTML="This is My Label";
  this.label=label;
  AJS.ACN(td_label, label);

  AJS.ACN(this.g_window, table);

  AJS.getBody().insertBefore(this.g_window, this.overlay.nextSibling);

}

WaitDlg.prototype.setOverlayDimension = function() {
  var page_size = AJS.getWindowSize();
  if((navigator.userAgent.toLowerCase().indexOf("firefox") != -1))
   AJS.setWidth(this.overlay, "100%");
  else
   AJS.setWidth(this.overlay, page_size.w);

  var max_height = Math.max(AJS.getScrollTop()+page_size.h, AJS.getScrollTop()+this.height);
  if(max_height < AJS.getScrollTop())
    AJS.setHeight(this.overlay, max_height);
  else
    AJS.setHeight(this.overlay, AJS.getScrollTop()+page_size.h);
}

WaitDlg.prototype.setWidthNHeight = function() {
  //Set size
  AJS.setWidth(this.g_window, this.width);
  AJS.setHeight(this.g_window, this.height);
}

WaitDlg.prototype.setTopNLeft = function() {
  var page_size = AJS.getWindowSize();
  AJS.setLeft(this.g_window, ((page_size.w - this.width)/2)-13);

  this.center_window=true;

  if(this.center_window) {
    var fl = ((page_size.h - this.height) /2) - 15 + AJS.getScrollTop();
    AJS.setTop(this.g_window, fl);
  }
  else {
    if(this.g_window.offsetHeight < page_size.h)
      AJS.setTop(this.g_window, AJS.getScrollTop());
  }
}

WaitDlg.prototype.setVerticalPosition = function() {
  var page_size = AJS.getWindowSize();
  var st = AJS.getScrollTop();
  if(this.g_window.offsetWidth <= page_size.h || st <= this.g_window.offsetTop) {
    AJS.setTop(this.g_window, st);
  }
}

WaitDlg.prototype.setFullScreenOption = function() {
  if(this.full_screen) {
    var page_size = AJS.getWindowSize();

    overlay_h = page_size.h;

    this.width = Math.round(this.overlay.offsetWidth - (this.overlay.offsetWidth/100)*10);
    this.height = Math.round(overlay_h - (overlay_h/100)*10);


  }
}

WaitDlg.prototype.defaultSize = function() {
  this.width = 300;
  this.height = 300;
}
WaitDlg.addOnWinResize = function(funcs) {
  funcs = AJS.$A(funcs);
  AJS.map(funcs, function(fn) { AJS.AEV(window, "resize", fn); });
}

