
Element element = ThemeManager.getElementById(id)
Properties properties = new Properties()
for (Object key : propertyMap.keySet()) {
    properties.put(key, propertyMap.get(key))
}

FormatType styleType = (FormatType) Manager.getTypeRegistry().lookup(TypeFamily.FORMAT, "style")
Style style = (Style) ElementFormatter.getFormatByType(element,styleType)
Style currentStyleLayer = uiStates.getCurrentStyleLayer()
if (currentStyleLayer != null) {
    style = currentStyleLayer
}

if (style.getName() != null || "".equals(viewName)) {
    viewName = "*"
}

style.setPropertiesFor(viewName, path, properties)
eventManager.notify(Events.STYLES_MODIFIED_EVENT, new EventContext(element, null))
