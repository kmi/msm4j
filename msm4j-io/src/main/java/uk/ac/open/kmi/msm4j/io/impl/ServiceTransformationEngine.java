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

package uk.ac.open.kmi.msm4j.io.impl;

import com.google.common.collect.ImmutableList;
import com.google.inject.*;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.open.kmi.msm4j.Service;
import uk.ac.open.kmi.msm4j.io.*;
import uk.ac.open.kmi.msm4j.io.util.FilenameFilterForTransformer;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ServiceTransformationEngine is a Singleton Facade to any registered Service Transformation implementations.
 * Transformation implementations are automatically detected and registered at runtime
 *
 * @author <a href="mailto:carlos.pedrinaci@open.ac.uk">Carlos Pedrinaci</a> (KMi - The Open University)
 * @since 18/07/2013
 */
@Singleton
public class ServiceTransformationEngine {
    private static final Logger log = LoggerFactory.getLogger(ServiceTransformationEngine.class);
    /**
     * Map containing the loaded plugins indexed by media type. This map is automatically populated at runtime
     * based on the detected implementations in the classpath.
     */
    @Inject(optional = true)
    private final Map<String, Provider<ServiceTransformer>> loadedPluginsMap;

    @Inject
    protected ServiceTransformationEngine(Map<String, Provider<ServiceTransformer>> transformersMap) {
        this.loadedPluginsMap = transformersMap;
    }

    public static void main(String[] args) {

        Options options = new Options();
        // automatically generate the help statement
        HelpFormatter formatter = new HelpFormatter();
        // create the parser
        CommandLineParser parser = new GnuParser();

        options.addOption("h", "help", false, "print this message");
        options.addOption("i", "input", true, "input directory or file to transform");
        options.addOption("f", "format", true, "format to use for parsing the files.");
        options.addOption("e", "export", true, "export format. Default: " + MediaType.TEXT_TURTLE.getMediaType());
        options.addOption("s", "save", false, "save the resulting services to files? Default: false.");

        // parse the command line arguments
        CommandLine line;
        String input = null;
        File inputFile, outputFolder = null;
        String mediaType = null;
        Syntax export = null;
        boolean saveResults = false;
        try {
            line = parser.parse(options, args);

            // Help
            if (line.hasOption("help")) {
                formatter.printHelp(ServiceTransformationEngine.class.getCanonicalName(), options);
                System.exit(0);
            }

            // Input
            input = line.getOptionValue("i");
            if (input == null) {
                // The input should not be null
                formatter.printHelp(80, "java " + ServiceTransformationEngine.class.getCanonicalName(), "Options:", options, "You must provide an input", true);
                System.exit(-2);
            }

            // Input Format
            mediaType = line.getOptionValue("f");
            if (mediaType == null) {
                // The media type should not be null
                formatter.printHelp(80, "java " + ServiceTransformationEngine.class.getCanonicalName(),
                        "Options:", options, "You must provide an input media type", true);
                System.exit(-2);
            }

            // Export Format
            export = Syntax.valueOf(line.getOptionValue("e", Syntax.TTL.getName()));
            if (export == null) {
                Syntax[] supportedSyntaxes = Syntax.values();
                StringBuilder infoSyntaxes = new StringBuilder();
                for (Syntax supportedSyntax : supportedSyntaxes) {
                    infoSyntaxes.append(supportedSyntax.name()).append("\n");
                }

                // The export media type should not be null
                formatter.printHelp(80, "java " + ServiceTransformationEngine.class.getCanonicalName(),
                        "Options:", options, "You must provide an export format. Supported syntaxes are: "
                                + infoSyntaxes.toString(), true);
                System.exit(-2);
            }

            // Save
            saveResults = line.hasOption("s");

        } catch (ParseException e) {
            formatter.printHelp(80, "java " + ServiceTransformationEngine.class.getCanonicalName(), "Options:", options,
                    "Error parsing arguments", true);
            System.exit(-1);
        }

        // Initialise the injector
        Injector injector = Guice.createInjector(new TransformerModule());
        ServiceTransformationEngine transformationEngine = injector.getInstance(ServiceTransformationEngine.class);
        ServiceWriter writer = injector.getInstance(ServiceWriter.class);

        inputFile = new File(input);
        if (inputFile == null || !inputFile.exists()) {
            formatter.printHelp(80, "java " + ServiceTransformationEngine.class.getCanonicalName(), "Options:", options, "Input not found", true);
            System.out.println(inputFile.getAbsolutePath());
            System.exit(-2);
        }

        if (saveResults) {
            if (inputFile.isDirectory()) {
                outputFolder = new File(inputFile.getAbsolutePath() + "-MSM");
            } else {
                outputFolder = new File(inputFile.getParentFile().getAbsolutePath() + "/MSM");
            }
        }

        // Check the format is supported
        if (!transformationEngine.canTransform(mediaType)) {
            Set<String> supportedTypes = transformationEngine.getSupportedMediaTypes();
            StringBuilder builder = new StringBuilder();
            for (String type : supportedTypes) {
                builder.append(type).append("\n");
            }

            formatter.printHelp(80, "java " + ServiceTransformationEngine.class.getCanonicalName(), "Options:", options,
                    "\nMedia type - " + mediaType + " - is not supported. Supported media types are: \n" +
                            builder.toString(),
                    true);
            System.exit(-4);
        }


        File[] filesToTransform;

        if (inputFile.isDirectory()) {
            FilenameFilter filesFilter = new FilenameFilterForTransformer(transformationEngine.getTransformer
                    (mediaType));
            if (filesFilter == null) {
                log.warn("Unable to find a file filter for this media type. No services transformed");
                System.out.println("Unable to find a file filter for this media type. No services transformed");
                System.exit(0);
            }

            filesToTransform = inputFile.listFiles(filesFilter);

        } else {
            filesToTransform = new File[1];
            filesToTransform[0] = inputFile;
        }

        int result = batchTransform(outputFolder, mediaType, export, saveResults, transformationEngine,
                writer, filesToTransform);

        log.info("Correctly transformed {} service(s).", result);
        System.exit(result);

    }

