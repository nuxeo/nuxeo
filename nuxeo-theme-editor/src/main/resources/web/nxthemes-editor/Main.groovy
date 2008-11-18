package nxthemesEditor

import java.io.*
import javax.ws.rs.*
import javax.ws.rs.core.*
import java.util.regex.Matcher
import java.util.regex.Pattern
import org.nuxeo.ecm.core.rest.*
import org.nuxeo.ecm.webengine.model.*
import org.nuxeo.ecm.webengine.model.impl.*
import org.nuxeo.ecm.webengine.model.exceptions.*
import org.nuxeo.ecm.webengine.*
import org.nuxeo.theme.*
import org.nuxeo.theme.elements.*
import org.nuxeo.theme.formats.*
import org.nuxeo.theme.formats.widgets.*
import org.nuxeo.theme.formats.styles.*
import org.nuxeo.theme.formats.layouts.*
import org.nuxeo.theme.events.*
import org.nuxeo.theme.fragments.*
import org.nuxeo.theme.presets.*
import org.nuxeo.theme.templates.*
import org.nuxeo.theme.themes.*
import org.nuxeo.theme.types.*
import org.nuxeo.theme.perspectives.*
import org.nuxeo.theme.uids.*
import org.nuxeo.theme.views.*
import org.nuxeo.theme.editor.*

@WebModule(name="nxthemes-editor")

@Path("/nxthemes-editor")
@Produces(["text/html", "*/*"])
public class Main extends DefaultModule {
    
    @GET @POST
    @Path("perspectiveSelector")
    public Object renderPerspectiveSelector(@QueryParam("org.nuxeo.theme.application.path") String path) {
      return getTemplate("perspectiveSelector.ftl").arg("perspectives", getPerspectives())
    }

    @GET @POST
    @Path("themeSelector")
    public Object renderThemeSelector(@QueryParam("org.nuxeo.theme.application.path") String path) {
      return getTemplate("themeSelector.ftl").arg(
              "current_theme_name", getCurrentThemeName(path)).arg(
              "themes", getThemes(path)).arg(
              "pages", getPages(path))
    }

  @GET @POST
  @Path("canvasModeSelector")
  public Object renderCanvasModeSelector(@QueryParam("org.nuxeo.theme.application.path") String path) {
    return getTemplate("canvasModeSelector.ftl")
  }

  @GET @POST
  @Path("backToCanvas")
  public Object renderBackToCanvas(@QueryParam("org.nuxeo.theme.application.path") String path) {
    return getTemplate("backToCanvas.ftl")
  }

  @GET @POST
  @Path("themeManager")
  public Object renderThemeManager(@QueryParam("org.nuxeo.theme.application.path") String path) {
    return getTemplate("themeManager.ftl").arg("themes", getThemeDescriptors())
  }

  @GET @POST
  @Path("fragmentFactory")
  public Object renderFragmentFactory(@QueryParam("org.nuxeo.theme.application.path") String path) {
    return getTemplate("fragmentFactory.ftl").arg("fragments", getFragments())
  }

  @GET @POST
  @Path("elementEditor")
  public Object renderElementEditor(@QueryParam("org.nuxeo.theme.application.path") String path) {
    return getTemplate("elementEditor.ftl").arg("selected_element", getSelectedElement())
  }

  @GET @POST
  @Path("elementDescription")
  public Object renderElementDescription(@QueryParam("org.nuxeo.theme.application.path") String path) {
    return getTemplate("elementDescription.ftl").arg("selected_element", getSelectedElement())
  }

  @GET @POST
  @Path("elementPadding")
  public Object renderElementPadding(@QueryParam("org.nuxeo.theme.application.path") String path) {
    return getTemplate("elementPadding.ftl").arg(
            "selected_element", getSelectedElement()).arg(
            "padding_of_selected_element", getPaddingOfSelectedElement())
  }

  @GET @POST
  @Path("elementProperties")
  public Object renderElementProperties(@QueryParam("org.nuxeo.theme.application.path") String path) {
    return getTemplate("elementProperties.ftl").arg(
            "selected_element", getSelectedElement()).arg(
            "element_properties", getSelectedElementProperties())
  }

  @GET @POST
  @Path("elementStyle")
  public Object renderElementStyle(@QueryParam("org.nuxeo.theme.application.path") String path) {
    return getTemplate("elementStyle.ftl").arg("selected_element", getSelectedElement()).arg(
            "selected_view_name", getViewNameOfSelectedElement()).arg(
            "style_edit_mode", getStyleEditMode()).arg(
            "style_of_selected_element", getStyleOfSelectedElement()).arg(
            "current_theme_name", getCurrentThemeName(path)).arg(
            "style_layers_of_selected_element", getStyleLayersOfSelectedElement()).arg(
            "inherited_style_name_of_selected_element", getInheritedStyleNameOfSelectedElement()).arg(
            "named_styles", getNamedStyles())
  }

  @GET @POST
  @Path("elementWidget")
  public Object renderElementWidget(@QueryParam("org.nuxeo.theme.application.path") String path) {
    return getTemplate("elementWidget.ftl").arg(
            "selected_element", getSelectedElement()).arg(
            "selected_view_name", getViewNameOfSelectedElement()).arg(
            "view_names_for_selected_element", getViewNamesForSelectedElement(path))
  }

  @GET @POST
  @Path("elementVisibility")
  public Object renderElementVisibility(@QueryParam("org.nuxeo.theme.application.path") String path) {
    return getTemplate("elementVisibility.ftl").arg(
            "selected_element", getSelectedElement()).arg(
            "is_selected_element_always_visible", isSelectedElementAlwaysVisible()).arg(
            "perspectives", getPerspectives())
  }

  @GET @POST
  @Path("stylePicker")
  public Object renderStylePicker(@QueryParam("org.nuxeo.theme.application.path") String path) {
    return getTemplate("stylePicker.ftl").arg(
            "style_category", getSelectedStyleCategory()).arg(
            "preset_groups", getPresetGroupsForSelectedCategory()).arg(
            "presets_for_selected_group", getPresetsForSelectedGroup())
  }
  
