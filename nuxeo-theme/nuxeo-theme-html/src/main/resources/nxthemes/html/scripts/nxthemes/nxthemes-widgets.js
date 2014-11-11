/*

 NXThemes UI library - widgets

 Author: Jean-Marc Orliaguet <jmo@chalmers.se>

*/

NXThemes.registerWidgets({

  contextmenu: function(def) {
    var widget = NXThemes.Canvas.createNode({
      tag: 'div',
      classes: ['nxthemesContextMenu'],
      style: {position: 'absolute', display: 'none'}
    });
    return new NXThemes.ContextualMenu(widget, def);
  },

  contextactions: function(def) {
    var widget = NXThemes.Canvas.createNode({
      tag: 'div',
      classes: ['nxthemesContextActions'],
      style: {position: 'absolute', display: 'none'}
    });
    return new NXThemes.ContextualActions(widget, def);
  },

  tooltip: function(def) {
    var widget = NXThemes.Canvas.createNode({
      tag: 'div',
      classes: ['nxthemesTooltip'],
      style: {position: 'absolute', display: 'none'}
    });
    return new NXThemes.Tooltip(widget, def);
  },

  panel: function(def) {
    var widget = NXThemes.Canvas.createNode({
      tag: 'div',
      style: {display: 'none'}
    });
    return new NXThemes.Panel(widget, def);
  },

  area: function(def) {
    var widget = $(def.widget.area);
    return new NXThemes.Area(widget, def);
  },

  button: function(def) {
    var widget = NXThemes.Canvas.createNode({
      tag: 'span',
      classes: ['nxthemesButton']
    });
    return new NXThemes.Button(widget, def);
  },

  tabs: function(def) {
    var widget = NXThemes.Canvas.createNode({
      tag: 'div',
      classes: ['nxthemesTabs']
    });
    return new NXThemes.Tabs(widget, def);
  }

});


// Panels

NXThemes.Panel = Class.create();
NXThemes.Panel.prototype = Object.extend(new NXThemes.View(), {

  setup: function() {
    this.url_field = this.def.widget.url || 'url';
  },

  inspect: function() {
    return '[NXThemes Panel]';
  },

  render: function(data) {
    var view = this;

    // avoid rendering the panel twice
    if (view._rendering) {
      return;
    }
    view._rendering = true;

    var url = data.get(this.url_field);
    if (!url) {
      NXThemes.warn("Panel '" + view.def.id + "' found no url in model: " + this.model.def.id);
      return;
    }
    url = url.replace('%{here}', window.location.href);

    var loading = data.get('loading');
    if (loading) {
      var loadingElement = $(loading);
      if (loadingElement) {
        loadingElement.show();
      }
    }

    var script = data.get('script');
    if (script) {
      this.script_id = this.def.model;
      NXThemes.Canvas.addScript(this.script_id, script);
    }

    var css = data.get('css');
    if (css) {
      this.css_id = this.def.model;
      NXThemes.Canvas.addStyleSheet(this.css_id, css);
    }

    var widget = this.widget;
    var model = this.model;
    var filter_id = this.def.filter;
    var options = {
      method: "get",
      requestHeaders: ["If-Modified-Since", new Date(0)], // Prevent IE from caching pages
      onComplete: function(req) {
        var old_html = widget.innerHTML;
        var new_html = req.responseText;

        if (filter_id) {
          var filter = NXThemes.getFilterById(filter_id);
          if (filter) {
            new_html = filter(new_html);
          }
        }
        widget.innerHTML = new_html;

        if (new_html != old_html) {
          NXThemes.parse(widget);
        }
        view.resetControllers();
        if (loading) {
          var loadingElement = $(loading);
          if (loadingElement) {
            loadingElement.hide();
          }
        }

        // scroll to the top
        window.scrollTo(0, 0);

        view._rendering = false;
        view.ready();
      }
    };

    var form_data = $H(data.get('form')) || new Hash();
    if (form_data.keys().length > 0) {
      var i = url.indexOf('?');
      if (i > 0) {
        var query_string = url.substr(i+1);
        var query_params = $H(query_string.toQueryParams()).update(form_data);
        url = url.substr(0, i) + '?' + query_params.toQueryString();
      } else {
        url += '?' + form_data.toQueryString();
      }
    }
    new Ajax.Request(url, options);
  },

  teardown: function() {
    if (this.css_id) {
      NXThemes.Canvas.removeStyleSheet(this.css_id);
    }
    if (this.script_id) {
      NXThemes.Canvas.removeScript(this.script_id);
    }
  }

});

