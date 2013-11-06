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

package uk.ac.open.kmi.msm4j.transformer.hrests;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.tidy.Tidy;
import uk.ac.open.kmi.msm4j.Service;
import uk.ac.open.kmi.msm4j.io.ServiceReader;
import uk.ac.open.kmi.msm4j.io.ServiceTransformer;
import uk.ac.open.kmi.msm4j.io.Syntax;
import uk.ac.open.kmi.msm4j.io.TransformationException;
import uk.ac.open.kmi.msm4j.io.impl.ServiceReaderImpl;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:jacek.kopecky@open.ac.uk">Jacek Kopecky</a> (KMi - The Open University)
 * @author <a href="mailto:carlos.pedrinaci@open.ac.uk">Carlos Pedrinaci</a> (KMi - The Open University)
 * @since 0.2
 */
public class HrestsTransformer implements ServiceTransformer {

    private static final Logger log = LoggerFactory.getLogger(HrestsTransformer.class);

    private static final String XSLT = "hrests.xslt";

    private final Tidy parser;
    private final TransformerFactory xformFactory;
    private Transformer transformer;

    // Include information about the software version
    private static final String VERSION_PROP_FILE = "hrests-transformer.properties";
    private static final String VERSION_PROP = "hrests-transformer.version";
    private static final String VERSION_UNKNOWN = "Unknown";

    private
    @Inject(optional = true)
    @Named(VERSION_PROP)
    String version = VERSION_UNKNOWN;

//    private
//    @Inject
//    @Named("xslt")
//    String xsltUrl = null;

    // Supported Media Type
    public static String mediaType = "text/html";

    // Supported File Extensions
    private static List<String> fileExtensions = new ArrayList<String>();

    static {
        fileExtensions.add("hrests");
        fileExtensions.add("html");
        fileExtensions.add("xhtml");
    }


    public HrestsTransformer() {
        parser = new Tidy();
        xformFactory = TransformerFactory.newInstance();
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
    public List<Service> transform(InputStream originalDescription, String baseUri) throws TransformationException {
        // Exit early if no content is provided
        if (originalDescription == null) {
            return new ArrayList<Service>();
        }

        if (this.transformer == null) {
            configureTransformer();
        }

        ByteArrayInputStream istream = null;
        Document document = parser.parseDOM(originalDescription, System.out);

        DOMSource source = new DOMSource(document);
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        StreamResult stream = new StreamResult(bout);
        try {
            transformer.transform(source, stream);
            if (null == stream.getOutputStream()) {
                return new ArrayList<Service>();
            }

            // Now parse the resulting service descriptions
            byte[] data = ((ByteArrayOutputStream) stream.getOutputStream()).toByteArray();
            log.info("Resulting service:\n" + new String(data));

            istream = new ByteArrayInputStream(data);
            ServiceReader reader = new ServiceReaderImpl();
            return reader.parse(istream, baseUri, Syntax.RDFXML);
        } catch (TransformerException e) {
            throw new TransformationException(e);
        } finally {
            try {
                bout.close();
            } catch (IOException e) {
                log.error("Problem closing the hRESTS transformation output stream", e);
            }

            if (istream != null)
                try {
                    istream.close();
                } catch (IOException e) {
                    log.error("Problem closing the hRESTS input stream", e);
                }
        }

    }

    private void configureTransformer() {
        URL xsltUrl = getClass().getResource(XSLT);
        log.debug("Loading XSLT from {}", xsltUrl);
        File xsltFile = new File(xsltUrl.toString());

        try {
            transformer = this.xformFactory.newTransformer(new StreamSource(xsltFile));
        } catch (TransformerConfigurationException e) {
            log.error("Exception while configuring XSLT transformer", e);
        }
    }
}
