package com.mesilat.cube;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.config.util.JiraHome;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import javax.inject.Named;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import mondrian.olap.MondrianServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

@Named("com.mesilat.cube-service")
@ExportAsService(CubeService.class)
public class CubeServiceImpl implements CubeService, InitializingBean, DisposableBean {
    private static final Logger LOGGER = LoggerFactory.getLogger("com.mesilat.cube-server");

    private final Properties dbConfig = new Properties();
    @ComponentImport
    private final PluginSettingsFactory settingsFactory;
    @ComponentImport
    private final JiraHome jiraHome;
    @ComponentImport
    private final ActiveObjects ao;

    @Override
    public void registerCatalog(String catalogName, InputStream schema) throws IOException {
    }
    @Override
    public void unregisterCatalog(String catalogName) {
    }
    @Override
    public Properties getDbConfig() {
        return dbConfig;
    }
    @Override
    public String readSchema(String name){
        CatalogData cd = ao.get(CatalogData.class, name);
        return cd == null? null: new String(cd.getData());
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        dbConfig.clear();
        INSTANCE.set(this);

        PluginSettings settings = settingsFactory.createGlobalSettings();
        if (settings.get(JdbcPropertyResource.JDBC_URL) != null
            && settings.get(JdbcPropertyResource.JDBC_DRIVER) != null
            && settings.get(JdbcPropertyResource.JDBC_USERNAME) != null
            && settings.get(JdbcPropertyResource.JDBC_PASSWORD) != null
        ){
            dbConfig.setProperty("Provider",     "mondrian");
            dbConfig.setProperty("Jdbc",         settings.get(JdbcPropertyResource.JDBC_URL).toString());
            dbConfig.setProperty("JdbcUser",     settings.get(JdbcPropertyResource.JDBC_USERNAME).toString());
            dbConfig.setProperty("JdbcPassword", settings.get(JdbcPropertyResource.JDBC_PASSWORD).toString());
            dbConfig.setProperty("JdbcDrivers",  settings.get(JdbcPropertyResource.JDBC_DRIVER).toString());
        } else {
            CubeService.DATASOURCE.set(new CubeDataSource());

            Properties props = new Properties();
            File dbConfigPath = new File(jiraHome.getHome(), "dbconfig.xml");
            try (InputStream in = new FileInputStream(dbConfigPath)){
                DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
                documentBuilderFactory.setNamespaceAware(false);
                DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
                Document xmlDoc = documentBuilder.parse(new InputSource(in));
                Element ds = getChild(xmlDoc.getDocumentElement(), "jdbc-datasource");
                if (ds == null){
                    ClassLoader origCL = Thread.currentThread().getContextClassLoader();
                    try {
                        Thread.currentThread().setContextClassLoader(ComponentManager.class.getClassLoader());
                        Context initContext = new InitialContext();
                        CubeService.DATASOURCE.set((DataSource)initContext.lookup("java:comp/env/jdbc/JiraDS"));
                    } finally {
                        Thread.currentThread().setContextClassLoader(origCL);
                    }
                } else {
                    NodeList attrs = ds.getChildNodes();
                    for (int i = 0; i < attrs.getLength(); i++){
                        if (attrs.item(i) instanceof Element){
                            Element e = (Element)attrs.item(i);
                            props.put(e.getNodeName(), e.getTextContent());
                        }
                    }
                    dbConfig.setProperty("Provider",     "mondrian");
                    dbConfig.setProperty("Jdbc",         props.getProperty("url"));
                    dbConfig.setProperty("JdbcUser",     props.getProperty("username"));
                    dbConfig.setProperty("JdbcPassword", props.getProperty("password"));
                    dbConfig.setProperty("JdbcDrivers",  props.getProperty("driver-class"));
                }
            }
        }

        Thread t = new Thread(()->{
            try {
                ao.moduleMetaData().awaitInitialization();
            } catch (ExecutionException | InterruptedException ex) {
                LOGGER.warn("AO not initialized", ex);
            }
            MondrianProperty.load(ao);
            CatalogData.initDefault(ao);
        });
        t.start();
    }
    @Override
    public void destroy() throws Exception {
        INSTANCE.set(null);
        for (MondrianServer server : listMondrianServers()){
            server.shutdown();
        }
    }

    public List<MondrianServer> listMondrianServers() {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        List<MondrianServer> servers = new ArrayList<>();
        try {
            for (ObjectName name : mbs.queryNames(new ObjectName("mondrian.server:type=Server,ID=*"), null)) {
                MondrianServer server = MondrianServer.forId(name.getKeyProperty("ID"));
                if (server != null) {
                    servers.add(server);
                }
            }
        } catch (MalformedObjectNameException ex) {
            LOGGER.error("Unexpected error looking up MBean \"mondrian.server:type=Server,ID=*\"", ex);
        }
        return servers;
    }

    @Autowired
    public CubeServiceImpl(
        final ActiveObjects ao,
        final JiraHome jiraHome,
        final PluginSettingsFactory settingsFactory
    ){
        this.ao = ao;
        this.jiraHome = jiraHome;
        this.settingsFactory = settingsFactory;
    }

    private static Element getChild(Element elt, String name){
        NodeList list = elt.getElementsByTagName(name);
        for (int i = 0; i < list.getLength(); i++){
            if (list.item(i) instanceof Element && list.item(i).getNodeName().equals(name)){
                return (Element)list.item(i);
            }
        }
        return null;
    }
}