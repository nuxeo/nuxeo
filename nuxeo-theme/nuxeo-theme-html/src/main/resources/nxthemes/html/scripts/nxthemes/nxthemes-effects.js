/*

 NXThemes UI library - effects

 Author: Jean-Marc Orliaguet <jmo@chalmers.se>

*/

NXThemes.registerEffects({

  show: function(node, options) {
    var delay = options.delay;
    if (delay) {
      return new NXThemes.Scheduler({
        delay: delay,
        onComplete: function() { node.show(); }
      });
    } else {
      node.show();
    }
  },

  hide: function(node, options) {
    var delay = options.delay;
    if (delay) {
      return new NXThemes.Scheduler({
        delay: delay,
        onComplete: function() { node.hide(); }
      });
    } else {
      node.hide();
    }
  },

  fadein: function(node, options) {
    var opacity = node.style.opacity;
    if (!opacity) {
      node.setOpacity(0);
    }
    Object.extend(options, {
      action: function(value) {
        if (value > opacity) {
          node.setOpacity(value);
        }
      },
      onComplete: function() {
        node.show();
      }
    });
    return new NXThemes.Scheduler(options);
  },

  fadeout: function(node, options) {
    var opacity = node.style.opacity;
    Object.extend(options, {
      action: function(value) {
        if (value < opacity) {
          node.setOpacity(1-value);
        }
      },
      onComplete: function() {
        node.hide();
        node.style.opacity = '';
      }
    });
    return new NXThemes.Scheduler(options);
  },

  slidedown: function(node, options) {
    if (node.visible()) {
      return;
    }
    var height = node.getHeight();
    node.makeClipping();
    Object.extend(options, {
      action: function(value) {
        node.setStyle({height: height*value + 'px', display: 'block'});
      },
      onComplete: function() {
        node.setStyle({height: height + 'px'});
        node.undoClipping();
      }
    });
    return new NXThemes.Scheduler(options);
  },

  slideup: function(node, options) {
    if (!node.visible()) {
      return;
    }
    var height = node.getHeight();
    node.makeClipping();
    Object.extend(options, {
      action: function(value) {
        node.setStyle({height: height*(1-value) + 'px'});
      },
      onComplete: function() {
        node.hide();
        node.setStyle({height: height + 'px'});
        node.undoClipping();
      }
    });
    return new NXThemes.Scheduler(options);
  },

  activate: function(node, options) {
    var bg = node.getBackgroundColor();
    if (!node._saved_bg_color) {
      node._saved_bg_color = bg;
    }
    Object.extend(options, {
      action: function(value) {
        node.setBackgroundColor({r: bg.r, g: bg.g, b: bg.b-value});
      }
    });
    return new NXThemes.Scheduler(options);
  },

  deactivate: function(node, options) {
    var bg = node._saved_bg_color;
    if (!bg) {
      return;
    }
    Object.extend(options, {
      action: function(value) {
        node.setBackgroundColor({r: bg.r, g: bg.g, b: bg.b-(1-value)});
      },
      onComplete: function() {
        node.setBackgroundColor(bg);
      }
    });
    return new NXThemes.Scheduler(options);
  }

});
