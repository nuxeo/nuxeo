
import org.nuxeo.theme.Manager
import org.nuxeo.theme.editor.PaddingInfo
import org.nuxeo.theme.elements.ElementFormatter
import org.nuxeo.theme.formats.layouts.Layout

Element element = Context.runScript("getSelectedElement.groovy")

top = ""
bottom = ""
left = ""
right = ""

if (element != null) {
    Layout layout = (Layout) ElementFormatter.getFormatFor(element, "layout")
    top = layout.getProperty("padding-top")
    bottom = layout.getProperty("padding-bottom")
    left = layout.getProperty("padding-left")
    right = layout.getProperty("padding-right")
}

return new PaddingInfo(top, bottom, left, right)
