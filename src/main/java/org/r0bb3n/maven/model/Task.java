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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * API Model, see <a href="https://github.com/SonarSource/sonarqube/blob/7.9.1/sonar-ws/src/main/protobuf/ws-ce.proto">
 * SonarQube source ws-ce.proto</a>
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Task {

  String analysisId;
  Status status;

  @RequiredArgsConstructor
  @Getter
  public enum Status {
    IN_PROGRESS(true),
    PENDING(true),
    SUCCESS(false),
    CANCELED(false),
    FAILED(false);

    final boolean ongoing;
  }
}
