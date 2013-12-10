package com.gentics.demoportal.search;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import org.apache.log4j.Logger;

import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.portalnode.action.GenericPluggableAction;
import com.gentics.api.portalnode.action.PluggableActionException;
import com.gentics.api.portalnode.action.PluggableActionRequest;
import com.gentics.api.portalnode.action.PluggableActionResponse;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.cr.CRConfigUtil;
import com.gentics.cr.CRRequest;
import com.gentics.cr.CRResolvableBean;
import com.gentics.cr.RequestProcessor;
import com.gentics.cr.StaticRPContainer;
import com.gentics.cr.configuration.StaticConfigurationContainer;
import com.gentics.cr.lucene.search.LuceneRequestProcessor;

/**
 * Pluggable Action which uses the content connector to access a lucene index
 * Request Parameters:
 * <ul>
 * <li><i>query</i> Query String for searching the lucene index</li>
 * <li><i>highlightquery</i> </li>
 * <li><i>count</i> Maximum number of results which shall be returned</li>
 * <li><i>start</i> Start Index of the first returned result (paging)</li>
 * <li><i>sorting</i> Sorting information in the form [fieldname]:asc|desc</li>
 * <li><i>attribute</i> Attribute(s) which will be fetched from the index (may be multivalue)</li>
 * <li><i></i></li>
 * </ul>
 */
public class GetResultAction extends GenericPluggableAction {
	/**
	 * The logger
	 */
	private static final Logger logger = Logger
			.getLogger(GetResultAction.class);

	/* (non-Javadoc)
	 * @see com.gentics.api.portalnode.action.PluggableAction#processAction(com.gentics.api.portalnode.action.PluggableActionRequest, com.gentics.api.portalnode.action.PluggableActionResponse)
	 */
	@SuppressWarnings("unchecked")
	public boolean processAction(PluggableActionRequest req,
			PluggableActionResponse res) throws PluggableActionException {

		// read the request parameters
		String query = ObjectTransformer.getString(req.getParameter("query"),
				null);
		String highlightquery = ObjectTransformer.getString(req
				.getParameter("highlightquery"), null);
		String scount = ObjectTransformer.getString(req.getParameter("count"),
				null);
		String sstart = ObjectTransformer.getString(req.getParameter("start"),
				null);
		String sorting = null;
		if (req.isParameterSet("sorting")) {
			sorting = ObjectTransformer.getString(req.getParameter("sorting"), null);
		}

		Collection attributes = ObjectTransformer.getCollection(req
				.getParameter("attribute"), Collections.EMPTY_LIST);

		// only query if the query string is empty
		if (!ObjectTransformer.isEmpty(query)) {
			try {
				long s1 = System.currentTimeMillis();
				// get the contentconnector config
				CRConfigUtil config = StaticConfigurationContainer.getConfig(
						this.getModule().getModuleId(), null);
				// get the request processor
				RequestProcessor rp = StaticRPContainer.getRP(config, 1);
				long e1 = System.currentTimeMillis();
				logger.debug("INSTANTIATE TOOK " + (e1 - s1) + "ms");
				long s2 = System.currentTimeMillis();
				// create the request
				CRRequest rq = new CRRequest();
				rq.setRequestFilter(query);
				rq.set("secondary", "true");
				if (highlightquery != null)
					rq
							.set(RequestProcessor.HIGHLIGHT_QUERY_KEY,
									highlightquery);
				if (scount != null && !"".equals(scount)) {
					rq.setCountString(scount);
				}
				if (sstart != null && !"".equals(sstart)) {
					rq.setStartString(sstart);
				}
				rq.set(RequestProcessor.META_RESOLVABLE_KEY, true);

				// add attributes to be returned
				rq.setAttributeArray((String[]) attributes.toArray(new String[attributes.size()]));

				if (sorting != null && !"".equals(sorting)) {
					rq.setSortArray(new String[] { sorting });
				}

				// get the objects
				Collection<CRResolvableBean> coll = rp.getObjects(rq);
				if (coll != null) {
					Iterator<CRResolvableBean> it = coll.iterator();
					if (it.hasNext()) {
						// the first object contains the meta information
						CRResolvableBean meta = it.next();
						// remove the meta object
						it.remove();
						res.setParameter("hits", meta
								.get(LuceneRequestProcessor.META_HITS_KEY));
						res.setParameter("start", meta
								.get(LuceneRequestProcessor.META_START_KEY));
						if (meta.get("suggestions") instanceof Map) {
							Map suggestions = (Map)meta.get("suggestions");
							if (!suggestions.isEmpty()) {
								res.setParameter("suggestions", suggestions);
							}
						}
					}

					// enhance the information about pages linking to binaries
					while (it.hasNext()) {
						CRResolvableBean object = it.next();
						// get the datasource
						Datasource ds = getContext().getModule().getGenticsPortletContext().getDatasource();
						String linkedInPages = ObjectTransformer.getString(object.get("linkedinpages"), null);
						if (!ObjectTransformer.isEmpty(linkedInPages)) {
							// split into the page id's
							String[] ids = linkedInPages.trim().split("\\s+");
							Collection<Resolvable> linkingPages = new Vector<Resolvable>();
							for (int i = 0; i < ids.length; i++) {
								String contentId = "10007." + ids[i].trim();
								// get the linking page
								Resolvable linkingPage = PortalConnectorFactory.getContentObject(contentId, ds);
								if (linkingPage != null) {
									linkingPages.add(linkingPage);
								}
							}
							object.set("linkedinpages", linkingPages);
						}
					}
				}
				long e2 = System.currentTimeMillis();
				long searchDuration = e2 - s2;
				logger.debug("Search took " + searchDuration + "ms");

				res.setParameter("result", coll);
				res.setParameter("timemsec", searchDuration);
				res.setParameter("timesec", (float)searchDuration/1000f);
				return true;
			} catch (Exception ex) {
				logger.error(ex.getMessage());
				ex.printStackTrace();
			}
		}

		return false;
	}
}