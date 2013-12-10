package com.gentics.demoportal;

import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.portalnode.action.GenericPluggableAction;
import com.gentics.api.portalnode.action.PluggableActionException;
import com.gentics.api.portalnode.action.PluggableActionRequest;
import com.gentics.api.portalnode.action.PluggableActionResponse;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;

/**
 * @author Clemens
 * 
 * the StartfolderAction will determine a start folder for a navigation
 * structure depending on a provided page and a root folder.
 * 
 * Parameters: 
 * rootContentId - the contentid of the topmost folder 
 * pageContentId - the contentid to start from
 * folderContentId - the contentid of the folder to start from (alternative to search starting with a page)
 * 
 * Return parameters:
 * contentid - the determined contentid
 * folderid - the folder id of the page's folder
 * 
 * Mode of operation: Assume you've got a folder structure as follows
 * 
 * root 
 * + folder a 
 *   + folder aa 
 *     + page aa1 
 *   + folder ab 
 * + folder b 
 *   + page b1 
 * + folder c
 * 
 * and you set the root folder's contentid as "rootContentId", as well as page
 * aa1's contentid as "pageContentId", the action will work it's way upwards,
 * until it encounters a folder which has the root folder as it's parent. So in
 * this case you will retrieve folder a's contentid as a result.
 */
public class StartfolderAction extends GenericPluggableAction {

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.gentics.api.portalnode.action.PluggableAction#processAction(com.gentics.api.portalnode.action.PluggableActionRequest,
	 *      com.gentics.api.portalnode.action.PluggableActionResponse)
	 */
	public boolean processAction(PluggableActionRequest request,
			PluggableActionResponse response) throws PluggableActionException {
		// retrieve parameters
		String pageContentId = ObjectTransformer.getString(request
				.getParameter("pageContentId"), null);
		String folderContentId = ObjectTransformer.getString(request
				.getParameter("folderContentId"), null);
		String rootContentId = ObjectTransformer.getString(request
				.getParameter("rootContentId"), null);

		try {
			// check parameters
			if ((pageContentId == null && folderContentId == null)
					|| rootContentId == null) {
				response.setFeedbackMessage("Missing parameters");
				throw new Exception("Missing parameters - pageContentId {"
						+ pageContentId + "} or folderContentId {"
						+ folderContentId + "} and rootContentId {"
						+ rootContentId + "} must not be null");
			}

			// retrieve datasource
			Datasource ds = getContext().getModule().getGenticsPortletContext()
					.getDatasource();
			if (ds == null) {
				response
						.setFeedbackMessage("No default datasource available for this portlet");
				throw new Exception(
						"No default datasource available for this portlet");
			}

			Object folder = null;

			if (pageContentId != null) {
				// load page from db
				Resolvable page = PortalConnectorFactory.getContentObject(
						pageContentId, ds);
				if (page != null) {
					folder = page.get("folder_id");
				}
			} else {
				// loag folder from db
				folder = PortalConnectorFactory.getContentObject(
						folderContentId, ds);
			}
			Resolvable r;
			String parentFolderId;
			
			
			
			// now resolve the topmost folder beneath the root folder 
			if (folder instanceof Resolvable) {
				r = (Resolvable) folder;
				parentFolderId = r.get("folder_id").toString();
				
				// add folderid to the response 
				response.setParameter("folderid", r.get("contentid"));

				// 10002.0 is the topmost folder so we have to stop there too
				while (!parentFolderId.equals(rootContentId)
						&& !parentFolderId.equals("10002.0")) {
					folder = r.get("folder_id");
					if (folder instanceof Resolvable) {
						r = (Resolvable) folder;
						parentFolderId = r.get("folder_id").toString();
					} else {
						throw new Exception(
								"Unable to resolve root folder, as the non-resolvable folder {"
										+ folder.toString()
										+ "} was encountered");
					}
				}

				// the topmost folder has been resolved
				response.setParameter("contentid", r.get("contentid")
						.toString());
				return true;
			} else {
				throw new Exception(
						"Unable to resolve root folder, as the non-resolvable folder {"
								+ folder.toString() + "} was encountered");
			}
		} catch (Exception e) {
			logger.error(
					"unable to generate datasource filter for page contentid {"
							+ pageContentId + "}", e);
			return false;
		}
	}
}