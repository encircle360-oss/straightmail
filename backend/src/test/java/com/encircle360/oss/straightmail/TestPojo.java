package com.encircle360.oss.straightmail;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * simple pojo for testing
 */
@Data
@Builder
public class TestPojo {

    List<Double> doubles;

    List<Integer> integers;

    List<Boolean> booleans;

    List<String> strings;

    String singleString;

    Double singleDouble;

    Integer singleInteger;

    Boolean singleBoolean;
}
