<div id="contextButton" class="context-edit-button" style="display:none">
  <a href="javascript:displayContextChooser()" title="__MSG_label.context.edit__" >__MSG_label.context.settings__</a>
</div>
<div id="contextChooser" style="display:none" class="contextPanel" >
  __MSG_Domain__ :
  <select name="contextPathChooser" id="contextPathChooser">
    <option value='/'>__MSG_label.context.all__</option>
  </select>
  <input type="button" value="__MSG_command.save__" onclick="saveContext()"/>
</div>
<div style="clear:both;"></div>
