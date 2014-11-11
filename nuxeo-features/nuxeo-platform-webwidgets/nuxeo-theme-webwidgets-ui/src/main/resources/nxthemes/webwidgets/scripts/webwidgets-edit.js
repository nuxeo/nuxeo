
NXThemesWebWidgets.uploaders = new Hash();

NXThemesWebWidgets.addWidget = function(info) {
    var area = info.target.getAttribute('area');
    if (!area) {
    	return;
    }
    var widgetName = info.source.getAttribute('typename');
    var order = info.order;
    var url = nxthemesBasePath + "/nxthemes-webwidgets/add_widget";
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
    	     'area': area,
             'widget_name': widgetName,
             'order': order
         },
         onSuccess: function(r) {
             info.controller.refreshViews();
         },
         onFailure: function(r) {
             var text = r.responseText;
             window.alert(text);
         }
    });
};

NXThemesWebWidgets.moveWidget = function(info) {
	var srcContainer = info.sourceContainer;
    var srcArea = srcContainer.getAttribute('area');
    var srcUid = info.source.getAttribute('id').split('_')[2];
    var destArea = info.target.getAttribute('area');
    var destOrder = info.order;
    var srcProvider = srcContainer.getAttribute('provider');
    var destProvider = info.target.getAttribute('provider');
    var destDecoration = info.target.getAttribute('decoration');
    
    var url = nxthemesBasePath + "/nxthemes-webwidgets/move_widget";
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             'src_area': srcArea,
             'src_uid': srcUid,
             'dest_area': destArea,
             'dest_order': destOrder
         },
         onSuccess: function(r) {
           var destUid = r.responseText;
           NXThemesWebWidgets.changeWidgetId(srcProvider, destProvider, srcUid, destUid);               
           var widget = NXThemesWebWidgets.getWidget(destProvider, destUid);
           widget._decoration = destDecoration;
           widget._provider = destProvider;
           widget.setHtml();
         },
         onFailure: function(r) {
             var text = r.responseText;
             window.alert(text);
             window.location.reload();
         }
    });
};

NXThemesWebWidgets.deleteWidget = function(provider, widgetUid) {
      var answer = confirm("Deleting, are you sure?")
      if (!answer) {
          return;
      }
      var widgetEl = NXThemesWebWidgets.getWidgetElement(provider, widgetUid);
      var url = nxthemesBasePath + "/nxthemes-webwidgets/remove_widget";
      new Ajax.Request(url, {
         method: 'post',
         parameters: {
             'provider': provider,
             'widget_uid': widgetUid
         },
         onSuccess: function(r) {
           widgetEl.remove();
         },
         onFailure: function(r) {
             var text = r.responseText;
             window.alert(text);
         }
      });
};

NXThemesWebWidgets.setWidgetState = function(provider, widgetUid, mode, state) {
    var widgetEl = NXThemesWebWidgets.getWidgetElement(provider, widgetUid);
    var widget = NXThemesWebWidgets.getWidget(provider, widgetUid);
    var url = nxthemesBasePath + "/nxthemes-webwidgets/set_widget_state";
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             'provider': provider,
             'widget_uid': widgetUid,
             'state': state
         },
         onSuccess: function(r) {
           var widget_mode = mode;
           if (state) {
               widget_mode = widget_mode + '/' + state;
           }
           widget.switchMode(widget_mode);
         },
         onFailure: function(r) {
             var text = r.responseText;
             window.alert(text);
         }
      });
};


NXThemesWebWidgets.setWidgetCategory = function(select) {
    var category = select.value;
    if (category === null) {
      return;
    }
    var url = nxthemesBasePath + "/nxthemes-webwidgets/set_widget_category";
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             'category': category
         },
         onSuccess: function(r) {
           NXThemes.getViewById("web widget factory").refresh();
         },
         onFailure: function(r) {
             var text = r.responseText;
             window.alert(text);
         }
    });
};

NXThemesWebWidgets.editPreferences = function(provider, widgetUid) {
  var widget = NXThemesWebWidgets.getWidget(provider, widgetUid);
  var frame = widget.createElement("div");
  var form = NXThemesWebWidgets.renderPreferenceEditForm(widget);
  form.addClassName('nxthemesWebWidgetsEditForm');
  form.action = "";
  form.onsubmit = NXThemesWebWidgets.changePreferences.bindAsEventListener(this);
  form.setAttribute("widget_uid", widgetUid);
  form.setAttribute("widget_provider", provider);
  
  var uploadBox = widget.createElement("div");
  uploadBox.setAttribute("id", 'webwidget_upload_' + widgetUid);
  uploadBox.setAttribute("class", 'nxthemesWebWidgetsUploadBox');
  uploadBox.hide();

  frame.appendChild(form);
  frame.appendChild(uploadBox);
  
  widget.setBody(frame);
};

