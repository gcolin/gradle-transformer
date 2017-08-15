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
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

/**
 * Transformer for web-fragment.xml.
 * 
 * @author Gaël COLIN
 * @since 1.0
 */
public class WebFragmentTransformer extends DomTransformer {

  private DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
  private List<Document> all = new ArrayList<>();
  private String newName;
  private Logger logger = Logger.getLogger(this.getClass().getName());

  public WebFragmentTransformer(String newName) {
    super(Arrays.asList("**/META-INF/web-fragment.xml"));
    this.newName = newName == null ? "merged" : newName;
  }

  public WebFragmentTransformer() {
    this(null);
  }

  public void setNewName(String newName) {
    this.newName = newName;
  }

  @Override
  public void transform(String path, InputStream is, List<Relocator> relocators) {
    try {
      all.add(factory.newDocumentBuilder().parse(is));
    } catch (ParserConfigurationException | IOException | SAXException ex) {
      logger.log(Level.SEVERE, ex.getMessage(), ex);
    }
  }

  @Override
  public boolean hasTransformedResource() {
    return !all.isEmpty();
  }

  @Override
  public void modifyOutputStream(ZipOutputStream jos) {
    if (hasTransformedResource()) {
      XPathFactory pathFactory = XPathFactory.newInstance();
      logger.log(Level.INFO, "find {0} fragments", all.size());
      List<Ordering> fragments = new ArrayList<>(all.size());
      for (Document doc : all) {
        try {
          Ordering order = new Ordering();
          order.document = doc;
          XPath xpath = pathFactory.newXPath();
          Node name = (Node) xpath.evaluate("/web-fragment/name", doc, XPathConstants.NODE);
          if (name != null) {
            order.name = name.getTextContent().trim();
            name.getParentNode().removeChild(name);
          }
          Node node = (Node) xpath.evaluate("/web-fragment/ordering", doc, XPathConstants.NODE);
          if (node != null) {
            NodeList befores = (NodeList) xpath.evaluate("before", node, XPathConstants.NODESET);
            for (int i = 0; i < befores.getLength(); i++) {
              Node before = befores.item(i);
              NodeList names = (NodeList) xpath.evaluate("name", before, XPathConstants.NODESET);
              for (int j = 0; j < names.getLength(); j++) {
                order.before.add(names.item(j).getTextContent().trim());
              }
              order.beforeOthers |= xpath.evaluate("others", before, XPathConstants.NODE) != null;
            }
            NodeList afters = (NodeList) xpath.evaluate("after", node, XPathConstants.NODESET);
            for (int i = 0; i < afters.getLength(); i++) {
              Node after = afters.item(i);
              NodeList names = (NodeList) xpath.evaluate("name", after, XPathConstants.NODESET);
              for (int j = 0; j < names.getLength(); j++) {
                order.after.add(names.item(j).getTextContent().trim());
              }
              order.afterOthers |= xpath.evaluate("others", after, XPathConstants.NODE) != null;
            }
            node.getParentNode().removeChild(node);
          }
          fragments.add(order);
        } catch (XPathExpressionException ex) {
          throw new RuntimeException(ex);
        }
      }

      Map<String, Ordering> fragmentMap = new HashMap<>();
      for (Ordering fragment : fragments) {
        fragmentMap.put(fragment.name, fragment);
      }

      for (int i = 0; i < fragments.size(); i++) {
        Ordering ordering = fragments.get(i);
        if (ordering.afterOthers) {
          for (int j = 0; j < fragments.size(); j++) {
            Ordering ordering2 = fragments.get(j);
            if (i != j && !ordering2.after.contains(ordering.name)) {
              ordering.after.add(ordering2.name);
            }
          }
        }
        if (ordering.beforeOthers) {
          for (int j = 0; j < fragments.size(); j++) {
            Ordering ordering2 = fragments.get(j);
            if (i != j && !ordering2.before.contains(ordering.name)) {
              ordering.before.add(ordering2.name);
            }
          }
        }
      }

      boolean change = true;
      while (change) {
        change = false;
        for (Ordering fragment : fragments) {
          int beforeSize = fragment.before.size();
          int afterSize = fragment.after.size();
          for (String name : fragment.before) {
            Ordering order = fragmentMap.get(name);
            if (order != null) {
              fragment.before.addAll(order.before);
            }
          }
          for (String name : fragment.after) {
            Ordering order = fragmentMap.get(name);
            if (order != null) {
              fragment.after.addAll(order.after);
            }
          }
          change |= beforeSize != fragment.before.size() || afterSize != fragment.after.size();
        }
      }

      try {
        Collections.sort(fragments, newFragmentComparator());
        
        if(logger.isLoggable(Level.INFO)) {
          logger.log(Level.INFO, "fragments : {0}", fragments.stream().map(Ordering::getName).collect(Collectors.toList()));
        }

        Document allFragments = factory.newDocumentBuilder().newDocument();
        Node root = fragments.get(0).document.getDocumentElement().cloneNode(false);
        allFragments.appendChild(allFragments.adoptNode(root));
        Node nameNode = allFragments.createElement("name");
        nameNode.appendChild(allFragments.createTextNode(newName));
        root.appendChild(nameNode);

        Set<String> before = new HashSet<>();
        Set<String> after = new HashSet<>();

        for (Ordering fragment : fragments) {
          if (fragment.before != null) {
            before.addAll(fragment.before);
          }
          if (fragment.after != null) {
            after.addAll(fragment.after);
          }
        }

        for (Ordering fragment : fragments) {
          before.remove(fragment.name);
          after.remove(fragment.name);
        }

        after.removeAll(before);

        boolean beforeOthers = fragments.stream().anyMatch(x -> x.beforeOthers);
        boolean afterOthers =
            before.isEmpty() && !beforeOthers && fragments.stream().anyMatch(x -> x.afterOthers);

        if (beforeOthers) {
          after = Collections.emptySet();
          before = Collections.emptySet();
        }

        if (beforeOthers || afterOthers || !before.isEmpty() || !after.isEmpty()) {
          Node ordering = allFragments.createElement("ordering");
          root.appendChild(ordering);
          addOrdering(allFragments, ordering, "before", beforeOthers, before);
          addOrdering(allFragments, ordering, "after", afterOthers, after);
        }

        for (Ordering ordering : fragments) {
          NodeList children = ordering.document.getDocumentElement().getChildNodes();
          for (int i = 0; i < children.getLength(); i++) {
            root.appendChild(allFragments.adoptNode(children.item(i).cloneNode(true)));
          }
        }
        write(allFragments, jos, "META-INF/web-fragment.xml");
      } catch (RuntimeException ex) {
        throw ex;
      } catch (Exception ex) {
        logger.log(Level.SEVERE, ex.getMessage(), ex);
      } finally {
        try {
          jos.closeEntry();
        } catch (IOException ex) {
          logger.log(Level.SEVERE, ex.getMessage(), ex);
        }
      }
    }
  }

