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

    /**
     * 名称的模糊查询
     * @param qName
     * @return
     */
    List<PeopleTest> selectByName(@Param("qName") String qName);

    /**
     * 按照年龄查询
     * @param age
     * @return
     */
    List<PeopleTest> selectByAge(@Param("age")int age);


    /**
     * 根据年龄区间进行匹配查询
     * @param min
     * @param max
     * @return
     */
    List<PeopleTest> selectByAgeInterval(@Param("min") Integer min, @Param("max") Integer max);


}