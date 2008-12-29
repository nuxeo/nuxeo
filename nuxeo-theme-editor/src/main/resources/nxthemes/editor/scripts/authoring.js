
if (typeof NXThemesEditor == "undefined") {
    NXThemesEditor = {
        writeMessage: function(msg) {
          var box = $("nxthemesStatusMessage");
          box.innerHTML = msg;
          box.show();
          NXThemes.Effects.get('fadeout')(box, {delay: 1700});
        },
        isLowerCase: function(s) {
          for (var i = 0; i < s.length; i= i+1) {
            var c = s.charAt(i);
            if ( !((c>="a") && (c<="z")) ) {
              return false;
            }
          }
          return true;
        },
        isLowerCaseOrSpace: function(s) {
          for (var i = 0; i < s.length; i= i+1) {
            var c = s.charAt(i);
            if ( !((c>="a") && (c<="z") || c == ' ')) {
              return false;
            }
          }
          return true;
        },
        isLowerCaseOrDigitOrUnderscoreOrDash: function(s) {
          for (var i = 0; i < s.length; i= i+1) {
            var c = s.charAt(i);
            if ( !((c>="a") && (c<="z") || c == '-' || c == '_' || (c>="0") && (c<="9"))) {
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
    var url = nxthemesBasePath + "/nxthemes-editor/delete_element";
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             id: element_id
         },
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
    var srcId = info.source.getAttribute('id');
    var destId = info.target.getAttribute('id');
    var order = info.order;
    var url = nxthemesBasePath + "/nxthemes-editor/move_element";
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             src_id: srcId,
             dest_id: destId,
             order: order
         },
         onComplete: function(r) {
             NXThemesEditor.refreshCanvas();
         }
    });
};

NXThemesEditor.addFragment = function(typeName, destId) {
    var url = nxthemesBasePath + "/nxthemes-editor/insert_fragment";
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             type_name: typeName,
             dest_id: destId
         },
         onComplete: function(r) {
             NXThemesEditor.refreshCanvas();
             NXThemesEditor.switchToEditCanvas()
         }
    });
    return false;
};

NXThemesEditor.editElement = function(info) {
    var id = info.target.getAttribute('id');
    var url = nxthemesBasePath + "/nxthemes-editor/select_element";
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             'id': id
         },
         onComplete: function(r) {
             NXThemes.getControllerById("editor perspectives").switchTo("edit element");
             NXThemes.getViewById("element editor tabs").switchTo("element editor perspectives/edit properties");             
         }
    });
};

NXThemesEditor.insertFragment = function(info) {
    var id = info.target.getAttribute('id');
    var url = nxthemesBasePath + "/nxthemes-editor/select_element";
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             'id': id
         },
         onComplete: function(r) {
             NXThemes.getControllerById("editor perspectives").switchTo("add fragments");
         }
    });
};

NXThemesEditor.changeElementStyle = function(info) {
    var id = info.target.getAttribute('id');
    var url = nxthemesBasePath + "/nxthemes-editor/select_element";

    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             'id': id
         },
         onComplete: function(r) {
             NXThemes.getControllerById("editor perspectives").switchTo("edit element");
             NXThemes.getViewById("element editor tabs").switchTo("element editor perspectives/edit style");
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
    var url = nxthemesBasePath + "/nxthemes-editor/update_element_width"; 
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             'id': id,
             'width': width
         },
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
    var url = nxthemesBasePath + "/nxthemes-editor/update_element_properties"; 
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
    var url = nxthemesBasePath + "/nxthemes-editor/update_element_widget"; 
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             'id': id,
             'view_name': viewName,
         },
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
    var url = nxthemesBasePath + "/nxthemes-editor/update_element_description"; 
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             'id': id,
             'description': description
         },
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
    
    var url = nxthemesBasePath + "/nxthemes-editor/update_element_style"; 
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
    var url = nxthemesBasePath + "/nxthemes-editor/update_element_style_css"; 
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
    var url = nxthemesBasePath + "/nxthemes-editor/update_element_visibility"; 
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             'id': id,
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
    var url = nxthemesBasePath + "/nxthemes-editor/update_element_widget"; 
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             'id': id,
             'view_name': viewName
         },
         onComplete: function(r) {
             NXThemesEditor.refreshCanvas();
         }
    });
};

