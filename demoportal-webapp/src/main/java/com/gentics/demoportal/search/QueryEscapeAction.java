package com.gentics.demoportal.search;

import java.util.Set;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.portalnode.action.GenericPluggableAction;
import com.gentics.api.portalnode.action.PluggableActionException;
import com.gentics.api.portalnode.action.PluggableActionRequest;
import com.gentics.api.portalnode.action.PluggableActionResponse;

/**
 * Pluggable Action that escapes the given parameters so that they can be used
 * in lucene queries
 */
public class QueryEscapeAction extends GenericPluggableAction {

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.gentics.api.portalnode.action.PluggableAction#processAction(com.gentics.api.portalnode.action.PluggableActionRequest,
	 *      com.gentics.api.portalnode.action.PluggableActionResponse)
	 */
	@SuppressWarnings("unchecked")
	public boolean processAction(PluggableActionRequest req,
			PluggableActionResponse res) throws PluggableActionException {
		// get the parameter names
		Set<String> params = (Set<String>) req.getParameterNames();

		// iterate over all parameters
		for (String param : params) {
			// get the parameter value
			String value = ObjectTransformer.getString(req.getParameter(param),
					null);
			if (value != null) {
				// escape the value and add to the response
				res.setParameter(param, escapeQuery(value));
			}
		}

		return true;
	}

	/**
	 * Escape the given query String for usage in a lucene query
	 * 
	 * @param query
	 *            given query string
	 * @return escaped query string
	 */
	private String escapeQuery(String query) {
		String ret = query;
		ret = ret.replace("]", "\\]");
		ret = ret.replace("[", "\\[");
		return ret;
	}
}
