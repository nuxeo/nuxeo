/*

 NXThemes UI library

 Author: Jean-Marc Orliaguet <jmo@chalmers.se>

*/

var NXThemes = {
  Version: "0.9",

  Controllers: new Hash(),
  Effects: new Hash(),
  Storages: new Hash(),
  Widgets: new Hash(),
  Filters: new Hash(),

  _subscribers: new Hash(),
  _models: new Hash(),
  _views: new Hash(),
  _controllers: new Hash(),
  _action_handlers: new Hash(),

  _initialized: new Hash(),
  _defs: new Hash(),
  
  init: function() {
    NXThemes.parse(document);
    NXThemes.getViews().invoke('init');
  },

  getModelById: function(id) {
    return this._models.get(id);
  },

  getControllerById: function(id) {
    return this._controllers.get(id);
  },

  getViewById: function(id) {
    var view = this._views.get(id);
    if (!view) {
      view = new NXThemes.DelayedView(id);
      this._views.set(id, view);
    }
    return view;
  },

  getViews: function() {
    return this._views.pluck('value');
  },

  registerControllers: function(controllers) {
    $H(controllers).each(function(c) {
      NXThemes.Controllers.set(c.key, c.value);
    });
  },

  registerEffects: function(effects) {
    $H(effects).each(function(c) {
      NXThemes.Effects.set(c.key, c.value);
    });
  },

  registerStorages: function(storages) {
    $H(storages).each(function(c) {
      NXThemes.Storages.set(c.key, c.value);
    });
  },

  registerWidgets: function(widgets) {
    $H(widgets).each(function(c) {
      NXThemes.Widgets.set(c.key, c.value);
    });
  },

  registerFilters: function(filters) {
    $H(filters).each(function(c) {
      NXThemes.Filters.set(c.key, c.value);
    });
  },

  getFilterById: function(id) {
    return this.Filters.get(id);
  },

  /* Session management */

  setCookie: function(id, value) {
    document.cookie = id + '=' + escape(value) + '; path=/';
  },

  getCookie: function(id) {
    var start, end;
    if (document.cookie.length > 0) {
      start = document.cookie.indexOf(id + "=");
      if (start != -1) {
        start = start + id.length +1;
        end = document.cookie.indexOf(";", start);
        if (end == -1) {
          end = document.cookie.length;
        }
        return unescape(document.cookie.substring(start, end));
      }
    }
    return "";
  },

  expireCookie: function(id) {
    var now = new Date();
    now.setTime(now.getTime() -1);
    document.cookie = id += "=; expires=" + now.toGMTString() + '; path=/';
  },

  getSessionData: function(id) {
    var results = document.cookie.match(id + '=(.*?)(;|$)');
    if (results) {
      var value = unescape(results[1]);
      return value.evalJSON(true);
    }
    return null;
  },

  setSessionData: function(id, data) {
    this.setCookie(id, Object.toJSON(data));
  },

  /* Error handling */
  warn: function(msg, context) {
    msg = "[NXThemes] " + msg;
    if (typeof console.log !== undefined) {
      console.log(msg);
    } else {
      window.alert(msg);
    }
  },

  /* Comparison */

  compare: function(a, b) {
    if (a === undefined || b === undefined) {
      return true;
    }
    if (typeof a == 'object' && typeof b == 'object') {
      if (a.hash !== undefined && b.hash !== undefined) {
        return a.hash() == b.hash();
      }
    }
    return (a == b);
  },

  /* Action handlers */
  addActions: function(actions) {
    $H(actions).each(function(c) {
      NXThemes._action_handlers.set(c.key, c.value);
    });
  },

  getAction: function(action_id) {
    return this._action_handlers.get(action_id);
  },

  /* Event system */
  subscribe: function(eventid, event) {
    if (this._subscribers.get(eventid) === undefined) {
      this._subscribers.set(eventid, []);
    }
    if (this._subscribers.get(eventid).findAll(function(e) {
      if (event === undefined) {
        return true;
      }
      if (event.scope !== undefined) {
        if (event.scope != e.scope) {
          return false;
        }
      }
      return (NXThemes.compare(event.subscriber, e.subscriber) &&
              NXThemes.compare(event.publisher, e.publisher));
    }).length === 0) {
      this._subscribers.get(eventid).push(event);
    }
  },

  unsubscribe: function(eventid, event) {
    var subscribers = this._subscribers;
    if (subscribers.get(eventid) === undefined) { return; }
    var new_subscribers = subscribers.get(eventid).reject(function(e) {
      if (event === undefined) {
        return true;
      }
      if (event.scope !== undefined) {
        if (event.scope != e.scope) {
          return false;
        }
      }
      return (NXThemes.compare(event.subscriber, e.subscriber) &&
              NXThemes.compare(event.publisher, e.publisher));
    });
    subscribers.set(eventid, new_subscribers);
    if (subscribers.get(eventid).size() === 0) {
      subscribers.unset(eventid);
    }
  },

  notify: function(eventid, event) {
    var subscribers = this._subscribers;
    var publisher = event.publisher;
    (subscribers.get(eventid) || []).findAll(function(e) {
      if (event === undefined) {
        return true;
      }
      if ((event.scope !== undefined) && (event.scope !== e.scope)) {
        return false;
      }
      return (NXThemes.compare(event.subscriber, e.subscriber) &&
              NXThemes.compare(event.publisher, e.publisher));
    }).each(function(e) {
      var handler = NXThemes.getEventHandler(eventid, e.subscriber);
      if (handler) {
        // set the publisher in case no publisher is specified.
        event.subscriber = e.subscriber;
        event.publisher = publisher;
        handler(event);
      }
    });
  },

  registerEventHandler: function(eventid, subscriber, handler) {
    var handlers = subscriber._handlers;
    if (handlers === undefined) {
      subscriber._handlers = new Hash();
    }
    subscriber._handlers.set(eventid, handler);
  },

  getEventHandler: function(eventid, subscriber) {
    return (subscriber._handlers || new Hash()).get(eventid);
  },
  
  /* Document parsing */

  _hasClassName: function(element, className) {
    var elementClassName = element.className;
    if (!elementClassName || elementClassName.length === 0) {
      return false;
    }
    if (elementClassName == className ||
        elementClassName.match(new RegExp("(^|\\s)" + className + "(\\s|$)"))) {
      return true;
    }
    return false;
  },

  _thisOrParentHasClassName: function(element, className) {
    var el = element;
    var hasClassName = NXThemes._hasClassName;
    while (el) {
      if (hasClassName(el, className)) {
        return true;
      }
      el = el.parentNode;
    }
    return false;
  },

  _jsonParse: function(el, text) {
    var res = {};
    try {
      res = text.unescapeHTML().evalJSON(true);
    } catch(e) {
      var msg = el.id + ": " + e.message + " (" + text + ")";
      NXThemes.warn(msg, el);
    }
    return res;
  },

  // first stage
  parse: function(node) {
    var elements = $A(node.getElementsByTagName("ins")).select(function(e) {
      return (e.className.match(new RegExp("model|view|controller")));
    });
    var length = elements.length;
    if (!length) {
      return;
    }
    var progress = {initialized: 0};

    this.registerEventHandler("initialized", progress, function(event) {
      var progress = event.scope;
      progress.initialized += 1;
      if (progress.initialized >= length) {
        NXThemes._load(node);
        NXThemes.unsubscribe("initialized", {scope: progress});
        NXThemes.notify("parsed", {publisher: node});
      }
    });

    NXThemes.subscribe("initialized",
      {subscriber: progress, scope: progress}
    );

    elements.each(function(el) {
      var url = el.getAttribute("cite");
      if (url) {
        var options = {
          onComplete: function(req) {
            NXThemes._eval(el, req.responseText);
            NXThemes.notify('initialized', {publisher: el, scope: progress});
          }
        };
        var parts = url.split('?');
        if (parts.length == 2) {
          url = parts[0];
          options.parameters = parts[1];
        }
        new Ajax.Request(url, options);
      } else {
        /* the definition is written inline */
        NXThemes._eval(el, el.innerHTML);
        el.innerHTML = "";
        NXThemes.notify('initialized', {publisher: el, scope: progress});
      }
    });
  },

  _eval: function(el, text) {
    var def = this._jsonParse(el, text);
    var id = def.id;
    if (!id) {
      NXThemes.warn("Component has no id: <pre>" + text + "</pre>", el);
    }
    el.componentid = id;
    this._defs.set([el.className, id], def);
  },

  // second stage
  _load: function(node) {
    var elements = $A(node.getElementsByTagName("ins"));
    var register = NXThemes._register;
    var hasClassName = NXThemes._hasClassName;
    ["controller", "view", "model"].each(function(type) {
      elements.each(function(el) {
        if (hasClassName(el, type)) {
          register(node, el, type);
        }
      });
    });
  },

  _register: function(node, el, classid) {
      var def = NXThemes._defs.get([classid, el.componentid]);
      var id = def.id;
      var factory;

      switch(classid) {

        case "controller":
          var controller_type = def.type;
          factory = NXThemes.Controllers.get(controller_type);
          if (!factory) {
            NXThemes.warn("No such controller type: " + controller_type);
            break;
          } else {
            var controller = factory(node, def);
            NXThemes._controllers.set(id, controller);
          }
          NXThemes.notify("registered controller",
            {publisher: controller, scope: id}
          );
          break;

        case "model":
          var model = new NXThemes.Model(node, def);
          NXThemes._models.set(id, model);
          NXThemes.notify("registered model",
            {publisher: model, scope: id}
          );
          break;

        case "view":
          var widget_type = def.widget.type;
          if (!widget_type) {
            NXThemes.warn("Must specify a widget type for " + classid + " id: " + id, el);
            break;
          } else if (NXThemes.Widgets.get(widget_type) === undefined) {
            NXThemes.warn("Unknown widget type '" + widget_type + "' for " + classid + " id: " + id, el);
            break;
          }

          var factory = NXThemes.Widgets.get(widget_type);
          if (factory === null) {
            NXThemes.warn("No such widget type: " + widget_type, el);
          } else {
            var view = factory(def);
            NXThemes._views.set(id, view);
          }

          /* create the view */
          if (view) {
            /* register the observed model */
            var model_id = def.model;
            if (model_id) {
              NXThemes.registerEventHandler("registered model", view,
              function(event) {
                var model = event.publisher;
                var view = event.subscriber;
                view.observe(model);
                NXThemes.unsubscribe("registered model",
                  {scope: model.hash()}
                );
              });
              NXThemes.subscribe("registered model",
                {subscriber: view, scope: model_id}
              );
            }

            /* insert the widget into the DOM */
            var replace = def.widget.replace;
            if (replace) {
              var replaced = $(replace);
              if (replaced) {
                replaced.parentNode.replaceChild(view.widget, replaced);
              } else {
                NXThemes.warn("Unknown node id: " + replace, el);
              }
            } else if (!def.widget.area) {
              el.parentNode.insertBefore(view.widget, el);
            }
            NXThemes.notify("registered view", {publisher: view, scope: id});
            view.load();
            view.resetControllers();
          }
          break;

      }
  }

};

