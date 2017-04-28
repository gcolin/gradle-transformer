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

package net.gcolin.transformers.test;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import net.gcolin.transformers.WebFragmentTransformer;

import org.apache.commons.io.IOUtils;
import org.apache.tools.zip.ZipOutputStream;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * A test.
 * 
 * @author Gaël COLIN
 * @since 1.0
 */
public class WebFragmentTransformerTest {

  private WebFragmentTransformer transformer;
  private ZipOutputStream jos;
  private ByteArrayOutputStream bout;

  @Before
  public void before() throws IOException {
    transformer = new WebFragmentTransformer();
    jos = mock(ZipOutputStream.class);
    bout = new ByteArrayOutputStream();
    doAnswer(invocation -> {
      bout.write((Integer) invocation.getArguments()[0]);
      return null;
    }).when(jos).write(anyInt());
    doAnswer(invocation -> {
      bout.write((byte[]) invocation.getArguments()[0]);
      return null;
    }).when(jos).write(anyObject());
    doAnswer(invocation -> {
      bout.write((byte[]) invocation.getArguments()[0], (Integer) invocation.getArguments()[1],
          (Integer) invocation.getArguments()[2]);
      return null;
    }).when(jos).write(anyObject(), anyInt(), anyInt());
    doAnswer(invocation -> {
      bout.flush();
      return null;
    }).when(jos).flush();
    doAnswer(invocation -> {
      bout.close();
      return null;
    }).when(jos).close();
  }

  @Test
  public void testNoOrder() throws IOException {
    load("noOrder.xml");
    transformer.setNewName("noOrder");
    Assert.assertTrue(transformer.hasTransformedResource());
    transformer.modifyOutputStream(jos);
    eq("noOrder.xml");
  }
  
  @Test
  public void testAfterOthers() throws IOException {
    load("afterOthers.xml");
    load("noOrder.xml");
    Assert.assertTrue(transformer.hasTransformedResource());
    transformer.modifyOutputStream(jos);
    eq("afterOthersResult.xml");
  }
  
  @Test
  public void testBeforeOthers() throws IOException {
    load("afterOthers.xml");
    load("noOrder.xml");
    load("beforeOthers.xml");
    Assert.assertTrue(transformer.hasTransformedResource());
    transformer.modifyOutputStream(jos);
    eq("beforeOthersResult.xml");
  }
  
  @Test
  public void testAfterName() throws IOException {
    load("afterOthers.xml");
    load("afterName.xml");
    load("noOrder.xml");
    Assert.assertTrue(transformer.hasTransformedResource());
    transformer.modifyOutputStream(jos);
    eq("afterNameResult.xml");
  }
  
  @Test
  public void testBeforeName() throws IOException {
    load("afterOthers.xml");
    load("noOrder.xml");
    load("beforeOthers.xml");
    load("afterName.xml");
    load("beforeName.xml");
    Assert.assertTrue(transformer.hasTransformedResource());
    transformer.modifyOutputStream(jos);
    eq("beforeNameResult.xml");
  }

  private void load(String path) throws IOException {
    try (InputStream in = this.getClass().getClassLoader().getResourceAsStream(path)) {
      transformer.transform(path, in, null);
    }
  }
  
  private void eq(String path) throws IOException {
    try (InputStream in = this.getClass().getClassLoader().getResourceAsStream(path)) {
      Assert.assertEquals(new String(IOUtils.toByteArray(in), StandardCharsets.UTF_8),
          new String(bout.toByteArray(), StandardCharsets.UTF_8));
    }
  }

}
