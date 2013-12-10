/*
 * @author Clemens
 * @date Oct 7, 2010
 * @version $Id: $
 */
package com.gentics.demoportal;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.json.JSONArray;
import org.json.JSONObject;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.lib.resolving.ResolvableBean;
import com.gentics.api.portalnode.action.GenericPluggableAction;
import com.gentics.api.portalnode.action.PluggableActionRequest;
import com.gentics.api.portalnode.action.PluggableActionResponse;

public class GetPagesAction extends GenericPluggableAction  {

    public boolean processAction(PluggableActionRequest request,
            PluggableActionResponse response) {
    	String host = ObjectTransformer.getString(request.getParameter("host"), null);
    	String portletappPrefix = ObjectTransformer.getString(request.getParameter("portletapp_prefix"), null);
        String sid = ObjectTransformer.getString(request.getParameter("sid"), null);
        String folderId = ObjectTransformer.getString(request.getParameter("folderId"), null);
        
        if (host == null) {
            logger.error("Missing parameter \"host\"");
            return false;
        }
        
        if (sid == null) {
            logger.error("Missing parameter \"sid\"");
            return false;
        }

        if (folderId == null) {
        	logger.error("Missing parameter \"folderId\"");
        	return false;
        }
        
        // some replacements for convenience
        host = host.replace("http:", "").replace("/", "");
        folderId = folderId.replace("10002.", "");
        
        String url = "http://"
            + host 
            + portletappPrefix
            + "/rest/folder/getPages/"
            + folderId 
            + "?sid=" 
            + sid;
        
        HttpClient client = new HttpClient();
        GetMethod get = new GetMethod(url);
        
        try {
        	get.setRequestHeader("Accept-Charset", "utf-8");
        	get.setRequestHeader("Content-Type", "application/json; charset=utf-8");
            client.executeMethod(get);
            String data = new String(get.getResponseBodyAsString());
            if (logger.isDebugEnabled()) {
                logger.debug("getPages request to {" + url + "} returned {" + data + "}");
            }
            
            
            JSONObject jo = new JSONObject(data);
            JSONArray jpages = jo.getJSONArray("pages");
            Collection<Resolvable> pages = new ArrayList<Resolvable>(jpages.length());
            for (int i = 0; i < jpages.length(); i++) {
				JSONObject jp = jpages.getJSONObject(i);
				pages.add(new PageBean(jp.getInt("id"), jp.getString("name"),
						jp.getInt("status")));
			}
            
            response.setParameter("pages", pages);
            return true;
        } catch (Exception e) {
            logger.error("Could not get pages. REST query was {" + url + "}", e);
        }
        
        return false;
    }
    
    /**
     * simple resolvable bean representation of a page 
     * @author Clemens
     */
    public static class PageBean extends ResolvableBean {
    	
    	/**
		 * generated
		 */
		private static final long serialVersionUID = 2251342357553662679L;

		private Integer id;
        
    	private String name;
        
        private Integer status;
        
        public PageBean(int id, String name, int status) {
            this.id = new Integer(id);
            this.name = name;
            this.status = new Integer(status);
        }
        
		@Override
		public Object get(String key) {
			if (key.equals("id")) {
			    return id;
			} else if (key.equals("name")) {
			    return name;
			} else if (key.equals("status")) {
			    return status;
			} else if (key.equals("contentid")) {
				return "10007." + id;
			} else {
			    return null;
			}
		}
    }
}
