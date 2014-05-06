aemin
========

Google closure compiler support with drop-in replacement for cq:includeClientLib without damaging existing functionality.

What works
----------
- Simple compilation is quite slow (whitespace-only is quite a bit faster), but it works, with /var/clientlibs caching
- includeClientLib tag alternative which allows for google closure compiler js & clientlib includes. example is found in /apps/aemin/components/exzample
    <%@taglib prefix="aemin" uri="http://www.steeleforge.com/aem/aemin/taglib/1.0.0" %>


What remains / TODOs
--------------------
- externs support. This is made non-trivial due to access to and general opaque nature of cq-widgets private impls such as ClientLibraryImpl, HtmlLibraryImpl, and FileBundle
- CompilationLevel.ADVANCED_OPTIMIZATIONS compilation is not a good idea w/o externs support
- "customJavascriptPaths" is ignored for now
- debug mode is also unsupported

Building
--------

This project uses Maven for building. Common commands:

From the root directory, run ``mvn -P local clean install`` to build the bundle and content package and install to a CQ instance.

From the bundle directory, run ``mvn -P local-publish clean install`` to build *just* the bundle and install to a CQ instance.

Using with VLT
--------------

To use vlt with this project, first build and install the package to your local CQ instance as described above. Then cd to `content/src/main/content/jcr_root` and run

    vlt --credentials admin:admin checkout -f ../META-INF/vault/filter.xml --force http://localhost:4502/crx

Once the working copy is created, you can use the normal ``vlt up`` and ``vlt ci`` commands.

Specifying CRX Host/Port
------------------------

The CRX host and port can be specified on the command line with:
mvn -Dcrx.host=otherhost -Dcrx.port=5502 <goals>