  @GET @POST
  @Path("areaStyleChooser")
  public Object renderAreaStyleChooser(@QueryParam("org.nuxeo.theme.application.path") String path) {
    return getTemplate("areaStyleChooser.ftl").arg(
            "style_category", getSelectedStyleCategory()).arg(
            "preset_groups", getPresetGroupsForSelectedCategory()).arg(
            "presets_for_selected_group", getPresetsForSelectedGroup()).arg(
            "selected_preset_group", getSelectedPresetGroup())
  }

  @GET @POST
  @Path("styleProperties")
  public Object renderStyleProperties(@QueryParam("org.nuxeo.theme.application.path") String path) {
    return getTemplate("styleProperties.ftl").arg(
            "style_layers_of_selected_element", getStyleLayersOfSelectedElement()).arg(
            "style_selectors", getStyleSelectorsForSelectedElement()).arg(
            "rendered_style_properties", getRenderedStylePropertiesForSelectedElement()).arg(
            "selected_style_selector", getSelectedStyleSelector()).arg(
            "style_properties", getStylePropertiesForSelectedElement()).arg(
            "style_categories", getStyleCategories()).arg(
            "element_style_properties", getElementStyleProperties())
  }
  
  @GET @POST
  @Path("select_element")
  public void selectElement(@QueryParam("id") String id) {
    def ctx = WebEngine.getActiveContext()
    SessionManager.setSelectedElementId(ctx, id)
  }
  
  @GET @POST
  @Path("add_page")
  public void addPage(@QueryParam("path") String path) {
    ThemeManager themeManager = Manager.getThemeManager()
    if (!path.contains("/")) {
        return
    }
    if (themeManager.getPageByPath(path) != null) {
        return
    }
    String themeName = path.split("/")[0]
    ThemeElement theme = themeManager.getThemeByName(themeName)
    PageElement page = (PageElement) ElementFactory.create("page")
    String pageName = path.split("/")[1]
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
    Response.writer(path)
  }
  
  @GET @POST
  @Path("add_theme")
  public void addTheme(@QueryParam("name") String name) {
      ThemeManager themeManager = Manager.getThemeManager()
      res = ""
      if (themeManager.getThemeByName(name) == null) {
          ThemeElement theme = (ThemeElement) ElementFactory.create("theme")
          theme.setName(name)
          Format themeWidget = FormatFactory.create("widget")
          themeWidget.setName("theme view")
          themeManager.registerFormat(themeWidget)
          ElementFormatter.setFormat(theme, themeWidget)
          // default page
          PageElement page = (PageElement) ElementFactory.create("page")
          page.setName("default")
          Format pageWidget = FormatFactory.create("widget")
          themeManager.registerFormat(pageWidget)
          pageWidget.setName("page frame")
          Format pageLayout = FormatFactory.create("layout")
          themeManager.registerFormat(pageLayout)
          Format pageStyle = FormatFactory.create("style")
          themeManager.registerFormat(pageStyle)
          ElementFormatter.setFormat(page, pageWidget)
          ElementFormatter.setFormat(page, pageStyle)
          ElementFormatter.setFormat(page, pageLayout)
          theme.addChild(page)
          themeManager.registerTheme(theme)
          res = String.format("%s/%s", name, "default")
      }
      Response.writer(res)
  }
  
  @GET @POST
  @Path("align_element")
  public void alignElement(@QueryParam("id") String id, @QueryParam("position") String position) {
      Element element = ThemeManager.getElementById(id)
      Layout layout = (Layout) ElementFormatter.getFormatFor(element, "layout")
      if (layout == null) {
          layout = (Layout) FormatFactory.create("layout")
          themeManager.registerFormat(layout)
          ElementFormatter.setFormat(element, layout)
      }
      if (element instanceof SectionElement) {
          if (position.equals("left")) {
              layout.setProperty("margin-left", "0")
              layout.setProperty("margin-right", "auto")
          } else if (position.equals("center")) {
              layout.setProperty("margin-left", "auto")
              layout.setProperty("margin-right", "auto")
          } else if (position.equals("right")) {
              layout.setProperty("margin-left", "auto")
              layout.setProperty("margin-right", "0")
          }
      } else {
          if (position.equals("left")) {
              layout.setProperty("text-align", "left")
          } else if (position.equals("center")) {
              layout.setProperty("text-align", "center")
          } else if (position.equals("right")) {
              layout.setProperty("text-align", "right")
          }
      }
      EventManager eventManager = Manager.getEventManager()
      eventManager.notify(Events.THEME_MODIFIED_EVENT, new EventContext(element, null))
  }
  
  
  @GET @POST
  @Path("assign_style_property")
  public void assignStyleProperty(@QueryParam("element_id") String id, @QueryParam("property") String property_name, @QueryParam("value") String value) {
      Element element = ThemeManager.getElementById(id)
      if (element == null) {
          return
      }
      Style style = (Style) ElementFormatter.getFormatFor(element, "style")
      if (style == null) {
          style = (Style) FormatFactory.create("style")
          Manager.getThemeManager().registerFormat(style)
          ElementFormatter.setFormat(element, style)
      }
      Widget widget = (Widget) ElementFormatter.getFormatFor(element, "widget")
      if (widget == null) {
          return
      }
      viewName = widget.getName()
      Properties properties = style.getPropertiesFor(viewName, "")
      if (properties == null) {
          properties = new Properties()
      }
      if (value) {
          properties.setProperty(property_name, value)
      } else {
          properties.remove(property_name)
      }
      style.setPropertiesFor(viewName, "", properties)
      EventManager eventManager = Manager.getEventManager()
      eventManager.notify(Events.THEME_MODIFIED_EVENT, new EventContext(element, null))
      eventManager.notify(Events.STYLES_MODIFIED_EVENT, new EventContext(style, null))
  }
  
  @GET @POST
  @Path("copy_element")
  public void copyElement(@QueryParam("id") String id) {
      def ctx = WebEngine.getActiveContext()
      SessionManager.setClipboardElementId(ctx, id)
  }
  
