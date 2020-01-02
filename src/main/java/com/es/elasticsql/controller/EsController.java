package com.es.elasticsql.controller;

import com.alibaba.fastjson.JSON;
import com.es.elasticsql.entity.PeopleTest;
import com.es.elasticsql.service.EsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.util.List;

@RestController
public class EsController {

    @Autowired
    DataSource dataSource;

    @Autowired
    private EsService esService;

    @RequestMapping("/queryAll")
    public String queryAll() {
        System.out.println(dataSource);
        List<PeopleTest> list = esService.queryAll();
        return JSON.toJSONString(list);
    }


}
