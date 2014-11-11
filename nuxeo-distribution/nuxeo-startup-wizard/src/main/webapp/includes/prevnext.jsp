
 </div>
 <div class="buttonContainer">
<%if (currentPage.prev()!=null) { %>
 <input type="button" class="glossyButton" id="btnPrev" value="<fmt:message key="label.action.prev"/>" onclick="navigateTo('<%=currentPage.prev().getAction()%>');"/>
<%}%>
 <input type="submit" class="glossyButton" id="btnNext" value="<fmt:message key="label.action.next"/>"/>
 </div>

</form>