  @GET @POST
  @Path("create_named_style")
  public void createNamedStyle(@QueryParam("id") String id, @QueryParam("theme_name") String themeName, @QueryParam("style_name") String styleName) {
      ThemeManager themeManager = Manager.getThemeManager()
      if (themeManager.getNamedObject(themeName, "style", styleName) == null) {
          Style style = (Style) FormatFactory.create("style");
          style.setName(styleName);
          themeManager.setNamedObject(themeName, "style", style);
          themeManager.registerFormat(style);
      }
      themeManager.makeElementUseNamedStyle(id, styleName, themeName);
      EventManager eventManager = Manager.getEventManager()
      eventManager.notify(Events.THEME_MODIFIED_EVENT, new EventContext(element, null));
      eventManager.notify(Events.STYLES_MODIFIED_EVENT, new EventContext(element, null));
  }
  
  @GET @POST
  @Path("delete_element")
  public String deleteElement(@QueryParam("id") String id) {
      Element element = ThemeManager.getElementById(id)
      Element parent = (Element) element.getParent()
      ThemeManager themeManager = Manager.getThemeManager()
      if (element instanceof ThemeElement || element instanceof PageElement) {
          themeManager.destroyElement(element)
      } else if (element instanceof CellElement) {
          if (element.hasSiblings()) {
              Element sibling = (Element) element.getNextNode()
              if (sibling == null) {
                  sibling = (Element) element.getPreviousNode()
              }
              FormatType layoutType = (FormatType) Manager.getTypeRegistry().lookup(TypeFamily.FORMAT, "layout")
              Format layout1 = ElementFormatter.getFormatByType(element, layoutType)
              if (layout1 != null) {
                  width1 = layout1.getProperty("width")
                  if (width1 != null) {
                      Format layout2 = ElementFormatter.getFormatByType(sibling, layoutType)
                      if (layout2 != null) {
                          width2 = layout2.getProperty("width")
                          newWidth = Utils.addWebLengths(width1, width2)
                          if (newWidth != null) {
                              layout2.setProperty("width", newWidth)
                          }
                      }
                  }
              }
              // remove cell
              themeManager.destroyElement(element)
          } else {
              // remove parent section
              themeManager.destroyElement(parent)
          }
      } else if (element instanceof Fragment) {
          themeManager.destroyElement(element)
      }
      EventManager eventManager = Manager.getEventManager()
      eventManager.notify(Events.THEME_MODIFIED_EVENT, new EventContext(null, null))
      Response.writer(id)
  }
  
  @GET @POST
  @Path("delete_named_style")
  public void deleteNamedStyle(@QueryParam("id") String id, @QueryParam("theme_name") String themeName, @QueryParam("style_name") String styleName) {
      ThemeManager themeManager = Manager.getThemeManager()
      Style inheritedStyle = (Style) themeManager.getNamedObject(themeName, "style", styleName);
      themeManager.deleteFormat(inheritedStyle);
      themeManager.makeElementUseNamedStyle(id, null, themeName);
      themeManager.removeNamedObject(themeName, "style", styleName);
  }
  
  @GET @POST
  @Path("duplicate_element")
  public String duplicateElement(@QueryParam("id") String id) {
      Element element = ThemeManager.getElementById(id)
      Element duplicate = Manager.getThemeManager().duplicateElement(element, true)
      // insert the duplicated element
      element.getParent().addChild(duplicate)
      duplicate.moveTo(element.getParent(), element.getOrder() + 1)
      EventManager eventManager = Manager.getEventManager()
      eventManager.notify(Events.STYLES_MODIFIED_EVENT, new EventContext(null, null))
      eventManager.notify(Events.THEME_MODIFIED_EVENT, new EventContext(null, element))
      Response.writer(duplicate.getUid())
  }
  
  @GET @POST
  @Path("expire_themes")
  public void expireThemes() {
      EventManager eventManager = Manager.getEventManager()
      eventManager.notify(Events.THEME_MODIFIED_EVENT, new EventContext(null, null))
  }
  
  @GET @POST
  @Path("insert_fragment")
  public void insertFragment(@QueryParam("dest_id") String destId, @QueryParam("type_name") String typeName) {
      ThemeManager themeManager = Manager.getThemeManager()
      Element destElement = ThemeManager.getElementById(destId)
      // create the new fragment
      String fragmentTypeName = typeName.split("/")[0]
      Fragment fragment = FragmentFactory.create(fragmentTypeName)
      // add a temporary view to the fragment
      Format widget = FormatFactory.create("widget")
      String viewTypeName = typeName.split("/")[1]
      widget.setName(viewTypeName)
      themeManager.registerFormat(widget)
      ElementFormatter.setFormat(fragment, widget)
      // insert the fragment
      destElement.addChild(fragment)
      EventManager eventManager = Manager.getEventManager()
      eventManager.notify(Events.THEME_MODIFIED_EVENT, new EventContext(fragment, destElement))
  }
  
  @GET @POST
  @Path("insert_section_after")
  public void insertSectionAfter(@QueryParam("id") String id) {
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
      String elementTypeName = element.getElementType().getTypeName()
      if (elementTypeName.equals("section")) {
          newSection.insertAfter(element)
      } else if (elementTypeName.equals("page")) {
          element.addChild(newSection)
      }
      EventManager eventManager = Manager.getEventManager()
      eventManager.notify(Events.THEME_MODIFIED_EVENT, new EventContext(newSection, null))      
  }
  
  @GET @POST
  @Path("load_theme")
  public void loadTheme(@QueryParam("src") String src) {
      int res = 1
      try {
          Manager.getThemeManager().loadTheme(src)
      } catch (ThemeIOException e) {
          res = 0
      }
      if (res) {
          EventManager eventManager = Manager.getEventManager()
          eventManager.notify(Events.THEME_MODIFIED_EVENT, new EventContext(null, null))
          eventManager.notify(Events.STYLES_MODIFIED_EVENT, new EventContext(null, null))
      }
      Response.writer(res)
  }
  
