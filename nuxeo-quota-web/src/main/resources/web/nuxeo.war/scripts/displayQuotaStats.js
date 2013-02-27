function invokeGetQuotaStatistics(currentDocId, currentLang) {
	var ctx = {
	};

	var getQuotasExec = jQuery().automation('Quotas.GetStatistics');
	getQuotasExec.setContext(ctx);
	getQuotasExec.addParameter("documentRef", currentDocId);
	getQuotasExec.addParameter("language", currentLang);
	getQuotasExec.executeGetBlob(
     function(data, status, xhr) {
		jQuery.plot(jQuery("#quota_stats"), data, {series: {pie: {show: true,label: {show: false}}}});
	}, function(xhr, status, errorMessage) {
		jQuery('<div>Can not display statistics. Please run initial computation from Admin Center/Quota</div>').appendTo('#quota_stats');
	}, true);
};

function displayQuotaStats(currentDocId, currentLang) {
	invokeGetQuotaStatistics(currentDocId, currentLang);
};