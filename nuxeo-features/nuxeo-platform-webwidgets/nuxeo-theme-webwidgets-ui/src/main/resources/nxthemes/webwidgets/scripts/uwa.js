
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

UWA.proxies = {
};

NV_PATH=nxContextPath;
NV_MODULES=nxContextPath;

UWA.Data = {

  AJAX_PROXY_URL:  nxContextPath + '/ajaxProxy/',

  needProxy: function(url) {
      if (url.indexOf("http")==0) {
          return true;
      }
      return false;
  },

  getText: function(url, callback) {
    var onComplete = function(req) {
      if (typeof callback == "function") {
        callback(req.responseText);
      }
    };

    if (this.needProxy(url)) {
        this.request(this.AJAX_PROXY_URL,
          {parameters: "?url=" + encodeURIComponent(url), onComplete: onComplete});
    }
    else {
        this.request(url, {onComplete: onComplete});
    }
  },

  getXml: function(url, callback) {
    var onComplete = function(req) {
      if (typeof callback == "function") {
        var xml = req.responseText;
        var xotree = new XML.ObjTree();
        var tree = xotree.parseXML(xml);
        callback(tree);
      }
    };
    if (this.needProxy(url)) {
        this.request(this.AJAX_PROXY_URL,
          {parameters: "?url=" + encodeURIComponent(url), onComplete: onComplete});
    }
    else {
        this.request(url, {onComplete: onComplete});
    }
  },

  getFeed: function(url, callback) {
    var onComplete = function(req) {
      if (typeof callback == "function") {
       var xml = req.responseText;
       var feed = new FeedParser(xml);
       feed.parse();
       callback(feed);
      }
    };
    if (this.needProxy(url)) {
        this.request(this.AJAX_PROXY_URL,
          {parameters: "?url=" + encodeURIComponent(url), onComplete: onComplete});
    }
    else {
        this.request(url, {onComplete: onComplete});
    }
  },

  getJson: function(url, callback) {
    var onComplete = function(req) {
      try {
        eval("var j = " + req.responseText);
        if (typeof callback == "function") {
          callback(j);
        }
      } catch(e) {
        UWA.log(e);
      }
    }
    if (this.needProxy(url)) {
        this.request(this.AJAX_PROXY_URL,
          {parameters: "?url=" + encodeURIComponent(url), onComplete: onComplete});
    }
    else {
        this.request(url, {onComplete: onComplete});
    }
  },

  request: function(url, request) {
    var method = request.method || 'GET';
    var parameters = request.parameters;
    var cache = request.cache;
    if (cache) {
      if (parameters) {
        parameters += '&cache=' + encodeURIComponent(cache);
      } else {
        parameters = '?cache=' + encodeURIComponent(cache);
      }
    }
    new Ajax.Request(url, {
      parameters: parameters,
      method: method.toLowerCase(),
      onComplete: request.onComplete || function(req) {}
    });
  }
};
