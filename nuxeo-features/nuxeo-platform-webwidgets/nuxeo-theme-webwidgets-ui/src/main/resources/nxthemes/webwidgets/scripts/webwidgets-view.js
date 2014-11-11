
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
    var area = model.area;
    var mode = model.mode;

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
             var container = panel.parentNode;
             NXThemesWebWidgets.renderPanel(container, panel_data);
             container.removeChild(panel); 
         },
         onFailure: function(r) {
             var text = r.responseText;
             window.alert(text);
         }
    });

  });
};


// Initialization

Event.observe(window, "load", NXThemesWebWidgets.init);
