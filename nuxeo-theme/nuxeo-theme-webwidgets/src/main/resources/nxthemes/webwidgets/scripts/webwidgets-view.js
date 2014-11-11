
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

    Seam.Component.getInstance("nxthemesWebWidgetManager").getPanelData(
      provider, region, mode, function(r) {
        var panel_data = r.evalJSON(true);
        var container = panel.parentNode;
        NXThemesWebWidgets.renderPanel(provider, decoration, container, panel_data);
        container.removeChild(panel);
    });

  });
};


// Initialization
Seam.Remoting.displayLoadingMessage = function() {};
Seam.Remoting.hideLoadingMessage = function() {};

// TODO remove later by registering a /seam/remoting/interface.js?nxthemesWebWidgetManager resource.
Seam.Remoting.type.nxthemesWebWidgetManager = function() {
  this.__callback = new Object();
  Seam.Remoting.type.nxthemesWebWidgetManager.prototype.getPanelData = function(p0, p1, p2, callback) {
    return Seam.Remoting.execute(this, "getPanelData", [p0, p1, p2], callback);
  }
}
Seam.Remoting.type.nxthemesWebWidgetManager.__name = "nxthemesWebWidgetManager";
Seam.Component.register(Seam.Remoting.type.nxthemesWebWidgetManager);


Event.observe(window, "load", NXThemesWebWidgets.init);
