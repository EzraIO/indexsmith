/*
 *   Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License").
 *   You may not use this file except in compliance with the License.
 *   A copy of the License is located at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   or in the "license" file accompanying this file. This file is distributed
 *   on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *   express or implied. See the License for the specific language governing
 *   permissions and limitations under the License.
 */

package io.github.outakeshi.indexsmith.sql.util;


import lombok.experimental.UtilityClass;

import java.util.regex.Pattern;

@UtilityClass
public class OperatorUtils {
  /**
   * Wildcard pattern matcher util.
   * Percent (%) character for wildcard,
   * Underscore (_) character for a single character match.
   * @param pattern string pattern to match.
   * @return if text matches pattern returns true; else return false.
   */
  public static boolean matches(String text, String pattern) {
    return Pattern.compile(patternToRegex(pattern)).matcher(text).matches();
  }

  /**
   * Checks if text matches regular expression pattern.
   * @param pattern string pattern to match.
   * @return if text matches pattern returns true; else return false.
   */
  public static int matchesRegexp(String text, String pattern) {
    return Pattern.compile(pattern).matcher(text).matches() ? 1 : 0;
  }

  private static final char DEFAULT_ESCAPE = '\\';

  private static String patternToRegex(String patternString) {
    StringBuilder regex = new StringBuilder(patternString.length() * 2);
    regex.append('^');
    boolean escaped = false;
    for (char currentChar : patternString.toCharArray()) {
      if (!escaped && currentChar == DEFAULT_ESCAPE) {
        escaped = true;
      } else {
        switch (currentChar) {
          case '%':
            if (escaped) {
              regex.append("%");
            } else {
              regex.append(".*");
            }
            escaped = false;
            break;
          case '_':
            if (escaped) {
              regex.append("_");
            } else {
              regex.append('.');
            }
            escaped = false;
            break;
          default:
            switch (currentChar) {
              case '\\':
              case '^':
              case '$':
              case '.':
              case '*':
              case '[':
              case ']':
              case '(':
              case ')':
              case '|':
              case '+':
                regex.append('\\');
                break;
              default:
            }

            regex.append(currentChar);
            escaped = false;
        }
      }
    }
    regex.append('$');
    return regex.toString();
  }
}
