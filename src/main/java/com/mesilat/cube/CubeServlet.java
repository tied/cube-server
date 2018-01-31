package com.mesilat.cube;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.util.JiraVelocityUtils;
import com.atlassian.jira.util.collect.MapBuilder;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.templaterenderer.TemplateRenderer;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import mondrian.xmla.impl.*;
import mondrian.server.DynamicContentFinder;
import mondrian.server.RepositoryContentFinder;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import mondrian.olap.MondrianProperties;
import mondrian.olap.MondrianServer;
import mondrian.server.Repository;
import mondrian.server.StringRepositoryContentFinder;
import mondrian.spi.CatalogLocator;
import mondrian.spi.impl.CatalogLocatorImpl;
import mondrian.xmla.XmlaHandler;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;

@Scanned
public class CubeServlet extends MondrianXmlaServlet {
    private static final long serialVersionUID = 1L;

    private final Map<String, DynamicContentFinder> finders = new HashMap<>();
    private final CubeService cubeService;
    @ComponentImport
    private final TemplateRenderer renderer;
    @ComponentImport
    private final JiraAuthenticationContext authenticationContext;
    @ComponentImport
    private final ActiveObjects ao;
        
    @Override
    protected RepositoryContentFinder makeContentFinder(String dataSources) {
        if (!finders.containsKey(dataSources)) {
            finders.put(dataSources, new DynamicContentFinder(dataSources));
        }
        return finders.get(dataSources);
    }
    @Override
    public void destroy() {
        for (DynamicContentFinder finder : finders.values()) {
            finder.shutdown();
        }
        super.destroy();
    }
    //@Override
    protected XmlaHandler.ConnectionFactory createConnectionFactory(ServletConfig servletConfig) throws ServletException {
        if (server == null) {
            Repository repository = new CubeRepository(ao, new CatalogLocatorImpl(), cubeService);
            server = MondrianServer.createWithRepository(repository, new CatalogLocatorImpl());
        }
        return (XmlaHandler.ConnectionFactory)server;
    }
    protected XmlaHandler.ConnectionFactory createConnectionFactoryBak(ServletConfig servletConfig) throws ServletException {
        if (server == null) {
            CatalogLocator catalogLocator = makeCatalogLocator(servletConfig);

            RepositoryContentFinder contentFinder;
            try (InputStream in = this.getClass().getResourceAsStream("datasources.xml")){
                Properties dbConfig = cubeService.getDbConfig();
                contentFinder = new StringRepositoryContentFinder(
                    MessageFormat.format(IOUtils.toString(in),
                            dbConfig.getProperty("Jdbc").replace("&","&amp;"),
                            dbConfig.getProperty("JdbcUser"),
                            dbConfig.getProperty("JdbcPassword"),
                            dbConfig.getProperty("JdbcDrivers")
                    )
                );
            } catch (IOException ex) {
                throw new ServletException(ex);
            }

            server = MondrianServer.createWithRepository(contentFinder, catalogLocator);
        }
        return (XmlaHandler.ConnectionFactory)server;
    }
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/html;charset=utf-8");
        Map<String, Object> initContext = MapBuilder
            .<String, Object> newBuilder()
            .add("mondrianProperties", MondrianProperties.instance())
            .toMap();
        Map<String, Object> root = JiraVelocityUtils.getDefaultVelocityParams(initContext, authenticationContext);
        renderer.render("templates/config.vm", root, resp.getWriter());
    }

    @Autowired
    public CubeServlet(CubeService cubeService,
        TemplateRenderer renderer,
        JiraAuthenticationContext authenticationContext,
        ActiveObjects ao
    ){
        this.cubeService = cubeService;
        this.renderer = renderer;
        this.authenticationContext = authenticationContext;
        this.ao = ao;
    }
}