Event.observe(window, "load", NXThemes.init);

NXThemes.Set = Class.create();
NXThemes.Set.prototype = {

  initialize: function(x) {
    this._elements = new Hash();
    if (typeof x == 'string') {
      x = [x];
    }
    $A(x).each(function(e) {
      this.add(e);
    }.bind(this));
  },

  add: function(x) {
    if (typeof x == 'string') {
      x = [x];
    }
    $A(x).each(function(e) {
      this._elements.set(e, true);
    }.bind(this));
  },

  remove: function(x) {
    if (typeof x == 'string') {
      x = [x];
    }
    $A(x).each(function(e) {
      this._elements.unset(e);
    }.bind(this));
  },

  contains: function(x) {
    return this.entries().indexOf(x) > -1;
  },

  entries: function() {
    return this._elements.keys();
  },

  _each: function(iterator) {
    this.entries()._each(iterator);
  },

  each: Enumerable.each
};

// Controller

NXThemes.Controller = Class.create();
NXThemes.Controller.prototype = {

  initialize: function(node, def) {
    this.node = node;
    this.def = def;
    this.views = new NXThemes.Set();
    this.registered = new NXThemes.Set();
    this.setup();
  },

  hash: function() {
    return this.def.id;
  },

  getSessionId: function() {
    return 'nxthemes_controller_' + this.hash();
  },

  refreshViews: function() {
    this.views.entries().each(function(v) {
      NXThemes.getViewById(v).refresh();
    });
  },

  setup: function() {
    /* to override */
  },

  register: function(view) {
    /* to override */
  },

  unregister: function(view) {
    /* to override */
  },

  update: function(view) {
    /* to override */
  }
};

