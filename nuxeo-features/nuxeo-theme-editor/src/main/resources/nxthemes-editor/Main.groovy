package editor

import java.io.*
import javax.ws.rs.*
import javax.ws.rs.core.*
import javax.ws.rs.core.Response.ResponseBuilder
import net.sf.json.JSONObject
import org.nuxeo.ecm.core.rest.*
import org.nuxeo.ecm.webengine.forms.*
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
import org.nuxeo.theme.properties.*
import org.nuxeo.theme.templates.*
import org.nuxeo.theme.themes.*
import org.nuxeo.theme.types.*
import org.nuxeo.theme.perspectives.*
import org.nuxeo.theme.uids.*
import org.nuxeo.theme.views.*
import org.nuxeo.theme.editor.*

@WebObject(type="nxthemes-editor", guard="group=administrators")
@Produces(["text/html", "*/*"])
public class Main extends ModuleRoot {

    @GET
    @Path("perspectiveSelector")
    public Object renderPerspectiveSelector(@QueryParam("org.nuxeo.theme.application.path") String path) {
      return getTemplate("perspectiveSelector.ftl").arg("perspectives", getPerspectives())
    }

    @GET
    @Path("themeSelector")
    public Object renderThemeSelector(@QueryParam("org.nuxeo.theme.application.path") String path,
            @QueryParam("org.nuxeo.theme.application.name") String name) {
      return getTemplate("themeSelector.ftl").arg(
              "current_theme_name", getCurrentThemeName(path, name)).arg(
              "themes", getThemes(path, name))
    }

  @GET
  @Path("pageSelector")
  public Object renderPageSelector(@QueryParam("org.nuxeo.theme.application.path") String path,
          @QueryParam("org.nuxeo.theme.application.name") String name) {
    return getTemplate("pageSelector.ftl").arg(
            "current_theme_name", getCurrentThemeName(path, name)).arg(
            "pages", getPages(path, name))
  }

  @GET
  @Path("canvasModeSelector")
  public Object renderCanvasModeSelector(@QueryParam("org.nuxeo.theme.application.path") String path) {
    return getTemplate("canvasModeSelector.ftl")
  }

  @GET
  @Path("presetManager")
  public Object renderPresetManager(@QueryParam("org.nuxeo.theme.application.path") String path,
          @QueryParam("org.nuxeo.theme.application.name") String name) {
    return getTemplate("presetManager.ftl").arg(
            "current_theme_name", getCurrentThemeName(path, name)).arg(
            "preset_groups", getPresetGroups()).arg(
            "preset_manager_mode", getPresetManagerMode()).arg(
            "selected_preset_group", getSelectedPresetGroup())
  }

  @GET
  @Path("styleManager")
  public Object renderStyleManager(@QueryParam("org.nuxeo.theme.application.path") String path,
          @QueryParam("org.nuxeo.theme.application.name") String name) {
    def styles = getNamedStyles(path, name)
    Style selectedStyle = getSelectedNamedStyle()
    if (!styles.contains(selectedStyle) && !styles.isEmpty()) {
        selectedStyle = styles[0];
    }
    return getTemplate("styleManager.ftl").arg(
            "named_styles", styles).arg(
            "style_manager_mode", getStyleManagerMode()).arg(
            "selected_named_style", selectedStyle).arg(
            "selected_named_style_css", getRenderedPropertiesForNamedStyle(selectedStyle)).arg(
            "current_theme_name", getCurrentThemeName(path, name))
  }

  @GET
  @Path("themeManager")
  public Object renderThemeManager(@QueryParam("org.nuxeo.theme.application.path") String path,
          @QueryParam("org.nuxeo.theme.application.name") String name) {
    return getTemplate("themeManager.ftl").arg(
            "current_theme_name", getCurrentThemeName(path, name))
  }

  @GET
  @Path("fragmentFactory")
  public Object renderFragmentFactory(@QueryParam("org.nuxeo.theme.application.path") String path) {
    String fragmentType = getSelectedFragmentType()
    String fragmentVIew = getSelectedFragmentView()
    return getTemplate("fragmentFactory.ftl").arg(
            "selected_fragment_type", fragmentType).arg(
            "selected_fragment_view", fragmentVIew).arg(
            "fragments", getFragments(path)).arg(
            "views", getViews(fragmentType, path)).arg(
            "selected_element_id", getSelectedElementId())
  }

  @GET
  @Path("elementEditor")
  public Object renderElementEditor(@QueryParam("org.nuxeo.theme.application.path") String path) {
    return getTemplate("elementEditor.ftl").arg("selected_element", getSelectedElement())
  }

  @GET
  @Path("elementDescription")
  public Object renderElementDescription(@QueryParam("org.nuxeo.theme.application.path") String path) {
    return getTemplate("elementDescription.ftl").arg("selected_element", getSelectedElement())
  }

  @GET
  @Path("elementPadding")
  public Object renderElementPadding(@QueryParam("org.nuxeo.theme.application.path") String path) {
    return getTemplate("elementPadding.ftl").arg(
            "selected_element", getSelectedElement()).arg(
            "padding_of_selected_element", getPaddingOfSelectedElement())
  }

