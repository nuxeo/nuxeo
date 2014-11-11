
import org.nuxeo.theme.Manager
import org.nuxeo.theme.elements.ElementFormatter
import org.nuxeo.theme.elements.Element
import org.nuxeo.theme.formats.FormatType
import org.nuxeo.theme.formats.widgets.Widget
import org.nuxeo.theme.types.TypeFamily

Element element = Context.runScript("getSelectedElement.groovy")

FormatType widgetType = (FormatType) Manager.getTypeRegistry().lookup(TypeFamily.FORMAT, "widget")
return (Widget) ElementFormatter.getFormatByType(element, widgetType)
