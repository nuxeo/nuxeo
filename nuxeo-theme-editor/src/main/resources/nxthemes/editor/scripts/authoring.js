
if (typeof NXThemesEditor == "undefined") {
    NXThemesEditor = {
        writeMessage: function(msg) {
          var box = $("nxthemesStatusMessage");
          box.innerHTML = msg;
          box.show();
          NXThemes.Effects.get('fadeout')(box, {delay: 1700});
        },
        isAlpha: function(s) {
          for (var i = 0; i < s.length; i= i+1) {
            var c = s.charAt(i);
            if ( !((c>="A") && (c<="Z")) && !((c>="a") && (c<="z")) ) {
              return false;
            }
          }
          return true;
        }
    };

}

NXThemesEditor.setCanvasMode =  function(info) {
    var target = Event.element(info);
    var mode = target.getAttribute("name");
    NXThemes.setCookie("nxthemes.mode", mode);
    NXThemes.getViewById("canvas mode selector").refresh();
    NXThemesEditor.refreshCanvas();
};

NXThemesEditor.deleteThemeOrPage = function(info) {
    var id =  info.target.getAttribute('id');
    if (id === null) {
      return;
    }
    var element_id = id.substr(4);
    var url = webEngineContextPath + "/nxthemes/editor/delete_element?id=" + encodeURIComponent(element_id);
    new Ajax.Request(url, {
         method: 'get',
         onComplete: function(r) {
             var text = r.responseText;
             if (text === "") {
                 window.alert("Could not delete the element.");
             } else {
                 NXThemes.getViewById("theme selector").refresh();
                 NXThemesEditor.refreshCanvas();
             }
         }
    });
};

NXThemesEditor.moveElement = function(info) {
    var src_id = info.source.getAttribute('id');
    var dest_id = info.target.getAttribute('id');
    var order = info.order;
    var url = webEngineContextPath + "/nxthemes/editor/move_element?src_id=" + encodeURIComponent(src_id) + "&dest_id=" + encodeURIComponent(dest_id) + "&order=" + encodeURIComponent(order);
    new Ajax.Request(url, {
         method: 'get',
         onComplete: function(r) {
             NXThemesEditor.refreshCanvas();
         }
    });
};

NXThemesEditor.insertFragment = function(info) {
    var dest_id = info.target.getAttribute('id');
    var order = info.order;
    var type_name = info.source.getAttribute('typename');
    var url = webEngineContextPath + "/nxthemes/editor/insert_fragment?type_name=" + encodeURIComponent(type_name) + "&dest_id=" + encodeURIComponent(dest_id) + "&order=" + encodeURIComponent(order);
    new Ajax.Request(url, {
         method: 'get',
         onComplete: function(r) {
             NXThemesEditor.refreshCanvas();
             NXThemesEditor.switchToEditCanvas()
         }
    });
};

NXThemesEditor.editElement = function(info) {
    var id = info.target.getAttribute('id');
    var url = webEngineContextPath + "/nxthemes/editor/select_element?id=" + encodeURIComponent(id); 
    new Ajax.Request(url, {
         method: 'get',
         onComplete: function(r) {
             NXThemes.getControllerById("editor perspectives").switchTo("edit element");
             NXThemes.getViewById("element editor").refresh();
             var v = NXThemes.getViewById("element editor tabs");
             if (typeof v !== 'undefined') {
                 v.switchTo("element editor perspectives/edit properties");
             }
         }
    });
};

NXThemesEditor.changeElementStyle = function(info) {
    var id = info.target.getAttribute('id');
    var url = webEngineContextPath + "/nxthemes/editor/select_element?id=" + encodeURIComponent(id); 
    new Ajax.Request(url, {
         method: 'get',
         onComplete: function(r) {
             NXThemes.getControllerById("editor perspectives").switchTo("edit element");
             NXThemes.getViewById("element editor").refresh();
             var v = NXThemes.getViewById("element editor tabs");
             if (typeof v !== 'undefined') {
                 v.switchTo("element editor perspectives/edit style");
             }
         }
    });
};