// Identifiable DOM elements.
if (!NXThemes.Identifiable) {
  NXThemes.Identifiable = new Object();
}
Object.extend(NXThemes.Identifiable, {

  isIdentifiable: function(node) {
    node = $(node);

    if (node.nodeType != 1) {
      return false;
    }

    if (node.tagName.toLowerCase() == "ins") {
      return false;
    }

    return true;
  },

  getIdentifiable: function(node) {
    node = $(node);
    if (this.isIdentifiable(node)) { return node; }
    return this.getParent(node);
  },

  getParent: function(node) {
    while (node) {
      node = node.parentNode;
      if (!node) {
        return null;
      }
      if (this.isIdentifiable(node)) { return node; }
    }
    return null;
  }

});

if (!NXThemes.Canvas) {
  NXThemes.Canvas = {
    _styles: new Hash(),
    _scripts: new Hash() 
  };
}

Object.extend(NXThemes.Canvas, {

  getModel: function(node) {
    if (!node) {
      return null;
    }
    var model_node = $(node).down("ins.model");
    if (model_node === null) {
      return null;
    }
    var first_node = $(node).down();
    if (first_node === null) {
      return null;
    }
    if (model_node == first_node) {
      var id = model_node.componentid;
      if (id) {
        return NXThemes.getModelById(id);
      }
    }
    return null;
  },

  createNode: function(options) {
    var node = $(document.createElement(options.tag));
    $A(options.classes).each(function(c) {
      node.addClassName(c);
    });
    node.setStyle(options.style);
    $H(options.attributes).each(function(attr) {
      node.setAttribute(attr.key, attr.value);
    });
    if (options.text) {
      node.appendChild(document.createTextNode(options.text));
    }
    var parent = options.parent;
    if (typeof parent != "undefined") {
      parent.appendChild(node);
    }
    return node;
  },

  getFirstParentNodeWithAnId: function(node) {
    while (node) {
      node = node.parentNode;
      if (!node) {
        return null;
      }
      if (node.getAttribute("id")) {
        return node;
      }
    }
    return null;
  },

  addStyleSheet: function(id, src) {
    if (this._styles.get(id) !== undefined) {
      return;
    }
    var head = document.getElementsByTagName("head")[0];
    var link = document.createElement("link");
    link.id = "nxthemes-style-" + id;
    link.rel = "stylesheet";
    link.href = src;
    link.type = "text/css";
    head.appendChild(link);
    this._styles.set(id, src);
  },

  updateStyleSheet: function(id, src) {
	if (this._styles.get(id) !== undefined) {
	  return;
	}
    var style = document.getElementById("nxthemes-style-" + id);
    if (style !== null) {
      style.href = src;
    }
  },
  
  getStyleSheetUrl: function(id) {
	var style = document.getElementById("nxthemes-style-" + id);
    if (style !== null) {
	  return style.href;
    }
  },

  removeStyleSheet: function(id) {
    if (this._styles.get(id) !== undefined) {
      this._styles.unset(id);
    }
    var style = document.getElementById("nxthemes-style-" + id);
    if (style) {
      $(style).remove();
    }
  },

  addScript: function(id, src) {
    if (this._scripts.get(id) !== undefined) {
      return;
    }
    var head = document.getElementsByTagName("head")[0];
    var script = document.createElement("script");
    script.id = "nxthemes-script-" + id;
    script.src = src;
    script.type = "text/javascript";
    head.appendChild(script);
    this._scripts.set(id, src);
  },

  removeScript: function(id) {
    if (this._scripts.get(id) !== undefined) {
      this._scripts.unset(id);
    }
    var script = document.getElementById("nxthemes-script-" + id);
    if (script) {
      $(script).remove();
    }
  }

});