  @GET
  @Path("elementProperties")
  public Object renderElementProperties(@QueryParam("org.nuxeo.theme.application.path") String path) {
    return getTemplate("elementProperties.ftl").arg(
            "selected_element", getSelectedElement()).arg(
            "element_properties", getSelectedElementProperties())
  }

  @GET
  @Path("elementStyle")
  public Object renderElementStyle(@QueryParam("org.nuxeo.theme.application.path") String path,
          @QueryParam("org.nuxeo.theme.application.name") String name) {
    return getTemplate("elementStyle.ftl").arg(
            "selected_element", getSelectedElement()).arg(
            "style_of_selected_element", getStyleOfSelectedElement()).arg(
            "current_theme_name", getCurrentThemeName(path, name)).arg(
            "style_layers_of_selected_element", getStyleLayersOfSelectedElement()).arg(
            "inherited_style_name_of_selected_element", getInheritedStyleNameOfSelectedElement()).arg(
            "named_styles", getNamedStyles(path, name))
  }

  @GET
  @Path("elementWidget")
  public Object renderElementWidget(@QueryParam("org.nuxeo.theme.application.path") String path) {
    return getTemplate("elementWidget.ftl").arg(
            "selected_element", getSelectedElement()).arg(
            "selected_view_name", getViewNameOfSelectedElement()).arg(
            "view_names_for_selected_element", getViewNamesForSelectedElement(path))
  }

  @GET
  @Path("elementVisibility")
  public Object renderElementVisibility(@QueryParam("org.nuxeo.theme.application.path") String path) {
    return getTemplate("elementVisibility.ftl").arg(
            "selected_element", getSelectedElement()).arg(
            "perspectives_of_selected_element", getPerspectivesOfSelectedElement()).arg(
            "is_selected_element_always_visible", isSelectedElementAlwaysVisible()).arg(
            "perspectives", getPerspectives())
  }

  @GET
  @Path("stylePicker")
  public Object renderStylePicker(@QueryParam("org.nuxeo.theme.application.path") String path,
          @QueryParam("org.nuxeo.theme.application.name") String name) {
    return getTemplate("stylePicker.ftl").arg(
            "style_category", getSelectedStyleCategory()).arg(
            "current_theme_name", getCurrentThemeName(path, name)).arg(
            "selected_preset_group", getSelectedPresetGroup()).arg(
            "preset_groups", getPresetGroupsForSelectedCategory()).arg(
            "presets_for_selected_group", getPresetsForSelectedGroup(path, name))
  }

  @GET
  @Path("areaStyleChooser")
  public Object renderAreaStyleChooser(@QueryParam("org.nuxeo.theme.application.path") String path,
          @QueryParam("org.nuxeo.theme.application.name") String name) {
    return getTemplate("areaStyleChooser.ftl").arg(
            "style_category", getSelectedStyleCategory()).arg(
            "current_theme_name", getCurrentThemeName(path, name)).arg(
            "preset_groups", getPresetGroupsForSelectedCategory()).arg(
            "presets_for_selected_group", getPresetsForSelectedGroup(path, name)).arg(
            "selected_preset_group", getSelectedPresetGroup())
  }

  @GET
  @Path("styleProperties")
  public Object renderStyleProperties(@QueryParam("org.nuxeo.theme.application.path") String path) {
    return getTemplate("styleProperties.ftl").arg(
            "selected_element", getSelectedElement()).arg(
            "style_edit_mode", getStyleEditMode()).arg(
            "style_selectors", getStyleSelectorsForSelectedElement()).arg(
            "rendered_style_properties", getRenderedStylePropertiesForSelectedElement()).arg(
            "selected_style_selector", getSelectedStyleSelector()).arg(
            "style_properties", getStylePropertiesForSelectedElement()).arg(
            "style_categories", getStyleCategories()).arg(
            "selected_view_name", getViewNameOfSelectedElement()).arg(
            "element_style_properties", getElementStyleProperties())
  }

  @GET
  @Path("render_view_icon")
  public Response renderViewIcon(@QueryParam("name") String viewTypeName) {
      byte[] content = org.nuxeo.theme.editor.Editor.getViewIconContent(viewTypeName)
      ResponseBuilder builder = Response.ok(content)
      // builder.type(???)
      return builder.build();
   }

  @GET
  @Path("render_css_preview")
  public String renderCssPreview() {
      String selectedElementId = getSelectedElementId()
      Style selectedStyleLayer = getSelectedStyleLayer()
      String selectedViewName = getViewNameOfSelectedElement()
      Element selectedElement = getSelectedElement()
      return Editor.renderCssPreview(selectedElement, selectedStyleLayer, selectedViewName)
  }


