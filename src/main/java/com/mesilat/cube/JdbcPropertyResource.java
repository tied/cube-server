package com.mesilat.cube;

import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

@Path("/jdbc")
@Scanned
public class JdbcPropertyResource {
    public static final String JDBC_URL      = "jdbc.url";
    public static final String JDBC_USERNAME = "jdbc.username";
    public static final String JDBC_PASSWORD = "jdbc.password";
    public static final String JDBC_DRIVER   = "jdbc.driver";
    public static final String[] JDBC = new String[] { JDBC_URL,JDBC_USERNAME,JDBC_PASSWORD,JDBC_DRIVER };

    @ComponentImport
    private final PluginSettingsFactory settingsFactory;
    @ComponentImport
    private final TransactionTemplate transactionTemplate;

    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response get(){
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode result = getJdbcSettings(mapper);
        return Response.ok(result).build();
    }
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response put(ObjectNode props){
        try {
            PluginSettings settings = settingsFactory.createGlobalSettings();
            if (isEmpty(props, JDBC_URL) && isEmpty(props, JDBC_DRIVER) && isEmpty(props, JDBC_USERNAME) && isEmpty(props, JDBC_PASSWORD)){
                transactionTemplate.execute(()->{
                    settings.remove(JDBC_URL);
                    settings.remove(JDBC_DRIVER);
                    settings.remove(JDBC_USERNAME);
                    settings.remove(JDBC_PASSWORD);
                    return null;
                });
            } else {
                String url = getProperty(props, JDBC_URL);
                String driver = getProperty(props, JDBC_DRIVER);
                String username = getProperty(props, JDBC_USERNAME);
                String password = getProperty(props, JDBC_PASSWORD);

                try {
                    DriverManager.getDriver(url);
                } catch(SQLException ignore) {
                    DelegatingDriver.register(driver);
                }
                boolean connectionSuccessful = false;
                try (Connection conn = DriverManager.getConnection(url, username, password)){
                    connectionSuccessful = true;
                }
                if (connectionSuccessful){
                    transactionTemplate.execute(()->{
                        settings.put(JDBC_URL, url);
                        settings.put(JDBC_DRIVER, driver);
                        settings.put(JDBC_USERNAME, username);
                        settings.put(JDBC_PASSWORD, password);
                        return null;
                    });
                } else {
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Connection failed").build();
                }
            }
        } catch(Exception ex){
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        }
        return Response.ok("Properties saved").build();
    }

    private ObjectNode getJdbcSettings(ObjectMapper mapper){
        PluginSettings settings = settingsFactory.createGlobalSettings();
        final ObjectNode result = mapper.createObjectNode();
        Arrays.asList(JDBC).stream().forEach((prop)->{
            Object val = settings.get(prop);
            if (val != null){
                if (!(val instanceof String) || !val.toString().isEmpty()){
                    result.put(prop, val.toString());
                }
            }
        });
        return result;
    }
    private boolean isEmpty(ObjectNode props, String propName){
        JsonNode node = props.get(propName);
        if (node == null){
            return true;
        }
        if (node.asText() == null || node.asText().isEmpty()){
            return true;
        }
        return false;
    }
    private String getProperty(ObjectNode props, String propName){
        JsonNode node = props.get(propName);
        return (node == null)? "": node.asText();
    }

    @Inject
    public JdbcPropertyResource(PluginSettingsFactory settingsFactory, TransactionTemplate transactionTemplate){
        this.settingsFactory = settingsFactory;
        this.transactionTemplate = transactionTemplate;
    }
}