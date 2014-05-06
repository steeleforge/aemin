package com.steeleforge.aem.aemin;

import java.io.IOException;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.widget.HtmlLibrary;
import com.day.cq.widget.HtmlLibraryManager;


@SlingServlet(label = "Alternative HTML Library Servlet", 
		   description = "Supports google closure compiler processing",
		   resourceTypes = {"widgets/clientlib", "cq/ClientLibraryFolder"},
		   selectors = { "aemin" },
		   extensions = { "js", "map" },
		   generateComponent = true,
		   metatype = true)
@Service
public class ClosureHtmlLibraryServlet extends SlingSafeMethodsServlet {
	private static final long serialVersionUID = -6579740499107137020L;
	private static final Logger log = LoggerFactory.getLogger(ClosureHtmlLibraryServlet.class);
	
	@Reference(policy = ReferencePolicy.STATIC, cardinality = ReferenceCardinality.MANDATORY_UNARY)
	HtmlLibraryManager libraryManager;
	
	@Reference(policy = ReferencePolicy.STATIC, cardinality = ReferenceCardinality.MANDATORY_UNARY)
	HtmlLibraryManagerDelegate libraryManagerDelegate;
	

	protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
	    throws IOException {
	      HtmlLibrary library = libraryManager.getLibrary(request);
	      if (null == library) {
	        response.sendError(404);
	        return;
	      }
	      libraryManagerDelegate.send(request, response, library);
	}
	
	@Activate
	protected void activate(ComponentContext context) {
		// no op
	}
}