  @GET
  @Path("xml_export")
  public Response xmlExport(@QueryParam("src") String src, @QueryParam("download") Integer download, @QueryParam("indent") Integer indent) {
      if (src == null) {
          return
      }
      ThemeDescriptor themeDef = Manager.getThemeManager().getThemeDescriptor(src)

      ThemeSerializer serializer = new ThemeSerializer();
      if (indent == null) {
          indent = 0
      }

      String xml = serializer.serializeToXml(src, indent);
      if (xml == null) {
          return
      }

      ResponseBuilder builder = Response.ok(xml)
      if (download != null) {
          builder.header("Content-disposition", String.format(
                  "attachment; filename=theme-%s.xml", themeDef.getName()))
      }
      builder.type("text/xml")
      return builder.build()
  }

  @POST
  @Path("clear_selections")
  public void clearSelections() {
    SessionManager.setElementId(null);
    SessionManager.setStyleEditMode(null);
    SessionManager.setStyleLayerId(null);
    SessionManager.setStyleSelector(null);
    SessionManager.setNamedStyleId(null);
    SessionManager.setStylePropertyCategory(null);
    SessionManager.setStyleCategory(null);
    SessionManager.setPresetGroup(null);
    SessionManager.setClipboardElementId(null);
    SessionManager.setFragmentView(null);
    SessionManager.setFragmentType(null);
  }

  @POST
  @Path("select_element")
  public void selectElement() {
    String id = ctx.getForm().getString("id")
    SessionManager.setElementId(id)
    // clean up
    SessionManager.setStyleEditMode(null);
    SessionManager.setStyleLayerId(null);
    SessionManager.setNamedStyleId(null);
    SessionManager.setStyleSelector(null);
    SessionManager.setStylePropertyCategory(null);
    SessionManager.setStyleCategory(null);
    SessionManager.setPresetGroup(null);
    SessionManager.setFragmentView(null);
    SessionManager.setFragmentType(null);
  }

  @POST
  @Path("add_page")
  public String addPage() {
      String pagePath = ctx.getForm().getString("path")
    try {
        return Editor.addPage(pagePath)
      } catch (Exception e) {
          throw new ThemeEditorException(e.getMessage(), e)
      }
  }

  @POST
  @Path("add_theme")
  public String addTheme() {
      String name = ctx.getForm().getString("name")
    try {
        return Editor.addTheme(name)
      } catch (Exception e) {
          throw new ThemeEditorException(e.getMessage(), e)
      }
  }

  @POST
  @Path("align_element")
  public void alignElement() {
      FormData form = ctx.getForm()
      String id = form.getString("id")
      String position = form.getString("position")
      Element element = ThemeManager.getElementById(id)
    try {
        Editor.alignElement(element, position)
      } catch (Exception e) {
          throw new ThemeEditorException(e.getMessage(), e)
      }

  }

  @POST
  @Path("assign_style_property")
  public void assignStyleProperty() {
      FormData form = ctx.getForm()
      String id = form.getString("element_id")
      String propertyName = form.getString("property")
      String value = form.getString("value")
      Element element = ThemeManager.getElementById(id)
    try {
        Editor.assignStyleProperty(element, propertyName, value)
      } catch (Exception e) {
          throw new ThemeEditorException(e.getMessage(), e)
      }
  }

  @POST
  @Path("copy_element")
  public void copyElement() {
      String id = ctx.getForm().getString("id")
      SessionManager.setClipboardElementId(id)
  }

  @POST
  @Path("set_preset_category")
  public void setPresetCategory() {
      String themeName = ctx.getForm().getString("theme_name")
      String presetName = ctx.getForm().getString("preset_name")
      String category = ctx.getForm().getString("category")
      try {
          Editor.setPresetCategory(themeName, presetName, category)
      } catch (Exception e) {
          throw new ThemeEditorException(e.getMessage(), e)
      }
  }

  @POST
  @Path("copy_preset")
  public void copyPreset() {
      String id = ctx.getForm().getString("id")
      SessionManager.setClipboardPresetId(id)
  }

  @POST
  @Path("paste_preset")
  public String pastePreset() {
      FormData form = ctx.getForm()
      String themeName = form.getString("theme_name")
      String newPresetName = form.getString("preset_name")
      String presetName = getClipboardPreset()
      if (presetName == null) {
          throw new ThemeEditorException("Nothing to paste")
      }

      PresetType preset = PresetManager.getPresetByName(presetName)
      if (preset == null) {
          throw new ThemeEditorException("Preset not found: " + presetName)
      }

      try {
          Editor.addPreset(themeName, newPresetName, preset.getCategory(), preset.getValue())
      } catch (Exception e) {
          throw new ThemeEditorException(e.getMessage(), e)
      }
  }

  @POST
  @Path("create_named_style")
  public void createNamedStyle() {
      FormData form = ctx.getForm()
      String id = form.getString("id")
      String themeName = form.getString("theme_name")
      String styleName = form.getString("style_name")
      Element element = ThemeManager.getElementById(id)
    try {
        Editor.createNamedStyle(element, styleName, themeName)
      } catch (Exception e) {
          throw new ThemeEditorException(e.getMessage(), e)
      }
  }

