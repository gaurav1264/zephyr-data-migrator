package com.prestige.zephyr.migrator.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ZapiController {

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity getInfo(){
        return ResponseEntity.ok("All is well");
    }

}