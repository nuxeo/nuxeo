
if (typeof NXThemesStyleEditor == "undefined") {
    NXThemesStyleEditor = {
        'currentProperty': null
    };
}

NXThemesStyleEditor.refreshPreview = function() {
    var previewArea = document.getElementById('stylePreviewArea');
    var element  = previewArea.getAttribute('element');
    NXThemesStyleEditor.renderElement(element, previewArea);
};

NXThemesStyleEditor.refreshCssPreview = function() {
    var url = webEngineContextPath + "/nxthemes-editor/render_css_preview"; 
    new Ajax.Request(url, {
         method: 'get',
         onComplete: function(r) {
           var text = r.responseText;
           $('previewCss').innerHTML = text;
         }
    });
};

NXThemesStyleEditor.closeStylePicker = function() {
    NXThemes.getControllerById('style editor perspectives').switchTo('style properties');
};

NXThemesStyleEditor.renderElement = function(id, area) {
    // render an element inside an area
    var options = {
      method: 'get',
      onComplete: function(req) {
        var temp = document.createElement("div");
        temp.innerHTML = req.responseText;
        var elementList = $(temp).select('#' + id);
        if (elementList.length > 0) {
          var element = elementList[0];
          var html = element.innerHTML;
          // remove inline xmlns="..."
          html = html.replace(/xmlns="(.*?)"/g, "");
          // remove inline onclick="..."
          html = html.replace(/onclick="(.*?)"/g, "");
          area.innerHTML = html;
        }
      }
    };
    var url = window.location.href;
    var i = url.indexOf('?');
    var query_params = $H({'engine': 'fragments-only'});
    if (i > 0) {
      var query_string = url.substr(i+1);
      query_params = query_params.update($H(query_string.toQueryParams()));
    }
    url = url.substr(0, i) + '?' + query_params.toQueryString();
    new Ajax.Request(url, options);
};

NXThemesStyleEditor.chooseStyleSelector = function(select) {
  var value = select.value;
  if (value === '') {
    value = null;
  }
  NXThemesStyleEditor.setStyleSelector(select.value);
};

NXThemesStyleEditor.setPresetGroup = function(select) {
    var group = select.value;
    var url = webEngineContextPath + "/nxthemes-editor/select_preset_group?group=" + encodeURIComponent(group); 
    new Ajax.Request(url, {
         method: 'get',
         onComplete: function(r) {
           NXThemesStyleEditor.refreshStylePicker();
         }
    });
};

NXThemesStyleEditor.setStyleSelector = function(selector) {
    var url = webEngineContextPath + "/nxthemes-editor/select_style_selector?selector=" + encodeURIComponent(selector); 
    new Ajax.Request(url, {
         method: 'get',
         onComplete: function(r) {
             NXThemes.getControllerById("style editor perspectives").switchTo("style properties");
             NXThemes.getViewById("style properties").refresh();
         }
    });
};

NXThemesStyleEditor.createStyle = function() {
    var url = webEngineContextPath + "/nxthemes-editor/create_style"; 
    new Ajax.Request(url, {
         method: 'get',
         onComplete: function(r) {
             NXThemes.getViewById("element style").refresh();
             NXThemesEditor.writeMessage("New style created.");
         }
    });
};

NXThemesStyleEditor.getSelectorInContext = function(element, context) {
  if (element == context) {
    return "";
  }
  var i, node, tag, name, selector, classnames;
  node = element;

  // If no context is passed, use the document itself.
  if (!context) {
    context = document;
  }

  // start from the first contained element
  context = context.childNodes.item(0);

  var selectors = new Array();
  while (node) {
    if (node == context) {
      break;
    }
    tag = node.tagName;
    if (!tag) {
      break;
    }
    tag = tag.toLowerCase();
    classnames = node.className;
    name = '';
    if (classnames) {
      name = classnames.replace(' ', '.');
    }
    selector = name ? tag + '.' + name : tag;
    // ignore tags that have the "ignore" attribute set
    if (!node.getAttribute("ignore")) {
      selectors.push(selector);
    }
    node = node.parentNode;
  }

  // reverse the array
  selectors.reverse();
  return selectors.join(" ");
};

NXThemesStyleEditor.refreshEditor = function() {
  NXThemes.getViewById("element style").refresh();
};

NXThemesStyleEditor.selectTag = function(info) {
    var target = info.target;
    Event.stop(info);
    var area = $('stylePreviewArea');
    var selector = NXThemesStyleEditor.getSelectorInContext(target, area);
    NXThemesStyleEditor.setStyleSelector(selector);
    return false;
};

NXThemesStyleEditor.setCurrentStyleLayer = function(uid) {
    var url = webEngineContextPath + "/nxthemes-editor/select_style_layer?uid=" + encodeURIComponent(uid); 
      new Ajax.Request(url, {
         method: 'get',
         onComplete: function(r) {
             NXThemes.getControllerById('style editor perspectives').switchTo('default');
             NXThemes.getViewById("element style").refresh();
         }
      });
};

