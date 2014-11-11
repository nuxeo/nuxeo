
if (typeof NXThemesEditor == "undefined") {
    NXThemesEditor = {
        writeMessage: function(msg) {
          var box = $("nxthemesStatusMessage");
          box.innerHTML = msg;
          box.show();
          NXThemes.Effects.fadeout(box, {delay: 1700});
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
    Seam.Component.getInstance("nxthemesEditorAction").deleteElement(
       element_id,
      function(r) {
        if (r === "") {
          window.alert("Could not delete the element.");
        } else {
          NXThemes.getViewById("theme selector").refresh();
          NXThemesEditor.refreshCanvas();
        }
      });
};

NXThemesEditor.moveElement = function(info) {
    var src_id = info.source.getAttribute('id');
    var dest_id = info.target.getAttribute('id');
    var order = info.order;
    Seam.Component.getInstance("nxthemesEditorAction").moveElement(
        src_id, dest_id, order, NXThemesEditor.refreshCanvas);
};

NXThemesEditor.insertFragment = function(info) {
    var dest_id = info.target.getAttribute('id');
    var order = info.order;
    var type_name = info.source.getAttribute('typename');
    Seam.Component.getInstance("nxthemesEditorAction").insertFragment(
        type_name, dest_id, order, NXThemesEditor.refreshCanvas);
};

NXThemesEditor.editElement = function(info) {
    var id = info.target.getAttribute('id');
    Seam.Component.getInstance("nxthemesEditorAction").selectElement(id,
       function(r) {
         NXThemes.getControllerById("editor perspectives").switchTo("edit element");
         NXThemes.getViewById("element editor").refresh();
         var v = NXThemes.getViewById("element editor tabs");
         if (typeof v !== 'undefined') {
             v.switchTo("element editor perspectives/edit properties");
         }
       });
};

NXThemesEditor.changeElementStyle = function(info) {
    var id = info.target.getAttribute('id');
    Seam.Component.getInstance("nxthemesEditorAction").selectElement(id,
       function(r) {
         NXThemes.getControllerById("editor perspectives").switchTo("edit element");
         NXThemes.getViewById("element editor").refresh();
         var v = NXThemes.getViewById("element editor tabs");
         if (typeof v !== 'undefined') {
             v.switchTo("element editor perspectives/edit style");
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
    Seam.Component.getInstance("nxthemesEditorAction").setSize(
       id, width, NXThemesEditor.refreshCanvas);
};

NXThemesEditor.updateElementProperties = function(info) {
    var form = Event.findElement(info, "form");
    var id = null;
    var propertyMap = new Seam.Remoting.Map();
    $A(Form.getElements(form)).each(function(i) {
        var name = i.name;
        var value = Form.Element.getValue(i);
        if (name == "id") {
          id = value;
        } else {
          propertyMap.put(i.name, value);
        }
      });
    Seam.Component.getInstance("nxthemesEditorAction").updateElementProperties(
       id, propertyMap,
       function(r) {
         NXThemesEditor.writeMessage("Properties updated.");
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

    Seam.Component.getInstance("nxthemesEditorAction").updateElementWidget(
       id, viewName,
       function(r) {
         NXThemesEditor.writeMessage("Widget changed.");
       });
};

NXThemesEditor.updateElementStyle = function() {
    var form = $('nxthemesElementStyle');
    var id, path, viewName;
    var propertyMap = new Seam.Remoting.Map();
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
          propertyMap.put(name.substr(9), value);
        }
      });
    Seam.Component.getInstance("nxthemesEditorAction").updateElementStyle(
       id, viewName, path, propertyMap,
       function(r) {
         NXThemesStyleEditor.refreshCssPreview();
         NXThemesEditor.writeMessage("Style updated.");
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
    Seam.Component.getInstance("nxthemesEditorAction").updateElementStyleCss(
       id, viewName, cssSource,
      function(r) {
        NXThemesStyleEditor.refreshCssPreview();
        NXThemesEditor.writeMessage("Style updated.");
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
    Seam.Component.getInstance("nxthemesEditorAction").setElementVisibility(
       id, perspectives, alwaysVisible,
       function(r) {
         NXThemes.getViewById("element visibility").refresh();
         NXThemesEditor.writeMessage("Visibility changed.");
       });
};

NXThemesEditor.setElementWidget = function(info) {
    var id =  info.target.getAttribute('id');
    if (id === null) {
      return;
    }
    var viewName = info.options.choice;
    Seam.Component.getInstance("nxthemesEditorAction").updateElementWidget(
       id, viewName, NXThemesEditor.refreshCanvas);
};

NXThemesEditor.copyElement = function(info) {
    var id =  info.target.getAttribute('id');
    Seam.Component.getInstance("nxthemesEditorAction").copyElements(
       [id], function(r) {});
};

NXThemesEditor.splitElement = function(info) {
    var id =  info.target.getAttribute('id');
    Seam.Component.getInstance("nxthemesEditorAction").splitElement(
       id, NXThemesEditor.refreshCanvas);
};

NXThemesEditor.setElementPadding = function(info) {
    var id = info.target.getAttribute('id');
    Seam.Component.getInstance("nxthemesEditorAction").selectElement(id,
      function(r) {
        NXThemes.getControllerById("editor perspectives").switchTo("edit padding");
      });
};

NXThemesEditor.updateElementPadding = function(info) {
    var form = Event.findElement(info, "form");
    var id = null;
    var propertyMap = new Seam.Remoting.Map();
    $A(Form.getElements(form)).each(function(i) {
        var name = i.name;
        var value = Form.Element.getValue(i);
        propertyMap.put(i.name, value);
      });
    Seam.Component.getInstance("nxthemesEditorAction").updateElementLayout(
       propertyMap, NXThemesEditor.refreshCanvas);
};

NXThemesEditor.alignElement = function(info) {
    var target = info.target;
    var id =  target.getAttribute('id');
    if (id === null) {
      return;
    }
    var position = info.options.choice;
    Seam.Component.getInstance("nxthemesEditorAction").alignElement(
       id, position, NXThemesEditor.refreshCanvas);
};

NXThemesEditor.duplicateElement = function(info) {
    var id =  info.target.getAttribute('id');
    Seam.Component.getInstance("nxthemesEditorAction").duplicateElement(
       id, NXThemesEditor.refreshCanvas);
};

NXThemesEditor.pasteElement = function(info) {
    var dest_id =  info.target.getAttribute('id');
    Seam.Component.getInstance("nxthemesEditorAction").pasteElements(
       dest_id, NXThemesEditor.refreshCanvas);
};

NXThemesEditor.deleteElement = function(info) {
    var id =  info.target.getAttribute('id');
    Seam.Component.getInstance("nxthemesEditorAction").deleteElement(
       id, NXThemesEditor.refreshCanvas);
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
    Seam.Component.getInstance("nxthemesEditorAction").addTheme(name,
      function(r) {
        if (r === "") {
          window.alert("The theme name is already taken.");
        } else {
          NXThemes.setCookie("nxthemes.theme", r);
          NXThemes.getViewById("theme selector").refresh();
          NXThemesEditor.refreshCanvas();
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
    Seam.Component.getInstance("nxthemesEditorAction").addPage(themeName + '/' + name,
      function(r) {
        if (r === "") {
          window.alert("The page name is already taken.");
        } else {
          NXThemes.setCookie("nxthemes.theme", r);
          NXThemes.getViewById("theme selector").refresh();
          NXThemesEditor.refreshCanvas();
        }
      });
};

NXThemesEditor.backToCanvas = function() {
    NXThemes.getControllerById('editor buttons').select('edit canvas');
    NXThemes.getControllerById('editor perspectives').switchTo('edit canvas');
};

NXThemesEditor.addSection = function(info) {
    var target = Event.element(info);
    var id = target.getAttribute('sectionid') || target.getAttribute('pageid');
    Seam.Component.getInstance("nxthemesEditorAction").insertSectionAfter(
       id, NXThemesEditor.refreshCanvas);
};

NXThemesEditor.alignSection = function(info) {
    var target = Event.element(info);
    var id = target.getAttribute('sectionid');
    var position = target.getAttribute('position');
    Seam.Component.getInstance("nxthemesEditorAction").alignElement(
       id, position, NXThemesEditor.refreshCanvas);
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
    Seam.Component.getInstance("nxthemesEditorAction").setStyleCategory(category,
      function(r) {
        NXThemes.getControllerById('area style perspectives').switchTo('style chooser');
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
      Seam.Component.getInstance("nxthemesEditorAction").assignStyleProperty(element_id, property,
          value,
      function(r) {
        NXThemes.getViewById("style css").hide();
        NXThemes.getViewById("style css").show();
        NXThemesEditor.refreshCanvas();
      });
  }
};

NXThemesEditor.setPresetGroup = function(select) {
  Seam.Component.getInstance("nxthemesEditorAction").setPresetGroup(select.value,
    function(r) {
      NXThemes.getViewById("area style chooser").refresh();
    });
};

NXThemesEditor.closeAreaStyleChooser = function() {
    NXThemes.getControllerById('area style perspectives').switchTo('default');
};

NXThemesEditor.refresh = function() {
  Seam.Component.getInstance("nxthemesEditorAction").expireThemes(
        NXThemesEditor.refreshCanvas);
  NXThemesEditor.writeMessage("Canvas refreshed.");
};

NXThemesEditor.exit = function() {
  NXThemes.expireCookie("nxthemes.theme");
  NXThemes.expireCookie("nxthemes.engine");
  NXThemes.expireCookie("nxthemes.perspective");
  window.location.reload();
};

NXThemesEditor.repairTheme = function(themeName) {
    Seam.Component.getInstance("nxthemesEditorAction").repairTheme(
       themeName,
    function(r) {
      NXThemes.getViewById("theme manager").refresh();
      NXThemesEditor.writeMessage("Theme repaired.");
    });
};

NXThemesEditor.loadTheme = function(src) {
    Seam.Component.getInstance("nxthemesEditorAction").loadTheme(
       src,
      function(r) {
        var msg = r ? "Theme loaded." : "The theme could not be loaded.";
        NXThemes.getViewById("theme manager").refresh();
        NXThemesEditor.writeMessage(msg);
      });
};

NXThemesEditor.saveTheme = function(src) {
    Seam.Component.getInstance("nxthemesEditorAction").saveTheme(
       src,
      function(r) {
        var msg = r ? "Theme saved." : "The theme could not be saved.";
        NXThemes.getViewById("theme manager").refresh();
        NXThemesEditor.writeMessage(msg);
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
NXThemes.setContextPath('/nuxeo');

NXThemes.addActions({
    'move element': NXThemesEditor.moveElement,
    'insert fragment': NXThemesEditor.insertFragment,
    'edit element': NXThemesEditor.editElement,
    'duplicate element': NXThemesEditor.duplicateElement,
    'update element properties': NXThemesEditor.updateElementProperties,
    'update element widget': NXThemesEditor.updateElementWidget,
    'update element style': NXThemesEditor.updateElementStyle,
    'update element style css': NXThemesEditor.updateElementStyleCss,
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
