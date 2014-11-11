
NXThemesWebWidgets = {};
NXThemesWebWidgets.modules = new Hash();
NXThemesWebWidgets.widgets = new Hash();
NXThemesWebWidgets.decorations = new Hash();

NXThemesWebWidgets.getWidgetById = function(uid) {
  return NXThemesWebWidgets.widgets.get(uid);
};

NXThemesWebWidgets.changeWidgetId = function(oldUid, newUid) {
  var widget = NXThemesWebWidgets.widgets.get(oldUid);
  var widgetEl = $('webwidget_' + oldUid);
  if (widgetEl) {
    widgetEl.setAttribute('id', 'webwidget_' + newUid);
  }
  widget.id = newUid;
  NXThemesWebWidgets.widgets.set(newUid, widget);
  NXThemesWebWidgets.widgets.unset(oldUid);
};

NXThemesWebWidgets.WebWidget = Class.create();
NXThemesWebWidgets.WebWidget.prototype = {

  initialize: function(uid) {
    this.id = uid;
    this.elements = {}; // HTML elements
    this.preferences = []; // preference schema
    this.metas = {}; // metadata
    this.data = {}; // preference data
    this.callbacks = {};
    this.title = '';
    this.body = null;
    this.lang = '';
    this.locale = '';
    // private variables
    this._name = '';
    this._html = '';
    this._icon = '';
    this._body = '';
    this._provider = '';
    this._decoration = '';
    this._mode = '';
  },

  getProviderName: function() {
    return this._provider;
  },

  draw: function() {
    var uid = this.id;
    $('webwidget_' + uid).innerHTML = this._html;

    this.elements['title'] = UWA.$element($('webwidget_' + uid + '_title'));
    this.elements['body'] = UWA.$element($('webwidget_' + uid + '_body'));
    this.elements['icon'] = UWA.$element($('webwidget_' + uid + '_icon'));

    this.body = this.elements['body'];
    this.setIcon(this._icon);
    this.onLoad();
  },

  switchMode: function(widget_mode) {
    this._mode = widget_mode;
    this.setHtml();
  },

  setHtml: function() {
    var widget = this;
    var widget_mode = this._mode;
    var decoration = widget._decoration;
    var widget_decoration = widget.getDecoration(decoration);
    if (widget_decoration) {
      var template = widget_decoration[widget_mode];
      if (template == null) {
        var default_state_mode = widget_mode.split('/')[0] + '/*';
        template = widget_decoration[default_state_mode];
      }
      widget.decorate(template, widget_mode);
      widget.draw();
    } else {
      var url = "/nuxeo/nxthemes-webwidgets-decoration/?decoration=" + encodeURIComponent(decoration);
      var options = {
        method: 'get',
        asynchronous: false,
        onComplete: function(req) {
          var widget_decoration = req.responseText.evalJSON(true);
          widget.setDecoration(decoration, widget_decoration)
          var template = widget_decoration[widget_mode];
          if (template === null) {
            var default_state_mode = widget_mode.split('/')[0] + '/*';
            template = widget_decoration[default_state_mode];
          }
          widget.decorate(template, widget_mode);
          widget.draw();
       }
      };
      new Ajax.Request(url, options);
    }
  },

  getDecoration: function(decoration) {
    return NXThemesWebWidgets.decorations.get(decoration);
  },

  setDecoration: function(decoration, decorations) {
    NXThemesWebWidgets.decorations.set(decoration, decorations)
  },

  decorate: function(html, widget_mode) {
     var uid = this.id;
     var metas = this.metas;
     var mode = widget_mode.split('/')[0];

     html = html.replace(/%WIDGET_BODY%/g, this._body);
     html = html.replace(/%ICON_AREA%/g, 'webwidget_' + uid + '_icon');
     html = html.replace(/%TITLE_AREA%/g, 'webwidget_' + uid + '_title');
     html = html.replace(/%BODY_AREA%/g, 'webwidget_' + uid + '_body');
     html = html.replace(/%DRAG_AREA%/g, "nxthemesWebWidgetDragHandle");
     html = html.replace(/%WIDGET_NAME%/g, this._name);
     html = html.replace(/%WIDGET_AUTHOR%/g, metas['author'] || '');
     html = html.replace(/%WIDGET_DESCRIPTION%/g, metas['description'] || '');
     html = html.replace(/%ACTION_EDIT_PREFERENCES%/g,
                                 "NXThemesWebWidgets.editPreferences('" + uid + "');");
     html = html.replace(/%ACTION_SHADE_WINDOW%/g,
                                 "NXThemesWebWidgets.setWidgetState('" + uid + "', '" + mode + "', 'shaded');");
     html = html.replace(/%ACTION_UNSHADE_WINDOW%/g,
                                 "NXThemesWebWidgets.setWidgetState('" + uid + "', '" + mode + "', '*');");
     html = html.replace(/%ACTION_DELETE_WIDGET%/g,
                                 "NXThemesWebWidgets.deleteWidget('" + uid + "');");
     this._html = html;
  }

};


Object.extend(NXThemesWebWidgets.WebWidget.prototype, UWA.Widget.prototype);

NXThemesWebWidgets.renderPanel = function(provider, decoration, panel, data) {
  var mode = data.mode;
  var widget_types = data.widget_types;

  $A(data.widget_items).each(function(item) {
    var uid = item.uid;
    var name = item.name;
    var widget_type = widget_types[name];

    var widget = new NXThemesWebWidgets.WebWidget(uid);
    NXThemesWebWidgets.widgets.set(uid, widget);

    // Set name
    widget._name = name;
    widget._provider = provider;
    widget._decoration = decoration;

    // Inline styles
    var styles = widget_type.styles;
    var styles_id = "webwidget_style_" + name.toLowerCase().replace(/ /g, '_');
    if (styles && !$(styles_id)) {
        var el = document.createElement('style');
        el.setAttribute("type", "text/css");
        el.setAttribute("id", styles_id);
        el.innerHTML = styles;
        var head = document.getElementsByTagName("head")[0];
        head.appendChild(el);
    }

    // Inline scripts
    var scripts = widget_type.scripts;
    if (scripts) {
        try {
          eval(scripts);
        } catch(e) {
          widget.log(e);
        }
    }

    // metadata
    widget.metas = widget_type.metas;

    // Set preference schema
    widget.preferences = widget_type.preferences;

    // Set preference data
    var preference_data = item.preferences;
    if (preference_data !== null) {
      widget.data = preference_data;
    }

    // HTML
    var el = document.createElement('div');
    $(el).addClassName("nxthemesWebWidget");
    $(el).setAttribute('id', 'webwidget_' + uid);
    panel.appendChild(el);

    // Set icon
    widget._icon = widget_type.icon;

    // Set body
    widget._body = widget_type.body;

    // Observe resize events
    Event.observe(window, 'resize', widget.onResize.bindAsEventListener(widget));

    var widget_mode = mode;
    var state = item.state;
    if (state) {
      widget_mode = widget_mode + '/' + state;
    }
    widget.switchMode(widget_mode);
  });

};

