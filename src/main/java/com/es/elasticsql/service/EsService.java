package com.es.elasticsql.service;

import com.es.elasticsql.entity.PeopleTest;

import java.util.List;

public interface EsService {
    List<PeopleTest> queryAll();
}
