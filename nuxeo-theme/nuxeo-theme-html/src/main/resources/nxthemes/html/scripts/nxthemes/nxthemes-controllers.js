 /*

 NXThemes UI library - controllers

 Author: Jean-Marc Orliaguet <jmo@chalmers.se>

*/


NXThemes.registerControllers({

  'command': function(node, def) {
    return new NXThemes.CommandController(node, def);
  },

  'behaviour': function(node, def) {
    return new NXThemes.BehaviourController(node, def);
  },

  'form': function(node, def) {
    return new NXThemes.FormController(node, def);
  },

  'remote scripting': function(node, def) {
    return new NXThemes.RemoteScriptingController(node, def);
  },

  'focus observer': function(node, def) {
    return new NXThemes.FocusObserver(node, def);
  },

  'drag-and-drop': function(node, def) {
    return new NXThemes.DragAndDropController(node, def);
  },

  'perspective controller': function(node, def) {
    return new NXThemes.PerspectiveController(node, def);
  },

  'node expander': function(node, def) {
    return new NXThemes.NodeExpander(node, def);
  },

  'widget selector': function(node, def) {
    return new NXThemes.WidgetSelector(node, def);
  }

});

NXThemes.CommandController = Class.create();
NXThemes.CommandController.prototype = Object.extend(new NXThemes.Controller(), {

  setup: function() {
    var controller = this;
    NXThemes.registerEventHandler("command", controller, function(event) {
      var view = event.publisher;
      var controller = event.subscriber;

      // add some contextual info
      event.view = view;
      event.model = view.model;
      event.controller = controller;

      var action = event.options.action;
      /* the event handler calls the controller's registered handler */
      if (action) {
        var action_handler = NXThemes.getAction(action);
        if (action_handler) {
        action_handler(event);
        }
      }
    });
  },

  register: function(view) {
    var controller = this;
    NXThemes.subscribe("command", {subscriber: controller, publisher: view});
  },

  unregister: function(view) {
    NXThemes.unsubscribe("command", {subscriber: this, publisher: view});
  }

});

NXThemes.BehaviourController = Class.create();
NXThemes.BehaviourController.prototype = Object.extend(new NXThemes.Controller(), {

  setup: function() {
    this.handlers = new Hash();
  },

  register: function(view) {
    var controller = this;
    var rules = this.def.rules;
    var model = view.model;
    var widget = view.widget;
    var info = {model: model, view: view, controller: controller};

    var handlers = this.handlers;
    $H(rules).each(function(r) {
      var selector = Selector.findChildElements(widget, [r.key]);
      if (selector.length > 0) {
        $H(r.value).each(function(s) {
          var event_name = s.key;
          var action_id = s.value;
          var handler = NXThemes.getAction(action_id);
          if (handler === null) {
            NXThemes.warn(controller.def.id +
              " controller: no handler '" + action_id + "' found for event: " + r.key + "." + event_name, view.widget);
          } else {
            handlers.set(action_id, handler.bindAsEventListener(info));
            selector.each(function(el) {
                Event.observe(el, event_name, handler);
            });
          }
        });
      }
    });
  },

  unregister: function(view) {
    var rules = this.def.rules;
    var widget = view.widget;

    var handlers = this.handlers;
    $H(rules).each(function(r) {
      var selector = Selector.findChildElements(widget, [r.key]);
      if (selector.length > 0) {
        $H(r.value).each(function(s) {
          var event_name = s.key;
          var action_id = s.value;
          var handler = handlers.get(action_id);
          if (handler !== null) {
            selector.each(function(el) {
              Event.stopObserving(el, event_name, handler);
            });
          }
        });
      }
    });
  }

});

