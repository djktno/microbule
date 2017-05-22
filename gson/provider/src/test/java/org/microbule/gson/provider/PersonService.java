/*
 * Copyright (c) 2017 The Microbule Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.microbule.gson.provider;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/")
public interface PersonService {
//----------------------------------------------------------------------------------------------------------------------
// Other Methods
//----------------------------------------------------------------------------------------------------------------------

    @GET
    @Path("/people/{firstName}/{lastName}")
    @Produces(MediaType.APPLICATION_JSON)
    Person findPerson(@PathParam("firstName") String firstName, @PathParam("lastName") String lastName);

    @POST
    @Path("/people")
    @Consumes(MediaType.APPLICATION_JSON)
    void addPerson(Person person);

    @GET
    @Path("/people")
    @Produces(MediaType.APPLICATION_JSON)
    List<Person> all();
}
