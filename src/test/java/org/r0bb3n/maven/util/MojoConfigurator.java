/*
 * Copyright 2021 r0bb3n
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

package org.r0bb3n.maven.util;

import java.lang.reflect.Field;
import java.net.URL;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.apache.maven.plugins.annotations.Parameter;
import org.r0bb3n.maven.SonarQualityGateMojo;

/**
 * Util that provides allows configuring of a Mojo.
 *
 * <p>Reflection is used, because using @Parameter annotation of a Mojo
 * class is not possible, since the annotation is not available during runtime
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class MojoConfigurator {

  private final SonarQualityGateMojo mojo;

  public static MojoConfigurator configure(SonarQualityGateMojo mojo) {
    return new MojoConfigurator(mojo);
  }

  public MojoConfigurator setSonarHostUrl(URL sonarHostUrl) throws Exception {
    setField("sonarHostUrl", sonarHostUrl);
    return this;
  }

  public MojoConfigurator setSonarHostUrl(String sonarHostUrl) throws Exception {
    return setSonarHostUrl(new URL(sonarHostUrl));
  }

  public MojoConfigurator setSonarProjectKey(String sonarProjectKey) throws Exception {
    setField("sonarProjectKey", sonarProjectKey);
    return this;
  }

  public MojoConfigurator setSonarLogin(String sonarLogin) throws Exception {
    setField("sonarLogin", sonarLogin);
    return this;
  }

  public MojoConfigurator setSonarPassword(String sonarPassword) throws Exception {
    setField("sonarPassword", sonarPassword);
    return this;
  }

  public MojoConfigurator setSkip(boolean skip) throws Exception {
    setField("skip", skip);
    return this;
  }

  public MojoConfigurator setFailOnMiss(boolean failOnMiss) throws Exception {
    setField("failOnMiss", failOnMiss);
    return this;
  }

  public MojoConfigurator setBranch(String branch) throws Exception {
    setField("branch", branch);
    return this;
  }

  public MojoConfigurator setPullRequest(String pullRequest) throws Exception {
    setField("pullRequest", pullRequest);
    return this;
  }


  public MojoConfigurator setCheckTaskAttempts(int checkTaskAttempts) throws Exception {
    setField("checkTaskAttempts", checkTaskAttempts);
    return this;
  }

  public MojoConfigurator setCheckTaskIntervalS(int checkTaskIntervalS) throws Exception {
    setField("checkTaskIntervalS", checkTaskIntervalS);
    return this;
  }

  /**
   * Unfortunately the {@link Parameter} is not available during runtime, therefore this method
   * statically sets the defaults declared in the annotation (except for
   * {@link SonarQualityGateMojo#sonarProjectKey} since it makes use of expressions)
   *
   */
  public MojoConfigurator applyDefaults() throws Exception {
    setSonarHostUrl("http://localhost:9000");
    setSkip(false).setFailOnMiss(true);
    setCheckTaskAttempts(10).setCheckTaskIntervalS(5);
    return this;
  }

  /**
   * Set field by name.
   * @param fieldName declared field name
   * @param value value to set
   * @throws Exception reflection errors
   */
  private void setField(String fieldName, Object value) throws Exception {
    Class<?> clazz = mojo.getClass();
    Field field = null;
    while (field == null && !Object.class.equals(clazz)) {
      try {
        field = clazz.getDeclaredField(fieldName);
      } catch (NoSuchFieldException e) {
        // continue searching in super class
        clazz = clazz.getSuperclass();
      }
    }
    if (field == null) {
      throw new NoSuchFieldException(
          String.format("Field '%s' could not be found in class '%s' and its super classes",
              fieldName, mojo.getClass()));
    }
    field.setAccessible(true);
    field.set(mojo, value);
    field.setAccessible(false);
  }

}