NXThemes.FormController = Class.create();
NXThemes.FormController.prototype = Object.extend(new NXThemes.Controller(), {

  setup: function() {
    this.submitEvent = this.submitEvent.bindAsEventListener(this);
  },

  register: function(view) {
    Event.observe(view.widget, "submit", this.submitEvent);
  },

  unregister: function(view) {
    Event.stopObserving(view.widget, "submit", this.submitEvent);
  },

  submitEvent: function(e) {
    this.views.each(function(v) {
      var view = NXThemes.getViewById(v);
      var model = view.model;
      var widget = view.widget;

      var form_data = new Hash();
      $A(Form.getInputs(widget)).each(function(i) {
        form_data.set(i.name, Form.Element.getValue(i));
      });

      model.setData(form_data);
    });
    return false;
  }

});

NXThemes.RemoteScriptingController = Class.create();
NXThemes.RemoteScriptingController.prototype = Object.extend(
  new NXThemes.Controller(), {

  setup: function() {
    this.clickEvent = this.clickEvent.bindAsEventListener(this);
    this.submitEvent = this.submitEvent.bindAsEventListener(this);
  },

  register: function(view) {
    Event.observe(view.widget, "click", this.clickEvent);
    Event.observe(view.widget, "submit", this.submitEvent);
  },

  unregister: function(view) {
    Event.stopObserving(view.widget, "click", this.clickEvent);
    Event.stopObserving(view.widget, "submit", this.submitEvent);
  },

  submitEvent: function(e) {
    var target = Event.findElement(e, 'form');
    if (target && target != document) {
      var form = target;

      if (Form.getInputs(form, 'file').length > 0) {
        /* The form contains a file input, use an IFrame */
        var id = 'uploadIFrame';
        var onUploaded = "window.parent.NXThemes.getControllerById('" +
                         this.def.id + "').refreshViews()";
        var iframe = NXThemes.Canvas.createNode({
          tag: "iframe",
          style: {display: 'none'},
          attributes: {
            id: id,
            name: id,
            onload: onUploaded
            }
        });
        form.appendChild(iframe);
        form.target = id;
        return false;

      } else {
        /* AJAX request */
        var method = form.getAttribute('action');
        var params = Form.serialize(form);
        if (!method) {
          return;
        }
        var _request = this._request;
        var views = this.views;
        views.entries().each(function(v) {
          var view = NXThemes.getViewById(v);
          if (form.descendantOf(view.widget)) {
            _request(views, view, method, params);
          }
        });
      }

      Event.stop(e);
    }
  },

  clickEvent: function(e) {
    var target = $(Event.element(e));
    if (target.tagName.toLowerCase() != 'a') {
      target = Event.findElement(e, 'a');
    }
    if (target && target != document) {

      var href = target.href;
      var method = href;
      var params = {};

      if (href.match(/^javascript:/)) {
        eval(href);
      } else {
        var parts = href.split('?');

        if (parts.length == 2) {
          method = parts[0];
          params = parts[1];
        }
      }

      var _request = this._request;
      var views = this.views;
      views.entries().each(function(v) {
        var view = NXThemes.getViewById(v);
        if (target.descendantOf(view.widget)) {
          _request(views, view, method, params);
        }
      });
      Event.stop(e);
    }
  },

  _request: function(views, view, method, params) {
    var controller = this;
    var options = {
      onComplete: function(req) {
        var disp = req.getResponseHeader('content-disposition');
        if (disp && disp.match(/^attachment/)) {
          var url = method;
          if (params) {
            url += '?' + params;
          }
          window.location = url;
        }
        var content_type = req.getResponseHeader('content-type');
        if (content_type.match(/^text\/x-json/)) {
          var data = JSON.parse(req.responseText);
          view.model.updateData(data);
        }
        view.refresh();
      }
    };
    options.parameters = params;
    new Ajax.Request(method, options);
  }

});

