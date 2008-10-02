
import org.nuxeo.theme.Manager
import org.nuxeo.theme.themes.ThemeManager
import org.nuxeo.theme.elements.ThemeElement
import org.nuxeo.theme.elements.PageElement
import org.nuxeo.theme.elements.ElementFactory
import org.nuxeo.theme.formats.FormatFactory
import org.nuxeo.theme.elements.ElementFormatter
import org.nuxeo.theme.formats.Format

ThemeManager themeManager = Manager.getThemeManager()

path = Request.getParameter("path")

if (!path.contains("/")) {
    return ""
}

if (themeManager.getPageByPath(path) != null) {
    return ""
}

themeName = path.split("/")[0]
ThemeElement theme = themeManager.getThemeByName(themeName)

// add page
PageElement page = (PageElement) ElementFactory.create("page")
pageName = path.split("/")[1]
page.setName(pageName)
Format pageWidget = FormatFactory.create("widget")
pageWidget.setName("page frame")
themeManager.registerFormat(pageWidget)
Format pageLayout = FormatFactory.create("layout")
themeManager.registerFormat(pageLayout)
Format pageStyle = FormatFactory.create("style")
themeManager.registerFormat(pageStyle)
ElementFormatter.setFormat(page, pageWidget)
ElementFormatter.setFormat(page, pageStyle)
ElementFormatter.setFormat(page, pageLayout)

themeManager.registerPage(theme, page)
Response.writer.write(path)

