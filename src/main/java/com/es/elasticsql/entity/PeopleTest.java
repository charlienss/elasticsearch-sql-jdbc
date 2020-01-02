package com.es.elasticsql.entity;

import lombok.Data;

import java.io.Serializable;

@Data
public class PeopleTest  implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    private Long id;

    /**
     * name
     */
    private String name;

    /**
     * age
     */
    private Integer age;

    /**
     * createtime
     */
    private String createtime;

}