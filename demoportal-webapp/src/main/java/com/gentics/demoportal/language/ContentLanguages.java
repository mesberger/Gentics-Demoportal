/**
 * 
 */
package com.gentics.demoportal.language;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.lib.resolving.ResolvableBean;
import com.gentics.api.portalnode.action.GenericPluggableAction;
import com.gentics.api.portalnode.action.PluggableActionContext;
import com.gentics.api.portalnode.action.PluggableActionException;
import com.gentics.api.portalnode.action.PluggableActionRequest;
import com.gentics.api.portalnode.action.PluggableActionResponse;
import com.gentics.api.portalnode.connector.PortalConnectorHelper;
import com.gentics.api.portalnode.portal.PortalPropertySetter;

/**
 * Pluggable Action to provide Data needed for the Language portlet for setting
 * the content language<br/> Request Parameters:
 * <ul>
 * <li><i>object</i> Currently rendered object</li>
 * </ul>
 * Response Parameters:
 * <ul>
 * <li><i>languages</i> Collection of information objects about the content
 * languages. Every object contains the properties:
 * <ul>
 * <li><i>id</i> id (code) of the language</li>
 * <li><i>language</i> name of the language in the current portal language</li>
 * <li><i>contentid</i> contentid of the content in that language. Is empty,
 * if the content is not available in that language</li>
 * </ul>
 * </li>
 * <li><i>pagename</i> Name of the page</li>
 * <li><i>pagelanguageid</i> id (code) of the language of the page content</li>
 * <li><i>pagelanguagename</i> name of the language of the page content in the
 * current portal language</li>
 * </ul>
 */
public class ContentLanguages extends GenericPluggableAction {
	/**
	 * Map of locales for finding via language code
	 */
	protected Map<String, Locale> locales = null;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.gentics.api.portalnode.action.GenericPluggableAction#init(com.gentics.api.portalnode.action.PluggableActionContext)
	 */
	public void init(PluggableActionContext context)
			throws PluggableActionException {
		super.init(context);
		Locale[] availableLocales = Locale.getAvailableLocales();
		locales = new HashMap<String, Locale>(availableLocales.length);
		for (Locale locale : availableLocales) {
			locales.put(locale.getLanguage(), locale);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.gentics.api.portalnode.action.PluggableAction#processAction(com.gentics.api.portalnode.action.PluggableActionRequest,
	 *      com.gentics.api.portalnode.action.PluggableActionResponse)
	 */
	@SuppressWarnings("unchecked")
	public boolean processAction(PluggableActionRequest request,
			PluggableActionResponse response) throws PluggableActionException {
		Datasource ds = getModule().getGenticsPortletContext().getDatasource();
		Object o = request.getParameter("object");
		Resolvable object = null;
		if (o instanceof Resolvable) {
			object = (Resolvable) o;
		} else {
			logger
					.error("Error while getting language information: object is no resolvable (or not set at all)");
			return false;
		}

		try {
			Object locObject = PortalPropertySetter
					.get("portal.language.locale");
			Locale portalLocale = null;
			if (locObject instanceof Locale) {
				portalLocale = (Locale) locObject;
			}

			Collection<String> languages = ObjectTransformer.getCollection(
					object.get("content_languages"), Collections.EMPTY_LIST);
			Collection<Resolvable> infoObjects = new Vector<Resolvable>();
			for (String code : languages) {
				Locale locale = locales.get(code);

				infoObjects.add(new LanguageInformation(code, locale
						.getDisplayLanguage(portalLocale), ObjectTransformer
						.getString(PortalConnectorHelper.getLanguageVariant(
								object, code, ds), null)));
			}

			response.setParameter("languages", infoObjects);
			response.setParameter("pagename", object.get("name"));
			Locale pageLang = locales.get(object.get("content_language"));
			if (pageLang != null) {
				response.setParameter("pagelanguagename", pageLang
						.getDisplayLanguage(portalLocale));
				response.setParameter("pagelanguageid", pageLang.getLanguage());
			}
			return true;
		} catch (Exception e) {
			logger.error(
					"Error while getting language information for object {"
							+ object + "}", e);
			return false;
		}
	}

	/**
	 * Language information object containing the locale and the contentid
	 */
	public static class LanguageInformation extends ResolvableBean {
		/**
		 * Serial version UID
		 */
		private static final long serialVersionUID = 8183568711034359993L;

		/**
		 * Language (in the current portal language)
		 */
		private String language;

		/**
		 * Contentid of the content in this language (empty if not available)
		 */
		private String contentid;

		/**
		 * Id of the language
		 */
		private String id;

		/**
		 * Create an instance of the language information object
		 * 
		 * @param id
		 *            id of the language
		 * @param language
		 *            language (in the current portal language)
		 * @param contentId
		 *            content id of the content in the language (may be empty)
		 */
		public LanguageInformation(String id, String language, String contentid) {
			this.id = id;
			this.language = language;
			this.contentid = contentid;
		}

		/**
		 * Get the language as displayable string)
		 * 
		 * @return language
		 */
		public String getLanguage() {
			return language;
		}

		/**
		 * Get the contentid
		 * 
		 * @return contentid
		 */
		public String getContentid() {
			return contentid;
		}

		/**
		 * Get the id
		 * 
		 * @return id
		 */
		public String getId() {
			return id;
		}
	}
}