NXThemesEditor.setSize = function(info) {
    var form = Event.findElement(info, "form");
    var id = null;
    var width = null;
    $A(Form.getElements(form)).each(function(i) {
        var name = i.name;
        var value = Form.Element.getValue(i);
        if (name == "id") {
          id = value;
        } else if (name == "width") {
          width = value;
        }
      });
    var url = webEngineContextPath + "/nxthemes/editor/update_element_size?id=" + encodeURIComponent(id) + "&width=" + encodeURIComponent(width); 
    new Ajax.Request(url, {
         method: 'get',
         onComplete: function(r) {
             NXThemesEditor.refreshCanvas();
         }
    });       
};

NXThemesEditor.updateElementProperties = function(info) {
    var form = Event.findElement(info, "form");
    var id = null;
    var propertyMap = $H();
    $A(Form.getElements(form)).each(function(i) {
        var name = i.name;
        var value = Form.Element.getValue(i);
        if (name == "id") {
          id = value;
        } else {
          propertyMap.set(i.name, value);
        }
    });
    var url = webEngineContextPath + "/nxthemes/editor/update_element_properties"; 
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             id: id,
             property_map: propertyMap.toJSON()
         },
         onComplete: function(r) {
             NXThemes.getViewById("element properties").refresh();
             NXThemesEditor.writeMessage("Properties updated.");
         }
    });
};

NXThemesEditor.updateElementWidget = function(info) {
    var form = Event.findElement(info, "form");
    var id = null;
    var viewName = "";
    $A(Form.getElements(form)).each(function(i) {
        var name = i.name;
        var value = Form.Element.getValue(i);
        if (name == "id") {
          id = value;
        } else if (name == "viewName") {
          viewName = value;
        }
    });
    if (!viewName) {
        return;
    }
    var url = webEngineContextPath + "/nxthemes/editor/update_element_widget?id=" + encodeURIComponent(id) + "&view_name=" + encodeURIComponent(viewName); 
    new Ajax.Request(url, {
         method: 'get',
         onComplete: function(r) {
             NXThemesEditor.writeMessage("Widget changed.");
         }
    });
};


NXThemesEditor.updateElementDescription = function(info) {
    var form = Event.findElement(info, "form");
    var id = null;
    var description = "";
    $A(Form.getElements(form)).each(function(i) {
        var name = i.name;
        var value = Form.Element.getValue(i);
        if (name == "id") {
          id = value;
        } else if (name == "description") {
          description = value;
        }
    });

    if (!description) {
      return;
    }
    var url = webEngineContextPath + "/nxthemes/editor/update_element_description?id=" + encodeURIComponent(id) + "&description=" + encodeURIComponent(description); 
    new Ajax.Request(url, {
         method: 'get',
         onComplete: function(r) {
             NXThemesEditor.writeMessage("Description changed.");
         }
    });         
};


NXThemesEditor.updateElementStyle = function() {
    var form = $('nxthemesElementStyle');
    var id, path, viewName;
    var propertyMap = $H();
    $A(Form.getElements(form)).each(function(i) {
        var name = i.name;
        var value = Form.Element.getValue(i);
        if (name == "id") {
          id = value;
        } else if (name == "path") {
          path = value;
        } else if (name == "viewName") {
          viewName = value;
        } else if (name.indexOf('property:') === 0) {
          propertyMap.set(name.substr(9), value);
        }
    });
    
    var url = webEngineContextPath + "/nxthemes/editor/update_element_style"; 
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             id: id,
             view_name: viewName,
             path: path,
             property_map: propertyMap.toJSON()
         },
         onComplete: function(r) {
             NXThemesStyleEditor.refreshCssPreview();
             NXThemesEditor.writeMessage("Style updated.");
         }
    });
};