  @POST
  @Path("create_style")
  public void createStyle() {
      Element element = getSelectedElement()
    try {
        Editor.createStyle(element)
      } catch (Exception e) {
          throw new ThemeEditorException(e.getMessage(), e)
      }
  }

  @POST
  @Path("delete_element")
  public void deleteElement() {
      FormData form = ctx.getForm()
      String id = form.getString("id")
      Element element = ThemeManager.getElementById(id)
    try {
        Editor.deleteElement(element)
      } catch (Exception e) {
          throw new ThemeEditorException(e.getMessage(), e)
      }
  }

  @POST
  @Path("delete_named_style")
  public void deleteNamedStyle() {
      FormData form = ctx.getForm()
      String id = form.getString("id")
      String themeName = form.getString("theme_name")
      String styleName = form.getString("style_name")
      Element element = ThemeManager.getElementById(id)
    try {
        Editor.deleteNamedStyle(element, styleName, themeName)
      } catch (Exception e) {
          throw new ThemeEditorException(e.getMessage(), e)
      }
  }

  @POST
  @Path("duplicate_element")
  public String duplicateElement() {
      FormData form = ctx.getForm()
      String id = form.getString("id")
      Element element = ThemeManager.getElementById(id)
    try {
        return Editor.duplicateElement(element)
      } catch (Exception e) {
          throw new ThemeEditorException(e.getMessage(), e)
      }
  }

  @POST
  @Path("expire_themes")
  public void expireThemes() {
    try {
        Editor.expireThemes()
      } catch (Exception e) {
          throw new ThemeEditorException(e.getMessage(), e)
      }
  }

  @POST
  @Path("insert_fragment")
  public void insertFragment() {
      FormData form = ctx.getForm()
      String destId = form.getString("dest_id")
      String typeName = form.getString("type_name")
      Element destElement = ThemeManager.getElementById(destId)
    try {
        Editor.insertFragment(destElement, typeName)
      } catch (Exception e) {
          throw new ThemeEditorException(e.getMessage(), e)
      }
  }

  @POST
  @Path("insert_section_after")
  public void insertSectionAfter() {
      FormData form = ctx.getForm()
      String id = form.getString("id")
      Element element = ThemeManager.getElementById(id)
    try {
        Editor.insertSectionAfter(element)
      } catch (Exception e) {
          throw new ThemeEditorException(e.getMessage(), e)
      }
  }

  @POST
  @Path("select_preset_manager_mode")
  public void selectPresetManagerMode() {
      FormData form = ctx.getForm()
      String mode = form.getString("mode")
      SessionManager.setPresetManagerMode(mode)
  }

  @POST
  @Path("select_fragment_type")
  public void selectFragmentType() {
      FormData form = ctx.getForm()
      String type = form.getString("type")
      SessionManager.setFragmentType(type)
      SessionManager.setFragmentView(null)
  }

  @POST
  @Path("select_fragment_view")
  public void selectFragmentView() {
      FormData form = ctx.getForm()
      String view = form.getString("view")
      SessionManager.setFragmentView(view)
  }

  @POST
  @Path("add_preset")
  public String addPreset() {
      FormData form = ctx.getForm()
      String themeName = form.getString("theme_name")
      String presetName = form.getString("preset_name")
      String category = form.getString("category")
      String value = form.getString("value")
      if (value == null) {
          value = ""
      }
      try {
          return Editor.addPreset(themeName, presetName, category, value)
      } catch (Exception e) {
          throw new ThemeEditorException(e.getMessage(), e)
      }
  }

  @POST
  @Path("convert_to_preset")
    public void convertValueToPreset() {
      FormData form = ctx.getForm()
      String themeName = form.getString("theme_name")
      String presetName = form.getString("preset_name")
      String category = form.getString("category")
      String value = form.getString("value")
      if (value == null) {
          throw new ThemeEditorException("Preset value cannot be null")
      }
      try {
          Editor.convertCssValueToPreset(themeName, category, presetName, value)
      } catch (Exception e) {
          throw new ThemeEditorException(e.getMessage(), e)
      }
  }

  @POST
  @Path("edit_preset")
  public void editPreset() {
      FormData form = ctx.getForm()
      String themeName = form.getString("theme_name")
      String presetName = form.getString("preset_name")
      String value = form.getString("value")
    try {
        Editor.editPreset(themeName, presetName, value)
      } catch (Exception e) {
          throw new ThemeEditorException(e.getMessage(), e)
      }
  }

  @POST
  @Path("rename_preset")
  public void renamePreset() {
      FormData form = ctx.getForm()
      String themeName = form.getString("theme_name")
      String oldName = form.getString("old_name")
      String newName = form.getString("new_name")
    try {
        Editor.renamePreset(themeName, oldName, newName)
      } catch (Exception e) {
          throw new ThemeEditorException(e.getMessage(), e)
      }
  }