  @GET @POST
  @Path("make_element_use_named_style")
  public void makeElementUseNamedStyle(@QueryParam("id") String id, @QueryParam("style_name") String styleName, @QueryParam("theme_name") String themeName) {
      Element element = ThemeManager.getElementById(id)
      FormatType styleType = (FormatType) Manager.getTypeRegistry().lookup(TypeFamily.FORMAT, "style")
      Style style = (Style) ElementFormatter.getFormatByType(element, styleType)
      ThemeManager themeManager = Manager.getThemeManager()
      // Make the style no longer inherits from other another style if 'inheritedName' is null
      if (inheritedName == null) {
          ThemeManager.removeInheritanceTowards(style)
      } else {
          String themeId = themeName.split("/")[0];
          Style inheritedStyle = (Style) themeManager.getNamedObject(themeId, "style", inheritedName)
          if (inheritedStyle != null) {
              themeManager.makeFormatInherit(style, inheritedStyle)
          }
      }
      EventManager eventManager = Manager.getEventManager()
      eventManager.notify(Events.THEME_MODIFIED_EVENT, new EventContext(element, null))
      eventManager.notify(Events.STYLES_MODIFIED_EVENT, new EventContext(element, null))
  }
  
  @GET @POST
  @Path("move_element")
  public void moveElement(@QueryParam("src_id") String srcId, @QueryParam("dest_id") String destId, @QueryParam("order") String order) {
      Element srcElement = ThemeManager.getElementById(srcId)
      Element destElement = ThemeManager.getElementById(destId)
      // move the element
      srcElement.moveTo(destElement, order)
      EventManager eventManager = Manager.getEventManager()
      eventManager.notify(Events.THEME_MODIFIED_EVENT, new EventContext(srcElement, destElement))
  }
  
  @GET @POST
  @Path("paste_element")
  public void pasteElement(@QueryParam("dest_id") String destId) {
      String id = getClipboardElement()
      if (id == null) {
          return
      }
      Element destElement = ThemeManager.getElementById(destId)
      if (destElement.isLeaf()) {
          destElement = (Element) destElement.getParent()
      }
      Element element = ThemeManager.getElementById(id)
      if (element != null) {
          destElement.addChild(Manager.getThemeManager().duplicateElement(element, true))
      }
      EventManager eventManager = Manager.getEventManager()
      eventManager.notify(Events.STYLES_MODIFIED_EVENT, new EventContext(null, null))
      eventManager.notify(Events.THEME_MODIFIED_EVENT, new EventContext(null, destElement))
  }
  
  @GET @POST
  @Path("render_css_preview")
  public String renderCssPreview() {
      String selectedElementId = getSelectedElementId()
      String selectedLayerId = getSelectedStyleLayerId()
      String selectedStyleLayer = getSelectedStyleLayer()
      String selectedViewName = getViewNameOfSelectedElement()
      String selectedElement = getSelectedElement()
      FormatType styleType = (FormatType) Manager.getTypeRegistry().lookup(TypeFamily.FORMAT, "style")
      Style style = (Style) ElementFormatter.getFormatByType(selectedElement, styleType)
      if (selectedStyleLayer != null) {
          style = selectedStyleLayer
      }
      if (style == null) {
          Response.writer("")
          return
      }
      StringBuilder css = new StringBuilder()
      List<Style> styles = new ArrayList<Style>()
      for (Format ancestor : ThemeManager.listAncestorFormatsOf(style)) {
          styles.add(0, (Style) ancestor)
      }
      styles.add(style)
      for (Style s : styles) {
          String viewName = selectedViewName
          if (s.getName() != null) {
              viewName = "*"
          }
          for (path in s.getPathsForView(viewName)) {
              css.append("#stylePreviewArea")
              css.append(' ').append(path).append(" {")
              Properties styleProperties = s.getPropertiesFor(viewName, path)
              Enumeration<?> propertyNames = Utils.getCssProperties().propertyNames()
              while (propertyNames.hasMoreElements()) {
                  propertyName = (String) propertyNames.nextElement()
                  value = styleProperties.getProperty(propertyName)
                  if (value == null) {
                      continue
                  }
                  css.append(propertyName)
                  css.append(':')
                  PresetType preset = ThemeManager.resolvePreset(value)
                  if (preset != null) {
                      value = preset.getValue()
                  }
                  css.append(value)
                  css.append(';')
             }
             css.append('}')
          }
      }
      Response.writer(css.toString())
  }
  
  @GET @POST
  @Path("repair_theme")
  public void repairTheme(@QueryParam("theme_name") String themeName) {
      ThemeElement theme = Manager.getThemeManager().getThemeByName(themeName)
      res = 1
      if (theme == null) {
          res = 0
      }
      if (res) {
          ThemeManager.repairTheme(theme)
          EventManager eventManager = Manager.getEventManager()
          eventManager.notify(Events.THEME_MODIFIED_EVENT, new EventContext(theme, null))
          eventManager.notify(Events.STYLES_MODIFIED_EVENT, new EventContext(theme, null))
      }
      Response.writer(res)
  }
  
  @GET @POST
  @Path("save_theme")
  public void saveTheme(@QueryParam("src") String src, @QueryParam("indent") String inden ) {
      int res = 1
      try {
          ThemeManager.saveTheme(src, new Integer(indent))
      } catch (ThemeIOException e) {
          res = 0
      }
      Response.writer(res)
  }
  
  @GET @POST
  @Path("select_preset_group")
  public void selectPresetGroup(@QueryParam("group") String group) {
      def ctx = WebEngine.getActiveContext()
      SessionManager.setPresetGroup(ctx, group)
  }
  
  @GET @POST
  @Path("select_style_category")
  public void selectStyleCategory(@QueryParam("category") String category) {
      def ctx = WebEngine.getActiveContext()
      SessionManager.setStyleCategory(ctx, category)
  }
  
  @GET @POST
  @Path("select_style_edit_mode")
  public void selectStyleEditMode(@QueryParam("mode") String mode) {
      def ctx = WebEngine.getActiveContext()
      SessionManager.setStyleEditMode(ctx, mode)
  }
  
  @GET @POST
  @Path("select_style_layer")
  public void selectStyleLayer(@QueryParam("uid") String uid) {
      Style layer = (Style) ThemeManager.getFormatById(uid)
      if (layer != null) {
          SessionManager.setStyleLayerId(ctx, uid)
      }
  }
  
  @GET @POST
  @Path("select_style_property_category")
  public void selectStylePropertyCategory(@QueryParam("category") String category) {
      def ctx = WebEngine.getActiveContext()
      SessionManager.setStylePropertyCategory(ctx, category)
  }
  
