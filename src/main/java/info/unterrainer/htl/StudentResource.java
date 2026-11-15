package info.unterrainer.htl;

import info.unterrainer.htl.service.DatabaseService;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.net.URI;
import java.util.Map;

@Path("/students")
public class StudentResource {
    @Inject
    DatabaseService ds;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response students() {
        return Response.ok(ds.getStudents()).build();
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStudent(@PathParam("id") long id) {
        Student student = ds.getStudent(id);

        if (student == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Student not found"))
                    .build();
        }

        return Response.ok(student).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createStudent(Student incoming, @Context UriInfo uriInfo) {
        // Falls dein DatabaseService die ID nicht selbst vergibt, hier Fallback:
        Long id = incoming.getId();
        if (id == null || id == 0L) {
            long maxId = 0L;

            for (Student s : ds.getStudents()) {
                Long sid = s.getId();
                if (sid != null && sid > maxId) {
                    maxId = sid;
                }
            }

            incoming.setId(maxId + 1L);
        }

        ds.addStudent(incoming);

        URI location = uriInfo.getAbsolutePathBuilder()
                .path(String.valueOf(incoming.getId()))
                .build();

        return Response.created(location)
                .entity(incoming)
                .build();
    }

    @DELETE
    @Path("/{id}")
    public Response deleteStudent(@PathParam("id") long id) {
        if (!ds.containsStudent(id)) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Student not found"))
                    .build();
        }

        ds.delStudent(id);
        return Response.noContent().build();
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateStudent(@PathParam("id") long id, Student incoming) {
        Student existing = ds.getStudent(id);

        if (existing == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Student not found"))
                    .build();
        }

        existing.setFullName(incoming.getFullName());
        existing.setEmail(incoming.getEmail());
        existing.setBirthDate(incoming.getBirthDate());

        return Response.ok(existing).build();
    }
}