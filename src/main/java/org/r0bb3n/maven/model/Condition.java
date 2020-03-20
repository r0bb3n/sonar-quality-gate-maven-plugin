package org.r0bb3n.maven.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Condition {

  private Status status;
  private String metricKey;
//  private String comparator;
//  private Integer periodIndex;
//  private String errorThreshold;
//  private String actualValue;
}