  @GET @POST
  @Path("select_style_selector")
  public void selectStyleSelector(@QueryParam("selector") String selector) {
      def ctx = WebEngine.getActiveContext()
      SessionManager.setStyleSelector(ctx, selector)      
  }
  
  @GET @POST
  @Path("update_element_description")
  public void updateElementDescription(@QueryParam("id") String id, @QueryParam("description") String description) {
      Element element = ThemeManager.getElementById(id)
      element.setDescription(description)
      EventManager eventManager = Manager.getEventManager()
      eventManager.notify(Events.THEME_MODIFIED_EVENT, new EventContext(element, null))
  }
  
  @GET @POST
  @Path("update_element_properties")
  public void updateElementProperties(@QueryParam("id") String id, @QueryParam("property_map") String property_map) {
      Map propertyMap = JSONObject.fromObject(property_map)
      Element element = ThemeManager.getElementById(id)
      Properties properties = new Properties()
      for (Object key : propertyMap.keySet()) {
          properties.put(key, propertyMap.get(key))
      }
      FieldIO.updateFieldsFromProperties(element, properties)
      EventManager eventManager = Manager.getEventManager()
      eventManager.notify(Events.THEME_MODIFIED_EVENT, new EventContext(element, null))    
  }

  @GET @POST
  @Path("update_element_size")
  public void updateElementSize(@QueryParam("id") String id, @QueryParam("width") String width) {
      Format layout = ThemeManager.getFormatById(id)
      layout.setProperty("width", width)
      EventManager eventManager = Manager.getEventManager()
      eventManager.notify(Events.THEME_MODIFIED_EVENT, new EventContext(layout, null))
  }

  @GET @POST
  @Path("update_element_style_css")
  public void updateElementStyleCss(@QueryParam("id") String id, @QueryParam("view_name") String viewName, @QueryParam("css_source") String cssSource) {
      Style selectedStyleLayer = getSelectedStyleLayer()
      Element element = ThemeManager.getElementById(id)
      FormatType styleType = (FormatType) Manager.getTypeRegistry().lookup(TypeFamily.FORMAT, "style")
      Style style = (Style) ElementFormatter.getFormatByType(element, styleType)
      if (selectedStyleLayer != null) {
          style = selectedStyleLayer
      }
     if (style.getName() != null || "".equals(viewName)) {
         viewName = "*"
     }
     Utils.loadCss(style, cssSource, viewName)
     EventManager eventManager = Manager.getEventManager()
     eventManager.notify(Events.STYLES_MODIFIED_EVENT, new EventContext(element, null))
  }

  @GET @POST
  @Path("split_element")
  public void splitElement(@QueryParam("id") String id) {
      Element element = ThemeManager.getElementById(id)
      if (!element.getElementType().getTypeName().equals("cell")) {
          return
      }
      ThemeManager themeManager = Manager.getThemeManager()
      Element newCell = ElementFactory.create("cell")
      Format cellWidget = FormatFactory.create("widget")
      cellWidget.setName("cell frame")
      themeManager.registerFormat(cellWidget)
      Format cellLayout = FormatFactory.create("layout")
      themeManager.registerFormat(cellLayout)
      FormatType layoutType = (FormatType) Manager.getTypeRegistry().lookup(TypeFamily.FORMAT, "layout")
      Format layout = ElementFormatter.getFormatByType(element, layoutType)
      String width = layout.getProperty("width")
      if (width != null) {
          String halfWidth = org.nuxeo.theme.html.Utils.divideWebLength(width, 2)
          if (halfWidth != null) {
              cellLayout.setProperty("width", halfWidth)
              layout.setProperty("width", org.nuxeo.theme.html.Utils.substractWebLengths(width, halfWidth))
          }
      }
      Format cellStyle = FormatFactory.create("style")
      themeManager.registerFormat(cellStyle)
      ElementFormatter.setFormat(newCell, cellWidget)
      ElementFormatter.setFormat(newCell, cellLayout)
      ElementFormatter.setFormat(newCell, cellStyle)
      newCell.insertAfter(element)
      EventManager eventManager = Manager.getEventManager()
      eventManager.notify(Events.THEME_MODIFIED_EVENT, new EventContext(element, null))
  }

  @GET @POST
  @Path("update_element_style")
  public void updateElementStyle(@QueryParam("id") String id, @QueryParam("view_name") String viewName, @QueryParam("path") String path, @QueryParam("property_map") String property_map) {
      Map propertyMap = JSONObject.fromObject(property_map)
      Element element = ThemeManager.getElementById(id)
      Properties properties = new Properties()
      for (Object key : propertyMap.keySet()) {
          properties.put(key, propertyMap.get(key))
      }
      FormatType styleType = (FormatType) Manager.getTypeRegistry().lookup(TypeFamily.FORMAT, "style")
      Style style = (Style) ElementFormatter.getFormatByType(element, styleType)
      Style currentStyleLayer = getSelectedStyleLayer()
      if (currentStyleLayer != null) {
          style = currentStyleLayer
      }
      if (style.getName() != null || "".equals(viewName)) {
          viewName = "*"
      }
      style.setPropertiesFor(viewName, path, properties)
      EventManager eventManager = Manager.getEventManager()
      eventManager.notify(Events.STYLES_MODIFIED_EVENT, new EventContext(element, null))
  }

  @GET @POST
  @Path("update_element_visibility")
  public void updateElementVisibility(@QueryParam("id") String id, @QueryParam("perspectives") List<String> perspectives, @QueryParam("always_visible") boolean always_visible) {
      List<String> perspectivesList = new ArrayList<String>()
      if (perspectives != null) {
          for (p in perspectives) {
              perspectivesList.add(p)
          }
      }
      Element element = ThemeManager.getElementById(id)
      PerspectiveManager perspectiveManager = Manager.getPerspectiveManager()
      if (alwaysVisible) {
          perspectiveManager.setAlwaysVisible(element)
      } else {
          // initially make the element visible in all perspectives
          if (perspectivesList.isEmpty()) {
              perspectiveManager.setVisibleInAllPerspectives(element)
          } else {
              perspectiveManager.setVisibleInPerspectives(element, perspectivesList)
          }
      }
      EventManager eventManager = Manager.getEventManager()
      eventManager.notify(Events.THEME_MODIFIED_EVENT, new EventContext(element, null))
  }
  
