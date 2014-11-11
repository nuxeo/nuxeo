selected_element_id = Context.runScript("getSelectedElement.groovy")

Element selectedElement = Manager.getElementById(selected_element_id);
FormatType widgetType = (FormatType) Manager.getTypeRegistry().lookup(TypeFamily.FORMAT, "widget");
return (Widget) ElementFormatter.getFormatByType(selectedElement, widgetType);