  @POST
  @Path("delete_preset")
  public void deletePreset() {
      FormData form = ctx.getForm()
      String themeName = form.getString("theme_name")
      String presetName = form.getString("preset_name")
     try {
        Editor.deletePreset(themeName, presetName)
      } catch (Exception e) {
          throw new ThemeEditorException(e.getMessage(), e)
      }
  }

  @POST
  @Path("make_element_use_named_style")
  public void makeElementUseNamedStyle() {
      FormData form = ctx.getForm()
      String id = form.getString("id")
      String styleName = form.getString("style_name")
      String themeName = form.getString("theme_name")
      Element element = ThemeManager.getElementById(id)
    try {
        Editor.makeElementUseNamedStyle(element, styleName, themeName)
      } catch (Exception e) {
          throw new ThemeEditorException(e.getMessage(), e)
      }
  }

  @POST
  @Path("move_element")
  public void moveElement() {
      FormData form = ctx.getForm()
      String srcId = form.getString("src_id")
      String destId = form.getString("dest_id")
      def order = form.getString("order") as Integer
      Element srcElement = ThemeManager.getElementById(srcId)
      Element destElement = ThemeManager.getElementById(destId)
    try {
       Editor.moveElement(srcElement, destElement, order)
      } catch (Exception e) {
          throw new ThemeEditorException(e.getMessage(), e)
      }
  }

  @POST
  @Path("paste_element")
  public String pasteElement() {
      FormData form = ctx.getForm()
      String destId = form.getString("dest_id")
      String id = getClipboardElement()
      if (id == null) {
          throw new ThemeEditorException("Nothing to paste")
      }
      Element element = ThemeManager.getElementById(id)
    try {
        return Editor.pasteElement(element, destId)
      } catch (Exception e) {
          throw new ThemeEditorException(e.getMessage(), e)
      }
  }

  @POST
  @Path("repair_theme")
  public void repairTheme() {
    FormData form = ctx.getForm()
      String src = form.getString("src")
    try {
        Editor.repairTheme(src)
      } catch (Exception e) {
          throw new ThemeEditorException(e.getMessage(), e)
      }
  }

  @POST
  @Path("save_theme")
  public void saveTheme() {
      FormData form = ctx.getForm()
      String src = form.getString("src")
      def indent = form.getString("indent") as Integer
      try {
          Editor.saveTheme(src, indent)
      } catch (Exception e) {
          throw new ThemeEditorException(e.getMessage(), e)
      }
  }

  @POST
  @Path("load_theme")
  public void loadTheme() {
      FormData form = ctx.getForm()
      String src = form.getString("src")
      try {
          Editor.loadTheme(src)
      } catch (Exception e) {
          throw new ThemeEditorException(e.getMessage(), e)
      }
  }

  @POST
  @Path("delete_theme")
  public void deleteTheme() {
      FormData form = ctx.getForm()
      String src = form.getString("src")
      try {
          Editor.deleteTheme(src)
      } catch (Exception e) {
          throw new ThemeEditorException(e.getMessage(), e)
      }
  }

  @POST
  @Path("save_changes")
  public void saveChanges() {
      try {
          Editor.saveChanges()
      } catch (Exception e) {
          throw new ThemeEditorException(e.getMessage(), e)
      }
  }

  @POST
  @Path("select_preset_group")
  public void selectPresetGroup() {
      FormData form = ctx.getForm()
      String group = form.getString("group")
      SessionManager.setPresetGroup(group)
  }

  @POST
  @Path("select_style_category")
  public void selectStyleCategory() {
      FormData form = ctx.getForm()
      String category = form.getString("category")
      SessionManager.setStyleCategory(category)
  }

  @POST
  @Path("select_style_edit_mode")
  public void selectStyleEditMode() {
      FormData form = ctx.getForm()
      String mode = form.getString("mode")
      SessionManager.setStyleEditMode(mode)
  }

  @POST
  @Path("select_style_layer")
  public void selectStyleLayer() {
      FormData form = ctx.getForm()
      String uid = form.getString("uid")
      Style layer = (Style) ThemeManager.getFormatById(uid)
      if (layer != null) {
          SessionManager.setStyleLayerId(uid)
      }
  }

  @POST
  @Path("select_named_style")
  public void selectNamedStyle() {
      FormData form = ctx.getForm()
      String uid = form.getString("uid")
      Style style = (Style) ThemeManager.getFormatById(uid)
      if (style != null) {
          SessionManager.setNamedStyleId(uid)
      }
  }

  @POST
  @Path("select_style_property_category")
  public void selectStylePropertyCategory() {
      FormData form = ctx.getForm()
      String category = form.getString("category")
      SessionManager.setStylePropertyCategory(category)
  }

  @POST
  @Path("select_style_selector")
  public void selectStyleSelector() {
      FormData form = ctx.getForm()
      String selector = form.getString("selector")
      SessionManager.setStyleSelector(selector)
  }

  @POST
  @Path("select_style_manager_mode")
  public void selectStyleManagerMode() {
      FormData form = ctx.getForm()
      String mode = form.getString("mode")
      SessionManager.setStyleManagerMode(mode)
  }