// Contextual menu
NXThemes.ContextualMenu = Class.create();
NXThemes.ContextualMenu.prototype = Object.extend(new NXThemes.View(), {

  setup: function() {
    this.showEvent = this.showEvent.bindAsEventListener(this);
    this.hideEvent = this.hideEvent.bindAsEventListener(this);
    this.callEvent = this.callEvent.bindAsEventListener(this);
    this.mouseOverEvent = this.mouseOverEvent.bindAsEventListener(this);

    Event.observe(this.widget, "mousedown", function(e) {Event.stop(e);});
    Event.observe(this.widget, "mouseup", this.callEvent);
    Event.observe(this.widget, "mouseover", this.mouseOverEvent);
    Event.observe(document, "mousedown", this.hideEvent);

    this.area = $(this.def.area);
    if (this.area) {
      Event.observe(this.area, "mouseup", this.showEvent);
      this.area.oncontextmenu = new Function("return false");
    }
    this._displayed = false;

    // custom CSS class
    var cssClass = this.def.widget.cssClass;
    if (cssClass) {
      this.widget.addClassName(cssClass);
    }
  },

  render: function(data) {
    this.widget.innerHTML = '';
    this._renderFragment(this.widget, this.def.widget, data);
    this.ready();
  },

  _getSubmenu: function(element) {
    if (!element) {
      return null;
    }
    var nodes = element.childNodes;
    for (var i=0; i<nodes.length; i=i+1) {
      var node = $(nodes[i]);
      if (node.nodeType != 1) {
        continue;
      }
      if (!node.hasClassName("submenu")) {
        continue;
      }
      return node;
    }
    return null;

  },

  _renderFragment: function(container, fragment, data) {
    var noicon = this.def.widget.noIcon;
    var createNode = NXThemes.Canvas.createNode;
    var newSeparator = false;
    fragment.items.each(function(item) {
      var type = item.type;
      var visible = item.visible;
      var disabled = false;
      if (data && visible) {
        if (!data.get(visible)) {
          if (this.def.widget.showDisabledItems) {
            disabled = true;
          } else {
            return;
          }
        }
      }
      switch (type) {
        case "title":
          var title = data.get('title');
          if (title) {
            var div = createNode({
              tag: 'div',
              classes: ['title'],
              parent: container,
              text: title
            });
          }
          break;

        case "item":
          var options = {
            tag: 'a',
            style: {display: 'block'},
            classes: ['menuitem'],
            attributes: {
              action: item.action,
              href: 'javascript:void(0)'
            },
            parent: container
          }
          var confirm = item.confirm;
          if (confirm && !disabled) {
            options.attributes.confirm = confirm;
          }
          if (disabled) {
            options.attributes.disabled = true;
            options.classes.push("disabled");
          }
          var a = createNode(options);
          var icon = disabled ? noicon: (item.icon || noicon);
          createNode({
            tag: 'img',
              attributes: {src: nxthemesBasePath + icon, alt: '*', width: '16px', height: '16px'},
            parent: a
          });
          a.appendChild(document.createTextNode(item.label));
          newSeparator = true;
          break;

        case "selection":
          var choices = item.choices;
          (data.get(choices) || []).each(function(s) {
            var options = {
              tag: 'a',
              style: {display: 'block', paddingRight: '10px'},
              classes: s.selected ? ['selected'] : [],
              attributes: {
                action: item.action,
                choice: s.choice,
                href: "javascript:void(0)"
              },
              parent: container
            }
            var a = createNode(options);
            var icon = disabled ? noicon: (item.icon || noicon);
            createNode({
              tag: 'img',
              attributes: {src: nxthemesBasePath + icon, alt: '', width: '16px', height: '16px'},
              parent: a
            });
            a.appendChild(document.createTextNode(s.label));
            a.appendChild(createNode({
              tag: 'img',
              attributes: {src: nxthemesBasePath + noicon, alt: '', width: '16px', height: '16px'}
              }));
          });
          newSeparator = true;
          break;

        case "separator":
          if (newSeparator) {
            var node = createNode({
              tag: 'div',
              classes: ['separator'],
              parent: container
            });
          }
          newSeparator = false;
          break;

        case "submenu":
          var arrow = this.def.widget.arrow;
          var options = {
            tag: 'a',
            classes: ['submenuitem'],
            style: {display: 'block'},
            attributes: {href: 'javascript:void(0)'}
          };
          if (disabled) { options.classes = ['disabled']; }
          var submenuitem = container.appendChild(createNode(options));
          var icon = item.icon || noicon;
          if (arrow) {
            createNode({
              tag: 'img',
              attributes: {src: nxthemesBasePath + arrow, alt: '>'},
              classes: ['arrow'],
              parent: submenuitem
            });
          }
          createNode({
            tag: 'img',
            attributes: {src: nxthemesBasePath + icon, alt: '*'},
            parent: submenuitem
          });
          if (!this.submenuLeft) {
            this.submenuLeft = $(this.widget).getWidth() -2;
          }
          var submenu = createNode({
            tag: 'div',
            classes: ['submenu'],
            style: {
              position: 'absolute',
              left: this.submenuLeft + 'px',
              display: 'none',
              width: 'auto',
              margin: '-20px 0 0 0'
            },
            parent: submenuitem
          });
          submenuitem.appendChild(document.createTextNode(item.label));
          this._renderFragment(submenu, item, data);
          newSeparator = true;
          break;
      }
    }.bind(this));
  },

  prepare: function() {
    var selected = this.selected;
    if (!selected) return;

    // Display the menu inside the screen
    var widget = this.widget;
    widget.moveTo({x: this.mouseX, y: this.mouseY, fit: true});
  },

  /* Event handlers */
  showEvent: function(e) {
    this.mouseX = Event.pointerX(e);
    this.mouseY = Event.pointerY(e);

    if (Math.abs(this.mouseX - this.startX) > 2 ||
        Math.abs(this.mouseY != this.startY) > 2) return;

    var element = Event.element(e);
    
    // get the first node that is identifiable and that has a model
    var node = element;
    while (node) {
      var selected = NXThemes.Identifiable.getIdentifiable(node);
      if (selected === null) {
        return;
      }
      var model = NXThemes.Canvas.getModel(selected);
      if (model != null) {
        break;
      }
      node = node.parentNode;
    }
   
    // if no model is associated in the view definition, require that the model is obtained from the canvas
    if (typeof this.def.model == "undefined" && model == null) {
      return;
    }
    
    var target_type = this.def.widget.targetType;
    if (target_type !== null) {
      var model_type = model.getType();
      if (model_type !== null && model_type != target_type) {
        return;
      }  
    }
    
    var data = model.getData();
    if (data === null) return;
        
    if (!this._containVisibleItems(data)) return;

    var widget = this.widget;
    this.selected = selected;
    this._displayed = true;
    this.focus();
    this.show();
    return false;
  },

  _containVisibleItems: function(data) {
      if (this.def.widget.showDisabledItems) return true;
      var items = this.def.widget.items;
      for (var i=0; i< items.length; i=i+1) {
        var visible = items[i].visible;
        if (typeof visible == "undefined" || data.get(visible)) {
          return true;
        }
      };
      return false;
  },

  hideEvent: function(e) {
    this._displayed = false;
    this.defocus();
    this.hide();

    this.startX = Event.pointerX(e);
    this.startY = Event.pointerY(e);
  },

  callEvent: function(e) {
    Event.stop(e);
    var element = Event.element(e);
    if (element.getAttribute("disabled")) return;
    var action = element.getAttribute("action");
    if (!action) return;
    var choice = element.getAttribute("choice") || action;
    var confirm = element.getAttribute("confirm");
    this.hide();
    if (confirm) {
      if (!window.confirm(confirm)) return;
    }
    /* notify the controller to take action */
    var info = {
      target: this.selected,
      publisher: this,
      subscriber: this.controller,
      options: {action: action, choice: choice}
    }
    NXThemes.notify("command", info);
  },

  mouseOverEvent: function(e) {
    var here = Event.element(e);
    if ($(here).hasClassName("submenuitem")) {
      var menu = this._getSubmenu(here);
      if (!menu) return;
      $(here.parentNode).select(".submenu").each(
        function(v) {
          $(v).hide();
        }
      );
      $(menu).show();
    }
    if ($(here).hasClassName("menuitem")) {
      $(here.parentNode).select(".submenu").each(
        function(v) {
          $(v).hide();
        }
      );
    }
  }

});


