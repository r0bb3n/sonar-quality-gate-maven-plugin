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

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hamcrest.CustomMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

/**
 * Provides specific exception matchers
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ExceptionMatchers {

  /**
   * Matches an exception with specific message
   * @param expectedMsg excepted exception message
   * @return Exception matcher
   */
  public static Matcher<Exception> hasMessage(String expectedMsg) {
    return hasMessageThat(Matchers.equalTo(expectedMsg));
  }

  /**
   * Matches an exception with specific message
   * @param msgMatcher matcher for the exception message
   * @return Exception matcher
   */
  public static Matcher<Exception> hasMessageThat(Matcher<String> msgMatcher) {
    return new CustomMatcher<>("Exception message matching: " + msgMatcher) {
      @Override
      public boolean matches(Object actual) {
        return actual instanceof Exception && msgMatcher.matches(((Exception) actual).getMessage());
      }

      @Override
      public void describeMismatch(Object item, Description description) {
        description.appendText("was ");
        if (item instanceof Exception) {
          Exception exc = (Exception) item;
          description.appendValue(exc.getMessage());
        } else {
          description.appendValue("not even an Exception - was: " + item);
        }
      }
    };
  }

}
