/*
 * Copyright (c) 2015. Knowledge Media Institute - The Open University
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

import junit.framework.Assert;
import org.jukito.JukitoModule;
import org.jukito.JukitoRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.open.kmi.msm4j.Service;
import uk.ac.open.kmi.msm4j.io.ServiceReader;
import uk.ac.open.kmi.msm4j.io.ServiceWriter;
import uk.ac.open.kmi.msm4j.io.Syntax;
import uk.ac.open.kmi.msm4j.io.TransformationException;
import uk.ac.open.kmi.msm4j.io.impl.ServiceTransformationEngine;
import uk.ac.open.kmi.msm4j.io.impl.TransformerModule;

import java.io.*;
import java.util.Collection;
import java.util.List;

@RunWith(JukitoRunner.class)
public class SwaggerTransformerIT {

    private static final Logger log = LoggerFactory.getLogger(SwaggerTransformerIT.class);
    private static final String API_DOCS_URL = "http://localhost:10000/api-docs";

    private SwaggerTransformer importer;

    @Before
    public void setUp() throws Exception {
        importer = new SwaggerTransformer();
    }

    @Test
    public void testTransformRemoteDescription() {
        Collection<Service> services = null;

        try {
            services = importer.transform(null, API_DOCS_URL + "/pet-store/");
        } catch (TransformationException e) {
            e.printStackTrace();
        }

        Assert.assertNotNull("Service collection should not be null", services);
        Assert.assertTrue("There should be one service", 1 == services.size());

        try {
            services = importer.transform(null, API_DOCS_URL + "/iserve/");
        } catch (TransformationException e) {
            e.printStackTrace();
        }

        Assert.assertNotNull("Service collection should not be null", services);
        Assert.assertTrue("There should be one service", 1 == services.size());
    }

    @Test
    public void testTransformReadAndWrite(ServiceReader reader, ServiceWriter writer) {
        Collection<Service> services = null;
        try {
            services = importer.transform(null, API_DOCS_URL + "/iserve/");
        } catch (TransformationException e) {
            e.printStackTrace();
        }

        Assert.assertNotNull("Service collection should not be null", services);
        Assert.assertTrue("There should be one service", 1 == services.size());

        try {
            for (Service service : services) {
                File ontoFile = new File("temp.n3");
                FileOutputStream fos = new FileOutputStream(ontoFile);
                writer.serialise(service, fos, Syntax.N3);
                fos.flush();
                fos.close();

                List<Service> parsedServices = reader.parse(new FileInputStream(ontoFile), "http://test", Syntax.N3);
                Assert.assertNotNull(parsedServices.get(0));

                ontoFile.delete();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    @Test
    public void testPluginBasedTransformation(ServiceTransformationEngine serviceTransformationEngine) {
        try {
            List<Service> services = serviceTransformationEngine.transform(API_DOCS_URL + "/pet-store/", SwaggerTransformer.mediaType);
            Assert.assertNotNull("Service collection should not be null", services);
            Assert.assertTrue("There should be one service", 1 == services.size());
        } catch (Exception e) {
            log.error("Problems transforming the service. Continuing", e);
        }


    }

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

}
