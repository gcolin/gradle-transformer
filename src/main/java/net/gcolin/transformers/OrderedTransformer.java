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

import com.github.jengelman.gradle.plugins.shadow.relocation.Relocator;

import org.apache.commons.io.IOUtils;
import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipOutputStream;
import org.gradle.api.GradleException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Transformer that order files and merge them.
 * 
 * @author Gaël COLIN
 * @since 1.0
 */
public class OrderedTransformer extends PatternTransformer {

  private final Map<String, List<List<String>>> files = new HashMap<>();

  public OrderedTransformer(List<String> patterns) {
    super(patterns);
  }

  @Override
  public void transform(String path, InputStream is, List<Relocator> relocators) {
    try {
      List<String> file = IOUtils.readLines(is, StandardCharsets.UTF_8);
      if (!file.isEmpty()) {
        List<List<String>> fileAll = files.get(path);
        if (fileAll == null) {
          fileAll = new ArrayList<>();
          files.put(path, fileAll);
        }
        fileAll.add(file);
      }
    } catch (IOException ex) {
      throw new GradleException(ex.getMessage(), ex);
    }
  }

  @Override
  public boolean hasTransformedResource() {
    return !files.isEmpty();
  }

  @Override
  public void modifyOutputStream(ZipOutputStream jos) {
    if (hasTransformedResource()) {
      for (Entry<String, List<List<String>>> entry : files.entrySet()) {
        Collections.sort(entry.getValue(), (a1, a2) -> a1.get(0).compareTo(a2.get(0)));
        try {
          jos.putNextEntry(new ZipEntry(entry.getKey()));
          Writer writer = new OutputStreamWriter(jos, StandardCharsets.UTF_8);
          for(List<String> file : entry.getValue()) {
            for(String line :file) {
              writer.write(line);
              writer.write('\n');
            }
          }
          writer.flush();
          jos.closeEntry();
        } catch (IOException ex) {
          throw new GradleException(ex.getMessage(), ex);
        }
      }
    }
  }

}
