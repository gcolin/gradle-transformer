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

import org.apache.tools.zip.ZipOutputStream;
import org.gradle.api.GradleException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

/**
 * Generic XML merge.
 * 
 * @author Gaël COLIN
 * @since 1.0
 */
public class XmlMergeTransformer extends DomTransformer {

  private DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
  private Map<String, List<Document>> documents = new HashMap<>();
  private Function<String, String> mergeXpath;
  private Logger logger = Logger.getLogger(this.getClass().getName());

  public XmlMergeTransformer(Function<String, String> mergeXpath, List<String> patterns) {
    super(patterns);
    this.mergeXpath = mergeXpath;
  }

  @Override
  public void transform(String path, InputStream is, List<Relocator> relocators) {
    try {
      Document document = factory.newDocumentBuilder().parse(is);
      List<Document> list = documents.get(path);
      if (list == null) {
        list = new ArrayList<>();
        documents.put(path, list);
      }
      list.add(document);
    } catch (SAXException | IOException | ParserConfigurationException ex) {
      throw new GradleException(ex.getMessage(), ex);
    }
  }

  @Override
  public boolean hasTransformedResource() {
    return !documents.isEmpty();
  }

  @Override
  public void modifyOutputStream(ZipOutputStream jos) {
    XPath xpath = XPathFactory.newInstance().newXPath();
    for (Entry<String, List<Document>> entry : documents.entrySet()) {
      Document root = entry.getValue().get(0);
      if (entry.getValue().size() > 1) {
        String xpathExpression = mergeXpath.apply(entry.getKey());
        logger.log(Level.INFO, "assemble {0} with xpath {1}", new Object[]{entry.getKey(), xpathExpression});
        try {
          if (xpathExpression != null) {
            XPathExpression expression = xpath.compile(xpathExpression);
            Node append = ((NodeList) expression.evaluate(root, XPathConstants.NODESET)).item(0)
                .getParentNode();
            for (int i = 1; i < entry.getValue().size(); i++) {
              Document document = entry.getValue().get(i);
              NodeList elements = (NodeList) expression.evaluate(document, XPathConstants.NODESET);
              for (int j = 0; j < elements.getLength(); j++) {
                append.appendChild(root.adoptNode(elements.item(j).cloneNode(true)));
              }
            }
          } else {
            Node append = root.getDocumentElement();
            for (int i = 1; i < entry.getValue().size(); i++) {
              NodeList elements = entry.getValue().get(i).getDocumentElement().getChildNodes();
              for (int j = 0; j < elements.getLength(); j++) {
                append.appendChild(root.adoptNode(elements.item(j).cloneNode(true)));
              }
            }
          }
          write(root, jos, entry.getKey());
        } catch (XPathExpressionException ex) {
          throw new GradleException(ex.getMessage(), ex);
        }
      } else {
        write(root, jos, entry.getKey());
      }
    }
  }

}
