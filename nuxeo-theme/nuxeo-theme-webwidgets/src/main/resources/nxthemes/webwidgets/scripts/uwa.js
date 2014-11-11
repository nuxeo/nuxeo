
var Browser = {};
var UWA = {

  log: function(msg) {
      if (window.console) {
        console.log(msg); // firebug, safari
      } else if (window.opera) {
        opera.postError(msg)
      }
  }

};

UWA.Element = function() {};
UWA.Element.prototype = {

  getDimensions: function() {
    var dimensions = this.getSize();
    return {
      'width': dimensions.size.x,
      'height': dimensions.size.y
    }
  },

  addContent: function(content) {
    if (typeof content == 'string') {
      var node = document.createElement("div");
      node.innerHTML = content;
      return this.appendChild(node);
    }
    return this.appendChild(content);
  },

  appendText: function(text) {
    var node = document.createTextNode(text);
    return this.appendChild(node);
  },

  setText: function(text) {
    this[(typeof this.innerText != 'undefined') ? 'innerText' : 'textContent'] = text;
    return this;
  },

  setHTML: function(html) {
    this.innerHTML = html;
    return this;
  },

  setContent: function(content) {
    if (typeof content == 'string') {
      this.setHTML(content);
    } else if (typeof content == 'object') {
      this.empty();
      this.appendChild(content);
    }
    return this;
  },

  empty: function() {
    this.innerHTML = '';
    return this;
  },

  getParent: function() {
    return this.parentNode;
  },

  getChildren: function() {
    return this.childNodes;
  }

};

UWA.$element = function(el) {
  if (el) {
    if (!el.isUwaExtended) {
      Object.extend(el, UWA.Element);
      el.isUwaExtended = true;
    }
    return $(el);
  }
};

_ = function(msg) {
  return msg;
};

UWA.Widget = function() {};
UWA.Widget.prototype = {

  setIcon: function(icon) {
    var iconEl = this.elements['icon'];
    if (iconEl) {
      iconEl.innerHTML = '<img width="16" height="16" src="' + icon + '" />';
    }
  },

  getIcon: function() {
    return this._icon;
  },

  setTitle: function(title) {
    this.title = title;
    var titleEl = this.elements['title'];
    if (titleEl) {
      titleEl.innerHTML = title;
      this.callback('onUpdateTitle');
    }
  },

  getTitle: function() {
    return this.title;
  },

  setBody: function(content) {
    var bodyEl = this.elements['body'];
    if (bodyEl) {
      if (typeof content == "string") {
        bodyEl.innerHTML = content;
      } else if (typeof content == "object" || typeof content == "function") {
        bodyEl.innerHTML = '';
        bodyEl.appendChild(content);
      }
      this.callback('onUpdateBody');
    }
  },

  addBody: function(content) {
    var bodyEl = this.elements['body'];
    if (bodyEl) {
      if (typeof content == "string") {
        bodyEl.innerHTML = bodyEl.innerHTML + content;
      } else if (typeof content == "object") {
        bodyEl.appendChild(content);
      }
      this.callback('onUpdateBody');
    }
  },

  setCallback: function(name, fn) {
    this.callbacks[name] = fn;
  },

  callback: function(name, args, bind) {
    if (typeof bind == 'undefined') {
      bind = this;
    }
    try {
      if (this.callbacks[name]) {
        this.callbacks[name].apply(bind, [args]);
      }
    } catch(e) {
      this.log(e);
    }
  },

  createElement: function(tagName) {
    var el = document.createElement(tagName);
    return UWA.$element(el);
  },

  openURL: function(url) {
    window.open(url);
  },

  setAutoRefresh: function(delay) {
  },

  setSearchResultCount: function(number) {
  },

  setUnreadCount: function(number) {
  },

  log: function(msg) {
    UWA.log(msg);
  },

  getValue: function(name) {
    return this.data[name];
  },

  setValue: function(name, value) {
    this.data[name] = value;
  },

  onLoad: function() {
    this.log('widget.onLoad');
  },

  onRefresh: function() {
    this.log('widget.onRefresh');
    this.onLoad();
  },

  onResize: function() {
    this.log('widget.onResize');
  },

  onSearch: function() {
    this.log('widget.onSearch');
  },

  onResetSearch: function() {
    this.log('widget.onResetSearch');
  },

  onKeyboardAction: function() {
    this.log('widget.onKeyboardAction');
  }

};


UWA.proxies = {};

UWA.Data = {

  getText: function(url, callback) {
    alert('UWA.Data.getText not implemented');
  },

  getXml: function(url, callback) {
    alert('UWA.Data.getXml not implemented');
  },

  getFeed: function(url, callback) {
    alert('UWA.Data.getFeed not implemented');
  },

  getJson: function(url, callback) {
    alert('UWA.Data.getJson not implemented');
  },

  request: function(url, request) {
    alert('UWA.Data.request not implemented');
  }

};
