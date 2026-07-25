/*
 *   Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License").
 *   You may not use this file except in compliance with the License.
 *   A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   or in the "license" file accompanying this file. This file is distributed
 *   on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *   express or implied. See the License for the specific language governing
 *   permissions and limitations under the License.
 */

package io.github.outakeshi.indexsmith.sql.util;

import lombok.experimental.UtilityClass;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Formatting utilities for expression-like objects.
 */
@UtilityClass
public class ExpressionUtils {

  public static final String PATH_SEP = ".";

  /**
   * Format objects using their string representation.
   */
  public static String format(List<?> expressionList) {
    return expressionList.stream().map(String::valueOf).collect(Collectors.joining(","));
  }
}
