package com.demo.app.restapi.impl;


import java.time.LocalDateTime;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("status")
public class Status {

    @GET
    @Produces("text/plain")
    public String getStatus() {
    	LocalDateTime now = LocalDateTime.now(); 
        return "This is active Java code with server time " + now + " source from 10:11";
    }
}