  @POST
  @Path("update_element_description")
  public void updateElementDescription() {
      FormData form = ctx.getForm()
      String id = form.getString("id")
      String description = form.getString("description")
      Element element = ThemeManager.getElementById(id)
    try {
        Editor.updateElementDescription(element, description)
      } catch (Exception e) {
          throw new ThemeEditorException(e.getMessage(), e)
      }
  }

  @POST
  @Path("update_element_properties")
  public void updateElementProperties() {
      FormData form = ctx.getForm()
      String id = form.getString("id")
      String property_map = form.getString("property_map")
      Map propertyMap = JSONObject.fromObject(property_map)
      Element element = ThemeManager.getElementById(id)
    try {
        Editor.updateElementProperties(element, propertyMap)
      } catch (Exception e) {
          throw new ThemeEditorException(e.getMessage(), e)
      }
  }

  @POST
  @Path("update_element_width")
  public void updateElementWidth() {
      FormData form = ctx.getForm()
      String id = form.getString("id")
      String width = form.getString("width")
      Format layout = ThemeManager.getFormatById(id)
    try {
        Editor.updateElementWidth(layout, width)
      } catch (Exception e) {
          throw new ThemeEditorException(e.getMessage(), e)
      }
  }

  @POST
  @Path("update_element_style_css")
  public void updateElementStyleCss() {
      FormData form = ctx.getForm()
      String id = form.getString("id")
      String viewName = form.getString("view_name")
      String cssSource = form.getString("css_source")
      Element element = ThemeManager.getElementById(id)
      Style selectedStyleLayer = getSelectedStyleLayer()
    try {
        Editor.updateElementStyleCss(element, selectedStyleLayer, viewName, cssSource)
    } catch (Exception e) {
          throw new ThemeEditorException(e.getMessage(), e)
      }
  }

  @POST
  @Path("update_named_style_css")
  public void updateNamedStyleCss() {
      FormData form = ctx.getForm()
      String style_uid = form.getString("style_uid")
      String cssSource = form.getString("css_source")
      Style style = (Style) ThemeManager.getFormatById(style_uid)
      try {
        Editor.updateNamedStyleCss(style, cssSource)
    } catch (Exception e) {
          throw new ThemeEditorException(e.getMessage(), e)
      }
  }

  @POST
  @Path("split_element")
  public void splitElement() {
      FormData form = ctx.getForm()
      String id = form.getString("id")
      Element element = ThemeManager.getElementById(id)
    try {
        Editor.splitElement(element)
    } catch (Exception e) {
          throw new ThemeEditorException(e.getMessage(), e)
      }
   }

  @POST
  @Path("update_element_style")
  public void updateElementStyle() {
      FormData form = ctx.getForm()
      String id = form.getString("id")
      String path = form.getString("path")
      String viewName = form.getString("view_name")
      String property_map = form.getString("property_map")
      Map propertyMap = JSONObject.fromObject(property_map)
      Element element = ThemeManager.getElementById(id)
      Style currentStyleLayer = getSelectedStyleLayer()
    try {
        Editor.updateElementStyle(element, currentStyleLayer, path, viewName, propertyMap)
    } catch (Exception e) {
          throw new ThemeEditorException(e.getMessage(), e)
      }
  }

  @POST
  @Path("update_element_visibility")
  public String updateElementVisibility() {
      FormData form = ctx.getForm()
      String id = form.getString("id")
      List<String> perspectives = form.getList("perspectives")
      boolean alwaysVisible = Boolean.valueOf(form.getString("always_visible"))
      Element element = ThemeManager.getElementById(id)
    try {
        Editor.updateElementVisibility(element, perspectives, alwaysVisible)
    } catch (Exception e) {
          throw new ThemeEditorException(e.getMessage(), e)
      }
  }

  @POST
  @Path("update_element_layout")
  public void updateElementPadding() {
      FormData form = ctx.getForm()
      String property_map = form.getString("property_map")
      Map propertyMap = JSONObject.fromObject(property_map)
      Element element = getSelectedElement()
    try {
        Editor.updateElementLayout(element, propertyMap)
    } catch (Exception e) {
          throw new ThemeEditorException(e.getMessage(), e)
      }
  }

  @POST
  @Path("update_element_widget")
  public void updateElementWidget() {
      FormData form = ctx.getForm()
      String id = form.getString("id")
      String viewName = form.getString("view_name")
      Element element = ThemeManager.getElementById(id)
    try {
        Editor.updateElementWidget(element, viewName)
    } catch (Exception e) {
          throw new ThemeEditorException(e.getMessage(), e)
      }
  }

  @POST
  @Path("delete_style_view")
  public void deleteStyleView() {
      FormData form = ctx.getForm()
      String styleUid = form.getString("style_uid")
      String viewName = form.getString("view_name")
      String themeName = form.getString("theme_name")
      Style style = (Style) ThemeManager.getFormatById(styleUid)
    try {
        Editor.deleteStyleView(style, viewName)
    } catch (Exception e) {
          throw new ThemeEditorException(e.getMessage(), e)
      }
  }


