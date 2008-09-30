
selected_element_id = Context.runScript("getSelectedElement.groovy")
selected_layer_id = Context.runScript("getSelectedStyleLayer.groovy")

Element selectedElement = Manager.getElementById(selected_element_id);
FormatType styleType = (FormatType) Manager.getTypeRegistry().lookup(TypeFamily.FORMAT, "style");
Style style = (Style) ElementFormatter.getFormatByType(selectedElement, styleType);

Style currentStyleLayer = Manager.get ...

if (currentStyleLayer != null) {
    style = currentStyleLayer;
}
if (style == null) {
    return "";
}

StringBuilder css = new StringBuilder();
// TODO use Utils.styleToCss()

List<Style> styles = new ArrayList<Style>();
for (Format ancestor : ThemeManager.listAncestorFormatsOf(style)) {
    styles.add(0, (Style) ancestor);
}
styles.add(style);

currentViewName = uiManager.getCurrentViewName();
for (Style s : styles) {
    viewName = currentViewName;
    if (s.getName() != null) {
viewName = "*";
    }
    for (path : s.getPathsForView(viewName)) {
css.append('#').append(cssPreviewId);
css.append(' ').append(path).append(" {");

Properties styleProperties = s.getPropertiesFor(viewName,
path);
Enumeration<?> propertyNames = Utils.getCssProperties().propertyNames();
while (propertyNames.hasMoreElements()) {
    propertyName = (String) propertyNames.nextElement();
    value = styleProperties.getProperty(propertyName);
    if (value == null) {
continue;
    }
    css.append(propertyName);
    css.append(':');
    PresetType preset = ThemeManager.resolvePreset(value);
    if (preset != null) {
value = preset.getValue();
    }
    css.append(value);
    css.append(';');
}
css.append('}');
    }
}
return css.toString();