// Contextual actions
NXThemes.ContextualActions = Class.create();
Object.extend(NXThemes.ContextualActions.prototype,
              NXThemes.ContextualMenu.prototype);
Object.extend(NXThemes.ContextualActions.prototype, {

  _renderFragment: function(container, fragment, data) {
    var createNode = NXThemes.Canvas.createNode;
    fragment.items.each(function(item) {
      if (item.type != "item") return;
      var visible = item.visible;
      var disabled = false;
      if (data && visible) {
        if (!data.get(visible)) return;
     }
      var options = {
        tag: 'a',
        style: {display: 'block'},
        classes: [],
        attributes: {
          action: item.action,
          href: 'javascript:void(0)'
        }
      }

      var confirm = item.confirm;
      if (confirm && !disabled) {
        options.attributes.confirm = confirm;
      }

      var a = createNode(options);
      var noicon = this.def.noicon;
      var icon = disabled ? noicon: (item.icon || noicon);
      a.appendChild(createNode({
        tag: 'img',
        attributes: {src: nxthemesBasePath + icon, alt: '*', width: '16px', height: '16px'},
        container: a
      }));

      a.appendChild(document.createTextNode(item.label));
    });
  }

});


// Tooltip
NXThemes.Tooltip = Class.create();
NXThemes.Tooltip.prototype = Object.extend(new NXThemes.View(), {

  setup: function() {
    var showEvent = this.showEvent = this.showEvent.bindAsEventListener(this);
    var hideEvent = this.hideEvent = this.hideEvent.bindAsEventListener(this);
    this.moveEvent = this.moveEvent.bindAsEventListener(this);
    $A(this.def.selectors || []).each(function(s) {
      $$(s).each(function(e) {
        Event.observe(e, "mouseover", showEvent);
        Event.observe(e, "mouseout", hideEvent);
      });
    });
  },

  render: function(data) {
    this.widget.innerHTML = data.get('hint');
    this.ready();
  },

  prepare: function() {
    this.widget.moveTo({x: this.mouseX, y: this.mouseY +10, fit: true});
  },

  /* Event handlers */
  showEvent: function(e) {
    var selected = Event.element(e);

    var model = NXThemes.Canvas.getModel(selected);
    if (!model) return;

    var data = model.readData();
    if (!data) return;
    if (data.get('hint') === null) return;

    this.mouseX = Event.pointerX(e);
    this.mouseY = Event.pointerY(e);
    this.selected = selected;

    if (this.def.widget.follow) {
      Event.observe(document, "mousemove", this.moveEvent);
    }

    this._displayed = true;
    this.focus();
    this.show();
    Event.stop(e);
  },

  moveEvent: function(e) {
    this.widget.moveTo({x: Event.pointerX(e)+10, y: Event.pointerY(e)+10});
    Event.stop(e);
  },

  hideEvent: function(e) {
    var selected = Event.element(e);
    if (selected != this.selected) return;

    if (this.def.widget.follow) {
      Event.stopObserving(document, "mousemove", this.moveEvent);
    }

    this._displayed = false;
    this.defocus();
    this.hide();
    Event.stop(e);
  }

});


