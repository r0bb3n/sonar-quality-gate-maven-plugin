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

package org.r0bb3n.maven;

import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.Logger;
import org.apache.maven.plugin.logging.Log;

/**
 * Logging facade for maven plugin logger that redirects to logging framework
 */
@RequiredArgsConstructor
public class LogFacade implements Log {
  private final Logger log;

  @Override
  public boolean isDebugEnabled() {
    return log.isDebugEnabled();
  }

  @Override
  public void debug(CharSequence content) {
    log.debug(content);
  }

  @Override
  public void debug(CharSequence content, Throwable error) {
    log.debug(content, error);
  }

  @Override
  public void debug(Throwable error) {
    log.debug(error);
  }

  @Override
  public boolean isInfoEnabled() {
    return log.isInfoEnabled();
  }

  @Override
  public void info(CharSequence content) {
    log.info(content);
  }

  @Override
  public void info(CharSequence content, Throwable error) {
    log.info(content, error);
  }

  @Override
  public void info(Throwable error) {
    log.info(error);
  }

  @Override
  public boolean isWarnEnabled() {
    return log.isWarnEnabled();
  }

  @Override
  public void warn(CharSequence content) {
    log.warn(content);
  }

  @Override
  public void warn(CharSequence content, Throwable error) {
    log.warn(content, error);
  }

  @Override
  public void warn(Throwable error) {
    log.warn(error);
  }

  @Override
  public boolean isErrorEnabled() {
    return log.isWarnEnabled();
  }

  @Override
  public void error(CharSequence content) {
    log.error(content);
  }

  @Override
  public void error(CharSequence content, Throwable error) {
    log.error(content, error);
  }

  @Override
  public void error(Throwable error) {
    log.error(error);
  }
}
