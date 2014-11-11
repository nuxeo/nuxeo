import org.nuxeo.theme.Manager
import org.nuxeo.theme.elements.ElementFormatter
import org.nuxeo.theme.elements.Element
import org.nuxeo.theme.formats.FormatType
import org.nuxeo.theme.formats.styles.Style
import org.nuxeo.theme.types.TypeFamily

Element element = Context.runScript("getSelectedElement.groovy")

FormatType styleType = (FormatType) Manager.getTypeRegistry().lookup(TypeFamily.FORMAT, "style")
return (Style) ElementFormatter.getFormatByType(element, styleType)
