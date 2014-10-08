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

import com.hp.hpl.jena.datatypes.xsd.XSDDateTime;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.hp.hpl.jena.vocabulary.OWL2;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.open.kmi.msm4j.*;
import uk.ac.open.kmi.msm4j.io.ServiceReader;
import uk.ac.open.kmi.msm4j.io.Syntax;
import uk.ac.open.kmi.msm4j.io.util.FilterByRdfType;
import uk.ac.open.kmi.msm4j.io.util.FilterSomeRDFTypes;
import uk.ac.open.kmi.msm4j.nfp.Forum;
import uk.ac.open.kmi.msm4j.nfp.Provider;
import uk.ac.open.kmi.msm4j.nfp.TwitterAccount;
import uk.ac.open.kmi.msm4j.vocabulary.*;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

/**
 * ServiceReaderImpl
 * TODO: Provide Description
 * <p/>
 * Author: Carlos Pedrinaci (KMi - The Open University)
 * Date: 01/06/2013
 * Time: 12:23
 */
public class ServiceReaderImpl implements ServiceReader {

    private static final Logger log = LoggerFactory.getLogger(ServiceReaderImpl.class);

    /**
     * Read a stream in RDF and return the corresponding set of services
     * as Java Objects
     *
     * @param in      The input stream of MSM services
     * @param baseUri Base URI to use for parsing
     * @param syntax  used within the stream
     * @return The collection of Services parsed from the stream
     */
    @Override
    public List<Service> parse(InputStream in, String baseUri, Syntax syntax) {

        OntModel model = null;
        List<Service> result = new ArrayList<Service>();
        try {
            // create an empty model
            model = ModelFactory.createOntologyModel(OntModelSpec.RDFS_MEM);
            // Parse the stream into a model
            model.read(in, baseUri, syntax.getName());
            result = parseService(model);
        } finally {
            if (model != null)
                model.close();
        }

        return result;
    }

    public ArrayList<Service> parseService(OntModel model) {
        ArrayList<Service> result = new ArrayList<Service>();
        // Get the services
        Service service;
        Individual individual;
        ExtendedIterator<Individual> services = model.listIndividuals(MSM.Service);
        while (services.hasNext()) {
            individual = services.next();
            try {
                service = obtainService(individual);
                if (service != null) {
                    result.add(service);
                }
            } catch (URISyntaxException e) {
                log.error("Incorrect URI while parsing service.", e);
            }

        }

        return result;
    }

