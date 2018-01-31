package com.mesilat.cube;

import com.atlassian.activeobjects.external.ActiveObjects;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import mondrian.olap.DriverManager;
import mondrian.olap.MondrianServer;
import mondrian.olap4j.MondrianOlap4jDriver;
import mondrian.rolap.RolapConnection;
import mondrian.rolap.RolapConnectionProperties;
import mondrian.rolap.RolapSchema;
import mondrian.server.MondrianServerRegistry;
import mondrian.server.Repository;
import mondrian.spi.CatalogLocator;
import mondrian.util.ClassResolver;
import mondrian.util.LockBox;
import net.java.ao.Query;
import org.olap4j.OlapConnection;
import org.olap4j.OlapException;
import org.olap4j.OlapWrapper;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class CubeRepository implements Repository {
    private static final String DATASOURCE_NAME = "Provider=Mondrian;DataSource=Cube";
    private final ActiveObjects ao;
    private final CatalogLocator locator;
    private final CubeService service;
    private final Map<String,Map<String,RolapSchema>> catalogData = new HashMap<>();

    @Override
    public List<String> getDatabaseNames(RolapConnection connection) {
        return Collections.singletonList(DATASOURCE_NAME);
    }
    @Override
    public List<String> getCatalogNames(RolapConnection connection, String databaseName) {
        if (!DATASOURCE_NAME.equals(databaseName)){
            throw new RuntimeException(String.format("Invalid database name: %s", databaseName));
        }
        return Arrays.asList(ao.find(CatalogData.class, Query.select("NAME").order("NAME")))
            .stream()
            .map((cd)->{ return cd.getName(); })
            .collect(Collectors.toList());
    }
    @Override
    public Map<String, RolapSchema> getRolapSchemas(RolapConnection connection, String databaseName, String catalogName) {
        if (!DATASOURCE_NAME.equals(databaseName)){
            throw new RuntimeException(String.format("Invalid database name: %s", databaseName));
        }
        if (catalogData.containsKey(catalogName)){
            return catalogData.get(catalogName);
        }
        CatalogData cd = ao.get(CatalogData.class, catalogName);
        if (cd == null){
            throw new RuntimeException(String.format("Invalid catalog name: %s", catalogName));
        }

        RolapConnection rolapConnection = null;
        try {
            Document schemaDoc = parse(cd.getData());
            String schemaName = schemaDoc.getDocumentElement().getAttribute("name");
            rolapConnection = (RolapConnection)DriverManager.getConnection(connectString(service.getDbConfig(), catalogName), locator);
            Map<String, RolapSchema> schemas = Collections.singletonMap(schemaName, rolapConnection.getSchema());
            catalogData.put(catalogName, schemas);
            return schemas;
        } catch (ParserConfigurationException | SAXException | IOException ex) {
            throw new RuntimeException(String.format("Failed to parse schema data for catalog %s", catalogName), ex);
        } finally {
            if (rolapConnection != null) {
                rolapConnection.close();
            }
        }
    }
    @Override
    public List<Map<String, Object>> getDatabases(RolapConnection connection) {
        Map<String,Object> props = new HashMap<>();
        props.put("DataSourceName", DATASOURCE_NAME);
        props.put("DataSourceDescription", "Data source for Cube Plugin");
        props.put("URL", "http://localhost/cube");
        props.put("DataSourceInfo", connectString(service.getDbConfig()));
        props.put("ProviderName", "Mondrian");
        props.put("ProviderType", "MDP");
        props.put("AuthenticationMode", "Unauthenticated");
        return Collections.singletonList(props);
    }
    @Override
    public OlapConnection getConnection(MondrianServer server, String databaseName, String catalogName, String roleName, Properties props) throws SQLException {
        if (databaseName != null && !DATASOURCE_NAME.equals(databaseName)){
            throw new RuntimeException(String.format("Invalid database name: %s", databaseName));
        }
        String connectString = connectString(service.getDbConfig(), catalogName == null? "default": catalogName);

        // Save the server for the duration of the call to 'getConnection'.
        final LockBox.Entry entry = MondrianServerRegistry.INSTANCE.lockBox.register(server);
        final Properties properties = new Properties();
        properties.setProperty(RolapConnectionProperties.Instance.name(), entry.getMoniker());
        if (roleName != null) {
            properties.setProperty(RolapConnectionProperties.Role.name(), roleName);
        }
        properties.putAll(props);
        try {
            ClassResolver.INSTANCE.forName(MondrianOlap4jDriver.class.getName(), true);
        } catch (ClassNotFoundException e) {
            throw new OlapException("Cannot find mondrian olap4j driver");
        }

        if (!connectString.startsWith("jdbc:mondrian:")){
            connectString = "jdbc:mondrian:" + connectString;
        }
        final java.sql.Connection connection = java.sql.DriverManager.getConnection(connectString, properties);
        return ((OlapWrapper) connection).unwrap(OlapConnection.class);
    }
    @Override
    public void shutdown(){
    }

    public CubeRepository(ActiveObjects ao, CatalogLocator locator, CubeService service){
        this.ao = ao;
        this.locator = locator;
        this.service = service;
    }

    private static Document parse(byte[] data) throws ParserConfigurationException, SAXException, IOException{
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setValidating(false);
        documentBuilderFactory.setIgnoringComments(true);
        documentBuilderFactory.setAttribute("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        documentBuilderFactory.setNamespaceAware(true);
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        return documentBuilder.parse(new InputSource(new ByteArrayInputStream(data)));
    }
    private static String connectString(Properties dbConfig){
        return MessageFormat.format("Provider=mondrian;Jdbc={0};JdbcUser={1};JdbcPassword={2};JdbcDrivers={3}",
            dbConfig.getProperty("Jdbc"),
            dbConfig.getProperty("JdbcUser"),
            dbConfig.getProperty("JdbcPassword"),
            dbConfig.getProperty("JdbcDrivers")
        );
    }            
    private static String connectString(Properties dbConfig, String catalogName){
        return connectString(dbConfig) + (catalogName == null? "": ";Catalog=cube:" + catalogName);
    }            
}