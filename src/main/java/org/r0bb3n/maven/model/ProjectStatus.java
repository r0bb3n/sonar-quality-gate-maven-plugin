package org.r0bb3n.maven.model;

import java.util.List;
import lombok.Data;

@Data
public class ProjectStatus {

  private Status status;
  private boolean ignoredConditions;
  private List<Condition> conditions;
}