  /* API */

  public static ThemeDescriptor getThemeDescriptor(String themeName) {
      return ThemeManager.getThemeDescriptor(themeName)
  }

  public static List<Style> getNamedStyles(String applicationPath, name) {
      String currentThemeName = getCurrentThemeName(applicationPath, name)
      return Manager.getThemeManager().getNamedObjects(currentThemeName, "style")
  }

  public static List<FragmentType> getFragments(applicationPath) {
      def fragments = []
      String templateEngine = getTemplateEngine(applicationPath)
      for (f in Manager.getTypeRegistry().getTypes(TypeFamily.FRAGMENT)) {
          FragmentType fragmentType = (FragmentType) f
          if (fragments.contains(fragmentType)) {
              continue
          }
          fragments.add(fragmentType)
      }
      return fragments
  }

  public static List<ViewType> getViews(fragmentTypeName, applicationPath) {
      String templateEngine = getTemplateEngine(applicationPath)
      def views = []
      if (fragmentTypeName == null) {
          return views
      }
      FragmentType fragmentType = Manager.getTypeRegistry().lookup(TypeFamily.FRAGMENT, fragmentTypeName)
      if (fragmentType == null) {
          return views
      }
      for (ViewType viewType : ThemeManager.getViewTypesForFragmentType(fragmentType)) {
          if (templateEngine.equals(viewType.getTemplateEngine())) {
              views.add(viewType)
          }
      }
      return views
  }

  public static String getSelectedElementId() {
      return SessionManager.getElementId()
  }

  public static Element getSelectedElement() {
    String id = getSelectedElementId()
    if (id == null) {
      return null
    }
    return ThemeManager.getElementById(id)
  }

  public static String getClipboardElement() {
      return SessionManager.getClipboardElementId()
  }

