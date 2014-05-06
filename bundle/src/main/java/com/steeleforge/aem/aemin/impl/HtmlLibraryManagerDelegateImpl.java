package com.steeleforge.aem.aemin.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.zip.GZIPOutputStream;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.util.Text;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.widget.HtmlLibrary;
import com.day.cq.widget.HtmlLibraryManager;
import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.SourceFile;
import com.steeleforge.aem.aemin.ClosureOptimizationLevel;
import com.steeleforge.aem.aemin.HtmlLibraryManagerDelegate;

@Component(label = "aemin Library Manager", 
		   description = "HTML Library Manager Delegate Service", 
		   name = "com.steeleforge.aem.aemin.HtmlLibraryManagerDelegate",
		   immediate = true, metatype = true)
@Service({com.steeleforge.aem.aemin.HtmlLibraryManagerDelegate.class})
public class HtmlLibraryManagerDelegateImpl implements HtmlLibraryManagerDelegate {
	private static final Logger log = LoggerFactory.getLogger(HtmlLibraryManagerDelegateImpl.class);
	private static final String CACHE_PATH = "/var/clientlibs";
	private static final String DEFAULT_THEME = "default";
	private static final String FMT_SCRIPT = "<script type=\"text/javascript\" src=\"{0}\"></script>";
	
	// properties & defaults
	static final String DEFAULT_OPTIMIZATION = ClosureOptimizationLevel.NONE.name(); // note Enum#value#ordinal() does not appear to be valid here
	@Property(label = "Optimization Level", 
			  description = "None, Whitespace-only, Simple, Advanced: https://developers.google.com/closure/compiler/docs/compilation_optimizations",
			  value = "NONE",
			  options = {
			  	@PropertyOption(name = "NONE", value = "No Optimization"),
			  	@PropertyOption(name = "WHITESPACE", value = "Whitespace only"),
			  	@PropertyOption(name = "SIMPLE", value = "Simple"),
			  	@PropertyOption(name = "ADVANCED", value = "Advanced")
			  })
	static final String PROPERTY_OPTIMIZATION = "aemin.optimization.level";
	private ClosureOptimizationLevel optimization = ClosureOptimizationLevel.NONE;
	protected String propOpt = optimization.name().toLowerCase();
	
	@Reference(policy = ReferencePolicy.STATIC, cardinality = ReferenceCardinality.MANDATORY_UNARY)
	HtmlLibraryManager libraryManager;

	@Reference(policy = ReferencePolicy.STATIC, cardinality = ReferenceCardinality.MANDATORY_UNARY)
	ResourceResolverFactory resourceResolverFactory;
	ResourceResolver resolver = null;

	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	
	private String compile(HtmlLibrary library, ClosureOptimizationLevel opt, InputStream js) throws IOException {
		CompilationLevel compilationLevel = opt.toCompilationLevel();
		if (null == compilationLevel) {
			// return original input
			return IOUtils.toString(js);
		}
		SourceFile input = SourceFile.fromInputStream(getLibraryName(library), js);
		// TODO externs not supported, should avoid ADVANCED compilation
		SourceFile extern = SourceFile.fromCode("TODO", StringUtils.EMPTY);
		CompilerOptions options = new CompilerOptions();
		compilationLevel.setOptionsForCompilationLevel(options);
		// ES5 assumption to allow getters/setters
		options.setLanguageIn(CompilerOptions.LanguageMode.ECMASCRIPT5);
		Compiler compiler = new Compiler();
		compiler.compile(extern, input, options);
		return compiler.toSource();
	}
	
	public void send(SlingHttpServletRequest request, SlingHttpServletResponse response, HtmlLibrary library) {
	    InputStream libraryInputStream = null;
	    // NOTE: HtmlLibraryManager#getLibrary should have prepared ClientLibraryImpl
	    // and related binary stream should be ready
	    try {
	    	Node node = JcrUtils.getNodeIfExists(getLibraryNode(request, library), JcrConstants.JCR_CONTENT);
			response.setDateHeader("Last-Modified", 
					JcrUtils.getLongProperty(node, JcrConstants.JCR_LASTMODIFIED, 0L));
			response.setContentType(library.getType().contentType);
			response.setCharacterEncoding("utf-8");
			libraryInputStream = JcrUtils.readFile(node);
		} catch (RepositoryException re) {
			  log.debug("JCR issue retrieving library node at {}: ", library.getPath(), re.getMessage());
		}
	    try {
			if (libraryManager.isGzipEnabled()) {
				response.setHeader("Content-Encoding", "gzip");
			    GZIPOutputStream gzipOut = new GZIPOutputStream(response.getOutputStream());
			    IOUtils.copy(libraryInputStream, gzipOut);
			    gzipOut.finish();
			} else {
			    IOUtils.copy(libraryInputStream, response.getOutputStream());
			}
	    } catch (IOException ioe) {
			log.debug("gzip IO issue for library {}: ", library.getPath(), ioe.getMessage());
	    } finally {
	    	IOUtils.closeQuietly(libraryInputStream);
	    }
	}

