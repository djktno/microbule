package org.microbule.example.hello;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/")
public interface HelloResource {
    @Path("/hello/{name}")
    @Produces(MediaType.TEXT_PLAIN)
    @GET
    String sayHello(@PathParam("name") String name);
}