  @GET @POST
  @Path("update_element_widget")
  public void updateElementWidget(@QueryParam("id") String id, @QueryParam("view_name") String viewName) {
      Element element = ThemeManager.getElementById(id)
      FormatType widgetType = (FormatType) Manager.getTypeRegistry().lookup(TypeFamily.FORMAT, "widget")
      Format widget = ElementFormatter.getFormatByType(element, widgetType)
      ThemeManager themeManager = Manager.getThemeManager()      
      if (widget == null) {
          widget = FormatFactory.create("widget")
          themeManager.registerFormat(widget)
      }
      widget.setName(viewName)
      ElementFormatter.setFormat(element, widget)
      EventManager eventManager = Manager.getEventManager()
      eventManager.notify(Events.THEME_MODIFIED_EVENT, new EventContext(element, null))
  }
  
  /* API */
   
  public static List<ThemeDescriptor> getThemesDescriptors() {
    return ThemeManager.getThemesDescriptors()
  }
  
  public static List<Identifiable> getNamedStyles() {
      String currentThemeName = getCurrentThemeName()
      def styles = []
      List<Identifiable> namedStyles = Manager.getThemeManager().getNamedObjects(currentThemeName, "style")
      if (namedStyles) {
          styles.add("")
          for (namedStyle in namedStyles) {
              styles.add(namedStyle.getName())
           }
      }
      return styles
  }
  
  public static List<FragmentType> getFragments(applicationPath) {
      def fragments = []
      String templateEngine = getTemplateEngine(applicationPath)
      for (f in Manager.getTypeRegistry().getTypes(TypeFamily.FRAGMENT)) {
          FragmentType fragmentType = (FragmentType) f
          FragmentInfo fragmentInfo = new FragmentInfo(fragmentType)
          for (ViewType viewType : ThemeManager.getViewTypesForFragmentType(fragmentType)) {
              String viewTemplateEngine = viewType.getTemplateEngine()
              if (!"*".equals(viewType.getViewName()) && templateEngine.equals(viewTemplateEngine)) {
                  fragmentInfo.addView(viewType)
              }
          }
          if (fragmentInfo.size() > 0) {
               fragments.add(fragmentInfo)
          }
      }
      return fragments
  }
  
  public static String getSelectedElementId() {
      def ctx = WebEngine.getActiveContext()
      return SessionManager.getSelectedElementId(ctx)
  }
  
  public static Element getSelectedElement() {
    String id = getSelectedElementId()
    if (id == null) {
      return null
    }
    return ThemeManager.getElementById(id)
  } 
  
  public static String getClipboardElement() {
      def ctx = WebEngine.getActiveContext()
      return SessionManager.getClipboardElementId(ctx)
  }
  
  public static List<StyleLayer> getStyleLayersOfSelectedElement() {
      Style style = getStyleOfSelectedElement()
      if (style == null) {
        return []
      }
      Style selectedStyleLayer = getSelectedStyleLayer()
      def layers = []
      layers.add(new StyleLayer("This style", style.getUid(), style == selectedStyleLayer || selectedStyleLayer == null))
      for (Format ancestor : ThemeManager.listAncestorFormatsOf(style)) {
          layers.add(1, new StyleLayer(ancestor.getName(), ancestor.getUid(), ancestor == selectedStyleLayer))
      }
      return layers
  }
  
  public static boolean isSelectedElementAlwaysVisible() {
      Element selectedElement = getSelectedElement()
      return Manager.getPerspectiveManager().isAlwaysVisible(selectedElement)
  }
  
  public static List<PerspectiveType> getPerspectives() {
      return PerspectiveManager.listPerspectives()
  }
  
  public static List<PerspectiveType> getPerspectivesOfSelectedElement() {
      Element selectedElement = getSelectedElement()
      def perspectives = []
      for (PerspectiveType perspectiveType : Manager.getPerspectiveManager().getPerspectivesFor(selectedElement)) {
          perspectives.add(perspectiveType.name)
      }
      return perspectives
  }
  
  public static String getStyleEditMode() {
      def ctx = WebEngine.getActiveContext()
      return SessionManager.getStyleEditMode(ctx)
  }
  
  public static List<String> getStyleSelectorsForSelectedElement() {
      Element element = getSelectedElement()
      String viewName = getViewNameOfSelectedElement()
      Style style = getStyleOfSelectedElement()
      Style selectedStyleLayer = getSelectedStyleLayer()
      def selectors = []
      if (selectedStyleLayer != null) {
          style = selectedStyleLayer
      }
      if (style != null) {
          if (style.getName() != null) {
              viewName = "*"
          }
          Set<String> paths = style.getPathsForView(viewName)
          String current = getSelectedStyleSelector()
          if (current != null && !paths.contains(current)) {
              selectors.add(current)
          }
          for (path in paths) {
              selectors.add(path)
          }
      }
      return selectors
  }
  
  public static List<StyleFieldProperty> getElementStyleProperties() {
      Pattern cssCategoryPattern = Pattern.compile("<(.*?)>")
      Style style = getStyleOfSelectedElement()
      Style selectedStyleLayer = getSelectedStyleLayer()
      if (selectedStyleLayer != null) {
          style = selectedStyleLayer
      }
      List<StyleFieldProperty> fieldProperties = []
      if (style == null) {
          return fieldProperties
      }
      String path = getSelectedStyleSelector()
      if (path == null) {
          return fieldProperties
      }
      String viewName = getViewNameOfSelectedElement()
      if (style.getName() != null) {
          viewName = "*"
      }
      Properties properties = style.getPropertiesFor(viewName, path)
      String selectedCategory = getStylePropertyCategory()

      Properties cssProperties = Utils.getCssProperties()
      Enumeration<?> propertyNames = cssProperties.propertyNames()
      while (propertyNames.hasMoreElements()) {
          String name = (String) propertyNames.nextElement()
          String value = properties == null ? "" : properties.getProperty(name, "")
          String type = cssProperties.getProperty(name)
          if (!selectedCategory.equals("*")) {
              Matcher categoryMatcher = cssCategoryPattern.matcher(type)
              if (!categoryMatcher.find()) {
                  continue
              }
              if (!categoryMatcher.group(1).equals(selectedCategory)) {
                  continue
              }
          }
          fieldProperties.add(new StyleFieldProperty(name, value, type))
      }
      return fieldProperties    
  }
  
