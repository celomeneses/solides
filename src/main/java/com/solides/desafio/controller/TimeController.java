package com.solides.desafio.controller;

import com.solides.desafio.domain.Time;
import com.solides.desafio.service.TimeService;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;


@Path("/time")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class TimeController {

    @Inject
    TimeService service;

    @POST
    public Response criar(Time t){ return Response.status(201).entity(service.criar(t)).build(); }

    @GET
    public List<Time> listar(){ return service.listar(); }

    @GET @Path("{id}")
    public Response buscar(@PathParam("id") Long id){
        return service.buscar(id).map(Response::ok).orElse(Response.status(404)).build();
    }

    @PUT @Path("{id}")
    public Response atualizar(@PathParam("id") Long id, Time t){
        return Response.ok(service.atualizar(id,t)).build();
    }

    @DELETE @Path("{id}")
    public Response deletar(@PathParam("id") Long id){
        service.deletar(id); return Response.noContent().build();
    }
}
