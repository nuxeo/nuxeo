import org.nuxeo.theme.Manager
import org.nuxeo.theme.themes.ThemeManager
import org.nuxeo.theme.elements.Element
import org.nuxeo.theme.fragments.Fragment
import org.nuxeo.theme.fragments.FragmentFactory
import org.nuxeo.theme.elements.ThemeElement
import org.nuxeo.theme.elements.PageElement
import org.nuxeo.theme.elements.ElementFactory
import org.nuxeo.theme.formats.FormatFactory
import org.nuxeo.theme.elements.ElementFormatter
import org.nuxeo.theme.formats.Format
import org.nuxeo.theme.editor.Events
import org.nuxeo.theme.events.EventContext
import org.nuxeo.theme.events.EventManager

dest_id = Request.getParameter("dest_id")
type_name = Request.getParameter("type_name")

ThemeManager themeManager = Manager.getThemeManager()
Element destElement = ThemeManager.getElementById(dest_id)

// create the new fragment
fragmentTypeName = type_name.split("/")[0]
Fragment fragment = FragmentFactory.create(fragmentTypeName)

// add a temporary view to the fragment
Format widget = FormatFactory.create("widget")
viewTypeName = type_name.split("/")[1]
widget.setName(viewTypeName)
themeManager.registerFormat(widget)
ElementFormatter.setFormat(fragment, widget)

// insert the fragment
destElement.addChild(fragment)

EventManager eventManager = Manager.getEventManager()
eventManager.notify(Events.THEME_MODIFIED_EVENT, new EventContext(fragment, destElement))