NXThemesWebWidgets.renderPreferenceEditForm = function(widget) {
    var form = widget.createElement("form");
    var table = widget.createElement("table");
    var tbody = widget.createElement("tbody");
    var widgetUid = widget.id;
    var providerName = widget.getProviderName();

    for (var i = 0; i < widget.preferences.length; i++) {
      var tr = widget.createElement("tr");
      var pref = widget.preferences[i];

      var name = pref.name;
      var value = widget.getValue(name) || pref.defaultValue;
      var type = pref.type;
      if (type == 'hidden') {
        continue;
      }
      var label = widget.createElement("label");
      label.innerHTML = pref.label + ':';

      var tdl = widget.createElement("td");
      tdl.setStyle("width", "25%");
      tdl.appendChild(label);
      tr.appendChild(tdl);

      var tdc = widget.createElement("td");
      tdc.setStyle("width", "75%");
      var control = '';

      if (type == 'text') {
        control = '<input type="string" name="' + name + '" value="' + value + '" />';

      } else if (type == 'file') {
        new NXThemesWebWidgets.FileUploader(providerName, widgetUid, name, value, tdc);

      } else if (type == 'image') {
        new NXThemesWebWidgets.ImageUploader(providerName, widgetUid, name, value, tdc);

      } else if (type == 'password') {
        control = '<input type="password" name="' + name + '" value="' + value + '" />';

      } else if (type == 'textarea') {
        control = '<textarea type="string" name="' + name + '">' + value + '</textarea>';

      } else if (type == 'range') {
        control = '<select name="' + name + '">';
        if (parseInt(pref.step) > 0) {
          for (var v=parseInt(pref.min); v<=parseInt(pref.max); v+=parseInt(pref.step)) {
            if (value == v) {
              control += '<option value="' + v + '" selected="selected">' + v + '</option>';
            } else {
              control += '<option value="' + v + '">' + v + '</option>';
            }
          }
        }
        control += '</select>';

      } else if (type == 'list') {
        control = '<select name="' + name + '">';
        var options = pref.options;
        for (var j=0; j<options.length; j++) {
          var v = options[j].value;
          var l = options[j].label;
          if (value == v) {
            control += '<option value="' + v + '" selected="selected">' + l + '</option>';
          } else {
            control += '<option value="' + v + '">' + l + '</option>';
          }
        }
        control += '</select>';

      } else if (type == 'boolean') {
        if (value == 'true') {
          control += '<input name="' + name + '" type="checkbox" checked="checked" />';
        } else {
          control += '<input name="' + name + '" type="checkbox" />';
        }
      }

      if (control) {
        tdc.innerHTML = control;
      }
      tr.appendChild(tdc);
      tbody.appendChild(tr);
    }
    table.appendChild(tbody);
    form.appendChild(table);

    var submit = widget.createElement('input');
    submit.type = 'submit';
    submit.value = "Ok";
    var div = widget.createElement("div");
    div.appendChild(submit);
    form.appendChild(div);

    return form;
};


NXThemesWebWidgets.getUploader = function(providerName, widgetUid, name) {
  var id = providerName + '/' + widgetUid + '/' + name;
  return NXThemesWebWidgets.uploaders.get(id);
}


NXThemesWebWidgets.BaseUploader = Class.create();
NXThemesWebWidgets.BaseUploader.prototype = {

  initialize: function(providerName, widgetUid, name, value, el) {
     this.providerName = providerName;
     this.widgetUid = widgetUid;
     this.name = name;
     this.value = value;
     this.el = el;

     this.register();
     this.render();
   },

   getId: function() {
     return this.providerName + '/' + this.widgetUid + '/' + this.name;
   },

   register: function() {
     NXThemesWebWidgets.uploaders.set(this.getId(), this);
   },

  upload: function(e) {
    this.controlEl.hide();
    this.boxEl.show();
  },

  complete: function() {
    // TO OVERRIDE
  },

  render: function() {
    // TO OVERRIDE
  }

};