NXThemes.FocusObserver = Class.create();
NXThemes.FocusObserver.prototype = Object.extend(new NXThemes.Controller(), {

  setup: function() {

    var controller = this;

    NXThemes.registerEventHandler("gained focus", controller, function(event) {
      var view = event.publisher;
      var selected = event.context;
      var model = NXThemes.Canvas.getModel(selected);
      // observe the model that received the focus
      if (model) {
        view.observe(model);
      }
    });

    NXThemes.registerEventHandler("lost focus", controller, function(event) {
      var view = event.publisher;
      view.stopObserving();
      // observe the original model
      var model_id = view.def.model;
      if (model_id) {
        var model = NXThemes.getModelById(model_id);
        if (model !== null) {
          view.observe(model);
        }
      }
    });

    NXThemes.subscribe("gained focus", {subscriber: controller});
    NXThemes.subscribe("lost focus", {subscriber: controller});
  }

});

NXThemes.alreadyDragging = false;

NXThemes.DragAndDropController = Class.create();
NXThemes.DragAndDropController.prototype = Object.extend(
  new NXThemes.Controller(), {

  setup: function() {
    this.moveEvent = this.moveEvent.bindAsEventListener(this);
    this.dropEvent = this.dropEvent.bindAsEventListener(this);

    this._last_updated = 0;
    this.positions = NXThemes.getSessionData(this.getSessionId()) || $H({});
  },

  register: function(view) {
    var widget = view.widget;
    widget.onselectstart = new Function("return false");

    if (this.def.dragging.savePosition) {
      var positions = this.positions;
      $H(positions).each(function(p) {
        var moved = $(p.key);
        if (moved !== null) {
          var position = p.value;
          $(moved).setStyle({left: position.x + 'px', top: position.y + 'px' });
        }
      });
    }

    this.dragEvent = this.dragEvent.bindAsEventListener(
                                    Object.extend(this, {widget: widget}));
    Event.observe(widget, "mousedown", this.dragEvent);
  },


  unregister: function(view) {
    var widget = view.widget;
    Event.stopObserving(widget, "mousedown", this.dragEvent);
  },

  _findDraggable: function(e) {
    var el = Event.element(e);
    var handle = this.def.dragging.handle;
    if (handle) {
      if (!NXThemes._thisOrParentHasClassName(el, handle)) {
         return null;
      }
    }

    var element = this.def.dragging.element || '';
    if (element === null) {
      return null;
    }
    while(el.parentNode) {
      if (NXThemes._hasClassName(el, element)) {
        return el;
      }
      el = el.parentNode;
    }
    return null;
  },

  _findNext: function(el) {
    var shiftablezones = this._shiftablezones;
    while (el) {
      el = el.nextSibling;
      if (!el) {
        return null;
      }
      if (el.nodeType == 1) {
        if (shiftablezones.indexOf(el) >= 0) {
          return el;
        }
      }
    }
    return null;
  },

  dragEvent: function(e) {
    if (NXThemes.alreadyDragging) return false;
    if (!Event.isLeftClick(e)) return false;
    var draggable = this._findDraggable(e);
    if (!draggable) {
      return false;
    }
    if (e.cancelable) e.preventDefault();

    // start dragging
    NXThemes.alreadyDragging = true;

    this.target = $(draggable);

    var shifting = this.def.shifting;
    if (shifting) {
      if (shifting.element) {
        this._shiftablezones = $$('.' + shifting.element);
      }
      if (shifting.container) {
        this.sourceContainer = $(draggable).up('.' + shifting.container);
        this._containerzones = $$('.' + shifting.container);
      }
    }

    if (this.def.dropping) {
      if (this.def.dropping.target) {
        this._dropzones = $$('.' + this.def.dropping.target);
      }
    }

    var pos = draggable.cumulativeOffset();
    this.x0 = pos[0];
    this.y0 = pos[1];

    this.startDragX = Event.pointerX(e);
    this.startDragY = Event.pointerY(e);

    Event.observe(document, "mousemove", this.moveEvent);
    Event.observe(document, "mouseup", this.dropEvent);
  },

  startDrag: function(x, y) {
    var dragging = this.def.dragging;
    var shifting = this.def.shifting;
    var draggable = this.target;

    if (dragging.offset_x || dragging.offset_y ) {
      this.x1 = dragging.offset_x || -5;
      this.y1 = dragging.offset_y || -5;
    } else {
      this.x1 = x - this.x0;
      this.y1 = y - this.y0;
    }

    if (dragging.feedback) {
      var feedback = NXThemes.Canvas.createNode({tag: 'div'});
      feedback.clonePosition(draggable);
      feedback.setStyle({
        textAlign: draggable.getStyle('textAlign'),
        zIndex: parseInt(draggable.getStyle('zIndex') || 0) +1
      });
      if (dragging.feedback.clone) {
        var clone = $(draggable.cloneNode(true));
        clone.setStyle({
          margin: '0'
        });
        feedback.appendChild(clone);
      } else {
        feedback.setStyle({
          borderColor: dragging.feedback.border || '#000',
          backgroundColor: dragging.feedback.background || '#fc3',
          borderStyle: 'solid',
          borderWidth: '1px'
        })
      }
      feedback.setOpacity(dragging.feedback.opacity);
      document.getElementsByTagName('body')[0].appendChild(feedback);
      this.moved = feedback;
    } else {
      if (!this.def.shifting) {
        this.moved = draggable;
      }
    }
    var dragged = this.dragged = draggable;

    var source = dragging.source;
    if (source) {
      var dim = dragged.getDimensions();
      var color = source.color || "#ccc";
      dragged._savedBackgroundColor = dragged.style.backgroundColor;
      dragged._savedBorderColor = dragged.style.borderColor;
      dragged._savedHTML = dragged.innerHTML;
      dragged.style.backgroundColor = color;
      dragged.style.borderColor = color;
      dragged.style.height = dim.height + 'px';
      dragged.innerHTML = "";
    }

    this.moved.setStyle({position: 'absolute', cursor: 'move'});
    this.moved.moveTo({x: x-this.x1, y: y-this.y1});

    if (this.def.dropping) {
      var highlight = this.def.dropping.highlight;
      if (highlight && this._dropzones) {
        this._dropzones.each(function(el) {
          NXThemes.Effects.get('activate')(el,
            {duration: highlight.duration || 1000, precision: 50}
          );
        });
      }
    }

  },

  moveEvent: function(e) {
    var x = Event.pointerX(e);
    var y = Event.pointerY(e);
    var startDragX = this.startDragX;
    var startDragY = this.startDragY;

    if (startDragX != null && startDragY != null) {
      if (Math.abs(startDragX-x) < 4 && Math.abs(startDragY-y) < 4) {
        return false;
      } else {
        Event.stop(e);
        this.startDrag(x, y);
        this.startDragX = null;
        this.startDragY = null;
      }
    }

    var moved = this.moved;
    moved.moveTo({x: x-this.x1, y: y-this.y1});

    var now = new Date().getTime();
    if (now < this._last_updated + 200) return;
    this._last_updated = now;

    var shifting = this.def.shifting;
    if (shifting) {
      var shifted = false;
      this._shiftablezones.each(function(s) {
        if (s.within(x, y)) {
          var height = s.getHeight();
          var position = s.cumulativeOffset();
          var ys = position[1];
          if (y < ys + height/3) {
            var target = s;
          } else {
            var target = this._findNext(s);
          }

          var parent = s.parentNode;
          parent.insertBefore(this.dragged, target);
          this.droptarget = $(s).up('.' +  shifting.container);
          shifted = true;
          return;
        };
      }.bind(this));

      if (!shifted && this._containerzones) {
        this._containerzones.each(function(s) {
          if (s.within(x, y)) {
            s.appendChild(this.dragged);
            this.droptarget = s;
            return;
          };
        }.bind(this));
      }
    }

  },

  dropEvent: function(e) {
    NXThemes.alreadyDragging = false;
    Event.stopObserving(document, "mousemove", this.moveEvent);
    Event.stopObserving(document, "mouseup", this.dropEvent);
    var dragging = this.def.dragging;
    var shifting = this.def.shifting;
    var dropping = this.def.dropping;
	
    if (!this.dragged) return;

    var moved = this.moved;
    var dragged = this.dragged;
    var x = Event.pointerX(e);
    var y = Event.pointerY(e);

    if (this.def.dragging.savePosition) {
      var id = dragged.getAttribute("id");
      if (id != null) {
        var position = dragged.viewportOffset();
        this.positions.set(id, {x: position[0], y: position[1]});
        NXThemes.setSessionData(this.getSessionId(), this.positions)
      }
    }

    var inTarget = false;
    var dropzones = this._dropzones || [];
    if (this.def.dropping) {
      dropzones.each(function(d) {
        if (d.within(x, y)) {
          inTarget = true;
          this.target = d;
        };
      }.bind(this));
    }

    var zoomback = dragging.zoomback;
    if (inTarget) {
      zoomback = false;
    }

    if (dropping && dragged) {
      var action_id = dropping.action;
      if (action_id) {
        var action_handler = NXThemes.getAction(action_id);
        if (action_handler) {
            order = 0;
            var droptarget = this.droptarget;
            if (droptarget) {
                var shiftable = droptarget.select('.' + shifting.element);
                if (shiftable) {
                    order = shiftable.indexOf(dragged);
                }
            }
            action_handler({
                source: dragged,
                sourceContainer: this.sourceContainer,
                target: droptarget || this.target,
                order: order,
                controller: this
            });
        }
        var source = dragging.source;
        if (source) {
          dragged.style.height = null;
          dragged.innerHTML = dragged._savedHTML;
          dragged.style.backgroundColor = dragged._savedBackgroundColor;
          dragged.style.borderColor = dragged._savedBorderColor;
        }
      }
    }

    if (dragging.feedback) {
      if (zoomback && moved) {
        moved.moveTo({
          x: this.x0,
          y: this.y0,
          duration: zoomback.duration || 400,
          onComplete: function() {$(moved).remove()}
        });
      }
    }

    if (this.def.dropping) {
      var highlight = this.def.dropping.highlight;
      if (highlight && this._dropzones) {
        dropzones.each(function(el) {
          NXThemes.Effects.get('deactivate')(el,
            {duration: highlight.duration || 1000}
          );
        });
      }
      var zoomto = this.def.dropping.zoomto;
      if (zoomto) {
        var pos = this.target.cumulativeOffset();
        moved.moveTo({
          x: pos[0],
          y: pos[1],
          duration: zoomto.duration || 400,
          onComplete: function() { $(moved).remove(); }
        });
      }
    }

    if (dragging.feedback && !zoomback && !zoomto && moved) {
      $(moved).remove();
    }

    if (dragged) {
      dragged.setOpacity(1);
    }

    if (moved) moved.setStyle({cursor: ''});
    this.dragged = null;
  }

});