if (!window.Element) {
  var Element = new Object();
}

if (!Element.Methods) {
  Element.Methods = new Object();
}

Element.addMethods({

  within: function(element, x, y) {
    this.xcomp = x;
    this.ycomp = y;
    this.offset = Element.cumulativeOffset(element);
    return (y >= this.offset[1] &&
            y <  this.offset[1] + element.offsetHeight &&
            x >= this.offset[0] &&
            x <  this.offset[0] + element.offsetWidth);
  },
  
  setBackgroundColor: function(element, options) {
    var r = parseInt(options.r * 255);
    var g = parseInt(options.g * 255);
    var b = parseInt(options.b * 255);
    element.style.backgroundColor = 'rgb(' + r + ',' + g + ',' + b + ')';
  },

  getBackgroundColor: function(element) {
    var regExp = new RegExp("^rgb\\((\\d+),(\\d+),(\\d+)\\)$");
    var bgColor = element.getStyle('backgroundColor');
    if (bgColor == "transparent") {
      return null;
    }
    var match = regExp.exec(bgColor.replace(/\s+/g,''));
    if (!match) {
      return {r: 1, g: 1, b: 1};
    }
    return {r: match[1]/255, g: match[2]/255, b: match[3]/255};
  },

  moveTo: function(element, options) {
    var x = options.x;
    var y = options.y;
    if (options.duration) {
      var pos = Element.cumulativeOffset(element);
      var x0 = pos[0];
      var y0 = pos[1];
      new NXThemes.Scheduler(Object.extend(options, {
        action: function(value) {
          element.style.left = x0 + (x - x0) * value + 'px';
          element.style.top = y0 + (y - y0) * value + 'px';
        },
        onComplete: (options.onComplete || function() {}).bind(element)
      }));
    }
    if (options.fit) {
      var dimensions = element.getDimensions();
      var width = dimensions.width;
      var height = dimensions.height;
      var page_w = window.innerWidth || document.body.clientWidth;
      var page_h = window.innerHeight || document.body.clientHeight;

      var offset = Element.cumulativeScrollOffset(element);
      var offsetX = offset[0];
      var offsetY = offset[1];

      page_w = page_w + offsetX -16;
      page_h = page_h + offsetY -16;

      if (x + width > page_w) {
        x = page_w - width;
      }
      if (x < offsetX) {
        x = offsetX;
      }

      if (y + height > page_h) {
        y = page_h - height;
      }
      if (y < offsetY) {
        y = offsetY;
      }
    }
    element.style.left = x + 'px';
    element.style.top = y + 'px';
  }

});