NXThemesEditor.copyElement = function(info) {
    var id =  info.target.getAttribute('id');
    var url = nxthemesBasePath + "/nxthemes-editor/copy_element"; 
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
    var url = nxthemesBasePath + "/nxthemes-editor/split_element"; 
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             id: id
         },
         onComplete: function(r) {
             NXThemesEditor.refreshCanvas();
         }
    });       
};

NXThemesEditor.setElementPadding = function(info) {
    var id = info.target.getAttribute('id');
    var url = nxthemesBasePath + "/nxthemes-editor/select_element"; 
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             'id': id
         },
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
        propertyMap.set(i.name, value);
    });    
    var url = nxthemesBasePath + "/nxthemes-editor/update_element_layout"; 
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             property_map: propertyMap.toJSON()
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
    var url = nxthemesBasePath + "/nxthemes-editor/align_element"; 
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             id: id,
             position: position
         },
         onComplete: function(r) {
           NXThemesEditor.refreshCanvas();
         }
    });     
};

NXThemesEditor.duplicateElement = function(info) {
    var id =  info.target.getAttribute('id');
    var url = nxthemesBasePath + "/nxthemes-editor/duplicate_element"; 
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             id: id
         },
         onComplete: function(r) {
             NXThemes.getViewById("style css").hide();
             NXThemes.getViewById("style css").show();
             NXThemesEditor.refreshCanvas();
         }
    });       
};

NXThemesEditor.pasteElement = function(info) {
    var destId =  info.target.getAttribute('id');
    var url = nxthemesBasePath + "/nxthemes-editor/paste_element"; 
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             dest_id: destId
         },
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
    var url = nxthemesBasePath + "/nxthemes-editor/delete_element";
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             id: id
         },
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
        window.alert("Theme names cannot be empty.");
        return "";
    }
    if (!NXThemesEditor.isLowerCaseOrDigitOrUnderscoreOrDash(name)) {
        window.alert("Theme names may only contain lower-case alpha-numeric characters, digits, underscores and dashes");
        return "";
    }
    var url = nxthemesBasePath + "/nxthemes-editor/add_theme";
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             name: name
         },
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
        window.alert("Page names cannot be empty.");
        return "";
    }
    if (!NXThemesEditor.isLowerCaseOrDigitOrUnderscoreOrDash(name)) {
        window.alert("Page names may only contain lower-case alpha-numeric characters, digits, underscores and dashes.");
        return "";
    }
    var url = nxthemesBasePath + "/nxthemes-editor/add_page";
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             path: themeName + '/' + name
         },
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
    if (typeof NXThemesStyleEditor != 'undefined') {
      NXThemesStyleEditor.closeStylePicker();
    }
    NXThemes.getControllerById('editor buttons').select('edit canvas');
    NXThemes.getControllerById('editor perspectives').switchTo('edit canvas');
};

NXThemesEditor.backToCanvas = function() {
    NXThemesEditor.switchToEditCanvas();
}

NXThemesEditor.addPreset = function(themeName, category, view_id) {
    var name = prompt("Please enter a preset name:", "");
    if (name === "") {
        window.alert("Preset names cannot be empty.");
        return "";
    }
    if (!NXThemesEditor.isLowerCaseOrSpace(name)) {
        window.alert("Preset names may only contain lower case characters and spaces.");
        return "";
    }
    var url = nxthemesBasePath + "/nxthemes-editor/add_preset";
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             theme_name: themeName,
             preset_name: name,
             category: category
         },
         onComplete: function(r) {
             var text = r.responseText;
             if (text === "") {
                 window.alert("The preset name is already taken.");
             } else {
                 NXThemes.getViewById(view_id).refresh();
             }
         }
    });     
};