  public static List getStylePropertiesForSelectedElement() {
      String viewName = getViewNameOfSelectedElement()
      Style style = getStyleOfSelectedElement()
      Style selectedStyleLayer = getSelectedStyleLayer()
      if (selectedStyleLayer != null) {
          style = selectedStyleLayer
      }
      def fieldProperties = []
      if (style == null) {
          return fieldProperties
      }
      String path = getSelectedStyleSelector()
      if (path == null) {
          return fieldProperties
      }
      if (style.getName() != null) {
          viewName = "*"
      }
      Properties properties = style.getPropertiesFor(viewName, path)
      String selectedCategory = getSelectedStylePropertyCategory()
      Pattern cssCategoryPattern = Pattern.compile("<(.*?)>")
      Properties cssProperties = Utils.getCssProperties()
      Enumeration<?> propertyNames = cssProperties.propertyNames()
      while (propertyNames.hasMoreElements()) {
          String name = (String) propertyNames.nextElement()
          String value = properties == null ? "" : properties.getProperty(name, "")
                  String type = cssProperties.getProperty(name)
          if (!selectedCategory.equals("")) {
              Matcher categoryMatcher = cssCategoryPattern.matcher(type)
              if (!categoryMatcher.find()) {
                  continue
              }
              if (!categoryMatcher.group(1).equals(selectedCategory)) {
                  continue
              }
          }
          fieldProperties.add(new StyleFieldProperty(name, value, type))
      }
      return fieldProperties
  }
  
  public static String getStylePropertyCategory() {
      def ctx = WebEngine.getActiveContext()
      String category = SessionManager.getStylePropertyCategory(ctx)
      if (!category) {
          category = '*'
      }
      return category
  }
  
  public static List<StyleCategory> getStyleCategories() {
      String selectedStyleCategory = getStylePropertyCategory()
      Pattern cssCategoryPattern = Pattern.compile("<(.*?)>")
      Map<String, StyleCategory> categories = new LinkedHashMap<String, StyleCategory>()
      Enumeration<?> elements = Utils.getCssProperties().elements()
      categories.put("", new StyleCategory("*", "all", selectedStyleCategory.equals("*")))
      while (elements.hasMoreElements()) {
          Element element = (String) elements.nextElement()
          Matcher categoryMatcher = cssCategoryPattern.matcher(element)
          if (categoryMatcher.find()) {
              String value = categoryMatcher.group(1)
              boolean selected = value.equals(selectedStyleCategory)
              categories.put(value, new StyleCategory(value, value, selected))
          }
      }
      return new ArrayList<StyleCategory>(categories.values())
  }
  
  public static String getInheritedStyleNameOfSelectedElement() {
      Style style = getStyleOfSelectedElement()
      Style ancestor = (Style) ThemeManager.getAncestorFormatOf(style)
      if (ancestor != null) {
          return ancestor.getName()
      }
      return ""
  }

  public static String getSelectedStyleSelector() {
      def ctx = WebEngine.getActiveContext()
      return SessionManager.getSelectedStyleSelector(ctx)
  }
  
  public static Style getSelectedStyleLayer() {
      String selectedStyleLayerId = getSelectedStyleLayerId()
      if (selectedStyleLayerId == null) {
        return null
      }
      return (Style) ThemeManager.getFormatById(selectedStyleLayerId)
  }
  
  public static String getSelectedStyleLayerId() {
      def ctx = WebEngine.getActiveContext()
      return SessionManager.getSelectedStyleLayerId(ctx)
  } 
  
  public static Style getStyleOfSelectedElement() {
      Element element = getSelectedElement()
      if (element == null) {
        return null
      }
      FormatType styleType = (FormatType) Manager.getTypeRegistry().lookup(TypeFamily.FORMAT, "style")
      return (Style) ElementFormatter.getFormatByType(element, styleType)
  }
  
  public static PaddingInfo getPaddingOfSelectedElement() {
      Element element = getSelectedElement()
      String top = ""
      String bottom = ""
      String left = ""
      String right = ""
      if (element != null) {
          Layout layout = (Layout) ElementFormatter.getFormatFor(element, "layout")
          top = layout.getProperty("padding-top")
          bottom = layout.getProperty("padding-bottom")
          left = layout.getProperty("padding-left")
          right = layout.getProperty("padding-right")
      }
      return new PaddingInfo(top, bottom, left, right)
  }

  public static String getRenderedStylePropertiesForSelectedElement() {
      Style style = getStyleOfSelectedElement()
      Style currentStyleLayer = getSelectedStyleLayer()
      if (currentStyleLayer != null) {
          style = currentStyleLayer
      }
      if (style == null) {
          return ""
      }
      def viewNames = []
      String viewName = getViewNameOfSelectedElement()
      if (style.getName() != null) {
          viewName = "*"
      }
      viewNames.add(viewName)
      boolean RESOLVE_PRESETS = false
      boolean IGNORE_VIEW_NAME = true
      boolean IGNORE_CLASSNAME = true
      boolean INDENT = true
      return org.nuxeo.theme.html.Utils.styleToCss(style, viewNames, RESOLVE_PRESETS, IGNORE_VIEW_NAME, IGNORE_CLASSNAME, INDENT)
  }
  
  public static List<String> getPresetGroupsForSelectedCategory() {
      def groups = []
      String category = getSelectedStyleCategory()
      groups.add("")
      if (category == null) {
          return groups
      }
      def groupNames = []
      for (Type type : Manager.getTypeRegistry().getTypes(TypeFamily.PRESET)) {
          PresetType preset = (PresetType) type
          String group = preset.getGroup()
          if (!preset.getCategory().equals(category)) {
              continue
          }
          if (!groupNames.contains(group)) {
              groups.add(group)
          }
          groupNames.add(group)
      }
      return groups
  }
    
  public static Widget getWidgetOfSelectedElement() {
    Element element = getSelectedElement()
    if (element == null) {
      return null
    }
    FormatType widgetType = (FormatType) Manager.getTypeRegistry().lookup(TypeFamily.FORMAT, "widget")
    return (Widget) ElementFormatter.getFormatByType(element, widgetType)
  }
  
