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

package uk.ac.open.kmi.msm4j.util;

import com.hp.hpl.jena.vocabulary.DCTerms;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import uk.ac.open.kmi.msm4j.vocabulary.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class holding the prefixes of the vocabularies.
 * Placed in here for now but will require rearranging
 * <p/>
 * Author: Carlos Pedrinaci (KMi - The Open University)
 * Date: 31/05/2013
 * Time: 11:41
 */
public class Vocabularies {

    public static final Map<String, String> prefixes;

    static {
        HashMap<String, String> aMap = new HashMap<String, String>();
        aMap.put("rdf", RDF.getURI());
        aMap.put("rdfs", RDFS.getURI());
        aMap.put("msm", MSM.NS);
        aMap.put("sawsdl", SAWSDL.NS);
        aMap.put("wl", WSMO_LITE.NS);
        aMap.put("hr", HRESTS.NS);
        aMap.put("msm-wsdl", MSM_WSDL.NS);
        aMap.put("msm-nfp", MSM_NFP.NS);
        aMap.put("foaf", FOAF.NS);
        aMap.put("dcterms", DCTerms.NS);
        aMap.put("cc", CC.NS);
        aMap.put("sioc", SIOC.NS);
        aMap.put("schema", SCHEMA.NS);
        aMap.put("msm-swagger", MSM_SWAGGER.NS);
        aMap.put("http-status-codes", HTTP_STATUS_CODES.NS);
        aMap.put("http-methods", HTTP_METHODS.NS);

        prefixes = Collections.unmodifiableMap(aMap);
    }

}
