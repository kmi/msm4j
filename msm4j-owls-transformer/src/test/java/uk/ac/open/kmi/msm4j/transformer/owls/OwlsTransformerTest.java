
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

package uk.ac.open.kmi.msm4j.transformer.owls;

import junit.framework.Assert;
import org.jukito.JukitoModule;
import org.jukito.JukitoRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.open.kmi.msm4j.Service;
import uk.ac.open.kmi.msm4j.io.ServiceWriter;
import uk.ac.open.kmi.msm4j.io.impl.ServiceTransformationEngine;
import uk.ac.open.kmi.msm4j.io.impl.ServiceWriterImpl;
import uk.ac.open.kmi.msm4j.io.impl.TransformerModule;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.net.URI;
import java.util.*;

/**
 * Test class for the OWLS Importer
 * <p/>
 *
 * @author <a href="mailto:carlos.pedrinaci@open.ac.uk">Carlos Pedrinaci</a> (KMi - The Open University)
 * @since 18/07/2013
 */
@RunWith(JukitoRunner.class)
public class OwlsTransformerTest {

    private static final Logger log = LoggerFactory.getLogger(OwlsTransformerTest.class);
    private static final String OWLS_TC4 = "/services/OWLS-1.1/";

    // Number of files to transform. Integer.MAX_VALUE for all
    private static final int NUM_TESTS = 25 ;

    private OwlsTransformer importer;
    private ServiceWriter writer;
    private List<URI> testFolders;
    private FilenameFilter owlsFilter;

    /**
     * JukitoModule.
     */
    public static class InnerModule extends JukitoModule {
        @Override
        protected void configureTest() {

            // Ensure configuration is loaded
            install(new TransformerModule());
        }
    }

    @Before
    public void setUp() throws Exception {

        importer = new OwlsTransformer();
        writer = new ServiceWriterImpl();
        testFolders = new ArrayList<URI>();
        testFolders.add(OwlsTransformerTest.class.getResource(OWLS_TC4).toURI());

        owlsFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return (name.endsWith(".owl") || name.endsWith(".owls"));
            }
        };

    }

    @Test
    public void testTransformInputStream() throws Exception {

        // Add all the test collections
        log.info("Transforming test collections");
        for (URI testFolder : testFolders) {

            System.out.println(testFolder);

            log.info("Test collection: {} ", testFolder);
            File dir = new File(testFolder);

            // Test services
            Collection<Service> services;
            log.info("Transforming services");
            File[] owlsFiles = dir.listFiles(owlsFilter);
            List<File> files = Arrays.asList(owlsFiles);
            Collections.shuffle(files);

            for (int i = 0; i < NUM_TESTS && i < files.size(); i++) {
                File file = files.get(i);
                log.info("Transforming service {}", file.getAbsolutePath());
                try {
                    InputStream in = new FileInputStream(file);
                    services = importer.transform(in, null);
                    Assert.assertNotNull("Service collection should not be null", services);
                    Assert.assertEquals(1, services.size());
                } catch (Exception e) {
                    log.error("Problems transforming the service. Continuing", e);
                }
            }
        }
    }

    @Test
    public void testPluginBasedTransformation(ServiceTransformationEngine serviceTransformationEngine) {
        // Add all the test collections
        log.info("Transforming test collections");
        for (URI testFolder : testFolders) {
            log.info("Test collection: {} ", testFolder);
            File dir = new File(testFolder);

            // Test services
            Collection<Service> services;
            log.info("Transforming services");
            File[] owlsFiles = dir.listFiles(owlsFilter);
            List<File> files = Arrays.asList(owlsFiles);
            Collections.shuffle(files);

            for (int i = 0; i < NUM_TESTS && i < files.size(); i++) {
                File file = files.get(i);
                log.info("Transforming service {}", file.getAbsolutePath());
                try {
                    services = serviceTransformationEngine.transform(file, null, OwlsTransformer.mediaType);
                    Assert.assertNotNull("Service collection should not be null", services);
                    Assert.assertEquals(1, services.size());
                } catch (Exception e) {
                    log.error("Problems transforming the service. Continuing", e);
                }
            }
        }

    }
}
