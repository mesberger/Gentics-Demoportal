package com.gentics.demoportal.comments;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.expressionparser.ExpressionParser;
import com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.portalnode.action.GenericPluggableAction;
import com.gentics.api.portalnode.action.PluggableActionException;
import com.gentics.api.portalnode.action.PluggableActionRequest;
import com.gentics.api.portalnode.action.PluggableActionResponse;

/**
 * Pluggable Action that gets a collection of resolvables and enhances them by
 * adding comment information<br/> Request Parameters:
 * <ul>
 * <li><i>objects</i> Collection of Resolvables holding the objects</li>
 * <li><i>datasource</i> id of the datasource holding the comment data</li>
 * </ul>
 * Response Parameters:
 * <ul>
 * <li><i>objects</i> Collection of Resolvables enhanced with comment
 * information</li>
 * </ul>
 * 
 * @author norbert
 */
public class AddCommentInfo extends GenericPluggableAction {
	/**
	 * name of the request parameter holding of datasource id
	 */
	public final static String REQUEST_PARAM_DATASOURCE = "datasource";

	/**
	 * name of the request parameter holding the objects collection
	 */
	public final static String REQUEST_PARAM_OBJECTS = "objects";

	/**
	 * name of the response parameter holding the enhanced objects collection
	 */
	public final static String RESPONSE_PARAM_OBJECTS = "objects";

	/**
	 * Filter expression for getting the comments for a given contentid
	 */
	public final static String GET_COMMENTS_FILTER = "object.obj_type == 35000 AND object.forcontentid == data.contentid";

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.gentics.api.portalnode.action.PluggableAction#processAction(com.gentics.api.portalnode.action.PluggableActionRequest,
	 *      com.gentics.api.portalnode.action.PluggableActionResponse)
	 */
	public boolean processAction(PluggableActionRequest request,
			PluggableActionResponse response) throws PluggableActionException {
		String datasourceId = ObjectTransformer.getString(request
				.getParameter(REQUEST_PARAM_DATASOURCE), null);
		try {
			// get the datasource
			Datasource ds = getContext().getModule().getDatasource(datasourceId);
			if (ds == null) {
				logger
						.error("Error while processing action: datasource with id {"
								+ datasourceId + "} could not be found");
				response.setFeedbackMessage("configuration.error");
				return false;
			}
			// prepare the datasource filter
			DatasourceFilter filter = ds
					.createDatasourceFilter(ExpressionParser.getInstance()
							.parse(GET_COMMENTS_FILTER));

			// prepare the resolvable for the filter
			final Map<String, Object> data = new HashMap<String, Object>();
			filter.addBaseResolvable("data", new Resolvable() {
				/*
				 * (non-Javadoc)
				 * 
				 * @see com.gentics.api.lib.resolving.Resolvable#canResolve()
				 */
				public boolean canResolve() {
					return true;
				}

				/*
				 * (non-Javadoc)
				 * 
				 * @see com.gentics.api.lib.resolving.Resolvable#get(java.lang.String)
				 */
				public Object get(String key) {
					return data.get(key);
				}

				/*
				 * (non-Javadoc)
				 * 
				 * @see com.gentics.api.lib.resolving.Resolvable#getProperty(java.lang.String)
				 */
				public Object getProperty(String key) {
					return get(key);
				}
			});

			// get the collection of objects from the request
			Collection objects = ObjectTransformer.getCollection(request
					.getParameter(REQUEST_PARAM_OBJECTS),
					Collections.EMPTY_LIST);

			// prepare the resulting collection
			Collection<Resolvable> results = new Vector<Resolvable>(objects
					.size());

			// iterate through the collection of objects
			for (Iterator iterator = objects.iterator(); iterator.hasNext();) {
				Object o = iterator.next();
				if (o instanceof Resolvable) {
					// set the contentid into the filter
					Resolvable r = (Resolvable) o;
					data.put("contentid", r.get("contentid"));

					// count the number of comments
					int count = ds.getCount(filter);
					Resolvable lastComment = null;
					// get the last comment
					if (count > 0) {
						Collection comments = ds
								.getResult(
										filter,
										new String[] { "userid", "useremail",
												"text", "forcontentid", "ts",
												"username" },
										0,
										1,
										new Datasource.Sorting[] { new Datasource.Sorting(
												"ts", Datasource.SORTORDER_DESC) });
						if (comments.size() > 0) {
							lastComment = (Resolvable) comments.iterator()
									.next();
						}
					}

					// add the enhanced object into the results collection
					results.add(new EnhancedResolvable(r, count, lastComment));
				} else {
					// found a non-resolvable, ignoring it
					if (o != null) {
						logger
								.warn("Found an instance of {"
										+ o.getClass()
										+ "} in the collection, which is not a Resolable, ignoring it");
					} else {
						logger
								.warn("Found null in the collection, ignoring it");
					}
				}
			}

			// set the enhanced objects into the response
			response.setParameter(RESPONSE_PARAM_OBJECTS, results);
		} catch (Exception e) {
			logger.error("Error while getting the comments information", e);
			response.setFeedbackMessage("runtime.error");
			return false;
		}

		return true;
	}

	/**
	 * Class for the enhanced resolvable
	 * 
	 * @author norbert
	 */
	public static class EnhancedResolvable implements Resolvable {
		/**
		 * name of the enhanced property holding the number of comments
		 */
		public final static String COMMENTSCOUNT_NAME = "comments";

		/**
		 * name of the enhanced property holding the last comment
		 */
		public final static String LASTCOMMENT_NAME = "lastcomment";

		/**
		 * wrapped resolvable
		 */
		private Resolvable wrappedResolvable;

		/**
		 * number of comments
		 */
		private int commentsCount;

		/**
		 * last comment
		 */
		private Resolvable lastComment;

		/**
		 * Create an instance of the enhanced resolvable
		 * 
		 * @param wrappedResolvable
		 *            wrapped resolvable
		 * @param commentsCount
		 *            number of comments
		 * @param lastComment
		 *            last comment
		 */
		public EnhancedResolvable(Resolvable wrappedResolvable,
				int commentsCount, Resolvable lastComment) {
			this.wrappedResolvable = wrappedResolvable;
			this.commentsCount = commentsCount;
			this.lastComment = lastComment;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.gentics.api.lib.resolving.Resolvable#canResolve()
		 */
		public boolean canResolve() {
			return true;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.gentics.api.lib.resolving.Resolvable#get(java.lang.String)
		 */
		public Object get(String key) {
			if (COMMENTSCOUNT_NAME.equals(key)) {
				return commentsCount;
			} else if (LASTCOMMENT_NAME.equals(key)) {
				return lastComment;
			} else {
				return wrappedResolvable.get(key);
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.gentics.api.lib.resolving.Resolvable#getProperty(java.lang.String)
		 */
		public Object getProperty(String key) {
			return get(key);
		}
	}
}
