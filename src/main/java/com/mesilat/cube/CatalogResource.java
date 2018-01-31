package com.mesilat.cube;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import java.util.Arrays;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import net.java.ao.DBParam;
import net.java.ao.Query;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;

@Path("/catalog")
@Scanned
@AnonymousAllowed
public class CatalogResource {
    @ComponentImport
    private final ActiveObjects ao;
    
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response list(){
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode array = mapper.createArrayNode();
        Arrays.asList(ao.find(CatalogData.class, Query.select("NAME").order("NAME"))).stream().forEach((cd)->{
            array.add(cd.getName());
        });
        return Response.ok(array).build();
    }
    @GET
    @Path("/{name}")
    @Produces(MediaType.APPLICATION_XML + ";charset=utf-8")
    public Response get(@PathParam("name") String catalogName){
        CatalogData cd = ao.get(CatalogData.class, catalogName);
        if (cd == null){
            return Response.status(Status.NOT_FOUND).entity(String.format("Catalog '%s' could not be found", catalogName)).build();
        } else {
            return Response.ok(cd.getData()).build();
        }
    }
    @PUT
    @Path("/{name}")
    @Consumes(MediaType.APPLICATION_XML + ";charset=utf-8")
    public Response put(@PathParam("name") String catalogName, byte[] catalogData){
        return ao.executeInTransaction(()->{
            try {
                CatalogData cd = ao.get(CatalogData.class, catalogName);
                if (catalogData == null){
                    cd = ao.create(CatalogData.class, new DBParam("NAME", catalogName));
                    cd.setData(catalogData);
                    cd.save();
                } else {
                    cd.setData(catalogData);
                    cd.save();
                }
                return Response.ok().build();
            } catch(RuntimeException ex) {
                return Response.status(Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
            }
        });
    }

    @Inject
    public CatalogResource(ActiveObjects ao){
        this.ao = ao;
    }
}