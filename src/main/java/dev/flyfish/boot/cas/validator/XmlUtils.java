package dev.flyfish.boot.cas.validator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public final class XmlUtils {
    private static final Log LOG = LogFactory.getLog(XmlUtils.class);

    public static XMLReader getXmlReader() {
        try {
            return XMLReaderFactory.createXMLReader();
        } catch (SAXException var1) {
            SAXException e = var1;
            throw new RuntimeException("Unable to create XMLReader", e);
        }
    }

    public static List getTextForElements(String xmlAsString, final String element) {
        final List elements = new ArrayList(2);
        XMLReader reader = getXmlReader();
        DefaultHandler handler = new DefaultHandler() {
            private boolean foundElement = false;
            private StringBuffer buffer = new StringBuffer();

            public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                if (localName.equals(element)) {
                    this.foundElement = true;
                }

            }

            public void endElement(String uri, String localName, String qName) throws SAXException {
                if (localName.equals(element)) {
                    this.foundElement = false;
                    elements.add(this.buffer.toString());
                    this.buffer = new StringBuffer();
                }

            }

            public void characters(char[] ch, int start, int length) throws SAXException {
                if (this.foundElement) {
                    this.buffer.append(ch, start, length);
                }

            }
        };
        reader.setContentHandler(handler);
        reader.setErrorHandler(handler);

        try {
            reader.parse(new InputSource(new StringReader(xmlAsString)));
            return elements;
        } catch (Exception var6) {
            Exception e = var6;
            LOG.error(e, e);
            return null;
        }
    }

    public static String getTextForElement(String xmlAsString, final String element) {
        XMLReader reader = getXmlReader();
        final StringBuffer buffer = new StringBuffer();
        DefaultHandler handler = new DefaultHandler() {
            private boolean foundElement = false;

            public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                if (localName.equals(element)) {
                    this.foundElement = true;
                }

            }

            public void endElement(String uri, String localName, String qName) throws SAXException {
                if (localName.equals(element)) {
                    this.foundElement = false;
                }

            }

            public void characters(char[] ch, int start, int length) throws SAXException {
                if (this.foundElement) {
                    buffer.append(ch, start, length);
                }

            }
        };
        reader.setContentHandler(handler);
        reader.setErrorHandler(handler);

        try {
            reader.parse(new InputSource(new StringReader(xmlAsString)));
        } catch (Exception var6) {
            Exception e = var6;
            LOG.error(e, e);
            return null;
        }

        return buffer.toString();
    }
}
