
import org.nuxeo.theme.Manager
import org.nuxeo.theme.elements.ElementFormatter

Element element = Context.runScript("getSelectedElement.groovy")

FormatType widgetType = (FormatType) Manager.getTypeRegistry().lookup(TypeFamily.FORMAT, "widget")
return (Widget) ElementFormatter.getFormatByType(element, widgetType)
