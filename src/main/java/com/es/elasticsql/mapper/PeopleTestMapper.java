package com.es.elasticsql.mapper;


import com.es.elasticsql.entity.PeopleTest;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PeopleTestMapper {
    int deleteByPrimaryKey(Long id);

    int insert(PeopleTest record);

    int insertSelective(PeopleTest record);

    PeopleTest selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(PeopleTest record);

    int updateByPrimaryKey(PeopleTest record);

    List<PeopleTest> queryAll();

    List<PeopleTest> selectByName(@Param("qName") String qName);

    List<PeopleTest> selectByAge(@Param("age")int age);
}