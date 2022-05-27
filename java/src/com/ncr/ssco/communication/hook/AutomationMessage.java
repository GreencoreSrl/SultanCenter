package com.ncr.ssco.communication.hook;

import org.apache.log4j.Logger;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Created by stefanobertarello on 28/02/17.
 */
public class AutomationMessage {
    private static final Logger logger = Logger.getLogger(AutomationMessage.class);
    private String msgName = "";
    private Map<String, Object> fields = new HashMap(23);
    private XMLStreamWriter writer = null;
    private StringWriter sw = new StringWriter(150);

    public AutomationMessage(String msgName) {
        this.msgName = msgName;
    }

    public void addField(String name, String value) {
        this.fields.put(name, value);
    }

    public void addField(String name, Integer value) {
        this.fields.put(name, value);
    }

    public void addField(String name, Boolean value) {
        this.fields.put(name, value);
    }

    public void addField(String name, byte[] value) {
        this.fields.put(name, value);
    }

    public void addField(String name, short value) {
        this.fields.put(name, Short.valueOf(value));
    }

    public String xml() {
        boolean var19 = false;

        try {
            var19 = true;
            this.sw = new StringWriter(150);
            XMLOutputFactory t = XMLOutputFactory.newInstance();
            this.writer = t.createXMLStreamWriter(this.sw);
            this.writer.writeStartDocument();
            this.writeStartElement("message");
            this.writer.writeAttribute("name", this.msgName);
            this.writer.writeAttribute("msgid", "b2");
            this.writeStartElement("fields");
            StringBuilder value = new StringBuilder();
            Set fieldSet = this.fields.entrySet();
            Iterator i$ = fieldSet.iterator();

            while(true) {
                if(!i$.hasNext()) {
                    this.writeEndElement();
                    this.writer.writeEndDocument();
                    var19 = false;
                    break;
                }

                Map.Entry entry = (Map.Entry)i$.next();
                this.writeStartElement("field");
                this.writer.writeAttribute("name", (String)entry.getKey());
                Object var24 = entry.getValue();
                this.writer.writeAttribute("ftype", FieldTypes.getTypeName(var24.getClass()));
                value.setLength(0);
                if(var24.getClass() == byte[].class) {
                    byte[] arr$ = (byte[])var24;
                    int len$ = arr$.length;

                    for(int i$1 = 0; i$1 < len$; ++i$1) {
                        byte b = arr$[i$1];
                        char c = (char)b;
                        if((c & '\uff00') != 0) {
                            c = (char)(c & 255);
                        }

                        value.append(c);
                    }
                } else {
                    value.append(entry.getValue().toString());
                }

                this.writer.writeCharacters(value.toString());
                this.writeEndElement();
            }
        } catch (Throwable var22) {
            IllegalArgumentException iae = new IllegalArgumentException();
            logger.error("Error: ", var22);
            iae.initCause(var22);
            throw iae;
        } finally {
            if(var19) {
                if(this.writer != null) {
                    try {
                        this.writer.flush();
                        this.writer.close();
                    } catch (Throwable var20) {
                        AutomationException ae = new AutomationException();
                        ae.initCause(var20);
                        throw ae;
                    }
                }

            }
        }

        if(this.writer != null) {
            try {
                this.writer.flush();
                this.writer.close();
            } catch (Throwable var21) {
                AutomationException var25 = new AutomationException();
                var25.initCause(var21);
                throw var25;
            }
        }

        return this.sw.toString();
    }

    protected void writeStartElement(String elementName) {
        try {
            this.writer.writeStartElement(elementName);
        } catch (XMLStreamException var4) {
            AutomationException ae = new AutomationException();
            ae.initCause(var4);
            throw ae;
        }
    }

    protected void writeEmptyElement(String elementName) {
        try {
            this.writer.writeEmptyElement(elementName);
        } catch (XMLStreamException var4) {
            AutomationException ae = new AutomationException();
            ae.initCause(var4);
            throw ae;
        }
    }

    protected void writeEndElement() {
        try {
            this.writer.writeEndElement();
        } catch (XMLStreamException var3) {
            AutomationException ae = new AutomationException();
            ae.initCause(var3);
            throw ae;
        }
    }
}
