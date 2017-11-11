package ru.javaops.masterjava.xml.util;

import org.xml.sax.SAXException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import java.io.*;


/**
 * Marshalling/Unmarshalling JAXB helper
 * XML Facade
 */
public class JaxbParser {
    protected Schema schema;
    private JAXBContext jaxbContext;

    private ThreadLocal<Marshaller> jaxbMarshallerThreadLocal = new ThreadLocal<>();
    private ThreadLocal<Unmarshaller> jaxbUnmarshallerThreadLocal = new ThreadLocal<>();


    public JaxbParser(Class... classesToBeBound) {
        try {
            jaxbContext = JAXBContext.newInstance(classesToBeBound);
        } catch (JAXBException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public JaxbParser(Schema schema, Class... classesToBeBound) {
        try {
            jaxbContext = JAXBContext.newInstance(classesToBeBound);
            this.schema = schema;
        } catch (JAXBException e) {
            throw new IllegalArgumentException(e);
        }
    }

    //    http://stackoverflow.com/questions/30643802/what-is-jaxbcontext-newinstancestring-contextpath
    public JaxbParser(String context) {
        try {
            jaxbContext = JAXBContext.newInstance(context);
        } catch (JAXBException e) {
            throw new IllegalArgumentException(e);
        }
    }

    protected Marshaller getMarshaller() throws JAXBException {
        Marshaller marshaller = jaxbMarshallerThreadLocal.get();
        if (marshaller == null) {
            marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
            jaxbMarshallerThreadLocal.set(marshaller);
        }
        marshaller.setSchema(schema);
        return marshaller;
    }

    protected Unmarshaller getUnmarshaller() throws JAXBException {
        Unmarshaller unmarshaller = jaxbUnmarshallerThreadLocal.get();
        if (unmarshaller == null) {
            unmarshaller = jaxbContext.createUnmarshaller();
            jaxbUnmarshallerThreadLocal.set(unmarshaller);
        }
        unmarshaller.setSchema(schema);
        return unmarshaller;
    }

    // Unmarshaller
    public <T> T unmarshal(InputStream is) throws JAXBException {
        return (T) getUnmarshaller().unmarshal(is);
    }

    public <T> T unmarshal(Reader reader) throws JAXBException {
        return (T) getUnmarshaller().unmarshal(reader);
    }

    public <T> T unmarshal(String str) throws JAXBException {
        return (T) getUnmarshaller().unmarshal(new StringReader(str));
    }

    public <T> T unmarshal(XMLStreamReader reader, Class<T> elementClass) throws JAXBException {
        return (T) getUnmarshaller().unmarshal(reader, elementClass).getValue();
    }

    // Marshaller
    public void setMarshallerProperty(String prop, Object value) {
        try {
            getMarshaller().setProperty(prop, value);
        } catch (JAXBException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public String marshal(Object instance) throws JAXBException {
        StringWriter sw = new StringWriter();
        getMarshaller().marshal(instance, sw);
        return sw.toString();
    }

    public void marshal(Object instance, Writer writer) throws JAXBException {
        getMarshaller().marshal(instance, writer);
    }

    public void setSchema(Schema schema) {
        this.schema = schema;
    }

    public void validate(String str) throws IOException, SAXException {
        validate(new StringReader(str));
    }

    public void validate(Reader reader) throws IOException, SAXException {
        schema.newValidator().validate(new StreamSource(reader));
    }
}
