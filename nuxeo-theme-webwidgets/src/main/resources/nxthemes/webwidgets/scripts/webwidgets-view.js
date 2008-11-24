
NXThemesWebWidgets.deleteWidget = function(widgetUid) {
  alert("This function is not available in view mode.");
};

NXThemesWebWidgets.setWidgetState = function(widgetUid, mode, state) {
  alert("This function is not available in view mode.");
};

NXThemesWebWidgets.editPreferences = function(widgetUid) {
  alert("This function is not available in view mode.");
};

NXThemesWebWidgets.init = function() {
  var panels = $A(document.getElementsByTagName("ins")).select(function(e) {
    return (e.className.match(new RegExp("nxthemesWebWidgetPanel")));
  });
  panels.each(function(panel) {
    var text = panel.innerHTML;
    var model = text.evalJSON(true);
    var provider = model.provider;
    var region = model.region;
    var decoration = model.decoration;
    var mode = model.mode;

    var url = webEngineContextPath + "/nxthemes-webwidgets/get_panel_data";
    new Ajax.Request(url, {
         method: 'get',
         parameters: {
             'provider': provider,
             'region': region,
             'mode': mode
         },
         onComplete: function(r) {
             var text = r.responseText;
             var panel_data = r.evalJSON(true);
             var container = panel.parentNode;
             NXThemesWebWidgets.renderPanel(provider, decoration, container, panel_data);
             container.removeChild(panel);
         }
    });

  });
};


// Initialization

Event.observe(window, "load", NXThemesWebWidgets.init);
