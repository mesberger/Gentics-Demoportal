/**
 * 
 */
package com.gentics.demoportal;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.json.JSONObject;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.portalnode.action.GenericPluggableAction;
import com.gentics.api.portalnode.action.PluggableActionException;
import com.gentics.api.portalnode.action.PluggableActionRequest;
import com.gentics.api.portalnode.action.PluggableActionResponse;
import com.gentics.lib.etc.StringUtils;

/**
 * Pluggable action to translate a page into another language
 */
public class TranslatePageAction extends GenericPluggableAction {
	/**
	 * name of the request parameter containing the backend url
	 */
	public final static String BACKEND_URL_PARAM = "backend_url";

	/**
	 * name of the request parameter containing the sid
	 */
	public final static String SID_PARAM = "sid";

	/**
	 * name of the request parameter containing the page id
	 */
	public final static String PAGE_ID_PARAM = "page_id";

	/**
	 * name of the request parameter containing the language code
	 */
	public final static String LANGUAGE_PARAM = "language";

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.gentics.api.portalnode.action.PluggableAction#processAction(com.gentics.api.portalnode.action.PluggableActionRequest,
	 *      com.gentics.api.portalnode.action.PluggableActionResponse)
	 */
	public boolean processAction(PluggableActionRequest request,
			PluggableActionResponse response) throws PluggableActionException {
		String backendUrl = ObjectTransformer.getString(request
				.getParameter(BACKEND_URL_PARAM), "")
				+ "CNPortletapp/rest/page/translate/";
		String sid = ObjectTransformer.getString(request
				.getParameter(SID_PARAM), null);
		if (StringUtils.isEmpty(sid)) {
			throw new PluggableActionException(
					"Error while translating page: no SID given");
		}
		Integer pageId = ObjectTransformer.getInteger(request
				.getParameter(PAGE_ID_PARAM), null);
		if (pageId == null) {
			throw new PluggableActionException(
					"Error while translating page: no pageId given");
		}
		backendUrl += pageId;
		String language = ObjectTransformer.getString(request
				.getParameter(LANGUAGE_PARAM), null);
		if (StringUtils.isEmpty(language)) {
			throw new PluggableActionException(
					"Error while translating page: no language id given");
		}

		try {
			PostMethod postMethod = new PostMethod(backendUrl);
			postMethod.setQueryString(new NameValuePair[] {
					new NameValuePair("sid", sid),
					new NameValuePair("language", language) });

			HttpClient client = new HttpClient();
			int responseCode = client.executeMethod(postMethod);
			switch (responseCode) {
			case HttpServletResponse.SC_OK:
				JSONObject loadResponse = new JSONObject(postMethod
						.getResponseBodyAsString());
				if ("OK".equals(loadResponse.getJSONObject("responseInfo")
						.getString("responseCode"))) {
					String newPageId = loadResponse.getJSONObject("page")
							.getString("id");
					response.setParameter("id", newPageId);
					return true;
				} else {
					response.setFeedbackMessage(loadResponse.getJSONObject(
							"responseInfo").getString("responseMessage"));
					return false;
				}
			default:
				logger
						.error("Request to translate page failed: response code was "
								+ responseCode);
				response
						.setFeedbackMessage("Request to translate page failed: response code was "
								+ responseCode);
				return false;
			}
		} catch (Exception e) {
			throw new PluggableActionException("Translating the page {"
					+ pageId + "} into {" + language + "} failed", e);
		}
	}
}
