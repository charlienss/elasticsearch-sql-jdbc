package com.es.elasticsql.service.impl;

import com.es.elasticsql.entity.PeopleTest;
import com.es.elasticsql.mapper.PeopleTestMapper;
import com.es.elasticsql.service.EsService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
public class EsServiceImpl implements EsService {

    @Resource
    private PeopleTestMapper peopleTestMapper;

    @Override
    public List<PeopleTest> queryAll() {
        List<PeopleTest> list = new ArrayList<>();
        list =   peopleTestMapper.queryAll();
        String name="李";
        int age = 15;
        List<PeopleTest> list2 =   peopleTestMapper.selectByAge(age);
        // 模糊查询名称
        peopleTestMapper.selectByName(name);


        int min = 10;
        int max = 20;
        // 根据年龄段查询
        peopleTestMapper.selectByAgeInterval(min,max);
//        List<PeopleTest> list2 =   peopleTestMapper.selectByName(name);
        PeopleTest peopleTest = new PeopleTest();
        peopleTest.setAge(20);
        peopleTest.setName("klkkl");
        peopleTest.setCreatetime("2019-1-2");
//        peopleTestMapper.insert(peopleTest);
//        PeopleTest peopleTest = peopleTestMapper.selectByPrimaryKey(1L);
//        list.add(peopleTest);
        return list;
    }
}
