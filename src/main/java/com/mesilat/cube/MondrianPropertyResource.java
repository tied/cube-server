package com.mesilat.cube;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import mondrian.olap.MondrianProperties;
import net.java.ao.DBParam;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

@Path("/properties")
@Scanned
public class MondrianPropertyResource {
    @ComponentImport
    private final ActiveObjects ao;

    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response get(){
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode result = MondrianProperty.toNodeObject(mapper, MondrianProperties.instance());
        return Response.ok(result).build();
    }
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response put(ObjectNode props){
        try {
            Map<String,MondrianProperty> map = new HashMap<>();
            Arrays.asList(ao.find(MondrianProperty.class)).stream().forEach(p->{
                map.put(p.getName(), p);
            });

            ao.executeInTransaction(()->{
                props.getFields().forEachRemaining((e)->{
                    if (map.containsKey(e.getKey())){
                        MondrianProperty p = map.get(e.getKey());
                        if (!p.getValue().equals(e.getValue().asText())){
                            p.setValue(e.getValue().asText());
                            p.save();
                        }
                        map.remove(e.getKey());
                    } else {
                        MondrianProperty p = ao.create(MondrianProperty.class, new DBParam("NAME", e.getKey()));
                        p.setValue(e.getValue().asText());
                        p.save();
                    }
                });
                map.values().stream().forEach((p)->{
                    ao.delete(p);
                });
                return null;
            });
        } catch(Exception ex){
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        }

        props.getFields().forEachRemaining((e)->{
            MondrianProperties.instance().setProperty(e.getKey(), e.getValue().asText());
        });

        return Response.ok("Properties saved").build();
    }
    
    @Inject
    public MondrianPropertyResource(ActiveObjects ao){
        this.ao = ao;
    }
}