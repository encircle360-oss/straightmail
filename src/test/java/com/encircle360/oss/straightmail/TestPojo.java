package com.encircle360.oss.straightmail;

import java.util.List;

import lombok.Builder;
import lombok.Data;

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
