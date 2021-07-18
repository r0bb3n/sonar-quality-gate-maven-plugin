/*
 * Copyright 2020 r0bb3n
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.r0bb3n.maven.model;

import java.util.List;
import lombok.Data;

/**
 * API Model, see
 * <a href="https://github.com/SonarSource/sonarqube/blob/7.9.1/sonar-ws/src/main/protobuf/ws-qualitygates.proto">
 * SonarQube source ws-qualitygates.proto</a>
 */
@Data
public class ProjectStatus {

  private Status status;
  private boolean ignoredConditions;
  private List<Condition> conditions;

  public enum Status {
    OK,
    WARN,
    NONE,
    ERROR
  }

}
