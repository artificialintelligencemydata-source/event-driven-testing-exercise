
package com.acuver.autwit.internal.logging;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;
public final class JsonXmlLogBuilder {
    private static final ObjectMapper PRETTY_JSON = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private JsonXmlLogBuilder(){}
    public static String formatJson(String json){
        try{Object obj = PRETTY_JSON.readValue(json, Object.class); return PRETTY_JSON.writeValueAsString(obj);}catch(Exception e){return json;}
    }
    public static String formatXml(String xml){
        try{
            var doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new org.xml.sax.InputSource(new StringReader(xml)));
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            return writer.toString();
        }catch(Exception e){return xml;}
    }
    public static String build(String title, String rawPayload){
        if(rawPayload==null) return title+" - <null>";
        String trimmed = rawPayload.trim();
        boolean isJson = trimmed.startsWith("{") || trimmed.startsWith("[");
        boolean isXml = trimmed.startsWith("<");
        String pretty = isJson ? formatJson(rawPayload) : isXml ? formatXml(rawPayload) : rawPayload;
        return "\n============== "+title+" ==============\n" + pretty + "\n=======================================================\n";
    }
}