	public Node getLibraryNode(SlingHttpServletRequest request, HtmlLibrary library) {
		Node node = null;
		try {
			// we want the non-minified version as the root path
			String cacheRoot = Text.getRelativeParent((new StringBuilder(CACHE_PATH)
				.append(library.getPath(false))).toString(), 1);
			String optPath = (new StringBuilder(cacheRoot).append("/")
									.append(getLibraryName(library))).toString();
			node = JcrUtils.getNodeIfExists(optPath, getAdminSession());
			if (null == node) {
				// generate empty jcr:data to cache
				node = createEmptyCache(library, cacheRoot, getAdminSession());
			}
			// lib was modified after last cache write
			if (!node.hasNode(JcrConstants.JCR_CONTENT) || library.getLastModified(false) > 
				JcrUtils.getLongProperty(node.getNode(JcrConstants.JCR_CONTENT), 
						JcrConstants.JCR_LASTMODIFIED, 0L)) {
				// generate new binary, if possible
				node = populateCache(library, node.getPath(), getAdminSession());
			}
			// reassign with user session
			node = request.getResourceResolver().resolve(node.getPath()).adaptTo(Node.class);
		} catch(RepositoryException re) {
			log.debug(re.getMessage());
		} finally {
			getResolver().close();
		}
		return node;
	}
	
	private Node populateCache(HtmlLibrary library, String root, Session session) {
		Node cacheNode = null;
		try {
			String libPath = (new StringBuilder(CACHE_PATH)
				.append(library.getPath(false))).toString();
			Node src = JcrUtils.getNodeIfExists(libPath, session);
			cacheNode = session.getNode(root);
			if (null != src) {
					// this.lock.readLock().lock();
					// produced closure compiled src
					String compiled = compile(library, this.optimization, JcrUtils.readFile(src));
					// this.lock.readLock().unlock();
					// this.lock.writeLock().lock();
					// 
					JcrUtils.putFile(cacheNode.getParent(), 
							getLibraryName(library), 
							library.getType().contentType, 
							IOUtils.toInputStream(compiled, "UTF-8"));
					session.save();
					// this.lock.writeLock().unlock();
				}
		} catch (RepositoryException re) {
			log.debug(re.getMessage());
		} catch(IOException ioe) {
			log.debug(ioe.getMessage());
		}
		return cacheNode;
	}
	
	private Node createEmptyCache(HtmlLibrary library, String root, Session session)  {
		Node node = null;
		// this.lock.writeLock().lock();
		try {
			Node swap = JcrUtils.getOrCreateByPath(root, 
					JcrResourceConstants.NT_SLING_FOLDER, 
					JcrResourceConstants.NT_SLING_FOLDER, 
					session, true);
			node = swap.addNode(getLibraryName(library), JcrConstants.NT_FILE);
			swap = node.addNode(JcrConstants.JCR_CONTENT, JcrConstants.NT_RESOURCE);
			swap.setProperty(JcrConstants.JCR_LASTMODIFIED, 0L);
			swap.setProperty(JcrConstants.JCR_MIMETYPE, library.getType().contentType);
			swap.setProperty(JcrConstants.JCR_DATA, 
					session.getValueFactory().createBinary(new ByteArrayInputStream(new byte[0])));
			session.save();
			// this.lock.writeLock().unlock();
		} catch(RepositoryException re) {
			log.debug(re.getMessage());
		}
		return node;
	}
	
	private String getLibraryName(HtmlLibrary library) {
		return StringUtils.replace(library.getName(true), "min", 
				StringUtils.lowerCase(this.optimization.name()));
	}
	
	private ResourceResolver getResolver() {
		if (null == resolver || !resolver.isLive()) {
			try {
				resolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
			} catch (LoginException le) {
				log.debug(le.getMessage());
			}
		}
		return resolver;
	}
	
	private Session getAdminSession() {
		return getResolver().adaptTo(Session.class);
	}
	
	@Activate
	protected void activate(ComponentContext context) {
		this.propOpt = PropertiesUtil.toString(context.getProperties()
				.get(PROPERTY_OPTIMIZATION), DEFAULT_OPTIMIZATION);
		this.optimization = ClosureOptimizationLevel.fromString(this.propOpt);
	}
}