NXThemes.Scheduler = Class.create();
NXThemes.Scheduler.prototype = {

  initialize: function(options) {
    this.delay = options.delay || 0;
    this.duration = options.duration || 300;
    this.action = options.action || function(value) {};
    this.onComplete = options.onComplete || function() {};
    this.precision = options.precision || 25;

    this.started = false;
    this.start();
  },

  start: function() {
    this.startTime = (new Date).getTime();
    this.timer = setInterval(this.step.bind(this), this.precision);
   },

  step: function() {
    var pos = ((new Date).getTime() - this.startTime - this.delay) / this.duration;
    if (pos < 0) {
      return;
    }
    if (pos > 1) {
      this.stop();
    } else {
      this.action((1-Math.cos(pos*Math.PI))/2);
      this.started = true;
    }
  },

  stop: function() {
    clearInterval(this.timer);
    this.onComplete();
  }

};

// Model

NXThemes.Model = Class.create();
NXThemes.Model.prototype = {

  initialize: function(node, def) {
    this.node = node;
    this.def = def;
    // set the schema
    this.schema = this._setSchema();
    // set the storage adapter
    this.storage = this._setStorageAdapter();
  },

  hash: function() {
    return this.def.id;
  },
  
  getType: function() {
    return this.def.type;
  },

  getSessionId: function() {
    return 'nxthemes_model_' + this.hash();
  },

  // low-level I/O
  readData: function() {
    return this._data || new Hash(this.def.data);
  },

  writeData: function(data) {
    this._data = new Hash(data);
  },

  // high-level I/O
  getData: function() {
    this.storage.readTransaction(); /* asynchronous call */
    return this.readData();
  },

  setData: function(data) {
    this.storage.writeTransaction(data);
  },

  updateData: function(data) {
    var current_data = this.storage.read() || new Object();
    var new_data = $H(current_data).merge(data);
    this.setData(new_data);
  },

  addObserver: function(view) {
    var model = this;
    // observers subscribes to events on the model
    NXThemes.registerEventHandler('changed', view, function(event) {
      var view = event.subscriber;
      view.refresh();
    });
    NXThemes.subscribe('changed', {subscriber: view, publisher: model});
  },

  removeObserver: function(view) {
    var model = this;
    NXThemes.unsubscribe('changed', {subscriber: view, publisher: model});
  },

  /* Private API */
  _setSchema: function() {
    var initial_data = this.def.data;
    var schema = new Hash();
    $H(initial_data).each(function(f) {
      var field = f.key;
      var value = f.value;
      schema.set(field, typeof value);
    });
    return schema;
  },

  _setStorageAdapter: function() {
    var storage_def = this.def.storage;
    if (!storage_def) {
      storage_def = {type: 'ram'};
      this.def.storage = storage_def;
    }
    var model = this;
    var storage = NXThemes.Storages.get(storage_def.type)(this);

    // the model reacts to events on the storage and notifies observers
    NXThemes.registerEventHandler('stored', model, function(event) {
      var model = event.subscriber;
      NXThemes.notify('changed', {publisher: model});
    });

    NXThemes.subscribe('stored', {subscriber: model, publisher: storage});

    var refresh = storage_def.refresh;
    if (refresh && refresh > 0) {
      new PeriodicalExecuter(function() {storage.requestData();}, refresh);
    }

    return storage;
  }

};