// Area widget
NXThemes.Area = Class.create();
NXThemes.Area.prototype = Object.extend(new NXThemes.View(), {

  inspect: function() {
    return "[Area Widget]";
  }

});


// Button widget
NXThemes.Button = Class.create();
NXThemes.Button.prototype = Object.extend(new NXThemes.View(), {

  setup: function() {
    this.render();
    var clickEvent = this.clickEvent.bindAsEventListener(this);
    Event.observe(this.widget, "click", clickEvent);
  },

  inspect: function() {
    return "[Button Widget]";
  },

  render: function(data) {
    var label = this.def.label;
    var widget = this.widget;
    var classNames = this.def.classNames;
    if (classNames) {
      classNames.split(" ").each(function(c) {
        $(widget).addClassName(c);
      });
    }
    var link = this.def.link;
    if (link == null) {
      link = 'javascript:void(0)';
    }
    var mouseover = '';
    var hover = this.def.hover;
    if (hover != null) {
    	mouseover = ' onmouseover="' + hover + '"';
    }
    widget.innerHTML = '<b>&nbsp;</b><a href="' + link + '"' + mouseover + '>' + label + '</a>';
    this.ready();
  },

  clickEvent: function(e) {
    var perspective = this.def.toPerspective;
    var controller = this.def.perspectiveController;
    if (perspective != null && controller != null) {
      NXThemes.getControllerById(controller).switchTo(perspective);
    }
  },

  select: function() {
   $(this.widget).addClassName("selected");
  },

  deselect: function() {
   $(this.widget).removeClassName("selected");
  },

  highlight: function() {
   $(this.widget).addClassName("highlighted");
  },
	  
  dehighlight: function() { 
   $(this.widget).removeClassName("highlighted");
  }

});