  public static String getViewNameOfSelectedElement() {
    Widget widget = getWidgetOfSelectedElement()
    if (widget == null) {
      return ""
    }
    return widget.getName()
  }

  public static List<String> getViewNamesForSelectedElement(applicationPath) {
      Element selectedElement = getSelectedElement()
      String templateEngine = getTemplateEngine(applicationPath)
      def viewNames = []
      if (selectedElement == null) {
          return viewNames
      }
      if (!selectedElement.getElementType().getTypeName().equals("fragment")) {
          return viewNames
      }
      FragmentType fragmentType = ((Fragment) selectedElement).getFragmentType()
      for (ViewType viewType : ThemeManager.getViewTypesForFragmentType(fragmentType)) {
          String viewName = viewType.getViewName()
          String viewTemplateEngine = viewType.getTemplateEngine()
          if (!"*".equals(viewName) && templateEngine.equals(viewTemplateEngine)) {
              viewNames.add(viewName)
          }
      }
      return viewNames
  }
   
  public static List<FieldProperty> getSelectedElementProperties() {
      Element selectedElement = getSelectedElement()
      return org.nuxeo.theme.editor.Utils.getPropertiesOf(selectedElement) 
  }
  
  public static List<PresetInfo> getPresetsForSelectedGroup() {
      String category = getSelectedStyleCategory()
      String group = getSelectedPresetGroup()
      def presets = []
      for (type in Manager.getTypeRegistry().getTypes(TypeFamily.PRESET)) {
          PresetType preset = (PresetType) type
          if (!preset.getCategory().equals(category)) {
              continue
          }
          if (!preset.getGroup().equals(group)) {
              continue
          }
          presets.add(new PresetInfo(preset))
      }
      return presets
  }
  
  public static String getSelectedPresetGroup() {
      def ctx = WebEngine.getActiveContext()
      String category = SessionManager.getPresetGroup(ctx)
      return category
  }
  
  public static String getSelectedStyleCategory() {
        def ctx = WebEngine.getActiveContext()
        String category = SessionManager.getStyleCategory(ctx)
        if (!category) {
            category = "page"
        }
        return category
  }
  
  public static String getStylePropertiesForSelectedElement () {
      String viewName = getViewNameOfSelectedElement()
      Style style = getStyleOfSelectedElement()
      Style selectedStyleLayer = getSelectedStyleLayer()
      if (selectedStyleLayer != null) {
          style = selectedStyleLayer
      }
      def fieldProperties = []
      if (style == null) {
          return fieldProperties
      }
      String path = getSelectedStyleSelector()
      if (path == null) {
          return fieldProperties
      }
      if (style.getName() != null) {
          viewName = "*"
      }
      Properties properties = style.getPropertiesFor(viewName, path)
      String selectedCategory = getSelectedStylePropertyCategory()
      Pattern cssCategoryPattern = Pattern.compile("<(.*?)>")
      Properties cssProperties = Utils.getCssProperties()
      Enumeration<?> propertyNames = cssProperties.propertyNames()
      while (propertyNames.hasMoreElements()) {
          String name = (String) propertyNames.nextElement()
          String value = properties == null ? "" : properties.getProperty(name, "")
          String type = cssProperties.getProperty(name)
          if (!selectedCategory.equals("")) {
              Matcher categoryMatcher = cssCategoryPattern.matcher(type)
              if (!categoryMatcher.find()) {
                  continue
              }
              if (!categoryMatcher.group(1).equals(selectedCategory)) {
                  continue
              }
          }
          fieldProperties.add(new StyleFieldProperty(name, value, type))
      }
      return fieldProperties
  }
  
  public static String getTemplateEngine(applicationPath) {
      return ThemeManager.getTemplateEngineName(applicationPath)
  }
  
  public static String getDefaultTheme(applicationPath) {
    return ThemeManager.getDefaultTheme(applicationPath)
  }

  public static String getCurrentThemeName(applicationPath) {
    String defaultTheme = getDefaultTheme(applicationPath)
    def ctx = WebEngine.getActiveContext()
    String currentPagePath = ctx.getCookie("nxthemes.theme")
    if (currentPagePath == null) {
      currentPagePath = defaultTheme
    }
    return currentPagePath.split("/")[0]
  }

  public static List<PageElement> getPages(applicationPath) {
    ThemeManager themeManager = Manager.getThemeManager()
    def ctx = WebEngine.getActiveContext()
    String currentPagePath = ctx.getCookie("nxthemes.theme")
    String defaultTheme = getDefaultTheme(applicationPath)
    String defaultPageName = defaultTheme.split("/")[1]

    def pages = []
    if (!currentPagePath || !currentPagePath.contains("/")) {
      currentPagePath = defaultTheme
    }

    String currentThemeName = currentPagePath.split("/")[0]
    String currentPageName = currentPagePath.split("/")[1]
    ThemeElement currentTheme = themeManager.getThemeByName(currentThemeName)

    if (currentTheme == null) {
      return pages
    }

    for (PageElement page : ThemeManager.getPagesOf(currentTheme)) {
      String pageName = page.getName()
      String link = String.format("%s/%s", currentThemeName, pageName)
      String className = pageName.equals(currentPageName) ? "selected" : ""
      if (defaultPageName.equals(pageName)) {
        className += " default"
      }
      pages.add(new PageInfo(pageName, link, className))
    }
    return pages
  }

  public static List<ThemeElement> getThemes(applicationPath) {
    String defaultTheme = getDefaultTheme(applicationPath)
    def themes = []
    if (!defaultTheme.contains("/")) {
      return themes
    }
    String defaultThemeName = defaultTheme.split("/")[0]
    String defaultPageName = defaultTheme.split("/")[1]
    String currentThemeName = getCurrentThemeName(applicationPath)
    for (themeName in Manager.getThemeManager().getThemeNames()) {
      String link = String.format("%s/%s", themeName, defaultPageName)
      String className = themeName.equals(currentThemeName) ? "selected" : ""
      if (link.equals(defaultThemeName)) {
        className += " default"
      }
      themes.add(new ThemeInfo(themeName, link, className))
    }
    return themes
  }

}