NXThemesWebWidgets.ImageUploader = Class.create();
NXThemesWebWidgets.ImageUploader.prototype = Object.extend(new NXThemesWebWidgets.BaseUploader(), {

   render: function() {
     var controlEl = this.controlEl = document.createElement("div");
     var src = this.value;
     var msg = src ? "Change image" : "Set image";
     var html = '<button type="button">' + msg + '<button>';
     if (src) {
       html = '<div><img src="' + NXThemesWebWidgets.getWidgetDataUrl(src) + '" /></div>' + html;
     }
     controlEl.innerHTML = html;
     Event.observe(controlEl, "click", this.upload.bindAsEventListener(this));
     this.el.appendChild(controlEl);

     var boxEl = this.boxEl = document.createElement("div");
     var frameName = 'f' + this.widgetUid + '_' + this.name;
     boxEl.innerHTML = '<form style="padding: 0; margin: 0" action="' + nxthemesBasePath + '/nxthemes-webwidgets/upload_file?widget_uid=' + encodeURIComponent(this.widgetUid) + '&data=' + encodeURIComponent(this.name) + '&provider=' + encodeURIComponent(this.providerName) + '"' +
       ' method="post" enctype="multipart/form-data" target="' + frameName + '">' +             
       '<div><input type="file" name="file" size="8" onchange="submit()" /></div></form>';
     boxEl.hide();

     var iframeEl = this.iframeEl = document.createElement('iframe');
     iframeEl.style.display = "none";
     iframeEl.name = frameName;
     boxEl.appendChild(iframeEl);

     this.el.appendChild(boxEl);
  },

  complete: function() {
    var controlEl = this.controlEl;
    var boxEl = this.boxEl;
    var name = this.name;
    var providerName = this.providerName;
    var widgetUid = this.widgetUid;
    var widget = NXThemesWebWidgets.getWidget(providerName, widgetUid);
    var url = nxthemesBasePath + "/nxthemes-webwidgets/get_widget_data_info";
    new Ajax.Request(url, {
         method: 'get',
         parameters: {
             'provider': providerName,
             'widget_uid': widgetUid,
             'name': name
         },
         onSuccess: function(r) {
           var text = r.responseText;
           if (text) {
             var info = text.evalJSON(true);
             
             var timestamp = new Date().getTime();
             var data_url = 'nxwebwidgets://data/' + providerName + '/' + widgetUid + '/' + name + '/' + timestamp;
             var src = NXThemesWebWidgets.getWidgetDataUrl(data_url);
             widget.setValue(name, data_url);

             controlEl.innerHTML = '<div><img src="' + src + '" /></div>' +
               info['filename'] + ' (' + info['content-type'] + ')';
           } else {
               controlEl.innerHTML = '<img src="' + nxthemesBasePath + '/skin/nxthemes-webwidgets/img/exclamation.png" />';
           }
           controlEl.show();
           boxEl.hide();
         }
      });
    }
});

NXThemesWebWidgets.FileUploader = Class.create();
NXThemesWebWidgets.FileUploader.prototype = Object.extend(new NXThemesWebWidgets.BaseUploader(), {

   render: function() {
     var controlEl = this.controlEl = document.createElement("div");
     var src = this.value;
     var msg = src ? "Change file" : "Set file";
     controlEl.innerHTML = '<button type="button">' + msg + '<button>';
     Event.observe(controlEl, "click", this.upload.bindAsEventListener(this));
     this.el.appendChild(controlEl);

     var boxEl = this.boxEl = document.createElement("div");
     var frameName = 'f' + this.widgetUid + '_' + this.name;
     boxEl.innerHTML = '<form style="padding: 0; margin: 0" action="' + nxthemesBasePath + '/nxthemes-webwidgets/upload_file?widget_uid=' + encodeURIComponent(this.widgetUid) + '&data=' + encodeURIComponent(this.name) + '&provider=' + encodeURIComponent(this.providerName) + '"' +
       ' method="post" enctype="multipart/form-data" target="' + frameName + '">' +
       '<div><input type="file" name="file" size="8" onchange="submit()" /></div></form>';
     boxEl.hide();

     var iframeEl = this.iframeEl = document.createElement('iframe');
     iframeEl.style.display = "none";
     iframeEl.name = frameName;
     boxEl.appendChild(iframeEl);

     this.el.appendChild(boxEl);
  },

  complete: function() {
    var controlEl = this.controlEl;
    var boxEl = this.boxEl;
    var name = this.name;
    var providerName = this.providerName;
    var widgetUid = this.widgetUid;
    var widget = NXThemesWebWidgets.getWidget(providerName, widgetUid);
    var url = nxthemesBasePath + "/nxthemes-webwidgets/get_widget_data_info";
    new Ajax.Request(url, {
         method: 'get',
         parameters: {
             'provider': providerName,
             'widget_uid': widgetUid,
             'name': name
         },
         onSuccess: function(r) {
           var text = r.responseText;
           if (text) {
             var info = text.evalJSON(true);
             
             var timestamp = new Date().getTime();
             var data_url = 'nxwebwidgets://data/' + providerName + '/' + widgetUid + '/' + name + '/' + timestamp;
             var src = NXThemesWebWidgets.getWidgetDataUrl(data_url);
             widget.setValue(name, data_url);
             
             controlEl.innerHTML = info['filename'] + ' (' + info['content-type'] + ')';
          } else {
             controlEl.innerHTML = '<img src="' + nxthemesBasePath + '/skin/nxthemes-webwidgets/img/exclamation.png" />';
          }
          controlEl.show();
          boxEl.hide();
         },
         onFailure: function(r) {
             var text = r.responseText;
             window.alert(text);
         }
     });
  }
});