// Tabs widget
NXThemes.Tabs = Class.create();
NXThemes.Tabs.prototype = Object.extend(new NXThemes.View(), {

  setup: function() {
    this.render();
  },

  inspect: function() {
    return "[Tabs Widget]";
  },

  render: function(data) {
    var items = this.def.widget.items;
    var createNode = NXThemes.Canvas.createNode;

    var styleClass = this.def.widget.styleClass;
    if (styleClass != null) {
       this.widget.addClassName(styleClass);
    }
    var ul = createNode({tag: 'ul', parent: this.widget});

    var view_id = this.hash();
    var tabs = this.tabs = new Hash();
    $A(items).each(function(item) {
       var li = createNode({tag: 'li', parent: ul});
       var link = item.link;
       var switchTo = item.switchTo;
       tabs.set(switchTo, li);
       var href = 'javascript:void(0)';
       if (link != null) {
           href = link
       } else if (switchTo != null) {
           href = 'javascript: NXThemes.getViewById("' + view_id +'").switchTo("' + switchTo + '")';
       }
       var a = createNode({tag: 'a', parent: li, text: item.label,
           attributes: {'href': href}
       });

       var s = switchTo.split("/");
       var controller = NXThemes.getControllerById(s[0]);
       if (controller) {
         if (controller.getCurrentPerspective() == s[1]) {
           li.addClassName("selected");
         }
       }
    });
    this.widget.appendChild(createNode({tag: 'div', style: {clear: 'both'} }));
    this.ready();
  },

  switchTo: function(p) {
    if (p != null && p.indexOf("/") >= 0) {
      var s = p.split("/");
      NXThemes.getControllerById(s[0]).switchTo(s[1]);
      this.tabs.each(function(t) {
        var perspective = t.key;
        var li = t.value;
        if (perspective == p) {
          li.addClassName("selected");
        } else {
          li.removeClassName("selected");
        }
     });}
   }
});