NXThemes.PerspectiveController = Class.create();
NXThemes.PerspectiveController.prototype = Object.extend(
  new NXThemes.Controller(), {

  setup: function() {
    this._visible_views = new Hash();
    this.sessionid = this.getSessionId();
    this._current = null;

    NXThemes.registerEventHandler("parsed", this, function(event) {
      var perspective = this.getCurrentPerspective();
      if (perspective) {
        this.switchTo(perspective);
      }
    }.bind(this));
    NXThemes.subscribe("parsed", {subscriber: this, publisher: this.node});
  },

  register: function(view) {
    var visible = this._visible_views;
    var view_id = view.hash();
    var current_perspective = this._current;
    $A(view.def.perspectives).each(function(p) {
      if (visible.keys().indexOf(p) == -1) {
        visible.set(p, new NXThemes.Set());
      }
      visible.get(p).add(view_id);
    });
  },

  getCurrentPerspective: function() {
    return NXThemes.getSessionData(this.sessionid) || this.def.initial;
  },

  setCurrentPerspective: function(perspective) {
    NXThemes.setSessionData(this.sessionid, perspective);
  },

  update: function(view) {
    var current_perspective = this._current;
    if ($A(view.def.perspectives).indexOf(current_perspective) >= 0) {
      view.show();
    }
  },

  switchTo: function(perspective) {
    if (this._current == perspective) {
      this._visible_views.get(perspective).each(function(v) {
        NXThemes.getViewById(v).refresh();
      });
    }
    this._previous = this._current;
    this._current = perspective;
    this.setCurrentPerspective(perspective);

    var to_show = this._visible_views.get(perspective);
    var to_hide;
    if (to_show == null) {
      to_show = [];
      to_hide = this.views.entries();
    } else {
      to_hide = this.views.entries().select(function(v) {
        return !to_show.contains(v);
      });
    }
    to_hide.each(function(v) { NXThemes.getViewById(v).hide(); });
    to_show.each(function(v) { NXThemes.getViewById(v).show(); });
  },

  back: function() {
    this.switchTo(this._previous);
  },

  hide: function(perspective) {
    var to_hide = this._visible_views.get(perspective) || [];
    to_hide.each(function(v) { NXThemes.getViewById(v).hide(); });
  }

});


