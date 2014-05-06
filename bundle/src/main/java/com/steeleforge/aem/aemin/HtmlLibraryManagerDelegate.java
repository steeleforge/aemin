package com.steeleforge.aem.aemin;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;

import com.day.cq.widget.HtmlLibrary;

public interface HtmlLibraryManagerDelegate {
	public void send(SlingHttpServletRequest request, SlingHttpServletResponse response, HtmlLibrary library);
	public Node getLibraryNode(SlingHttpServletRequest request, HtmlLibrary library) throws RepositoryException, IOException;
}