// Storage adapter base class
NXThemes.StorageAdapter = Class.create();
NXThemes.StorageAdapter.prototype = {

  initialize: function(model) {
    this.model = model;
    this._queue = [];
    this._queued_data = new Hash();
    this.setup();
  },

  readTransaction: function(data) {
    // TODO: implement read access sequences
    this.requestData();
  },

  writeTransaction: function(data) {
    var access = this.model.def.storage.access;
    if (access) {
      var size = access.size;
      switch (access.type) {
        case 'queue':
          if (this._queue.length < size || size === null) {
            this._queue.push(data.get(access.signature));
          }
          break;

        case 'stack':
          this._queue.unshift(data.get(access.signature));
          if (size && size > 0) {
            this._queue = this._queue.slice(0, size);
          }
          break;

      }
    }
    this.storeData(data);
  },

  /* Public API */
  setup: function() {
    /* to be overridden */
  },

  requestData: function() {
    /* to be overridden */
  },

  storeData: function(data) {
    /* to be overridden */
  },

  // low-level I/O
  read: function() {
    // TODO implement a policy for reading data
    return this.model.readData();
  },

  write: function(data) {
    var access = this.model.def.storage.access;
    var stored;

    if (access && access.type) {
      var signature = data.get(access.signature);
      this._queued_data.set(signature, data);
      while (this._queue) {
        var next = this._queue[0];
        if (this._queued_data.get(next) !== undefined) {
          data = this._queued_data.get(next);
          stored = this._storeFields(data);
          this._queue.shift();
        } else {
          break;
        }
      }
    } else {
      stored = this._storeFields(data);
    }
    return stored;
  },

  _storeFields: function(data) {
    // filter out fields with the wrong data type
    var schema = this.model.schema;
    var new_data = new Hash();
    var current_data = this.read();
    schema.each(function(f) {
      var field = f.key;
      var value = data.get(field);
      if (typeof value == f.value) {
        new_data.set(field, value);
      } else {
        new_data.set(field, current_data.get(field));
      }
    });
    if (this.model._data === undefined ||
       !this._compareData(current_data, new_data)) {
      this.model.writeData(new_data);
      NXThemes.notify('stored', {publisher: this});
    }
    return new_data;
  },

  _compareData: function(a, b) {
    return Object.toJSON(a) == Object.toJSON(b);
  },

  merge: function(data) {
    var current_data = this.read();
    var new_data = current_data.update(data);
    this.write(new_data);
  }

};

/* default RAM storage */

NXThemes.RAMStorage = Class.create();
NXThemes.RAMStorage.prototype = Object.extend(
  new NXThemes.StorageAdapter(), {

  requestData: function() {
    /* nothing to do since the data is already there */
    this.write(this.read());
  },

  storeData: function(data) {
    /* Store the data directly */
    this.write(data);
  }

});

NXThemes.registerStorages({
  ram: function(model) {
    return new NXThemes.RAMStorage(model);
  }
});

// View

