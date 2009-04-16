Effect.ResizeWindow = Class.create();
Object.extend(Object.extend(Effect.ResizeWindow.prototype, Effect.Base.prototype), {
  initialize: function(win, top, left, width, height) {
    this.window = win;
    this.window.resizing = true;
    
    var size = win.getSize();
    this.initWidth    = parseFloat(size.width);
    this.initHeight   = parseFloat(size.height);

    var location = win.getLocation();
    this.initTop    = parseFloat(location.top);
    this.initLeft   = parseFloat(location.left);

    this.width    = width != null  ? parseFloat(width)  : this.initWidth;
    this.height   = height != null ? parseFloat(height) : this.initHeight;
    this.top      = top != null    ? parseFloat(top)    : this.initTop;
    this.left     = left != null   ? parseFloat(left)   : this.initLeft;

    this.dx     = this.left   - this.initLeft;
    this.dy     = this.top    - this.initTop;
    this.dw     = this.width  - this.initWidth;
    this.dh     = this.height - this.initHeight;
    
    this.r2      = $(this.window.getId() + "_row2");
    this.content = $(this.window.getId() + "_content");
        
    this.contentOverflow = this.content.getStyle("overflow") || "auto";
    this.content.setStyle({overflow: "hidden"});
    
    // Wired mode
    if (this.window.options.wiredDrag) {
      this.window.currentDrag = win._createWiredElement();
      this.window.currentDrag.show();
      this.window.element.hide();
    }

    this.start(arguments[5]);
  },
  
  update: function(position) {
    var width  = Math.floor(this.initWidth  + this.dw * position);
    var height = Math.floor(this.initHeight + this.dh * position);
    var top    = Math.floor(this.initTop    + this.dy * position);
    var left   = Math.floor(this.initLeft   + this.dx * position);

    if (window.ie) {
      if (Math.floor(height) == 0)  
        this.r2.hide();
      else if (Math.floor(height) >1)  
        this.r2.show();
    }      
    this.r2.setStyle({height: height});
    this.window.setSize(width, height);
    this.window.setLocation(top, left);
  },
  
  finish: function(position) {
    // Wired mode
    if (this.window.options.wiredDrag) {
      this.window._hideWiredElement();
      this.window.element.show();
    }

    this.window.setSize(this.width, this.height);
    this.window.setLocation(this.top, this.left);
    this.r2.setStyle({height: null});
    
    this.content.setStyle({overflow: this.contentOverflow});
      
    this.window.resizing = false;
  }
});

Effect.ModalSlideDown = function(element) {
  var windowScroll = WindowUtilities.getWindowScroll();    
  var height = element.getStyle("height");  
  element.setStyle({top: - (parseFloat(height) - windowScroll.top) + "px"});
  
  element.show();
  return new Effect.Move(element, Object.extend({ x: 0, y: parseFloat(height) }, arguments[1] || {}));
};


Effect.ModalSlideUp = function(element) {
  var height = element.getStyle("height");
  return new Effect.Move(element, Object.extend({ x: 0, y: -parseFloat(height) }, arguments[1] || {}));
};


