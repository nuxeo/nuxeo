<div id="nxthemesFragmentFactory" class="nxthemesScreen">

<h1 class="nxthemesEditor">Add fragment</h1>

<#if selected_element_id>

  <table class="fragmentFactory">
  <tr>
  <th style="width: 25%">
    1. Select fragment type
  </th>
  <th style="width: 25%">
    2. Select view
  </th>
  <th style="width: 25%">
    3. Apply a style
  </th>
  <th style="width: 25%">
    4. Review selection
  </th>
  </tr>

  <tr>
  <td style="vertical-align: top">
  <ul class="nxthemesSelector">
  <#list fragments?sort_by('typeName') as fragment>
    <li <#if fragment.getTypeName() = selected_fragment_type>class="selected"</#if>><a href="javascript:void(0)"
  onclick="NXThemesFragmentFactory.selectFragmentType('${fragment.getTypeName()?js_string}')">
    <img src="${basePath}/skin/nxthemes-editor/img/fragment-16.png" width="16" height="16" /> ${fragment.getTypeName()}</a></li>
  </#list>
  </ul>
  </td>

  <td style="vertical-align: top">
  <#if views>
  <ul class="nxthemesSelector">
  <#list views?sort_by('viewName') as view>
    <li <#if view.getViewName() = selected_fragment_view>class="selected"</#if>><a href="javascript:void(0)"
  onclick="NXThemesFragmentFactory.selectView('${view.getViewName()?js_string}')">
    <img src="${basePath}/skin/nxthemes-editor/img/view-16.png" width="16" height="16" /> ${view.getViewName()}</a></li>
  </#list>
  </ul>
  </#if>
  </td>

  <td style="vertical-align: top">
    <#if selected_fragment_type & selected_fragment_view>
    <ul class="nxthemesSelector">
    <#list styles?sort_by('name') as style>
      <li <#if style.name = selected_fragment_style>class="selected"</#if>><a href="javascript:void(0)"
      onclick="NXThemesFragmentFactory.selectStyle('${style.name?js_string}')"><img src="${basePath}/skin/nxthemes-editor/img/style-16.png" width="16" height="16" /> ${style.name}</a></li>
    </#list>
    </ul>
    </#if>
  </td>

  <td style="vertical-align: top; text-align: center">
    <#if selected_fragment_type & selected_fragment_view>

      <@nxthemes_themestyles cache="true" inline="true" theme="${current_theme_name}" />

      <@nxthemes_view resource="fragment-preview.json" />
      ${Root.createFragmentPreview(current_theme_name)}

      <div id="fragmentPreviewArea" theme="${current_theme_name}"></div>

      <form class="nxthemesForm">
        <div>
          <button onclick="NXThemesFragmentFactory.addFragment('${selected_fragment_type?js_string}/${selected_fragment_view?js_string}', '${selected_fragment_style?js_string}', '${selected_element_id?js_string}'); return false;">ADD FRAGMENT</button>

        </div>
      </form>
    </#if>
  </td>
  </tr>
  </table>

<#else>

<p class="nxthemesEditor">
  <em>Cannot find the container to insert a fragment into.</em>
</p>
</#if>

</div>