NXThemes.View = function() {};
NXThemes.View.prototype = {

  initialize: function(widget, def) {
    this.def = def;
    this.widget = widget;
    this.widget.view_id = this.hash();
    this.subviews = def.subviews || new Array();
    this._visible = false;
    this._displayed = true;

    this.setup();
  },

  hash: function() {
    return this.def.id;
  },

  getSessionId: function() {
    return 'nxthemes_view_' + this.hash();
  },

  /* Public API */
  inspect: function() {
    return "[NXThemes " + this.def.widget + "]";
  },

  setup: function() {
    /* to override: setup the view */
  },

  init: function() {
    /* to override: initialize the view */
  },

  render: function(data) {
    /* to override: render the view from the data */
  },

  select: function() {
    /* to override: select the view */
  },

  deselect: function() {
    /* to override: deselect the view */
  },

  prepare: function() {
    /* to override: prepare the widget before showing it */
  },

  teardown: function() {
    /* to override: tear down the widget after hiding it */
  },

  /* Private API */
  getControllers: function() {
    return this.def.controllers || [];
  },

  resetControllers: function() {
    var view = this;
    var view_id = view.hash();
    var controllers_ids = view.getControllers();
    controllers_ids.each(function(c) {
      var controller = NXThemes.getControllerById(c);
      if (controller) {
        controller.views.add(view_id);
        controller.unregister(view);
        controller.register(view);
        controller.update(view);
      } else {
        NXThemes.warn("No such controller id: " + c);
      }
    });
  },

  observe: function(model) {
    model.addObserver(this);
    this.model = model;
  },

  stopObserving: function() {
    if (this.model) {
      this.model.removeObserver(this);
    }
  },

  getData: function() {
    if (this.model) {
      return this.model.getData();
    }
  },

  readData: function() {
    if (this.model) {
      return this.model.readData();
    }
  },

  refresh: function() {
    if (!this._visible) {
      return;
    }
    var data = this.readData();
    if (data) {
      this.display(data);
    }
    // refresh the sub-views
    this.subviews.entries().each(function(v) {
      NXThemes.getViewById(v).refresh();
    });
  },

  load: function() {
    this.init();
    this.subviews.entries().each(function(v) {
      NXThemes.getViewById(v).load();
    });
  },

  reload: function() {
    var data = this.getData();
    if (data) {
      this.display(data);
      this.resetControllers();
    }
  },

  display: function(data) {
    this.render(data);
    if (this.def.render_effect) {
      $(this.widget).hide();
      this.applyEffect(this.def.render_effect);
    }
  },

  focus: function() {
    NXThemes.notify("gained focus", {publisher: this, context: this.selected});
  },

  defocus: function() {
    NXThemes.notify("lost focus", {publisher: this});
  },

  show: function() {
    if (this._visible || !this._displayed) {
      return;
    }
    
    var widget = $(this.widget);

    this._visible = true;

    // refresh the view
    this.refresh();

    // prepare the view
    this.prepare();

    if (this.def.show_effect) {
      this.applyEffect(this.def.show_effect);
    } else {
      widget.show();
    }
  },

  hide: function() {
    if (!this._visible) {
      return;
    }
    var widget = $(this.widget);

    if (this.def.hide_effect) {
      this.applyEffect(this.def.hide_effect);
    } else {
      widget.hide();
    }
    // tear down the view;
    this.teardown();
    this._visible = false;
  },

  applyEffect: function(options) {
    if (this.effect) {
      this.effect.stop();
    }
    var widget = this.widget;
    if (NXThemes.Effects.get(options.transition) !== undefined) {
      if (widget.style.display == "none") {
        widget.style.display = "";
      }
      this.effect = NXThemes.Effects.get(options.transition)(widget, options);
    }
  },
  
  ready: function() {
    var view = this;
    NXThemes.notify("view ready", {publisher: view, scope: view.hash()});
  }

};


NXThemes.DelayedView = Class.create();
NXThemes.DelayedView.prototype = {

  initialize: function(id) {
    this.id = id;
  },
  
  switchTo: function(perspective) {
    var delayed_view = this;
    NXThemes.registerEventHandler("view ready", delayed_view, function(event) {
      var real_view = event.publisher; 
      real_view.switchTo(perspective);
      NXThemes.unsubscribe("view ready", {subscriber: delayed_view, publisher: real_view});
    });
    NXThemes.subscribe("view ready", {subscriber: delayed_view, scope: delayed_view.id});
  },
  
  refresh: function() {
    var delayed_view = this;
    NXThemes.registerEventHandler("view ready", delayed_view, function(event) {
      var real_view = event.publisher; 
      real_view.refresh();    
      NXThemes.unsubscribe("view ready", {subscriber: delayed_view, publisher: real_view});
    });
    NXThemes.subscribe("view ready", {subscriber: delayed_view, scope: delayed_view.id});
  },
  
  load: function() {
    var delayed_view = this;
    NXThemes.registerEventHandler("view ready", delayed_view, function(event) {
      var real_view = event.publisher; 
      real_view.load();
      NXThemes.unsubscribe("view ready", {subscriber: delayed_view, publisher: real_view});
    });
    NXThemes.subscribe("view ready", {subscriber: delayed_view, scope: delayed_view.id});
  }

};