  public static String getClipboardPreset() {
      return SessionManager.getClipboardPresetId()
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
      return SessionManager.getStyleEditMode()
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
      String selectedCategory = getSelectedStylePropertyCategory()

      Properties cssProperties = org.nuxeo.theme.html.Utils.getCssProperties()
      Enumeration<?> propertyNames = cssProperties.propertyNames()
      while (propertyNames.hasMoreElements()) {
          String name = (String) propertyNames.nextElement()
          String value = properties == null ? "" : properties.getProperty(name, "")
          String category = ThemeManager.getPreviewCategoryForProperty(name)
          String type = cssProperties.getProperty(name)
          if (selectedCategory.equals("*") || selectedCategory.equals(category)) {
              fieldProperties.add(new StyleFieldProperty(name, value, type))
          }
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

      Properties cssProperties = org.nuxeo.theme.html.Utils.getCssProperties()
      Enumeration<?> propertyNames = cssProperties.propertyNames()
      while (propertyNames.hasMoreElements()) {
          String name = (String) propertyNames.nextElement()
          String value = properties == null ? "" : properties.getProperty(name, "")
          String category = ThemeManager.getPreviewCategoryForProperty(name)
          if (selectedCategory.equals("") || selectedCategory.equals(category)) {
              fieldProperties.add(new StyleFieldProperty(name, value, category))
          }
      }
      return fieldProperties
  }

  public static String getSelectedStylePropertyCategory() {
      String category = SessionManager.getStylePropertyCategory()
      if (!category) {
          category = '*'
      }
      return category
  }

  public static List<StyleCategory> getStyleCategories() {
      String selectedStyleCategory = getSelectedStylePropertyCategory()
      Map<String, StyleCategory> categories = new LinkedHashMap<String, StyleCategory>()
      categories.put("", new StyleCategory("*", "all", selectedStyleCategory.equals("*")))
      for (String category : ThemeManager.getPreviewCategories()) {
            boolean selected = category.equals(selectedStyleCategory)
            categories.put(category, new StyleCategory(category, category, selected))
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
      return SessionManager.getStyleSelector()
  }

  public static Style getSelectedStyleLayer() {
      String selectedStyleLayerId = getSelectedStyleLayerId()
      if (selectedStyleLayerId == null) {
        return null
      }
      return (Style) ThemeManager.getFormatById(selectedStyleLayerId)
  }

  public static String getSelectedStyleLayerId() {
      return SessionManager.getStyleLayerId()
  }

  public static String getSelectedNamedStyleId() {
      return SessionManager.getNamedStyleId()
  }

  public static Style getSelectedNamedStyle() {
      String selectedNamedStyleId = getSelectedNamedStyleId()
      if (selectedNamedStyleId == null) {
        return null
      }
      return (Style) ThemeManager.getFormatById(selectedNamedStyleId)
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

  public static String getRenderedPropertiesForNamedStyle(Style style) {
      if (style == null) {
          return ""
      }
      boolean RESOLVE_PRESETS = false
      boolean IGNORE_VIEW_NAME = false
      boolean IGNORE_CLASSNAME = true
      boolean INDENT = true
      return org.nuxeo.theme.html.Utils.styleToCss(style, style.getSelectorViewNames(), RESOLVE_PRESETS, IGNORE_VIEW_NAME, IGNORE_CLASSNAME, INDENT)
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

  /* Presets */

  public static List<String> getPresetGroupsForSelectedCategory() {
      return getPresetGroups(getSelectedStyleCategory())
  }

  public static List<String> getPresetGroups(String category) {
      def groups = []
      def groupNames = []
      for (PresetType preset : PresetManager.getGlobalPresets(null, category)) {
          String group = preset.getGroup()
          if (!groupNames.contains(group)) {
              groups.add(group)
          }
          groupNames.add(group)
      }
      return groups
  }

  public static List<PresetInfo> getGlobalPresets(String group) {
      def presets = []
      for (preset in  PresetManager.getGlobalPresets(group, null)) {
          presets.add(new PresetInfo(preset))
      }
      return presets
  }

  public static List<PresetInfo> getCustomPresets(String themeName) {
      def presets = []
      for (preset in PresetManager.getCustomPresets(themeName, null)) {
          presets.add(new PresetInfo(preset))
      }
      return presets
  }

  public static List<PresetInfo> getPresetsForSelectedGroup(applicationPath, name) {
      String category = getSelectedStyleCategory()
      String group = getSelectedPresetGroup()
      String themeName = getCurrentThemeName(applicationPath, name)
      def presets = []
      def presetTypes = group ? PresetManager.getGlobalPresets(group, category) : PresetManager.getCustomPresets(themeName, category)
      for (preset in presetTypes) {
          presets.add(new PresetInfo(preset))
      }
      return presets
  }

  public static String getFragmentFactoryMode() {
      return SessionManager.getFragmentFactoryMode()
  }

  public static String getPresetManagerMode() {
      return SessionManager.getPresetManagerMode()
  }

  public static String getStyleManagerMode() {
      return SessionManager.getStyleManagerMode()
  }

  public static List<String> getUnidentifiedPresetNames(String themeName) {
      return PresetManager.getUnidentifiedPresetNames(themeName)
  }

  public static String renderStyleView(Style style, String viewName) {
      List<String> viewNames = new ArrayList<String>()
      viewNames.add(viewName)
      return org.nuxeo.theme.html.Utils.styleToCss(style,
              viewNames, false, true, true, true)
  }

  public static List<String> getHardcodedColors(final themeName) {
      return Editor.getHardcodedColors(themeName)
  }

  public static List<String> getHardcodedImages(final themeName) {
      return Editor.getHardcodedImages(themeName)
  }

  /* Session */

  public static String getSelectedPresetGroup() {
      String category = SessionManager.getPresetGroup()
      return category
  }

  public static String getSelectedStyleCategory() {
        String category = SessionManager.getStyleCategory()
        if (!category) {
            category = "page"
        }
        return category
  }

  public static String getSelectedFragmentType() {
      return SessionManager.getFragmentType()
  }

  public static String getSelectedFragmentView() {
      return SessionManager.getFragmentView()
  }

  public static String getTemplateEngine(applicationPath) {
      return ThemeManager.getTemplateEngineName(applicationPath)
  }

  public static String getDefaultTheme(applicationPath, name) {
      String defaultTheme = ThemeManager.getDefaultTheme(applicationPath)
      if(defaultTheme == null || defaultTheme.equals("")) {
          def ctx = WebEngine.getActiveContext()
          def moduleName = ctx.getModule().getName()
          defaultTheme = ThemeManager.getDefaultTheme(applicationPath, name, moduleName)
      }
      return defaultTheme
  }

  public static String getCurrentThemeName(applicationPath, name) {
    String defaultTheme = getDefaultTheme(applicationPath, name)
    def ctx = WebEngine.getActiveContext()
    String currentPagePath = ctx.getCookie("nxthemes.theme")
    if (currentPagePath == null) {
        return defaultTheme.split("/")[0]
    }
    return currentPagePath.split("/")[0]
  }

  public static List<PageElement> getPages(applicationPath, name) {
    ThemeManager themeManager = Manager.getThemeManager()
    def ctx = WebEngine.getActiveContext()
    String currentPagePath = ctx.getCookie("nxthemes.theme")
    String defaultTheme = getDefaultTheme(applicationPath, name)
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

  public static List<ThemeInfo> getThemes(applicationPath, name) {
    def themes = []
    String defaultTheme = getDefaultTheme(applicationPath, name)
    String defaultThemeName = defaultTheme.split("/")[0]
    String defaultPageName = defaultTheme.split("/")[1]
    String currentThemeName = getCurrentThemeName(applicationPath, name)
    String templateEngine = getTemplateEngine(applicationPath)
    for (themeName in ThemeManager.getThemeNames(templateEngine)) {
      String path = String.format("%s/%s", themeName, defaultPageName)
      themes.add(new ThemeInfo(themeName, path))
    }
    return themes
  }

  public static ThemeManager getThemeManager() {
      return Manager.getThemeManager()
  }

}