NXThemesEditor.updateElementStyleCss = function() {
    var form = $('nxthemesElementStyleCSS');
    var cssSource, viewName, id;
    Form.getElements(form).each(function(i) {
        var name = i.name;
        var value = $F(i);
        if (name == "cssSource") {
          cssSource = value;
        } else if (name == "id") {
          id = value;
        } else if (name == "viewName") {
          viewName = value;
        }
    });
    var url = webEngineContextPath + "/nxthemes/editor/update_element_style_css"; 
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             id: id,
             view_name: viewName,
             css_source: cssSource
         },
         onComplete: function(r) {
             NXThemesStyleEditor.refreshCssPreview();
             NXThemesEditor.writeMessage("Style updated.");
         }
    });
};

NXThemesEditor.setElementVisibility = function(info) {
    var form = Event.findElement(info, "form");
    var id = null;
    var alwaysVisible = false;
    var perspectives = [];
    Form.getElements(form).each(function(i) {
        var name = i.name;
        var value = $F(i);
        if (name == "id") {
          id = value;
        } else if (name == "alwaysVisible") {
          alwaysVisible = value ? true : false;
        } else if (name == "perspectives") {
          perspectives = $F("perspectives");
        }
    });
    var url = webEngineContextPath + "/nxthemes/editor/update_element_visibility?id=" + encodeURIComponent(id); 
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             'always_visible': alwaysVisible,
             'perspectives': perspectives
         },
         onComplete: function(r) {
             NXThemes.getViewById("element visibility").refresh();
             NXThemesEditor.writeMessage("Visibility changed.");
         }
    });
};

NXThemesEditor.setElementWidget = function(info) {
    var id =  info.target.getAttribute('id');
    if (id === null) {
      return;
    }
    var viewName = info.options.choice;
    if (!viewName) {
        return;
    }
    var url = webEngineContextPath + "/nxthemes/editor/update_element_widget?id=" + encodeURIComponent(id) + "&view_name=" + encodeURIComponent(viewName); 
    new Ajax.Request(url, {
         method: 'get',
         onComplete: function(r) {
             NXThemesEditor.refreshCanvas();
         }
    });
};

NXThemesEditor.copyElement = function(info) {
    var id =  info.target.getAttribute('id');
    var url = webEngineContextPath + "/nxthemes/editor/copy_element"; 
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             id: id
         },
         onComplete: function(r) {
             NXThemesEditor.writeMessage("Element copied.");
         }
    });
};

NXThemesEditor.splitElement = function(info) {
    var id =  info.target.getAttribute('id');
    var url = webEngineContextPath + "/nxthemes/editor/split_element?id=" + encodeURIComponent(id); 
    new Ajax.Request(url, {
         method: 'get',
         onComplete: function(r) {
             NXThemesEditor.refreshCanvas();
         }
    });       
};

NXThemesEditor.setElementPadding = function(info) {
    var id = info.target.getAttribute('id');
    var url = webEngineContextPath + "/nxthemes/editor/select_element?id=" + encodeURIComponent(id); 
    new Ajax.Request(url, {
         method: 'get',
         onComplete: function(r) {
             NXThemes.getControllerById("editor perspectives").switchTo("edit padding");
         }
    });
};

NXThemesEditor.updateElementPadding = function(info) {
    var form = Event.findElement(info, "form");
    var id = null;
    var propertyMap = $H();
    $A(Form.getElements(form)).each(function(i) {
        var name = i.name;
        var value = Form.Element.getValue(i);
        propertyMap[i.name] = value;
    });
    var url = webEngineContextPath + "/nxthemes/editor/update_element_layout"; 
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             property_map: propertyMap
         },
         onComplete: function(r) {
             NXThemesEditor.refreshCanvas();
         }
    });
};

NXThemesEditor.alignElement = function(info) {
    var target = info.target;
    var id =  target.getAttribute('id');
    if (id === null) {
      return;
    }
    var position = info.options.choice;
    var url = webEngineContextPath + "/nxthemes/editor/align_element?id=" + encodeURIComponent(id) + "&position=" + encodeURIComponent(position); 
    new Ajax.Request(url, {
         method: 'get',
         onComplete: function(r) {
           NXThemesEditor.refreshCanvas();
         }
    });     
};