    private static int batchTransform(File outputFolder, String mediaType, Syntax exportSyntax,
                                      boolean saveResults, ServiceTransformationEngine transformationEngine,
                                      ServiceWriter writer, File[] filesToTransform) {

        int result = 0;
        File outputFile = null;
        List<Service> services;

        // Create the output folder if necessary
        if (saveResults && outputFolder != null && !outputFolder.exists()) {
            outputFolder.mkdir();
            log.info("Created output folder at {}", outputFolder.getAbsolutePath());
        }


        log.info("Processing {} file(s).", filesToTransform.length);
        for (File file : filesToTransform) {
            log.info("Transforming {}", file.getAbsolutePath());
            // Open the file, transform it and keep the results
            InputStream in = null;
            // By default system.out unless otherwise specified
            OutputStream out = System.out;
            try {
                in = new FileInputStream(file);
                services = transformationEngine.transform(in, mediaType);
                result = result + services.size();
                log.info("{} services transformed", services.size());

                String fileName = file.getName();
                String newFileName = fileName.substring(0,
                        fileName.length() - 4) + exportSyntax.getExtension();

                if (services.size() > 1) {
                    // Go through each and generate one file for each
                    for (int i = 0; i < services.size(); i++) {
                        try {
                            if (saveResults) {
                                outputFile = new File(outputFolder.getAbsolutePath() + "/" + newFileName + i);
                                out = new FileOutputStream(outputFile);
                            }
                            writer.serialise(services.get(i), out, exportSyntax);
                        } catch (FileNotFoundException e) {
                            log.error("File not found " + outputFile.getAbsolutePath() + ". Continuing.", e);
                        } finally {
                            // Close if we are using a file
                            if (out != null && saveResults) {
                                try {
                                    out.close();
                                } catch (IOException e) {
                                    log.error("Error while closing output file.", e);
                                    System.err.println("Error while closing output file.");
                                }
                            }
                        }
                    }
                } else {
                    try {
                        if (saveResults) {
                            outputFile = new File(outputFolder.getAbsolutePath() + "/" + newFileName);
                            out = new FileOutputStream(outputFile);
                        }
                        writer.serialise(services.get(0), out, exportSyntax);
                    } catch (FileNotFoundException e) {
                        log.error("File not found " + outputFile.getAbsolutePath() + ". Continuing.", e);
                    } finally {
                        // Close if we are using a file
                        if (out != null && saveResults) {
                            try {
                                out.close();
                            } catch (IOException e) {
                                log.error("Error while closing output file.", e);
                                System.err.println("Error while closing output file.");
                            }
                        }
                    }
                }
            } catch (FileNotFoundException e) {
                log.error("File not found " + file.getAbsolutePath() + ". Continuing...", e);
                System.err.println("File not found " + file.getAbsolutePath() + ". Continuing...");
            } catch (TransformationException e) {
                log.error("Problem transforming service. Continuing...", e);
                System.err.println("Error transforming service. Continuing...");
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        log.error("Error while closing output file.", e);
                        System.err.println("Error while closing input file.");
                    }
                }
            }
        }
        return result;
    }

    /**
     * Checks whether the ServiceTransformationEngine has support for a given media type.
     *
     * @param mediaType the media type to check for
     * @return true if it can be transformed, false otherwise.
     */
    public boolean canTransform(String mediaType) {
        return this.loadedPluginsMap.containsKey(mediaType);
    }

    /**
     * Obtains a set with all supported media types based on the loaded plugins
     *
     * @return the Set of media types supported.
     */
    public Set<String> getSupportedMediaTypes() {
        return this.loadedPluginsMap.keySet();
    }

    /**
     * Obtains the loaded transformer for a given media type or null if no transformer can deal with this type.
     *
     * @param mediaType the media type we are interested in
     * @return the corresponding Service ServiceTransformationEngine or null if none is adequate.
     */
    public ServiceTransformer getTransformer(String mediaType) {
        return this.loadedPluginsMap.get(mediaType).get();
    }

    /**
     * Gets the corresponding file extension to a given media type by checking with the appropriate transformer
     *
     * @param mediaType the media type for which to obtain the file extension
     * @return the file extension or null if it is not supported. Callers are advised to check first that the
     *         media type is supported {@see canTransform} .
     */
    public String getFileExtension(String mediaType) {
        if (mediaType == null) {
            return null;
        }

        Provider<ServiceTransformer> provider = this.loadedPluginsMap.get(mediaType);
        if (provider != null) {
            return provider.get().getSupportedFileExtensions().get(0);
        }

        return null;
    }

    /**
     * Gets the corresponding filename filter to a given media type by checking with the
     * appropriate transformer
     *
     * @param mediaType the media type for which to obtain the file extension
     * @return the filename filter or null if it is not supported. Callers are advised to check
     *         first that the media type is supported {@see canTransform} .
     */
    public FilenameFilter getFilenameFilter(String mediaType) {
        if (mediaType == null) {
            return null;
        }

        Provider<ServiceTransformer> provider = this.loadedPluginsMap.get(mediaType);
        if (provider != null) {
            return new FilenameFilterForTransformer(provider.get());
        }

        return null;
    }

    /**
     * Parses and transforms a file with service description(s), e.g. SAWSDL, OWL-S, hRESTS, etc., and
     * returns a list of Service objects defined in the file.
     *
     * @param originalFile The file or folder containing the semantic Web service description(s)
     * @param mediaType    The media type of the file to transform. This is used to select the right transformer.
     * @return A List with the services transformed conforming to MSM model
     */
    public List<Service> transform(File originalFile, String mediaType) throws TransformationException {
        return transform(originalFile, null, mediaType);
    }

    /**
     * Parses and transforms a file with service description(s), e.g. SAWSDL, OWL-S, hRESTS, etc., and
     * returns a list of Service objects defined in the file.
     *
     * @param originalFile The file or folder containing the semantic Web service description(s)
     * @param baseUri      The base URI to use while transforming the service description
     * @param mediaType    The media type of the file to transform. This is used to select the right transformer.
     * @return A List with the services transformed conforming to MSM model
     */
    public List<Service> transform(File originalFile, String baseUri, String mediaType) throws TransformationException {

        File[] filesToTransform;
        ImmutableList.Builder<Service> resultBuilder = ImmutableList.builder();

        if (originalFile != null && originalFile.exists()) {
            if (originalFile.isDirectory()) {
                FilenameFilter filesFilter = new FilenameFilterForTransformer(this.getTransformer
                        (mediaType));
                if (filesFilter == null) {
                    log.info("Unable to find a file filter for this media type.");
                    return resultBuilder.build();
                }

                filesToTransform = originalFile.listFiles(filesFilter);

            } else {
                filesToTransform = new File[1];
                filesToTransform[0] = originalFile;
            }

            for (File file : filesToTransform) {
                // Open the file, transform it and keep the results
                InputStream in = null;
                try {
                    in = new FileInputStream(file);
                    resultBuilder.addAll(transform(in, baseUri, mediaType));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    throw new TransformationException("Unable to open input file", e);
                } finally {
                    if (in != null) {
                        try {
                            in.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                            throw new TransformationException("Error while closing input file", e);
                        }
                    }
                }
            }
        }
        // Return the transformed services
        return resultBuilder.build();
    }

    /**
     * Parses and transforms a stream with service description(s), e.g. SAWSDL, OWL-S, hRESTS, etc., and
     * returns a list of Service objects defined in the stream.
     *
     * @param originalDescription The semantic Web service description(s)
     * @param mediaType           The media type of the file to transform. This is used to select the right transformer.
     * @return A List with the services transformed conforming to MSM model
     */
    public List<Service> transform(InputStream originalDescription, String mediaType) throws TransformationException {
        return transform(originalDescription, null, mediaType);
    }

    /**
     * Parses and transforms a stream with service description(s), e.g. SAWSDL, OWL-S, hRESTS, etc., and
     * returns a list of Service objects defined in the stream.
     *
     * @param baseUri   The base URI to use while transforming the service description
     * @param mediaType The media type of the file to transform. This is used to select the right transformer.
     * @return A List with the services transformed conforming to MSM model
     */
    public List<Service> transform(String baseUri, String mediaType) throws TransformationException {
        List<Service> result = new ArrayList<Service>();

            // Obtain or guess media type
        if (mediaType == null) {
                // TODO: Try to guess media type
            mediaType = "Unknown";
        }
        // Obtain the appropriate transformer
        if (this.canTransform(mediaType)) {
            // Invoke it
            ServiceTransformer transformer = this.getTransformer(mediaType);
            result = transformer.transform(null, baseUri);
        } else {
                // No transformer available
            throw new TransformationException(String.format("There is no available transformer for this media type: %s", mediaType));
        }

        // Return results
        return result;
    }

    /**
     * Parses and transforms a stream with service description(s), e.g. SAWSDL, OWL-S, hRESTS, etc., and
     * returns a list of Service objects defined in the stream.
     *
     * @param originalDescription The semantic Web service description(s)
     * @param baseUri             The base URI to use while transforming the service description
     * @param mediaType           The media type of the file to transform. This is used to select the right transformer.
     * @return A List with the services transformed conforming to MSM model
     */
    public List<Service> transform(InputStream originalDescription, String baseUri, String mediaType) throws TransformationException {

        List<Service> result = new ArrayList<Service>();

        if (originalDescription != null) {
            // Obtain or guess media type
            if (mediaType == null) {
                // TODO: Try to guess media type
                mediaType = "Unknown";
            }
            // Obtain the appropriate transformer
            if (this.canTransform(mediaType)) {
                // Invoke it
                ServiceTransformer transformer = this.getTransformer(mediaType);
                result = transformer.transform(originalDescription, baseUri);
            } else {
                // No transformer available
                throw new TransformationException(String.format("There is no available transformer for this media type: %s", mediaType));
            }
        }
        // Return results
        return result;
    }

}