  private void addOrdering(Document document, Node node, String orderName, boolean others,
      Set<String> names) {
    if (others || !names.isEmpty()) {
      Node orderNode = document.createElement(orderName);
      node.appendChild(orderNode);

      if (others) {
        orderNode.appendChild(document.createElement("others"));
      } else if (!names.isEmpty()) {
        for (String name : names) {
          Node nameElt = document.createElement("name");
          nameElt.appendChild(document.createTextNode(name));
          orderNode.appendChild(nameElt);
        }
      }
    }
  }

  public static class Ordering {

    private String name;
    private Document document;
    private boolean beforeOthers;
    private boolean afterOthers;
    private Set<String> after = new HashSet<>();
    private Set<String> before = new HashSet<>();
    
    public String getName() {
      return name;
    }

    @Override
    public String toString() {
      return "Ordering{" + "name=" + name + ", document=" + document + ", beforeOthers="
          + beforeOthers + ", afterOthers=" + afterOthers + ", after=" + after + ", before="
          + before + '}';
    }


  }

  /**
   * Create a Comparator for fragments.
   *
   * @return a Comparator for fragments.
   */
  public Comparator<Ordering> newFragmentComparator() {
    return new Comparator<Ordering>() {

      @Override
      public int compare(Ordering o1, Ordering o2) {
        if (o1.before != null && o1.before.contains(o2.name)) {
          return -1;
        }
        if (o2.before != null && o2.before.contains(o1.name)) {
          return 1;
        }
        if (o1.after != null && o1.after.contains(o2.name)) {
          return 1;
        }
        if (o2.after != null && o2.after.contains(o1.name)) {
          return -1;
        }
        return 0;
      }
    };
  }
}