NXThemes.NodeExpander = Class.create();
NXThemes.NodeExpander.prototype = Object.extend(new NXThemes.Controller(), {

  setup: function() {
    this.clickEvent = this.clickEvent.bindAsEventListener(this);
  },

  register: function(view) {
    var widget = view.widget;
    $A($$('.' + this.def.childclass), widget).each(function(v) {
      $(v).hide();
    });
    Event.observe(widget, "click", this.clickEvent);
  },

  unregister: function(view) {
    Event.stopObserving(view.widget, "click", this.clickEvent);
  },

  clickEvent: function(e) {
    var widget = e.currentTarget;
    var target = $(Event.element(e));
    if (!target.hasClassName(this.def.nodeclass)) {
      return;
    }

    var fold = target.hasClassName("selected");

    $A($$('.' + this.def.nodeclass), widget).each(function(v) {
      v.removeClassName("selected");
    });
    if (fold) {
      target.removeClassName("selected");
    } else {
      target.addClassName("selected");
    }

    var node = $(target).next("." + this.def.childclass);
    if (node == null) return;

    var effect = this.def.effect;
    if (effect == 'slide') {
      NXThemes.Effects.get('slidedown')(node, {duration: 300, precision: 10});
    } else {
      node.show();
    }

    $A($$('.' + this.def.childclass), widget).each(function(v) {
      if (v != node || (fold && v == node)) {
        if (effect == 'slide') {
          NXThemes.Effects.get('slideup')(v, {duration: 200, precision: 10});
        } else {
          v.hide();
        }
      }
    });
  }

});

