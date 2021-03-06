/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.adsi.parqueadero.rest.services;

import com.adsi.parqueadero.jpa.entities.Carros;
import com.adsi.parqueadero.jpa.entities.Pagos;
import com.adsi.parqueadero.jpa.entities.Puestos;
import com.adsi.parqueadero.jpa.entities.Tarifas;
import com.adsi.parqueadero.jpa.sessions.CarrosFacade;
import com.adsi.parqueadero.jpa.sessions.PagosFacade;
import com.adsi.parqueadero.jpa.sessions.PuestosFacade;
import com.adsi.parqueadero.jpa.sessions.TarifasFacade;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.Date;
import java.util.List;
import javax.ejb.EJB;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *
 * @author carlos
 */
@Path("pagos")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PagosREST {

    @EJB
    private CarrosFacade carroEJB;

    @EJB
    private TarifasFacade tarifaEJB;

    @EJB
    private PuestosFacade puestoEJB;

    @EJB
    private PagosFacade pagoEJB;

    @GET
    public List<Pagos> findAll() {
        return pagoEJB.findAll();
    }
    
    /*
    
    
    */
    
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("totalIngresos")
    public Response consultarTotalIngresos() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        Gson gson = gsonBuilder.create();
        int total = 0;
        List<Pagos> pagos = pagoEJB.findAll();
        for (Pagos pago : pagos) {
            total = total + pago.getTotalPrecio();
        }

        Pagos pago = new Pagos(total);
        return Response.status(Response.Status.OK)
                .entity(gson.toJson(pago))
                .build();
    }
    
    /*
    Este metodo permite retirar un carro de un puesto y crear el pago correspondiente al tiempo que permanecio 
    el automovil en el parqueadero.
    */
    
    @POST
    public Response createPago(@QueryParam("placa") String placa) {
        GsonBuilder gsonBuilder = new GsonBuilder();
        Gson gson = gsonBuilder.create();

        Tarifas tarifa = tarifaEJB.find(1);
        Pagos pago = new Pagos();
        Date date = new Date();

        if (carroEJB.findCarrosByPlaca(placa) != null) {
            Carros carro = carroEJB.findCarrosByPlaca(placa);
            Puestos puesto = puestoEJB.findPuestoByIdCarro(carro.getId());
            if (puestoEJB.findPuestoByIdCarro(carro.getId()) != null) {

                pago.setIdTarifas(tarifa);
                pago.setHoraActual(date);
                pago.setTotalPrecio(calcularTotal(carro.getHoraLlegada(), pago.getIdTarifas().getValor()));
                pago.setIdCarros(carro);
                pagoEJB.create(pago);

                puesto.setIdCarros(null);
                puestoEJB.edit(puesto);

                carro.setHoraSalida(date);
                carroEJB.edit(carro);

                return Response.status(Response.Status.OK)
                        .entity(gson.toJson("Carro removido exitosamente." + " Valor a pagar: " + pago.getTotalPrecio()))
                        .build();
            } else {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(gson.toJson("El auto no esta estacionado en ningun puesto."))
                        .build();
            }
        } else {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(gson.toJson("El auto no existe."))
                    .build();
        }
    }

    public int calcularTotal(Date horaLlegada, int valorTarifa) {
        Date date = new Date();
        long minutosLlegada = (horaLlegada.getTime() / 1000) / 60;
        long minutosActual = (date.getTime() / 1000) / 60;
        int totalPrecio = (int) ((minutosActual - minutosLlegada) * (valorTarifa / 60));
        return totalPrecio;
    }
    
    /*
    */
    @GET
    @Path("consultarPrecio")
    @Produces(MediaType.APPLICATION_JSON)
    public Response consultarPrecio(@QueryParam("placa") String placa) {
        GsonBuilder gsonBuilder = new GsonBuilder();
        Gson gson = gsonBuilder.create();
        Tarifas tarifa = tarifaEJB.find(1);
        if (carroEJB.findCarrosByPlaca(placa) != null) {
            Carros carro = carroEJB.findCarrosByPlaca(placa);
            if (puestoEJB.findPuestoByIdCarro(carro.getId()) != null) {
                int total = calcularTotal(carro.getHoraLlegada(), tarifa.getValor());
                Carros obj = new Carros(total);
                return Response.status(Response.Status.OK)
                        .entity(gson.toJson(obj))
                        .build();
            }else{
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(gson.toJson("El vehiculo no se le ha asignado un puesto."))
                        .build();
            }
        } else {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(gson.toJson("El vehiculo no esta registrado en el parqueadero."))
                    .build();
        }

    }
}
