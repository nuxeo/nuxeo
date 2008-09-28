
import org.nuxeo.theme.Manager
import org.nuxeo.theme.editor.Events
import org.nuxeo.theme.elements.CellElement
import org.nuxeo.theme.elements.Element
import org.nuxeo.theme.elements.ElementFactory
import org.nuxeo.theme.elements.ElementFormatter
import org.nuxeo.theme.elements.PageElement
import org.nuxeo.theme.elements.SectionElement
import org.nuxeo.theme.elements.ThemeElement
import org.nuxeo.theme.events.EventContext
import org.nuxeo.theme.events.EventManager
import org.nuxeo.theme.formats.Format
import org.nuxeo.theme.formats.FormatFactory
import org.nuxeo.theme.formats.FormatType
import org.nuxeo.theme.formats.layouts.Layout
import org.nuxeo.theme.formats.styles.Style
import org.nuxeo.theme.formats.widgets.Widget
import org.nuxeo.theme.fragments.Fragment
import org.nuxeo.theme.fragments.FragmentFactory
import org.nuxeo.theme.themes.ThemeManager

id = Request.getParameter("id")


ThemeManager themeManager = Manager.getThemeManager()

Element element = ThemeManager.getElementById(id)
Element newSection = ElementFactory.create("section")
Element newCell = ElementFactory.create("cell")

// section
Format sectionWidget = FormatFactory.create("widget")
sectionWidget.setName("section frame")
themeManager.registerFormat(sectionWidget)
Format sectionLayout = FormatFactory.create("layout")
sectionLayout.setProperty("width", "100%")
themeManager.registerFormat(sectionLayout)
Format sectionStyle = FormatFactory.create("style")
themeManager.registerFormat(sectionStyle)

ElementFormatter.setFormat(newSection, sectionWidget)
ElementFormatter.setFormat(newSection, sectionLayout)
ElementFormatter.setFormat(newSection, sectionStyle)

// cell
Format cellWidget = FormatFactory.create("widget")
cellWidget.setName("cell frame")
themeManager.registerFormat(cellWidget)
Format cellLayout = FormatFactory.create("layout")
themeManager.registerFormat(cellLayout)
cellLayout.setProperty("width", "100%")
Format cellStyle = FormatFactory.create("style")
themeManager.registerFormat(cellStyle)

ElementFormatter.setFormat(newCell, cellWidget)
ElementFormatter.setFormat(newCell, cellLayout)
ElementFormatter.setFormat(newCell, cellStyle)

newSection.addChild(newCell)

elementTypeName = element.getElementType().getTypeName()
if (elementTypeName.equals("section")) {
    newSection.insertAfter(element)
} else if (elementTypeName.equals("page")) {
    element.addChild(newSection)
}

EventManager eventManager = Manager.getEventManager()
eventManager.notify(Events.THEME_MODIFIED_EVENT, new EventContext(newSection, null))

