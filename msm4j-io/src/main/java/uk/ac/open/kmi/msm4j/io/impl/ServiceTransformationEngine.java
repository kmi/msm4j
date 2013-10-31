/*
 * Copyright (c) 2013. Knowledge Media Institute - The Open University
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

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import uk.ac.open.kmi.msm4j.Service;
import uk.ac.open.kmi.msm4j.io.ServiceTransformer;
import uk.ac.open.kmi.msm4j.io.TransformationException;
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
        ServiceTransformer transformer = this.loadedPluginsMap.get(mediaType).get();
        if (transformer != null)
            return transformer.getSupportedFileExtensions().get(0);

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
        ServiceTransformer transformer = this.loadedPluginsMap.get(mediaType).get();
        if (transformer != null)
            return new FilenameFilterForTransformer(transformer);

        return null;
    }

    /**
     * Parses and transforms a file with service description(s), e.g. SAWSDL, OWL-S, hRESTS, etc., and
     * returns a list of Service objects defined in the file.
     *
     * @param originalDescription The file containing the semantic Web service description(s)
     * @param mediaType           The media type of the file to transform. This is used to select the right transformer.
     * @return A List with the services transformed conforming to MSM model
     */
    public List<Service> transform(File originalDescription, String mediaType) throws TransformationException {
        return transform(originalDescription, null, mediaType);
    }

    /**
     * Parses and transforms a file with service description(s), e.g. SAWSDL, OWL-S, hRESTS, etc., and
     * returns a list of Service objects defined in the file.
     *
     * @param originalDescription The file containing the semantic Web service description(s)
     * @param baseUri             The base URI to use while transforming the service description
     * @param mediaType           The media type of the file to transform. This is used to select the right transformer.
     * @return A List with the services transformed conforming to MSM model
     */
    public List<Service> transform(File originalDescription, String baseUri, String mediaType) throws TransformationException {

        if (originalDescription != null && originalDescription.exists()) {
            // Open the file and transform it
            InputStream in = null;
            try {
                in = new FileInputStream(originalDescription);
                return transform(in, baseUri, mediaType);
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
        // Return an empty array if it could not be transformed.
        return new ArrayList<Service>();
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
