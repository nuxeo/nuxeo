
NXThemesWebWidgets = {};
NXThemesWebWidgets.modules = new Hash();
NXThemesWebWidgets.widgets = new Hash();
NXThemesWebWidgets.decorations = new Hash();

NXThemesWebWidgets.getWidget = function(provider, uid) {
  return NXThemesWebWidgets.widgets.get(provider + '_' + uid);
};

NXThemesWebWidgets.getWidgetElement = function(provider, uid) {
  return $('webwidget_' + provider + '_' + uid); 
};

NXThemesWebWidgets.getWidgetElementTitle = function(provider, uid) {
  return $('webwidget_' + provider + '_' + uid + '_title'); 
};

NXThemesWebWidgets.getWidgetElementBody = function(provider, uid) {
  return $('webwidget_' + provider + '_' + uid + '_body'); 
}; 

NXThemesWebWidgets.getWidgetElementIcon = function(provider, uid) {
  return $('webwidget_' + provider + '_' + uid + '_icon'); 
}; 

NXThemesWebWidgets.changeWidgetId = function(oldProvider, newProvider, oldUid, newUid) {
  if (oldProvider == newProvider && oldUid == newUid) {
      return;
  }
  var widget = NXThemesWebWidgets.widgets.get(oldProvider + '_' + oldUid);
  var widgetEl = NXThemesWebWidgets.getWidgetElement(oldProvider, oldUid);
  if (widgetEl) {
    widgetEl.setAttribute('id', 'webwidget_' + newProvider + '_' + newUid);
  }
  widget.id = newUid;
  NXThemesWebWidgets.widgets.set(newProvider + '_' + newUid, widget);
  NXThemesWebWidgets.widgets.unset(oldProvider + '_' + oldUid);
};

NXThemesWebWidgets.WebWidget = Class.create();
NXThemesWebWidgets.WebWidget.prototype = {

  initialize: function(provider, uid) {
    this.id = uid;
    this._provider = provider;    
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
    this._decoration = '';
    this._mode = '';
    NXThemesWebWidgets.widgets.set(provider + '_' + uid, this);
  },

  getProviderName: function() {
    return this._provider;
  },

  draw: function() {
    var uid = this.id;
    var provider = this._provider;
    NXThemesWebWidgets.getWidgetElement(provider, uid).innerHTML = this._html;

    this.elements['title'] = UWA.$element(NXThemesWebWidgets.getWidgetElementTitle(provider, uid));
    this.elements['body'] = UWA.$element(NXThemesWebWidgets.getWidgetElementBody(provider, uid));
    this.elements['icon'] = UWA.$element(NXThemesWebWidgets.getWidgetElementIcon(provider, uid));

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
      if (template === null) {
        var default_state_mode = widget_mode.split('/')[0] + '/*';
        template = widget_decoration[default_state_mode];
      }
      widget.decorate(template, widget_mode);
      widget.draw();
    } else {
      alert("Widget decoration not found: " + decoration);
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
     var provider = this._provider;
     
     html = html.replace(/%BASE_PATH%/g, nxthemesBasePath);
     html = html.replace(/%WIDGET_BODY%/g, this._body);
     html = html.replace(/%ICON_AREA%/g, 'webwidget_' + provider + '_' + uid + '_icon');
     html = html.replace(/%TITLE_AREA%/g, 'webwidget_' + provider + '_' + uid + '_title');
     html = html.replace(/%BODY_AREA%/g, 'webwidget_' + provider + '_' + uid + '_body');
     html = html.replace(/%DRAG_AREA%/g, "nxthemesWebWidgetDragHandle");
     html = html.replace(/%WIDGET_NAME%/g, this._name);
     html = html.replace(/%WIDGET_AUTHOR%/g, metas['author'] || '');
     html = html.replace(/%WIDGET_DESCRIPTION%/g, metas['description'] || '');
     html = html.replace(/%ACTION_EDIT_PREFERENCES%/g,
                         "NXThemesWebWidgets.editPreferences('" + provider + "', '" + uid + "');");
     html = html.replace(/%ACTION_SHADE_WINDOW%/g,
                         "NXThemesWebWidgets.setWidgetState('" + provider + "', '" + uid + "', '" + mode + "', 'shaded');");
     html = html.replace(/%ACTION_UNSHADE_WINDOW%/g,
                         "NXThemesWebWidgets.setWidgetState('" + provider + "', '" + uid + "', '" + mode + "', '*');");
     html = html.replace(/%ACTION_DELETE_WIDGET%/g,
                         "NXThemesWebWidgets.deleteWidget('" + provider + "', '" + uid + "');");
     this._html = html;
  }

};

NXThemesWebWidgets.getWidgetDataUrl = function(url) {
  if (!url.substr(0, 20) == 'nxwebwidgets://data/') {
      return url;
  }
  var path = url.substr(20).split('/');
  var providerName = path[0];
  var widgetUid = path[1];
  var dataName = path[2];
  var timestamp = path[3];
  return nxthemesBasePath + '/nxthemes-webwidgets/render_widget_data?widget_uid=' + widgetUid + '&data=' + dataName + '&provider=' + providerName + '&timestamp=' + timestamp; 
};

Object.extend(NXThemesWebWidgets.WebWidget.prototype, UWA.Widget.prototype);

NXThemesWebWidgets.renderPanel = function(panel, data) {
  var mode = data.mode;
  var provider = data.provider;
  var decoration = data.decoration;
  var widget_decoration = data.widget_decoration;
  NXThemesWebWidgets.decorations.set(decoration, widget_decoration)
    
  var widget_types = data.widget_types;
  
  $A(data.widget_items).each(function(item) {
    var uid = item.uid;
    var name = item.name;
    var widget_type = widget_types[name];

    var widget = new NXThemesWebWidgets.WebWidget(provider, uid);
    
    // Set name
    widget._name = name;
    widget._provider = provider;
    widget._decoration = decoration;

    // Inline styles
    var styles = widget_type.styles;
    var styles_id = "webwidget_style_" + name.toLowerCase().replace(/ /g, '_');
    if (styles && !$(styles_id)) {
        styles = styles.replace(/\${basePath}/g, nxthemesBasePath);
        var el = document.createElement('style');
        el.setAttribute("type", "text/css");
        el.setAttribute("id", styles_id);
        if (el.styleSheet) {   // IE
            el.styleSheet.cssText = styles;
        } else {
           el.appendChild(document.createTextNode(styles));
        }
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
    $(el).setAttribute('id', 'webwidget_' + provider + '_' + uid);
    panel.appendChild(el);

    // Set icon
    widget._icon = widget_type.icon || nxthemesBasePath + "/nxthemes-webwidgets/render_widget_icon?name=" + widget_type.name;
    
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

