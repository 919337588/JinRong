package com.jinrong.controller;

import com.jinrong.service.ValuationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/valuation")
public class ValuationController {

    @Autowired
    private ValuationService valuationService;

    @PostMapping("/askai")
    public List<Map<String, Object>> askai(  String sql,String date) {

        return valuationService.getValuationData(sql,date);
    }
    @PostMapping("/mock")
    public Object mock( String sql,int beginyear,int endyear,int waittime,double zsd,double zsd2) {

        return valuationService.mock(sql,beginyear,endyear,waittime,zsd,zsd2);
    }
}