NXThemesEditor.duplicateElement = function(info) {
    var id =  info.target.getAttribute('id');
    var url = webEngineContextPath + "/nxthemes/editor/duplicate_element?id=" + encodeURIComponent(id); 
    new Ajax.Request(url, {
         method: 'get',
         onComplete: function(r) {
             NXThemes.getViewById("style css").hide();
             NXThemes.getViewById("style css").show();
             NXThemesEditor.refreshCanvas();
         }
    });       
};

NXThemesEditor.pasteElement = function(info) {
    var dest_id =  info.target.getAttribute('id');
    var url = webEngineContextPath + "/nxthemes/editor/paste_element?dest_id=" + encodeURIComponent(dest_id); 
    new Ajax.Request(url, {
         method: 'get',
         onComplete: function(r) {
             NXThemes.getViewById("style css").hide();
             NXThemes.getViewById("style css").show();
             NXThemesEditor.refreshCanvas();
             NXThemesEditor.writeMessage("Element pasted.");
         }
    });
};

NXThemesEditor.deleteElement = function(info) {
    var id =  info.target.getAttribute('id');
    var url = webEngineContextPath + "/nxthemes/editor/delete_element?id=" + encodeURIComponent(id);
    new Ajax.Request(url, {
         method: 'get',
         onComplete: function(r) {
             NXThemesEditor.refreshCanvas();
         }
    });
};

NXThemesEditor.selectPerspective = function(info) {
    var form = Event.findElement(info, "form");
    var perspective = Form.findFirstElement(form).getValue();
    NXThemes.setCookie("nxthemes.perspective", perspective);
    NXThemesEditor.refreshCanvas();
};

NXThemesEditor.switchTheme = function(info) {
    var target = Event.element(info);
    var name = target.getAttribute("name");
    if (name !== null) {
      NXThemes.setCookie("nxthemes.theme", name);
      NXThemes.getViewById("theme selector").refresh();
      NXThemesEditor.refreshCanvas();
    }
};

NXThemesEditor.addTheme = function() {
    var name = prompt("Please enter a theme name:", "");
    if (name === "") {
        window.alert("Theme names must not be empty.");
        return "";
    }
    if (!NXThemesEditor.isAlpha(name)) {
        window.alert("Theme names must only contain alphabetic characters.");
        return "";
    }
    var url = webEngineContextPath + "/nxthemes/editor/add_theme?name=" + encodeURIComponent(name);
    new Ajax.Request(url, {
         method: 'get',
         onComplete: function(r) {
             var text = r.responseText;
             if (text === "") {
                 window.alert("The theme name is already taken.");
             } else {
                 NXThemes.setCookie("nxthemes.theme", text);
                 NXThemes.getViewById("theme selector").refresh();
                 NXThemesEditor.refreshCanvas();
             }
         }
    });
};

NXThemesEditor.addPage = function(themeName) {
    var name = prompt("Please enter a page name:", "");
    if (name === "") {
        window.alert("Page names must not be empty.");
        return "";
    }
    if (!NXThemesEditor.isAlpha(name)) {
        window.alert("Page names must only contain alphabetic characters.");
        return "";
    }
    var url = webEngineContextPath + "/nxthemes/editor/add_page?path=" + encodeURIComponent(themeName + '/' + name);
    new Ajax.Request(url, {
         method: 'get',
         onComplete: function(r) {
             var text = r.responseText;
             if (text === "") {
                 window.alert("The page name is already taken.");
             } else {
                 NXThemes.setCookie("nxthemes.theme", text);
                 NXThemes.getViewById("theme selector").refresh();
                 NXThemesEditor.refreshCanvas();
             }
         }
    });    
};

NXThemesEditor.switchToEditCanvas = function() {
    NXThemes.getControllerById('editor buttons').select('edit canvas');
    NXThemes.getControllerById('editor perspectives').switchTo('edit canvas');
};

NXThemesEditor.backToCanvas = function() {
    NXThemesEditor.switchToEditCanvas();
}

