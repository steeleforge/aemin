package com.steeleforge.aem.aemin.util;

import javax.servlet.jsp.PageContext;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.scripting.jsp.util.TagUtil;

public enum JspUtil {
	INSTANCE;
	
	/**
	 * Utility method to extract sling request from JSP page context
	 * 
	 * @param pageContext
	 * @return SlingHttpServletRequest instance
	 */
	public static SlingHttpServletRequest getSlingRequest(PageContext pageContext) {
		return TagUtil.getRequest(pageContext);
	}

	/**
	 * Utility method to extract sling script helper from SlingHttpServletRequest
	 * 
	 * @param request
	 * @return SlingScriptHelper instance
	 */
	public static SlingScriptHelper getSlingScriptHelper(SlingHttpServletRequest request) {
		SlingBindings bindings = (SlingBindings)request.getAttribute(SlingBindings.class.getName());
		return bindings.getSling();
	}

	/**
	 * Utility method to extract sling script helper from JSP page context
	 * 
	 * @param pageContext
	 * @return SlingScriptHelper instance
	 */
	public static SlingScriptHelper getSlingScriptHelper(PageContext pageContext) {
		SlingHttpServletRequest request = getSlingRequest(pageContext);
		return getSlingScriptHelper(request);
	}
	

	/**
	 * Acquire OSGi managed class from JSP page context
	 * 
	 * @param pageContext
	 * @param clazz ServiceType
	 * @return
	 */
	public static <T> T getService(PageContext pageContext, Class<T> clazz) {
		SlingScriptHelper sling = getSlingScriptHelper(pageContext);
		if (null == sling) {
			return null;
		}
		return sling.getService(clazz);
	}

}
