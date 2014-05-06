package com.steeleforge.aem.aemin.taglib;
import java.io.IOException;
import java.io.StringWriter;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.widget.HtmlLibraryManager;
import com.steeleforge.aem.aemin.util.JspUtil;

/**
 * Enables aemin closure compiler minification JS paths
 * 
 * @author david steele
 */
public class ClosureIncludeClientLibraryTag extends TagSupport {
    private static final long serialVersionUID = 7595788232387796604L;
    private static final Logger taglog = LoggerFactory.getLogger(ClosureIncludeClientLibraryTag.class);
    
    private String categories = StringUtils.EMPTY;
    private boolean themed = false;
    private String theme = StringUtils.EMPTY;
    private String js = StringUtils.EMPTY;
    private String css = StringUtils.EMPTY;

	public int doEndTag() throws JspException {
		SlingHttpServletRequest request = JspUtil.getSlingRequest(pageContext);
    	HtmlLibraryManager libraryManager = JspUtil.getService(pageContext, HtmlLibraryManager.class);
    	
    	try {
    		if (null != libraryManager) {
	    		JspWriter out = pageContext.getOut();
	    		StringWriter writerBuffer = new StringWriter();
	    		if (StringUtils.isNotBlank(getCategories())) {
	    			// categories
	    			libraryManager.writeIncludes(request, writerBuffer, 
	    					csvToArray(getCategories()));
	    		} else if (StringUtils.isNotBlank(getTheme())) {
	    			// theme
	    			libraryManager.writeThemeInclude(request, writerBuffer, 
	    					csvToArray(getTheme()));
	    		} else if (StringUtils.isNotBlank(getJs())) {
	    			if (isThemed()) {
	    				// themed js
	    				libraryManager.writeJsInclude(request, writerBuffer, isThemed(), getJs());
	    			} else {
	    				// theme default js
	    				libraryManager.writeJsInclude(request, writerBuffer, getJs());
	    			}
	    		} else if (StringUtils.isNotBlank(getCss())) {
	    			if (isThemed()) {
	        			// themed css
	    				libraryManager.writeCssInclude(request, writerBuffer, isThemed(), getCss());    				
	    			} else {
	        			// theme default css
	    				libraryManager.writeCssInclude(request, writerBuffer, getCss());    				
	    			}
	    		}
	    		// simple replacement of ".min.js" to ".aemin.js"
	    		String markup = writerBuffer.toString();
	    		out.write(StringUtils.replace(markup, ".min.js", ".aemin.js"));
	    	} else {
	    		taglog.debug("aemin Library Manager inaccessible");
	    	}
    	} catch (IOException ioe) {
    		taglog.debug(ioe.getMessage());
    	}
    	return TagSupport.EVAL_PAGE;
    }
	
	private static String[] csvToArray(String in) {
		String[] tokens = StringUtils.split(in, ",");
		for(int i=0;i < tokens.length;i++) {
			tokens[i] = StringUtils.trim(tokens[i]);
		}
		return tokens;
	}
    
    public String getCategories() {
		return categories;
	}

	public void setCategories(String categories) {
		this.categories = categories;
	}

	public boolean isThemed() {
		return themed;
	}

	public void setThemed(boolean themed) {
		this.themed = themed;
	}

	public String getTheme() {
		return theme;
	}

	public void setTheme(String theme) {
		this.theme = theme;
	}

	public String getJs() {
		return js;
	}

	public void setJs(String js) {
		this.js = js;
	}

	public String getCss() {
		return css;
	}

	public void setCss(String css) {
		this.css = css;
	}
}
