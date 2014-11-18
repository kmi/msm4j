/*
 * Copyright (c) 2014. Knowledge Media Institute - The Open University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.open.kmi.msm4j.transformer.sawsdl;

import com.ebmwebsourcing.easybox.api.*;
import com.ebmwebsourcing.easysawsdl10.api.SawsdlHelper;
import com.ebmwebsourcing.easyschema10.api.SchemaXmlObject;
import com.ebmwebsourcing.easyschema10.api.element.Element;
import com.ebmwebsourcing.easyschema10.api.type.Type;
import com.ebmwebsourcing.easywsdl11.api.element.*;
import com.ebmwebsourcing.easywsdl11.api.element.Operation;
import com.ebmwebsourcing.easywsdl11.api.element.Service;
import com.ebmwebsourcing.easywsdl11.api.type.TBindingOperationMessage;
import com.ebmwebsourcing.easywsdl11.api.type.TDocumented;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.open.kmi.msm4j.*;
import uk.ac.open.kmi.msm4j.io.ServiceTransformer;
import uk.ac.open.kmi.msm4j.io.TransformationException;
import uk.ac.open.kmi.msm4j.io.util.URIUtil;
import uk.ac.open.kmi.msm4j.vocabulary.MSM_WSDL;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.xpath.XPathFactory;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.CodeSource;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public class SawsdlTransformer implements ServiceTransformer {

    private static final Logger log = LoggerFactory.getLogger(SawsdlTransformer.class);

    // Include information about the software version
    private static final String VERSION_PROP_FILE = "sawsdl-transformer.properties";
    private static final String VERSION_PROP = "sawsdl-transformer.version";
    private static final String VERSION_UNKNOWN = "Unknown";
    private String version = VERSION_UNKNOWN;

    // Supported Media Type
    public static String mediaType = "application/wsdl+xml";

    // Supported File Extensions
    private static List<String> fileExtensions = new ArrayList<String>();

    static {
        fileExtensions.add("wsdl");
        fileExtensions.add("sawsdl");
    }

    private final XmlContextFactory xmlContextFactory;
    private final XmlContext xmlContext;
    private final XmlObjectReader reader;

    public SawsdlTransformer() throws TransformationException {

        // create factory: can be static
        xmlContextFactory = new XmlContextFactory();

        // create context: can be static
        xmlContext = xmlContextFactory.newContext();

        // create generic reader: cannot be static!!! not thread safe!!!
        reader = xmlContext.createReader();

        // Set the parsing properties
//        System.setProperty("javax.xml.xpath.XPathFactory",
//                "net.sf.saxon.xpath.XPathFactoryImpl");
//
//        System.setProperty("javax.xml.transform.TransformerFactory",
//                "net.sf.saxon.TransformerFactoryImpl");
//
//        System.setProperty("javax.xml.parsers.DocumentBuilderFactory",
//                "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");
//
//        System.setProperty("javax.xml.parsers.SAXParserFactory",
//                "com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl");

        log.debug(this.getJaxpImplementationInfo("DocumentBuilderFactory", DocumentBuilderFactory.newInstance().getClass()));
        log.debug(this.getJaxpImplementationInfo("XPathFactory", XPathFactory.newInstance().getClass()));
        log.debug(this.getJaxpImplementationInfo("TransformerFactory", TransformerFactory.newInstance().getClass()));
        log.debug(this.getJaxpImplementationInfo("SAXParserFactory", SAXParserFactory.newInstance().getClass()));
    }


    private String getJaxpImplementationInfo(String componentName, Class componentClass) {
        CodeSource source = componentClass.getProtectionDomain().getCodeSource();
        return MessageFormat.format(
                "{0} implementation: {1} loaded from: {2}",
                componentName,
                componentClass.getCanonicalName(),
                source == null ? "Java Runtime" : source.getLocation());
    }

    /**
     * Obtains the Media Type this plugin supports for transformation.
     * Although registered Media Types do not necessarily identify uniquely
     * a given semantic service description format it is used for identification
     * for now.
     *
     * @return the media type covered
     */
    @Override
    public String getSupportedMediaType() {
        return mediaType;
    }

    /**
     * Obtains the different file extensions that this plugin can be applied to.
     * Again this does not necessarily uniquely identify a format but helps filter the filesystem
     * when doing batch transformation and may also be used as final solution in cases where we fail
     * to identify a format.
     *
     * @return a List of file extensions supported
     */
    @Override
    public List<String> getSupportedFileExtensions() {
        return fileExtensions;
    }

    /**
     * Obtains the version of the plugin used. Relevant as this is based on plugins and 3rd party may
     * provide their own implementations. Having the version is useful for provenance information and debuggin
     * purposes.
     *
     * @return a String with the version of the plugin.
     */
    @Override
    public String getVersion() {
        return this.version;
    }

    /**
     * Parses and transforms a stream with service description(s), e.g. SAWSDL, OWL-S, hRESTS, etc., and
     * returns a list of Service objects defined in the stream.
     *
     * @param originalDescription The semantic Web service description(s)
     * @param baseUri             The base URI to use while transforming the service description
     * @return A List with the services transformed conforming to MSM model
     */
    @Override
    public List<uk.ac.open.kmi.msm4j.Service> transform(InputStream originalDescription, String baseUri) throws TransformationException {

        List<uk.ac.open.kmi.msm4j.Service> msmServices = new ArrayList<uk.ac.open.kmi.msm4j.Service>();
        if (originalDescription == null)
            return msmServices;

        Definitions definitions;
        try {

            definitions = reader.readDocument(originalDescription, Definitions.class);
            if (definitions == null)
                return msmServices;

            uk.ac.open.kmi.msm4j.Service msmSvc;
            com.ebmwebsourcing.easywsdl11.api.element.Service[] wsdlServices = definitions.getServices();
            for (com.ebmwebsourcing.easywsdl11.api.element.Service wsdlSvc : wsdlServices) {
                msmSvc = transform(wsdlSvc, baseUri, definitions);
                if (msmSvc != null)
                    msmServices.add(msmSvc);
            }

            return msmServices;
        } catch (XmlObjectReadException e) {
            log.error("Problems reading XML Object exception while parsing service", e);
            throw new TransformationException("Problems reading XML Object exception while parsing service", e);
        }

    }

    private uk.ac.open.kmi.msm4j.Service transform(Service wsdlSvc, String baseUri, Definitions definitions) {

        uk.ac.open.kmi.msm4j.Service msmSvc = null;
        if (wsdlSvc == null)
            return msmSvc;

        QName qname = wsdlSvc.inferQName();

        if (baseUri == null) {
            baseUri = qname.getNamespaceURI();
        }

        try {
            // Use WSDL 2.0 naming http://www.w3.org/TR/wsdl20/#wsdl-iri-references
            StringBuilder builder = new StringBuilder().
                    append(baseUri).append("#").
                    append("wsdl.service").append("(").append(qname.getLocalPart()).append(")");

            StringBuilder serviceUriBuilder = new StringBuilder().append(baseUri).append("/").append(qname.getLocalPart());
            URI svcUri = new URI(serviceUriBuilder.toString());
            msmSvc = new uk.ac.open.kmi.msm4j.Service(svcUri);
            msmSvc.setSource(URI.create(baseUri));
            msmSvc.setGrounding(new LiteralGrounding(builder.toString(), new URI(MSM_WSDL.XPointer.getURI()), new URI(MSM_WSDL.isGroundedIn.getURI())));
            msmSvc.setLabel(qname.getLocalPart());

            // Add documentation
            addComment(wsdlSvc, msmSvc);

            addModelReferences(wsdlSvc, msmSvc);

            // Process Operations


            uk.ac.open.kmi.msm4j.Operation msmOp;
            Port[] ports = wsdlSvc.getPorts();
            for (Port port : ports) {
                if (port.hasBinding()) {
                    BindingOperation[] operations = port.findBinding().getOperations();
                    for (BindingOperation operation : operations) {
                        msmOp = transform(operation, URIUtil.getNameSpace(svcUri), port.getName(), definitions);
                        msmSvc.addOperation(msmOp);
                    }

                }
            }


        } catch (URISyntaxException e) {
            log.error("Syntax exception while generating service URI", e);
        }

        return msmSvc;
    }

    private uk.ac.open.kmi.msm4j.Operation transform(BindingOperation wsdlOp, URI namespace, String portName, Definitions definitions) {

        uk.ac.open.kmi.msm4j.Operation msmOp = null;
        if (wsdlOp == null)
            return msmOp;

        StringBuilder builder = new StringBuilder(namespace.toASCIIString()).append("#").
                append("wsdl.interfaceOperation").
                append("(").append(portName).append("/").append(wsdlOp.getName()).append(")");

        try {
            URI opUri = new URI(new StringBuilder().append(namespace).append("/").append(portName).append("-").append(wsdlOp.getName()).toString());
            msmOp = new uk.ac.open.kmi.msm4j.Operation(opUri);
            msmOp.setSource(namespace);
            msmOp.setGrounding(new LiteralGrounding(builder.toString(), new URI(MSM_WSDL.XPointer.getURI()), new URI(MSM_WSDL.isGroundedIn.getURI())));
            msmOp.setLabel(wsdlOp.getName());

            // Add documentation
            addComment(wsdlOp, msmOp);

            if (msmOp.getComment() == null) {
                //Extract WSDL description from abstract part
                for (PortType portType : definitions.getPortTypes()) {
                    Operation abWsdlOp = portType.getOperationByName(wsdlOp.getName());
                    if (abWsdlOp != null) {
                        addComment(abWsdlOp, msmOp);
                    }
                }
            }

            // Add model references
            addModelReferences(wsdlOp, msmOp);

            // Process Inputs, Outputs and Faults
            BindingOperationInput input = wsdlOp.getInput();
            MessageContent mcIn = transform(input, namespace, portName, wsdlOp.getName());
            msmOp.addInput(mcIn);
            addModelReferences(input, mcIn);
//            addSchemaMappings(input, mcIn);

            BindingOperationOutput output = wsdlOp.getOutput();
            MessageContent mcOut = transform(output, namespace, portName, wsdlOp.getName());
            msmOp.addOutput(mcOut);
            addModelReferences(output, mcOut);
//            addSchemaMappings(output, mcOut);

            // TODO: Process faults

        } catch (URISyntaxException e) {
            log.error("Syntax exception while generating operation URI", e);
        }


        return msmOp;
    }

    private MessageContent transform(TBindingOperationMessage message, URI namespace, String portName, String opName) {

        MessageContent mc = null;
        if (message == null)
            return mc;

        String direction = (message instanceof BindingOperationInput) ? "In" : "Out";

        StringBuilder builder = new StringBuilder(namespace.toASCIIString()).append("#").
                append("wsdl.interfaceMessageReference").
                append("(").append(portName).append("/").append(opName).append("/").append(direction).append(")");


        try {
            URI mcUri = new URI(new StringBuilder().append(namespace).append("/").append(portName).append("-").append(opName).append("-").append(direction).toString());
            mc = new MessageContent(mcUri);
            mc.setSource(namespace);
            mc.setGrounding(new LiteralGrounding(builder.toString(), new URI(MSM_WSDL.XPointer.getURI()), new URI(MSM_WSDL.isGroundedIn.getURI())));

            mc.setLabel(message.getName());
            addComment(message, mc);

            // Process parts
//            List<Part> parts = message.getParts();
//            for (Part part : parts) {
//                mc.addMandatoryPart(transform(part));
//            }
        } catch (URISyntaxException e) {
            log.error("Syntax exception while generating message URI", e);
        }

        return mc;
    }

    private void addComment(TDocumented element, Resource resource) {
        Documentation doc = element.getDocumentation();
        if (doc != null && doc.getContent() != null && !doc.getContent().isEmpty())
            resource.setComment(doc.getContent());
    }

    private MessagePart transform(Part part) {
        return null;  //To change body of created methods use File | Settings | File Templates.
    }

    private void addModelReferences(XmlObject object, AnnotableResource annotableResource) {

        URI[] modelRefs = SawsdlHelper.getModelReference(object);
        for (URI modelRef : modelRefs) {
            annotableResource.addModelReference(new Resource(modelRef));
        }

    }

    private void addSchemaMappings(SchemaXmlObject object, MessagePart message) {

        // Handle liftingSchemaMappings (only on elements or types)
        if (object == null)
            return;

        URI[] liftList = null;
        URI[] lowerList = null;

        if (object instanceof Type) {
            liftList = SawsdlHelper.getLiftingSchemaMapping((Type) object);
            lowerList = SawsdlHelper.getLoweringSchemaMapping((Type) object);
        } else if (object instanceof Element) {
            liftList = SawsdlHelper.getLiftingSchemaMapping((Element) object);
            lowerList = SawsdlHelper.getLoweringSchemaMapping((Element) object);
        }

        for (URI liftUri : liftList) {
            message.addLiftingSchemaMapping(liftUri);
        }

        for (URI lowerUri : lowerList) {
            message.addLiftingSchemaMapping(lowerUri);
        }

    }

}