NXThemesWebWidgets.changePreferences = function(info) {
  Event.stop(info);
  var form = Event.findElement(info, "form");
  var preferencesMap = $H();
  var widgetUid = form.getAttribute("widget_uid");
  var providerName = form.getAttribute("widget_provider");
  var widget = NXThemesWebWidgets.getWidget(providerName, widgetUid);

  $A(Form.getElements(form)).each(function(i) {
     var type = i.type;
     if (type != "submit") {
       var name = i.name;
       var value = Form.Element.getValue(i);
       if (type == 'checkbox') {
         value = value == 'on' ? 'true' : 'false';
       }
       preferencesMap.set(name, value);
       widget.setValue(name, value);
     }
  });

  var url = nxthemesBasePath + "/nxthemes-webwidgets/update_widget_preferences";
  new Ajax.Request(url, {
         method: 'post',
         parameters: {
             'provider': providerName,
             'widget_uid': widgetUid,
             'preferences': Object.toJSON(preferencesMap)
         },
         onSuccess: function(r) {
           widget.draw();
         },
         onFailure: function(r) {
             var text = r.responseText;
             window.alert(text);
         }
  });
  return false;
};

NXThemesWebWidgets.exit = function() {
  NXThemes.expireCookie("nxthemes.mode");
  window.location.reload();
};

// Web widget panel
NXThemesWebWidgets.WebWidgetPanel = Class.create();
NXThemesWebWidgets.WebWidgetPanel.prototype = Object.extend(new NXThemes.View(), {

  setup: function() {
  },

  inspect: function() {
    return '[NXThemes Web Widget Panel]';
  },

  render: function(data) {
    var panel = this.widget;
    var area = data.get('area');
    var mode = data.get('mode');
    var provider = data.get('provider');
    var decoration = data.get('decoration');
    panel.setAttribute('area', area);
    panel.setAttribute('provider', provider);
    panel.setAttribute('decoration', decoration);
    var url = nxthemesBasePath + "/nxthemes-webwidgets/get_panel_data";
    new Ajax.Request(url, {
         method: 'get',
         parameters: {
             'area': area,
             'mode': mode
         },
         onSuccess: function(r) {
             var text = r.responseText;
             var panel_data = text.evalJSON(true);
             NXThemesWebWidgets.renderPanel(panel, panel_data);
         },
         onFailure: function(r) {
             // FIXME
         }
    });

  },

  teardown: function() {
  }

});

NXThemesWebWidgets.openFactoryPanel = function() {
  $('nxthemesWebWidgetFactoryPanel').show();
  $('nxthemesWebWidgetFactoryPanelStripOpen').hide();
  $('nxthemesWebWidgetFactoryPanelStripClose').show();
    $H(NXThemesWebWidgets.widgets).each(function(item) {
    var widget = item.value;
    widget.onResize();
  });
};

NXThemesWebWidgets.closeFactoryPanel = function() {
  $('nxthemesWebWidgetFactoryPanel').hide();
  $('nxthemesWebWidgetFactoryPanelStripOpen').show();
  $('nxthemesWebWidgetFactoryPanelStripClose').hide();
  $H(NXThemesWebWidgets.widgets).each(function(item) {
    var widget = item.value;
    widget.onResize();
  });
};

// Actions
NXThemes.addActions({
    'insert web widget': NXThemesWebWidgets.addWidget,
    'move web widget': NXThemesWebWidgets.moveWidget
});

NXThemes.registerWidgets({
  "web widget panel": function(def) {
    var widget = NXThemes.Canvas.createNode({
      tag: 'div',
      classes: ['nxthemesWebWidgetContainer'],
      style: {'display': 'none'}
    });
    return new NXThemesWebWidgets.WebWidgetPanel(widget, def);
  }
});