NXThemesEditor.addSection = function(info) {
    var target = Event.element(info);
    var id = target.getAttribute('sectionid') || target.getAttribute('pageid');
    var url = webEngineContextPath + "/nxthemes/editor/insert_section_after?id=" + encodeURIComponent(id);
    new Ajax.Request(url, {
         method: 'get',
         onComplete: function(r) {
             NXThemesEditor.refreshCanvas();
         }
    });
};

NXThemesEditor.alignSection = function(info) {
    var target = Event.element(info);
    var id = target.getAttribute('sectionid');
    var position = target.getAttribute('position');
    var url = webEngineContextPath + "/nxthemes/editor/align_element?id=" + encodeURIComponent(id) + "&position=" + encodeURIComponent(position); 
    new Ajax.Request(url, {
         method: 'get',
         onComplete: function(r) {
           NXThemesEditor.refreshCanvas();
         }
    }); 
};

NXThemesEditor.setAreaStyle = function(info) {
    var target = Event.element(info);
    var property = target.getAttribute('name');
    NXThemesEditor.currentProperty = property;
    var area = NXThemes.Canvas.getFirstParentNodeWithAnId(target);
    if (area !== null) {
        var id = area.getAttribute("id");
        if (id !== null) {
          NXThemesEditor.selectedElement = id;
        }
    }
    var category = null;
    if (property == 'background') {
      category = 'background';
    } else if (property == 'border-top') {
      category = 'border';
    } else if (property == 'border-left') {
      category = 'border';
    } else if (property == 'border-bottom') {
      category = 'border';
    } else if (property == 'border-right') {
      category = 'border';
    }
    var url = webEngineContextPath + "/nxthemes/editor/select_style_category?category=" + encodeURIComponent(category); 
    new Ajax.Request(url, {
         method: 'get',
         onComplete: function(r) {
           NXThemes.getControllerById('area style perspectives').switchTo('style chooser');
         }
    });
};

NXThemesEditor.updateAreaStyle = function(value) {
  NXThemesEditor.closeAreaStyleChooser();
  var element_id = NXThemesEditor.selectedElement;
  if (element_id === null) {
    return;
  }
  var property = NXThemesEditor.currentProperty;
  if (property !== null) {
      if (value == null) {
          value = '';
      }
      var url = webEngineContextPath + "/nxthemes/editor/assign_style_property?element_id=" + encodeURIComponent(element_id) + "&property=" + encodeURIComponent(property) + "&value=" + encodeURIComponent(value); 
      new Ajax.Request(url, {
         method: 'get',
         onComplete: function(r) {
           NXThemes.getViewById("style css").hide();
           NXThemes.getViewById("style css").show();
           NXThemesEditor.refreshCanvas();
         }
      });
  }
};

NXThemesEditor.setPresetGroup = function(select) {
    var group = select.value;
    var url = webEngineContextPath + "/nxthemes/editor/select_preset_group?group=" + encodeURIComponent(group); 
    new Ajax.Request(url, {
         method: 'get',
         onComplete: function(r) {
           NXThemes.getViewById("area style chooser").refresh();
         }
    });
};

NXThemesEditor.closeAreaStyleChooser = function() {
    NXThemes.getControllerById('area style perspectives').switchTo('default');
};

NXThemesEditor.refresh = function() {
  var url = webEngineContextPath + "/nxthemes/editor/expire_themes"; 
  new Ajax.Request(url, {
         method: 'get',
         onComplete: function(r) {
             NXThemesEditor.refreshCanvas();
             NXThemesEditor.writeMessage("Canvas refreshed.");
         }
  });
};

NXThemesEditor.exit = function() {
  NXThemes.expireCookie("nxthemes.theme");
  NXThemes.expireCookie("nxthemes.engine");
  NXThemes.expireCookie("nxthemes.perspective");
  var url = webEngineContextPath + "/nxthemes/editor/clear_selections"; 
  new Ajax.Request(url, {
         method: 'get',
         onComplete: function(r) {
             window.location.reload();
         }
  });
};

NXThemesEditor.repairTheme = function(themeName) {
    var url = webEngineContextPath + "/nxthemes/editor/repair_theme?themeName=" + encodeURIComponent(themeName); 
    new Ajax.Request(url, {
         method: 'get',
         onComplete: function(r) {
             NXThemes.getViewById("theme manager").refresh();
             NXThemesEditor.writeMessage("Theme repaired.");
         }
    });
};

