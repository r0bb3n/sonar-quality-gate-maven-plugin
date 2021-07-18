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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * tests for {@link Util}
 */
public class UtilTest {

  /**
   * check {@link Util#isBlank(String)}
   */
  @Test
  public void testIsBlank() {
    assertTrue("wrong return on: null", Util.isBlank(null));
    assertTrue("wrong return on: \"\"", Util.isBlank(""));
    assertTrue("wrong return on: \" \"", Util.isBlank(" "));
    assertTrue("wrong return on: \"\\t\"", Util.isBlank("\t"));
    assertFalse("wrong return on: \"data\"", Util.isBlank("data"));
  }

}