NXThemesStyleEditor.pickPropertyValue = function(info) {
    var target = info.target;
    var category = target.getAttribute('category');
    NXThemesStyleEditor.currentProperty = target.getAttribute('property');
    var url = webEngineContextPath + "/nxthemes-editor/select_style_category?category=" + encodeURIComponent(category); 
      new Ajax.Request(url, {
         method: 'get',
         onComplete: function(r) {
           NXThemes.getControllerById('style editor perspectives').switchTo('style picker');
         }
      });
};

NXThemesStyleEditor.setStyleEditMode = function(mode, fromMode) {
    if (fromMode == 'form') {
      NXThemesEditor.updateElementStyle();
    }
    if (fromMode == 'css') {
      NXThemesEditor.updateElementStyleCss();
    }
    var url = webEngineContextPath + "/nxthemes-editor/select_style_edit_mode?mode=" + encodeURIComponent(mode); 
    new Ajax.Request(url, {
         method: 'get',
         onComplete: function(req) {
           NXThemes.getViewById("style properties").refresh();
         }
    });
};

NXThemesStyleEditor.setStylePropertyCategory = function(category) {
    var url = webEngineContextPath + "/nxthemes-editor/select_style_property_category?category=" + encodeURIComponent(category); 
    new Ajax.Request(url, {
         method: 'get',
         onComplete: function(req) {
           NXThemes.getViewById("style properties").refresh();
         }
    });
};

NXThemesStyleEditor.makeElementUseNamedStyle = function(select) {
    var value = select.value;
    if (value === '') {
        value = null;
    }
    var form = $(select).up("form");
    var id = form.getAttribute("element");
    var theme_name = form.getAttribute("currentThemeName");
    var style_name = value;
    var url = webEngineContextPath + "/nxthemes-editor/make_element_use_named_style?id=" + encodeURIComponent(id) + "&theme_name=" + encodeURIComponent(theme_name);
    if (style_name) { 
        url += "&style_name=" + encodeURIComponent(style_name);
    }
    new Ajax.Request(url, {
         method: 'get',
         onComplete: function(req) {
             NXThemes.getViewById("element style").refresh();
         }
    });
};

NXThemesStyleEditor.createNamedStyle = function(id, currentThemeName) {
    var styleName = prompt("Please enter a style name:", "");
    if (styleName === null) {
        return;
    }
    if (styleName === "") {
        window.alert("Style names cannot be empty.");
        return;
    }
    var url = webEngineContextPath + "/nxthemes-editor/create_named_style?id=" + encodeURIComponent(id) + "&style_name=" + encodeURIComponent(styleName) + "&theme_name=" + encodeURIComponent(currentThemeName); 
    new Ajax.Request(url, {
         method: 'get',
         onComplete: function(req) {
             NXThemes.getViewById("element style").refresh();
         }
    });
};

NXThemesStyleEditor.deleteNamedStyle = function(id, currentThemeName, styleName) {
    var ok = confirm("Deleting style, are you sure?");
    if (!ok) {
        return;
    }
    var url = webEngineContextPath + "/nxthemes-editor/delete_named_style?id=" + encodeURIComponent(id) + "&style_name=" + encodeURIComponent(styleName) + "&theme_name=" + encodeURIComponent(currentThemeName); 
    new Ajax.Request(url, {
         method: 'get',
         onComplete: function(req) {
             NXThemes.getViewById("element style").refresh();
         }
    });
};

NXThemesStyleEditor.updateFormField = function(value) {
  var currentProperty = NXThemesStyleEditor.currentProperty;
  if (currentProperty !== null) {
    var propertyName = 'property:' + currentProperty;
    var inputs = Form.getInputs('nxthemesElementStyle', null, propertyName);
    if (inputs !== null) {
      inputs[0].value = value;
    }
  }
  NXThemesStyleEditor.closeStylePicker();
};

NXThemesStyleEditor.refreshStylePicker = function() {
    NXThemes.getViewById("style picker").refresh();
};

// widgets
NXThemes.registerWidgets({

  stylepreview: function(def) {
    var widget = NXThemes.Canvas.createNode({
      tag: 'div',
      classes: ['nxthemesStylePreview']
    });
    return new NXThemesStyleEditor.StylePreview(widget, def);
  }

});

NXThemesStyleEditor.StylePreview = Class.create();
NXThemesStyleEditor.StylePreview.prototype = Object.extend(new NXThemes.View(), {

  setup: function() {
    NXThemesStyleEditor.refreshPreview();
    NXThemesStyleEditor.refreshCssPreview();
  }

});

// actions
NXThemes.addActions({
  'select style tag': NXThemesStyleEditor.selectTag,
  'update style label': function(info) {
    var box = $('labelInfo');
    if (box === null) {
      return;
    }
    var x = Event.pointerX(info);
    var y = Event.pointerY(info);
    var target = info.target;
    var label= NXThemesStyleEditor.getSelectorInContext(target, $('stylePreviewArea'));
    if (!label) {
      label = '.';
    }
    box.innerHTML = label;
    box.setStyle({left: x+'px', top: y-45+'px'});
    box.show();
  },
  'hide style label': function(info) {
    $('labelInfo').hide();
  },
  'pick property value': NXThemesStyleEditor.pickPropertyValue
});