NXThemes.WidgetSelector = Class.create();
NXThemes.WidgetSelector.prototype = Object.extend(
  new NXThemes.Controller(), {

  setup: function() {
      this.clickEvent = this.clickEvent.bindAsEventListener(this);
      this.sessionid = this.getSessionId();
      this.selected = NXThemes.getSessionData(this.sessionid);
  },

  register: function(view) {
    Event.observe(view.widget, "click", this.clickEvent);
    if (view.def.id == this.selected) {
        view.select();
    }
  },

  unregister: function(view) {
    Event.stopObserving(view.widget, "click", this.clickEvent);
  },

  clickEvent: function(e) {
    var target = $(Event.element(e));
    var views = this.views;
    var sessionid = this.sessionid;
    views.entries().each(function(v) {
      var view = NXThemes.getViewById(v);
      if (target.descendantOf(view.widget)) {
        view.select();
        NXThemes.setSessionData(sessionid, view.def.id);
      } else {
        view.deselect();
      }
    });
  },

  select: function(id) {
    var views = this.views;
    var sessionid = this.sessionid;
    views.entries().each(function(v) {
      var view = NXThemes.getViewById(v);
      if (v == id) {
        view.select();
        NXThemes.setSessionData(sessionid, view.def.id);
      } else {
        view.deselect();
      }
    });
  }

});