    private Service obtainService(Individual individual) throws URISyntaxException {

        if (individual == null)
            return null;

        Service service = new Service(new URI(individual.getURI()));
        setInvocableEntityProperties(individual, service);

        NodeIterator hasOpValues = null;
        try {
            hasOpValues = individual.listPropertyValues(MSM.hasOperation);
            List<Operation> operations = obtainOperations(hasOpValues);
            service.setOperations(operations);
        } finally {
            if (hasOpValues != null)
                hasOpValues.close();
        }

        // Address
        if (individual.hasProperty(HRESTS.hasAddress)) {
            service.setAddress(individual.getProperty(HRESTS.hasAddress).getString());
        }

        // NFP parsing

        if (individual.hasProperty(MSM_NFP.hasTotalMashups)) {
            service.setTotalMashups(individual.getProperty(MSM_NFP.hasTotalMashups).getInt());
        }

        if (individual.hasProperty(MSM_NFP.hasRecentMashups)) {
            service.setRecentMashups(individual.getProperty(MSM_NFP.hasRecentMashups).getInt());
        }

        // Forum
        Resource forumValue = individual.getPropertyResourceValue(MSM_NFP.hasForum);
        if (forumValue != null) {
            Forum forum = new Forum(new URI(forumValue.getURI()));
            setResourceProperties(forumValue.as(Individual.class), forum);

            if (forumValue.hasProperty(MSM_NFP.hasVitality)) {
                forum.setVitality(forumValue.getProperty(MSM_NFP.hasVitality).getDouble());
            }
            if (forumValue.hasProperty(SIOC.has_host)) {
                try {
                    forum.setSite(new URL(forumValue.getProperty(SIOC.has_host).getString()));
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
            service.setForum(forum);
        }
        // Twitter account
        Resource twitterAccountValue = individual.getPropertyResourceValue(MSM_NFP.hasTwitterAccount);
        if (twitterAccountValue != null) {
            TwitterAccount twitterAccount = new TwitterAccount(new URI(twitterAccountValue.getURI()));
            setResourceProperties(twitterAccountValue.as(Individual.class), twitterAccount);
            service.setTwitterAccount(twitterAccount);
        }
        // Provider
        Resource providerValue = individual.getPropertyResourceValue(SCHEMA.provider);
        if (providerValue != null) {
            Provider provider = new Provider(new URI(providerValue.getURI()));
            setResourceProperties(providerValue.as(Individual.class), provider);
            if (providerValue.hasProperty(MSM_NFP.hasPopularity)) {
                provider.setPopularity(providerValue.getProperty(MSM_NFP.hasPopularity).getDouble());
            }
            service.setProvider(provider);
        }

        return service;
    }

    private void obtainGrounding(Individual individual, AnnotableResource resource) throws URISyntaxException {


        // generic grounding
        RDFNode rdfGrounding = individual.getPropertyValue(MSM.isGroundedIn);
        if (rdfGrounding != null) {
            if (rdfGrounding.isResource()) {
                resource.setGrounding(new ConceptGrounding(new URI(rdfGrounding.asResource().getURI()), new URI(MSM.isGroundedIn.getURI())));
            }
            if (rdfGrounding.isLiteral()) {
                if (rdfGrounding.asLiteral().getDatatypeURI() != null) {
                    resource.setGrounding(new LiteralGrounding(rdfGrounding.asLiteral().getString(), new URI(rdfGrounding.asLiteral().getDatatypeURI()), new URI(MSM.isGroundedIn.getURI())));
                } else {
                    resource.setGrounding(new LiteralGrounding(rdfGrounding.asLiteral().getString(), new URI(MSM.isGroundedIn.getURI())));
                }
            }
        }

        // WSDL grounding
        rdfGrounding = individual.getPropertyValue(MSM_WSDL.isGroundedIn);
        if (rdfGrounding != null) {
            if (rdfGrounding.isLiteral()) {
                if (rdfGrounding.asLiteral().getDatatypeURI() != null) {
                    resource.setGrounding(new LiteralGrounding(rdfGrounding.asLiteral().getString(), new URI(rdfGrounding.asLiteral().getDatatypeURI()), new URI(MSM_WSDL.isGroundedIn.getURI())));
                } else {
                    resource.setGrounding(new LiteralGrounding(rdfGrounding.asLiteral().getString(), new URI(MSM_WSDL.isGroundedIn.getURI())));
                }
            }
        }

        //Swagger grounding
        rdfGrounding = individual.getPropertyValue(MSM_WSDL.isGroundedIn);
        if (rdfGrounding != null) {
            if (rdfGrounding.isLiteral()) {
                if (rdfGrounding.asLiteral().getDatatypeURI() != null) {
                    resource.setGrounding(new LiteralGrounding(rdfGrounding.asLiteral().getString(), new URI(rdfGrounding.asLiteral().getDatatypeURI()), new URI(MSM_SWAGGER.isGroundedIn.getURI())));
                } else {
                    resource.setGrounding(new LiteralGrounding(rdfGrounding.asLiteral().getString(), new URI(MSM.isGroundedIn.getURI())));
                }
            }
        }

    }


    private List<Operation> obtainOperations(NodeIterator hasOpValues) throws URISyntaxException {

        List<Operation> operations = new ArrayList<Operation>();
        if (hasOpValues == null)
            return operations;

        Operation operation;
        Individual individual;
        RDFNode value;

        while (hasOpValues.hasNext()) {
            value = hasOpValues.next();
            if (value.canAs(Individual.class)) {
                individual = value.as(Individual.class);
                operation = new Operation(new URI(individual.getURI()));
                setInvocableEntityProperties(individual, operation);


                // Process Message Contents
                MessageContent mc;
                RDFNode rdfNode;
                rdfNode = individual.getPropertyValue(MSM.hasInput);
                mc = obtainMessageContent(rdfNode);
                if (mc != null)
                    operation.addInput(mc);

                rdfNode = individual.getPropertyValue(MSM.hasOutput);
                mc = obtainMessageContent(rdfNode);
                if (mc != null)
                    operation.addOutput(mc);

                rdfNode = individual.getPropertyValue(MSM.hasFault);
                mc = obtainMessageContent(rdfNode);
                if (mc != null)
                    operation.addFault(mc);

                rdfNode = individual.getPropertyValue(MSM.hasInputFault);
                mc = obtainMessageContent(rdfNode);
                if (mc != null)
                    operation.addInputFault(mc);

                rdfNode = individual.getPropertyValue(MSM.hasOutputFault);
                mc = obtainMessageContent(rdfNode);
                if (mc != null)
                    operation.addOutputFault(mc);

                // Address
                if (individual.hasProperty(HRESTS.hasAddress)) {
                    // Address
                    if (individual.hasProperty(HRESTS.hasAddress)) {
                        operation.setAddress(individual.getProperty(HRESTS.hasAddress).getString());
                    }
                }

                // Method
                if (individual.hasProperty(HRESTS.hasMethod)) {
                    operation.setMethod(new URI(individual.getProperty(HRESTS.hasMethod).getString()));
                }

                // ProducesContentTypes
                NodeIterator producesContentTypes = individual.listPropertyValues(HRESTS.producesContentType);
                while (producesContentTypes.hasNext()) {
                    RDFNode contentTypeNode = producesContentTypes.next();
                    if (contentTypeNode.isResource()) {
                        operation.addProducesContentType(new URI(contentTypeNode.asResource().getURI()));
                    }
                }

                NodeIterator acceptsContentTypes = individual.listPropertyValues(HRESTS.acceptsContentType);
                while (acceptsContentTypes.hasNext()) {
                    RDFNode contentTypeNode = acceptsContentTypes.next();
                    if (contentTypeNode.isResource()) {
                        operation.addAcceptsContentType(new URI(contentTypeNode.asResource().getURI()));
                    }
                }

                operations.add(operation);
            }
        }
        return operations;
    }

    private MessageContent obtainMessageContent(RDFNode inputNode) throws URISyntaxException {
        MessageContent result = null;
        if (inputNode == null || !inputNode.canAs(Individual.class)) {
            return result;
        }

        Individual individual = inputNode.as(Individual.class);
        result = new MessageContent(new URI(individual.getURI()));
        setAnnotableResourceProperties(individual, result);

        // Process mandatory parts
        List<MessagePart> mps;
        mps = obtainParts(individual, MSM.hasMandatoryPart);
        result.setMandatoryParts(mps);

        // Process optional parts
        mps = obtainParts(individual, MSM.hasOptionalPart);
        result.setOptionalParts(mps);

        // Process Lifting and Lowerings
        List<URI> mappings;
        mappings = obtainMappings(individual, SAWSDL.liftingSchemaMapping);
        result.setLiftingSchemaMappings(mappings);

        mappings = obtainMappings(individual, SAWSDL.loweringSchemaMapping);
        result.setLoweringSchemaMappings(mappings);

        return result;
    }

    private List<URI> obtainMappings(Individual individual, Property mappingProperty) throws URISyntaxException {

        List<URI> result = new ArrayList<URI>();
        if (individual == null || mappingProperty == null)
            return result;

        NodeIterator nodeIterator = null;
        Resource schemaMap;
        RDFNode node;
        try {
            nodeIterator = individual.listPropertyValues(mappingProperty);
            while (nodeIterator.hasNext()) {
                node = nodeIterator.next();
                if (node.isResource()) {
                    schemaMap = node.asResource();
                    result.add(new URI(schemaMap.getURI()));
                }
            }
        } finally {
            if (nodeIterator != null)
                nodeIterator.close();
        }
        return result;
    }

    private List<MessagePart> obtainParts(Individual individual, Property partsProperty) throws URISyntaxException {

        List<MessagePart> result = new ArrayList<MessagePart>();
        if (individual == null)
            return result;

        RDFNode node;
        MessagePart messagePart;
        NodeIterator nodeIterator = null;
        try {
            nodeIterator = individual.listPropertyValues(partsProperty);
            while (nodeIterator.hasNext()) {
                node = nodeIterator.next();
                messagePart = obtainMessagePart(node);
                if (messagePart != null)
                    result.add(messagePart);
            }
        } finally {
            if (nodeIterator != null)
                nodeIterator.close();
        }
        return result;
    }

    private MessagePart obtainMessagePart(RDFNode inputNode) throws URISyntaxException {

        MessagePart result = null;
        if (inputNode == null || !inputNode.canAs(Individual.class)) {
            return result;
        }

        Individual individual = inputNode.as(Individual.class);
        result = new MessagePart(new URI(individual.getURI()));
        setAnnotableResourceProperties(individual, result);

        // Process mandatory parts
        List<MessagePart> mps;
        mps = obtainParts(individual, MSM.hasMandatoryPart);
        result.setMandatoryParts(mps);

        // Process optional parts
        mps = obtainParts(individual, MSM.hasOptionalPart);
        result.setOptionalParts(mps);

        return result;
    }

    private void setInvocableEntityProperties(Individual individual, InvocableEntity invocEntity) throws URISyntaxException {

        if (individual == null || invocEntity == null)
            return;

        setAnnotableResourceProperties(individual, invocEntity);

        List<Condition> conditions = (List<Condition>) obtainAxioms(individual, Condition.class);
        invocEntity.setConditions(conditions);

        List<Effect> effects = (List<Effect>) obtainAxioms(individual, Effect.class);
        invocEntity.setEffects(effects);
    }

    private List<? extends LogicalAxiom> obtainAxioms(Individual individual, Class<? extends LogicalAxiom> axiomClass) throws URISyntaxException {

        List<? extends LogicalAxiom> result;
        Resource resourceType;

        if (axiomClass.equals(Condition.class)) {
            result = new ArrayList<Condition>();
            resourceType = WSMO_LITE.Condition;

        } else if (axiomClass.equals(Effect.class)) {
            result = new ArrayList<Effect>();
            resourceType = WSMO_LITE.Effect;
        } else {
            // We just sent it empty. The type does not matter
            return new ArrayList<Effect>();
        }

        RDFNode node;
        NodeIterator nodeIterator = null;

        // Process the appropriate model references
        try {
            nodeIterator = individual.listPropertyValues(SAWSDL.modelReference);
            FilterByRdfType typeFilter = new FilterByRdfType(resourceType);
            ExtendedIterator<RDFNode> filteredIter = nodeIterator.filterKeep(typeFilter);
            while (filteredIter.hasNext()) {
                node = filteredIter.next();
                Individual axiomIndiv = node.as(Individual.class); // Should be possible given the filter
                if (axiomClass.equals(Condition.class)) {
                    Condition cond;
                    if (axiomIndiv.isAnon()) {
                        cond = new Condition(null);
                    } else {
                        cond = new Condition(new URI(axiomIndiv.getURI()));
                    }
                    setResourceProperties(axiomIndiv, cond);
                    cond.setTypedValue(getAxiom(axiomIndiv));
                    ((List<Condition>) result).add(cond);

                } else if (axiomClass.equals(Effect.class)) {
                    Effect effect;
                    if (axiomIndiv.isAnon()) {
                        effect = new Effect(null);
                    } else {
                        effect = new Effect(new URI(axiomIndiv.getURI()));
                    }

                    setResourceProperties(axiomIndiv, effect);
                    effect.setTypedValue(getAxiom(axiomIndiv));
                    ((List<Effect>) result).add(effect);
                }
            }
        } finally {
            if (nodeIterator != null)
                nodeIterator.close();
        }

        return result;
    }

    private Object getAxiom(Individual axiomIndiv) {

        RDFNode value = axiomIndiv.getPropertyValue(RDF.value);
        if (value != null && value.isLiteral()) {
            return value.asLiteral().getValue();
        }

        return null;
    }

    private void setAnnotableResourceProperties(Individual individual, AnnotableResource annotRes) throws URISyntaxException {

        if (individual == null || annotRes == null)
            return;

        setResourceProperties(individual, annotRes);

        // Grounding
        obtainGrounding(individual, annotRes);

        URI nodeUri = null;
        NodeIterator nodeIter = null;
        ExtendedIterator<RDFNode> filteredIter;
        try {
            nodeIter = individual.listPropertyValues(SAWSDL.modelReference);
            // Process ModelReferences that are have no known type
            // Filter the modelRefs first then
            Set<Resource> filterTypes = new HashSet<Resource>();
            filterTypes.add(WSMO_LITE.Condition);
            filterTypes.add(WSMO_LITE.Effect);
            filterTypes.add(WSMO_LITE.NonfunctionalParameter);
            FilterSomeRDFTypes typesFilter = new FilterSomeRDFTypes(filterTypes);
            filteredIter = nodeIter.filterDrop(typesFilter);

            while (filteredIter.hasNext()) {
                RDFNode node = filteredIter.next();
                if (node.isResource()) {
                    if (node.isAnon()) {
                        log.warn("Unexpected blank node for generic model reference. Ignoring");
                    } else {
                        nodeUri = new URI(node.asResource().getURI());
                        annotRes.addModelReference(
                                new uk.ac.open.kmi.msm4j.Resource(nodeUri));
                    }
                }
            }
        } finally {
            if (nodeIter != null)
                nodeIter.close();
        }

        // Process NFPs
        try {
            nodeIter = individual.listPropertyValues(SAWSDL.modelReference);
            FilterByRdfType typeFilter = new FilterByRdfType(WSMO_LITE.NonfunctionalParameter);
            filteredIter = nodeIter.filterKeep(typeFilter);
            while (filteredIter.hasNext()) {
                RDFNode node = filteredIter.next();
                if (node.isResource()) {
                    annotRes.addNonFunctionalProperty(
                            new NonFunctionalProperty(new URI(node.asResource().getURI())));
                }
            }
        } finally {
            if (nodeIter != null)
                nodeIter.close();
        }

    }

    private void setResourceProperties(Individual individual, uk.ac.open.kmi.msm4j.Resource result) throws URISyntaxException {
        Resource res;
        result.setComment(individual.getComment(null));
        result.setLabel(individual.getLabel(null));

        // seeAlso
        NodeIterator seeAlsoIterator = individual.listPropertyValues(RDFS.seeAlso);
        for (RDFNode seeAlsoValue : seeAlsoIterator.toList()) {
            result.addSeeAlso(new URI(seeAlsoValue.asResource().getURI()));
        }

        // source
        res = individual.getPropertyResourceValue(DCTerms.source);
        if (res != null) {
            result.setSource(new URI(res.getURI()));
        }

        // creator
        res = individual.getPropertyResourceValue(DCTerms.creator);
        if (res != null) {
            result.setCreator(new URI(res.getURI()));
        }

        // created
        Date created = getDate(individual, DCTerms.created);
        if (created != null) {
            result.setCreated(created);
        }

        // issued
        Date issued = getDate(individual, DCTerms.issued);
        if (issued != null) {
            result.setIssued(issued);
        }

        //licenses
        NodeIterator licenseIterator = individual.listPropertyValues(DCTerms.license);
        for (RDFNode licenseValue : licenseIterator.toList()) {
            result.addLicense(new URI(licenseValue.asResource().getURI()));
        }

        //owl:sameAs
        NodeIterator sameAsIterator = individual.listPropertyValues(OWL2.sameAs);
        for (RDFNode sameAsValue : sameAsIterator.toList()) {
            result.addSameAs(new URI(sameAsValue.asResource().getURI()));
        }

    }

    private Date getDate(Individual individual, Property property) {

        Statement stmt = individual.getProperty(property);
        if (stmt != null) {
            Object obj = stmt.getObject();
            if (obj instanceof Literal) {
                Literal lit = (Literal) obj;
                XSDDateTime d = (XSDDateTime) lit.getValue();
                return d.asCalendar().getTime();
            }
        }
        return null;
    }
}
