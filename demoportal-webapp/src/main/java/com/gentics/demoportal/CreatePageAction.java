/**
 * 
 */
package com.gentics.demoportal;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.json.JSONObject;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.portalnode.action.GenericPluggableAction;
import com.gentics.api.portalnode.action.PluggableActionException;
import com.gentics.api.portalnode.action.PluggableActionRequest;
import com.gentics.api.portalnode.action.PluggableActionResponse;
import com.gentics.lib.etc.StringUtils;

/**
 * Pluggable action which calls the GCN backend to create a new page and returns the id of the new page<br/>
 * Parameters:
 * <ul>
 * <li><i>backend_url</i> url to the backend service (excluding 'CNPortletapp/rest/page/add', e.g. 'http://my.domain.com/')</li>
 * <li><i>sid</i> session id, with which the new page shall be created</li>
 * <li><i>folder_id</i> id of the folder to create the page in</li>
 * <li><i>template_id</i> id of the template for the new page</li>
 * <li><i>language</i> language code for the new page</li>
 * </ul>
 * Response:
 * <ul>
 * <li><i>id</i> page id of the new page</li>
 * </ul>
 */
public class CreatePageAction extends GenericPluggableAction {
	public final static String BACKEND_URL_PARAM = "backend_url";

	public final static String SID_PARAM = "sid";
	
	public final static String FOLDER_ID_PARAM = "folder_id";
	
	public final static String TEMPLATE_ID_PARAM = "template_id";

	public final static String LANGUAGE_PARAM = "language";

	/* (non-Javadoc)
	 * @see com.gentics.api.portalnode.action.PluggableAction#processAction(com.gentics.api.portalnode.action.PluggableActionRequest, com.gentics.api.portalnode.action.PluggableActionResponse)
	 */
	public boolean processAction(PluggableActionRequest request,
			PluggableActionResponse response) throws PluggableActionException {
		String backendUrl = ObjectTransformer.getString(request.getParameter(BACKEND_URL_PARAM), "") + "CNPortletapp/rest/page/create";
		String sid = ObjectTransformer.getString(request.getParameter(SID_PARAM), null);
		if (StringUtils.isEmpty(sid)) {
			throw new PluggableActionException("Error while creating new page: no SID given");
		}
		Integer folderId = ObjectTransformer.getInteger(request.getParameter(FOLDER_ID_PARAM), null);
		if (folderId == null) {
			throw new PluggableActionException("Error while creating new page: no folder_id given");
		}
		Integer templateId = ObjectTransformer.getInteger(request.getParameter(TEMPLATE_ID_PARAM), null);
		if (templateId == null) {
			throw new PluggableActionException("Error while creating new page: no template_id given");
		}
		String language = ObjectTransformer.getString(request.getParameter(LANGUAGE_PARAM), null);

		try {
			JSONObject createPageRequest = new JSONObject();
			createPageRequest.put("folderId", folderId);
			createPageRequest.put("templateId", templateId);
			if (language != null) {
				createPageRequest.put("language", language);
			}

			PostMethod postMethod = new PostMethod(backendUrl);
			postMethod.setQueryString(new NameValuePair[] {
				new NameValuePair("sid", sid)
			});
			postMethod.setRequestEntity(new StringRequestEntity(createPageRequest.toString(), "application/json", "UTF-8"));

			HttpClient client = new HttpClient();
			int responseCode = client.executeMethod(postMethod);
			switch(responseCode) {
			case HttpServletResponse.SC_OK:
				JSONObject createResponse = new JSONObject(postMethod.getResponseBodyAsString());
				if ("OK".equals(createResponse.getJSONObject("responseInfo").getString("responseCode"))) {
					String newPageId = createResponse.getJSONObject("page").getString("id");
					response.setParameter("id", newPageId);
					return true;
				} else {
					response.setFeedbackMessage(createResponse.getJSONObject("responseInfo").getString("responseMessage"));
					return false;
				}
			default:
				logger.error("Request to create new page failed: response code was " + responseCode);
				response.setFeedbackMessage("Request to create new page failed: response code was " + responseCode);
				return false;
			}
		} catch(Exception e) {
			throw new PluggableActionException("Creating new page failed", e);
		}
	}
}
