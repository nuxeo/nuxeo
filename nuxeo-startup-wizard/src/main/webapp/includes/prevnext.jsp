
 <br/><br/><br/>
<%if (currentPage.prev()!=null) { %>
 <input type="button" class="glossyButton" value="<fmt:message key="label.action.prev"/>" onclick="navigateTo('<%=currentPage.prev().getAction()%>');"/>
<%}%>
 <input type="submit" class="glossyButton" value="<fmt:message key="label.action.next"/>"/>

</form>