NXThemesEditor.loadTheme = function(src) {
    var url = webEngineContextPath + "/nxthemes/editor/load_theme?src=" + encodeURIComponent(src); 
    new Ajax.Request(url, {
         method: 'get',
         onComplete: function(r) {
           var text = r.responseText;
           var msg = text ? "Theme loaded." : "The theme could not be loaded.";
           NXThemes.getViewById("theme manager").refresh();
           NXThemesEditor.writeMessage(msg);
         }
    });
};

NXThemesEditor.saveTheme = function(src, indent) {
    var url = webEngineContextPath + "/nxthemes/editor/save_theme?src=" + encodeURIComponent(src) + "&indent=" + encodeURIComponent(indent); 
    new Ajax.Request(url, {
         method: 'get',
         onComplete: function(r) {
           var text = r.responseText;
           var msg = text ? "Theme saved." : "The theme could not be saved.";
           NXThemes.getViewById("theme manager").refresh();
           NXThemesEditor.writeMessage(msg);
         }
    });
};

NXThemesEditor.refreshCanvas = function() {
    NXThemes.getViewById("canvas area").refresh();
};

NXThemesEditor.StyleCss = Class.create();
NXThemesEditor.StyleCss.prototype = Object.extend(new NXThemes.View(), {

  show: function() {
    NXThemes.Canvas.addStyleSheet('nxthemes-css', '/nuxeo/nxthemes-css/?timestamp=' + new Date().getTime());
  },

  hide: function() {
    NXThemes.Canvas.removeStyleSheet('nxthemes-css');
  }

});

// widgets
NXThemes.registerWidgets({

  stylecss: function(def) {
    var widget = NXThemes.Canvas.createNode({
      tag: 'div'
    });
    return new NXThemesEditor.StyleCss(widget, def);
  }

});

// Initialization

NXThemes.addActions({
    'move element': NXThemesEditor.moveElement,
    'insert fragment': NXThemesEditor.insertFragment,
    'edit element': NXThemesEditor.editElement,
    'duplicate element': NXThemesEditor.duplicateElement,
    'update element properties': NXThemesEditor.updateElementProperties,
    'update element widget': NXThemesEditor.updateElementWidget,
    'update element style': NXThemesEditor.updateElementStyle,
    'update element style css': NXThemesEditor.updateElementStyleCss,
    'update element description': NXThemesEditor.updateElementDescription,
    'set element visibility': NXThemesEditor.setElementVisibility,
    'update element padding': NXThemesEditor.updateElementPadding,
    'copy element': NXThemesEditor.copyElement,
    'paste element': NXThemesEditor.pasteElement,
    'delete element': NXThemesEditor.deleteElement,
    'select perspective': NXThemesEditor.selectPerspective,
    'cancel event': function(info) {Event.stop(info);},
    'switch theme': NXThemesEditor.switchTheme,
    'set canvas mode': NXThemesEditor.setCanvasMode,
    'set size': NXThemesEditor.setSize,
    'add section': NXThemesEditor.addSection,
    'align section': NXThemesEditor.alignSection,
    'align element': NXThemesEditor.alignElement,
    'split element': NXThemesEditor.splitElement,
    'set element padding': NXThemesEditor.setElementPadding,
    'delete theme or page': NXThemesEditor.deleteThemeOrPage,
    'set element widget': NXThemesEditor.setElementWidget,
    'set area style': NXThemesEditor.setAreaStyle,
    'change element style': NXThemesEditor.changeElementStyle
});

// Filters
NXThemesEditor.cleanUpCanvas = function(html) {
    // remove inline xmlns="..."
    html = html.replace(/xmlns="(.*?)"/g, "");
    // remove inline onclick="..."
    html = html.replace(/onclick="(.*?)"/g, "");
    return html;
};

NXThemes.registerFilters({
    'clean up canvas': NXThemesEditor.cleanUpCanvas
});