NXThemesEditor.editPreset = function(themeName, presetName, value, view_id) {
    var value = prompt("Enter a preset value:", value);
    var url = nxthemesBasePath + "/nxthemes-editor/edit_preset";
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             theme_name: themeName,
             preset_name: presetName,
             value: value
         },
         onComplete: function(r) {
             NXThemes.getViewById(view_id).refresh();
         }
    });      
};
    
NXThemesEditor.addSection = function(info) {
    var target = Event.element(info);
    var id = target.getAttribute('sectionid') || target.getAttribute('pageid');
    var url = nxthemesBasePath + "/nxthemes-editor/insert_section_after";
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             id: id
         },         
         onComplete: function(r) {
             NXThemesEditor.refreshCanvas();
         }
    });
};

NXThemesEditor.alignSection = function(info, position) {
    var target = Event.element(info);
    var id = target.getAttribute('sectionid');;
    var url = nxthemesBasePath + "/nxthemes-editor/align_element"; 
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             id: id,
             position: position
         }, 
         onComplete: function(r) {
           NXThemesEditor.refreshCanvas();
         }
    }); 
};

NXThemesEditor.alignSectionLeft = function(info) {
    NXThemesEditor.alignSection(info, "left");
}

NXThemesEditor.alignSectionCenter = function(info) {
    NXThemesEditor.alignSection(info, "center");
}

NXThemesEditor.alignSectionRight = function(info) {
    NXThemesEditor.alignSection(info, "right");
}


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
    var url = nxthemesBasePath + "/nxthemes-editor/select_style_category"; 
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             category: category
         },          
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
      var url = nxthemesBasePath + "/nxthemes-editor/assign_style_property"; 
      new Ajax.Request(url, {
         method: 'post',
         parameters: {
             element_id: element_id,
             property: property,
             value: value
         }, 
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
    var url = nxthemesBasePath + "/nxthemes-editor/select_preset_group"; 
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             group: group
         }, 
         onComplete: function(r) {
           NXThemes.getViewById("area style chooser").refresh();
         }
    });
};

NXThemesEditor.closeAreaStyleChooser = function() {
    NXThemes.getControllerById('area style perspectives').switchTo('default');
};

NXThemesEditor.refresh = function() {
  var url = nxthemesBasePath + "/nxthemes-editor/expire_themes"; 
  new Ajax.Request(url, {
         method: 'post',
         onComplete: function(r) {
             NXThemesEditor.refreshCanvas();
             NXThemesEditor.writeMessage("Canvas refreshed.");
         }
  });
};

NXThemesEditor.exit = function() {
  NXThemes.expireCookie("nxthemes.theme");
  NXThemes.expireCookie("nxthemes.engine");
  NXThemes.expireCookie("nxthemes.mode");
  NXThemes.expireCookie("nxthemes.perspective");
  var url = nxthemesBasePath + "/nxthemes-editor/clear_selections"; 
  new Ajax.Request(url, {
         method: 'post',
         onComplete: function(r) {
             window.location.reload();
         }
  });
};

NXThemesEditor.repairTheme = function(themeName) {
    var url = nxthemesBasePath + "/nxthemes-editor/repair_theme"; 
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             theme: themeName
         },
         onComplete: function(r) {
             NXThemes.getViewById("theme manager").refresh();
             NXThemesEditor.writeMessage("Theme repaired.");
         }
    });
};

NXThemesEditor.loadTheme = function(src) {
    var ok = confirm("Unsaved changes will be lost, are you sure?");
    if (!ok) {
        return;
    }
    var url = nxthemesBasePath + "/nxthemes-editor/load_theme"; 
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             src: src
         },
         onComplete: function(r) {
           var text = r.responseText;
           var msg = text ? "Theme loaded." : "The theme could not be loaded.";
           NXThemes.getViewById("theme manager").refresh();
           NXThemesEditor.writeMessage(msg);
         }
    });
};

NXThemesEditor.saveTheme = function(src, indent) {
    var url = nxthemesBasePath + "/nxthemes-editor/save_theme"; 
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             src: src,
             indent: indent
         },
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
    'align section left': NXThemesEditor.alignSectionLeft,
    'align section center': NXThemesEditor.alignSectionCenter,
    'align section right': NXThemesEditor.alignSectionRight,        
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
