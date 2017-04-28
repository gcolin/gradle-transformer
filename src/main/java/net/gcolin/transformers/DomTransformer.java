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

import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipOutputStream;
import org.gradle.api.GradleException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.util.List;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Transformer with DOM.
 * 
 * @author Gaël COLIN
 * @since 1.0
 */
public abstract class DomTransformer extends PatternTransformer {

  public DomTransformer(List<String> patterns) {
    super(patterns);
  }

  protected void write(Document document, ZipOutputStream jos, String name) {
    try {
      jos.putNextEntry(new ZipEntry(name));
      removeBlank(document.getDocumentElement());
      indentBlank(document.getDocumentElement(), 1, document);
      javax.xml.transform.Transformer transformer =
          TransformerFactory.newInstance().newTransformer();

      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
      StreamResult result = new StreamResult(jos);
      DOMSource source = new DOMSource(document);
      transformer.transform(source, result);
    } catch (IOException | TransformerFactoryConfigurationError | TransformerException ex) {
      throw new GradleException(ex.getMessage(), ex);
    }
  }

  private void removeBlank(Node node) {
    if (node.getNodeType() == Node.TEXT_NODE && node.getTextContent().trim().isEmpty()) {
      node.getParentNode().removeChild(node);
    }
    NodeList children = node.getChildNodes();
    for (int i = children.getLength() - 1; i >= 0; i--) {
      removeBlank(children.item(i));
    }
  }

  private void indentBlank(Node node, int nb, Document document) {
    StringBuilder str = new StringBuilder("\n");
    for (int j = 0; j < nb; j++) {
      str.append("  ");
    }
    NodeList children = node.getChildNodes();
    boolean hasIndent = false;
    for (int i = 0; i < children.getLength(); i++) {
      Node inner = children.item(i);
      if (inner.getNodeType() != Node.TEXT_NODE) {
        node.insertBefore(document.createTextNode(str.toString()), children.item(i));
        indentBlank(inner, nb + 1, document);
        i++;
        hasIndent = true;
      }
    }
    if (hasIndent) {
      str.delete(str.length() - 2, str.length());
      node.appendChild(document.createTextNode(str.toString()));
    }
  }

}
