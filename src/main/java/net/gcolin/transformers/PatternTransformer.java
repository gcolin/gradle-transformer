/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package net.gcolin.transformers;

import com.github.jengelman.gradle.plugins.shadow.transformers.Transformer;

import org.gradle.api.file.FileTreeElement;
import org.gradle.api.tasks.util.PatternSet;

import java.util.List;

/**
 * Transformer with a pattern filter.
 * 
 * @author Gaël COLIN
 * @since 1.0
 */
public abstract class PatternTransformer implements Transformer {

  private final PatternSet patternSet = new PatternSet();
  
  public PatternTransformer(List<String> patterns) {
    patternSet.include(patterns);
  }
  
  @Override
  public boolean canTransformResource(FileTreeElement element) {
    return patternSet.getAsSpec().isSatisfiedBy(element);
  }
  
  public void exclude(String path) {
    patternSet.exclude(path);
  }
  
}
