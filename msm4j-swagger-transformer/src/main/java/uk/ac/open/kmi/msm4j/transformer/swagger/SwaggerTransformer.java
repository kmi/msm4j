
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

package uk.ac.open.kmi.msm4j.transformer.swagger;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.smartbear.swagger4j.*;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.open.kmi.msm4j.MessageContent;
import uk.ac.open.kmi.msm4j.MessagePart;
import uk.ac.open.kmi.msm4j.Service;
import uk.ac.open.kmi.msm4j.io.ServiceTransformer;
import uk.ac.open.kmi.msm4j.io.ServiceWriter;
import uk.ac.open.kmi.msm4j.io.Syntax;
import uk.ac.open.kmi.msm4j.io.TransformationException;
import uk.ac.open.kmi.msm4j.io.impl.ServiceWriterImpl;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.CodeSource;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class SwaggerTransformer implements ServiceTransformer {

    private static final Logger log = LoggerFactory.getLogger(SwaggerTransformer.class);

    // Include information about the software version
    private static final String VERSION_PROP_FILE = "swagger-transformer.properties";
    private static final String VERSION_PROP = "swagger-transformer.version";
    private static final String VERSION_UNKNOWN = "Unknown";
    private String version = VERSION_UNKNOWN;
    // Supported Media Type
    public static String mediaType = "application/json";
    // Supported File Extensions
    private static List<String> fileExtensions = new ArrayList<String>();

    static {
        fileExtensions.add("json");
    }

    private Model httpStatusCodeModel;


    public SwaggerTransformer() throws TransformationException {
        httpStatusCodeModel = ModelFactory.createDefaultModel();
        String file = this.getClass().getResource("/").getFile();
        httpStatusCodeModel.read(this.getClass().getResource("/http-statusCodes-2014-09-03.rdf").getFile());
    }

    public static void main(String[] args) throws TransformationException {

        Options options = new Options();
        // automatically generate the help statement
        HelpFormatter formatter = new HelpFormatter();
        // create the parser
        CommandLineParser parser = new GnuParser();

        options.addOption("h", "help", false, "print this message");
        options.addOption("i", "input", true, "input directory, file or URL of documents to transform");
        options.addOption("s", "save", false, "save result? (default: false)");
        options.addOption("f", "format", true, "format to use when serialising. Default: TTL.");

        // parse the command line arguments
        CommandLine line = null;
        String input = null;
        File inputFile;
        try {
            line = parser.parse(options, args);
            input = line.getOptionValue("i");
            if (line.hasOption("help")) {
                formatter.printHelp("SwaggerImporter", options);
                return;
            }
            if (input == null) {
                // The input should not be null
                formatter.printHelp(80, "java " + SwaggerTransformer.class.getCanonicalName(), "Options:", options, "You must provide an input", true);
                return;
            }
        } catch (ParseException e) {
            formatter.printHelp(80, "java " + SwaggerTransformer.class.getCanonicalName(), "Options:", options, "Error parsing input", true);
        }

        SwaggerTransformer importer = new SwaggerTransformer();

        ServiceWriter writer = new ServiceWriterImpl();

        File rdfDir = null;
        boolean save = line.hasOption("s");
        if (save) {
            rdfDir = new File("RDF");
            rdfDir.mkdir();
        }

        // Obtain details for serialisation format
        String format = line.getOptionValue("f", uk.ac.open.kmi.msm4j.io.Syntax.TTL.getName());
        uk.ac.open.kmi.msm4j.io.Syntax syntax = uk.ac.open.kmi.msm4j.io.Syntax.valueOf(format);


        try {
            URI inputUri = new URI(input);
            List<Service> services = importer.transform(inputUri.toURL().openStream(), inputUri.toASCIIString());
            writeServicesToFileSystem(services, rdfDir, "out.", syntax, writer);
            return;

        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        inputFile = new File(input);
        if (inputFile == null || !inputFile.exists()) {
            formatter.printHelp(80, "java " + SwaggerTransformer.class.getCanonicalName(), "Options:", options, "Input not found", true);
            System.out.println(inputFile.getAbsolutePath());
            return;
        }


        // Obtain input for transformation
        File[] toTransform;
        if (inputFile.isDirectory()) {
            // Get potential files to transform based on extension
            FilenameFilter swaggerFilter = new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return (name.endsWith(".json"));
                }
            };

            toTransform = inputFile.listFiles(swaggerFilter);
        } else {
            toTransform = new File[1];
            toTransform[0] = inputFile;
        }


        List<uk.ac.open.kmi.msm4j.Service> services;
        System.out.println("Transforming input");
        for (File file : toTransform) {
            try {
                services = importer.transform(new FileInputStream(file), null);
                writeServicesToFileSystem(services, rdfDir, file.getName(), syntax, writer);

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (TransformationException e) {
                e.printStackTrace();
            }
        }
    }

    public static void writeServicesToFileSystem(List<Service> services, File rdfDir, String fileName, Syntax syntax, ServiceWriter writer) {
        if (services != null) {
            System.out.println("Services obtained: " + services.size());

            File resultFile = null;
            if (rdfDir != null) {
                String newFileName = fileName.substring(0, fileName.lastIndexOf('.') + 1) + syntax.getExtension();
                resultFile = new File(rdfDir.getAbsolutePath() + "/" + newFileName);
            }

            if (rdfDir != null) {
                OutputStream out = null;
                try {
                    out = new FileOutputStream(resultFile);
                    for (uk.ac.open.kmi.msm4j.Service service : services) {
                        if (out != null) {
                            writer.serialise(service, out, syntax);
                            System.out.println("Service saved at: " + resultFile.getAbsolutePath());
                        } else {
                            writer.serialise(service, System.out, syntax);
                        }
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } finally {
                    if (out != null)
                        try {
                            out.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                }
            }

        }
    }

    public void setProxy(String proxyHost, String proxyPort) {
        if (proxyHost != null && proxyPort != null) {
            Properties prop = System.getProperties();
            prop.put("http.proxyHost", proxyHost);
            prop.put("http.proxyPort", proxyPort);
        }
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
        if (originalDescription == null && baseUri == null) {
            return msmServices;
        } else {
            URI base = null;
            if (baseUri != null) {
                try {
                    base = new URI(baseUri);
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            }

            try {
                log.info("Trying to retrieve the description from the Web...");
                ResourceListing resourceListing = Swagger.readSwagger(new URI(baseUri));
                msmServices.add(transform(resourceListing, baseUri));
            } catch (IOException e) {
                log.info("Unable to retrieve the swagger description from the Web");
            } catch (URISyntaxException e) {
                log.error("Provided wrong URI");
            }

            if (msmServices == null) {
                try {
                    log.info("Trying to parse the description from the provided InputStream...");
                    ResourceListing resourceListing = Swagger.createReader().readResourceListing(new InputStreamSwaggerSource(originalDescription, base));
                    msmServices.add(transform(resourceListing, baseUri));
                } catch (IOException e) {
                    log.info("Unable to parse the swagger from the provided InputStream");
                }

            }
            return msmServices;
        }

    }

    private uk.ac.open.kmi.msm4j.Service transform(ResourceListing resourceListing, String baseUri) {

        uk.ac.open.kmi.msm4j.Service msmSvc = null;
        if (resourceListing == null) {
            return msmSvc;
        }

        try {

            if (resourceListing.getBasePath() != null) {
                msmSvc = new Service(new URI(resourceListing.getBasePath()));
            } else {
                msmSvc = new Service(new URI(resourceListing.getApis().get(0).getDeclaration().getBasePath()));
            }

            msmSvc.setLabel(resourceListing.getInfo().getTitle());
            msmSvc.setComment(resourceListing.getInfo().getDescription());
            msmSvc.setSource(new URI(baseUri));
            msmSvc.addLicense(new URI(resourceListing.getInfo().getLicenseUrl()));
            if (resourceListing.getBasePath() != null) {
                msmSvc.setAddress(resourceListing.getBasePath());
            }
            msmSvc.setHrestsGrounding("$");

            for (ResourceListing.ResourceListingApi apiRef : resourceListing.getApis()) {
                ApiDeclaration apiDeclaration = apiRef.getDeclaration();

                for (Api api : apiDeclaration.getApis()) {
                    for (com.smartbear.swagger4j.Operation swaggerOp : api.getOperations()) {
                        String address = api.getPath();
                        msmSvc.addOperation(transform(swaggerOp, address, baseUri));
                    }
                }
            }

        } catch (URISyntaxException e) {
            log.error("Syntax exception while generating operation URI", e);
        }

        return msmSvc;
    }

    private uk.ac.open.kmi.msm4j.Operation transform(com.smartbear.swagger4j.Operation swaggerOperation, String address, String baseUri) {

        uk.ac.open.kmi.msm4j.Operation msmOp = null;
        if (swaggerOperation == null)
            return msmOp;

        try {
            URI opUri = new URI(new StringBuilder().append(swaggerOperation.getApi().getApiDeclaration().getBasePath()).append("/").append(swaggerOperation.getNickName()).toString());
            msmOp = new uk.ac.open.kmi.msm4j.Operation(opUri);
            msmOp.setLabel(swaggerOperation.getNickName());
            msmOp.setComment(swaggerOperation.getSummary());


            msmOp.setAddress(address + buildParametersTemplate(swaggerOperation.getParameters()));
            msmOp.setMethod(swaggerOperation.getMethod().name());
            String resourcePath = swaggerOperation.getApi().getApiDeclaration().getResourcePath();
            msmOp.setHrestsGrounding(new StringBuilder("$[?(@.resourcePath == '").append(resourcePath).append("')].apis.operations.[?(@.nickname == '").append(swaggerOperation.getNickName()).append("')]").toString());
            msmOp.setSource(new URI(new StringBuilder(baseUri).append(resourcePath).toString()));
            for (String mediaType : swaggerOperation.getConsumes()) {
                msmOp.addAcceptsContentType(mediaType);
            }
            for (String mediaType : swaggerOperation.getProduces()) {
                msmOp.addProducesContentType(mediaType);
            }

            msmOp.addInput(transform(swaggerOperation.getParameters(), opUri, msmOp.getHrestsGrounding()));
            msmOp.setOutputs(transformOutputs(swaggerOperation.getResponseMessages(), opUri, msmOp.getHrestsGrounding()));
            msmOp.setOutputFaults(transformOutputFaults(swaggerOperation.getResponseMessages(), opUri, msmOp.getHrestsGrounding()));

        } catch (URISyntaxException e) {
            log.error("Syntax exception while generating operation URI", e);
        }


        return msmOp;
    }

    private String buildParametersTemplate(List<Parameter> parameters) {
        StringBuilder r = new StringBuilder("");
        int i = 0;
        for (Parameter parameter : parameters) {
            if (parameter.getParamType().name().equals("query")) {
                if (r.toString().length() == 0) {
                    r.append("?");
                } else {
                    r.append("&");
                }
                r.append(parameter.getName()).append("={p").append(i++).append("}");
            }
        }
        return r.toString();
    }

    private List<MessageContent> transformOutputFaults(List<ResponseMessage> responseMessages, URI opUri, String operationGrounding) {
        List<MessageContent> mcs = null;
        if (responseMessages == null)
            return mcs;
        mcs = new ArrayList<MessageContent>();
        for (ResponseMessage responseMessage : responseMessages) {
            if (responseMessage.getCode() >= 400) {
                mcs.add(transform(responseMessage, opUri, operationGrounding));
            }
        }
        return mcs;

    }

    private List<MessageContent> transformOutputs(List<ResponseMessage> responseMessages, URI opUri, String operationGrounding) {
        List<MessageContent> mcs = null;
        if (responseMessages == null)
            return mcs;
        mcs = new ArrayList<MessageContent>();
        for (ResponseMessage responseMessage : responseMessages) {
            if (responseMessage.getCode() < 400) {
                mcs.add(transform(responseMessage, opUri, operationGrounding));
            }
        }
        return mcs;
    }

    private MessageContent transform(ResponseMessage responseMessage, URI opUri, String operationGrounding) {
        if (responseMessage != null) {
            try {
                MessageContent mc = new MessageContent(new URI(new StringBuilder(opUri.toASCIIString()).append("/out/").append(responseMessage.getCode()).toString()));

                MessagePart httpStatus = new MessagePart(new URI(new StringBuilder(mc.getUri().toASCIIString()).append("/httpStatus").toString()));
                mc.addMandatoryPart(httpStatus);

                MessagePart code = new MessagePart(new URI(getHttpStatusCodeUri(responseMessage.getCode())));
                code.setLabel(Integer.toString(responseMessage.getCode()));
                code.setHrestsGrounding(new StringBuilder(operationGrounding).append(".responseMessages.code.[?(@ == ").append(responseMessage.getCode()).append(")]").toString());

                MessagePart message = new MessagePart(new URI(new StringBuilder(httpStatus.getUri().toASCIIString()).append("/message").toString()));
                message.setLabel("Explanation of the HTTP status " + responseMessage.getCode());
                message.setComment(responseMessage.getMessage());
                message.setHrestsGrounding(new StringBuilder(operationGrounding).append(".responseMessages.[?(@.code == ").append(responseMessage.getCode()).append(")].message").toString());

                httpStatus.addMandatoryPart(code);
                httpStatus.addMandatoryPart(message);
                if (responseMessage.getResponseModel() != null) {
                    MessagePart responseModel = new MessagePart(new URI(new StringBuilder(mc.getUri().toASCIIString()).append("/").append(responseMessage.getResponseModel()).toString()));
                    responseModel.setLabel(responseMessage.getResponseModel());
                    responseModel.setHrestsGrounding(new StringBuilder(operationGrounding).append(".responseMessages.[?(@.code == ").append(responseMessage.getCode()).append(")].responseModel").toString());
                    mc.addMandatoryPart(responseModel);
                }

                return mc;
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private String getHttpStatusCodeUri(int code) {
        if (httpStatusCodeModel.listStatements(null, null, Integer.toString(code)).hasNext()) {
            return httpStatusCodeModel.listStatements(null, null, Integer.toString(code)).next().getSubject().getURI();
        }
        return null;
    }

    private MessageContent transform(List<Parameter> parameters, URI opUri, String operationGrounding) {
        MessageContent mc = null;
        if (parameters == null)
            return mc;

        try {
            mc = new MessageContent(new URI(new StringBuilder(opUri.toASCIIString()).append("/").append("in").toString()));
            int i = 0;
            for (Parameter parameter : parameters) {
                MessagePart mp = new MessagePart(new URI(new StringBuilder(mc.getUri().toASCIIString()).append("/").append(parameter.getName()).toString()));
                mp.setLabel(parameter.getName());
                mp.setComment(parameter.getDescription());
                mp.setHrestsGrounding(new StringBuilder(operationGrounding).append(".parameters.[?(@.name == '").append(parameter.getName()).append("')]").toString());

                if (parameter.isRequired()) {
                    mc.addMandatoryPart(mp);
                } else {
                    mc.addOptionalPart(mp);
                }
            }

        } catch (URISyntaxException e) {
            log.error("Syntax exception while generating operation URI", e);
        }

        return mc;
    }

}
