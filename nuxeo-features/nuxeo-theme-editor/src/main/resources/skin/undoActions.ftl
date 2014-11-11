
<#if undo_buffer>
<#if undo_buffer.canUndo()>
<div class="nxthemesUndoButtons" style="cursor: default">
  <a href="javascript:NXThemesEditor.undo('${current_theme_name}')"><img src="${basePath}/skin/nxthemes-editor/img/undo-14.png" style="vertical-align: middle" /> undo ${undo_buffer.getMessage()}</a>
</div>
</#if>
</#if>
