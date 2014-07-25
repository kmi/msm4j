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

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.TypeMapper;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.open.kmi.msm4j.*;
import uk.ac.open.kmi.msm4j.io.ServiceWriter;
import uk.ac.open.kmi.msm4j.io.Syntax;
import uk.ac.open.kmi.msm4j.io.extensionTypes.PddlType;
import uk.ac.open.kmi.msm4j.nfp.Forum;
import uk.ac.open.kmi.msm4j.nfp.Provider;
import uk.ac.open.kmi.msm4j.nfp.TwitterAccount;
import uk.ac.open.kmi.msm4j.util.Vocabularies;
import uk.ac.open.kmi.msm4j.vocabulary.*;

import java.io.OutputStream;
import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Service Writer Implementation
 * <p/>
 * Author: Carlos Pedrinaci (KMi - The Open University)
 * Date: 22/05/2013
 * Time: 02:13
 */
public class ServiceWriterImpl implements ServiceWriter {

    private static final Logger log = LoggerFactory.getLogger(ServiceWriterImpl.class);

    public ServiceWriterImpl() {
    }

    @Override
    public void serialise(Service service, OutputStream out, Syntax syntax) {
        Model model = generateModel(service);
        model.setNsPrefixes(Vocabularies.prefixes);

        try {
            model.write(out, syntax.getName());
        } finally {
            model.close();
        }
    }

    private void registerExtensionTypes(Model model) {
        // Register the expression types for conditions
        RDFDatatype rtype = PddlType.TYPE;
        TypeMapper.getInstance().registerDatatype(rtype);
    }

    public Model generateModel(Service service) {

        Model model = ModelFactory.createDefaultModel();
        registerExtensionTypes(model);

        // Exit early if null
        if (service == null)
            return model;

        // Create the service resource
        com.hp.hpl.jena.rdf.model.Resource current = model.createResource(service.getUri().toASCIIString());
        current.addProperty(RDF.type, MSM.Service);

        // Add the base annotations
        addResourceMetadata(model, service);
        // Add the modelReferences
        addModelReferences(model, service);
        // Add the grounding
        addGrounding(model, service);

        // Process Operations
        for (Operation op : service.getOperations()) {
            current.addProperty(MSM.hasOperation,
                    model.createResource(op.getUri().toASCIIString()));
            addOperationToModel(model, op);
        }

        // MSM-NFP extension
        addForum(model, service);
        addTwitterAccount(model, service);
        addProvider(model, service);

        addTotalMashups(model, service);
        addRecentMashups(model, service);

        return model;
    }

    private void addTotalMashups(Model model, Service service) {
        // Create Total Mashups Value
        if(service.getTotalMashups() != null){
            com.hp.hpl.jena.rdf.model.Resource current = model.createResource(service.getUri().toASCIIString());
            Literal totalMashups = model.createTypedLiteral(service.getTotalMashups());
            current.addProperty(MSM_NFP.hasTotalMashups,totalMashups);
        }
    }

    private void addRecentMashups(Model model, Service service) {
        // Create Recent Mashups Value
        if(service.getRecentMashups() != null){
            com.hp.hpl.jena.rdf.model.Resource current = model.createResource(service.getUri().toASCIIString());
            Literal recentMashups = model.createTypedLiteral(service.getRecentMashups());
            current.addProperty(MSM_NFP.hasRecentMashups,recentMashups);
        }
    }

    private void addProvider(Model model, Service service) {
        // Exit early if null
        if (service == null)
            return;

        // Add provider if present
        Provider provider = service.getProvider();
        if (provider != null) {
            // Create the resource
            com.hp.hpl.jena.rdf.model.Resource current = model.createResource(service.getUri().toASCIIString());
            com.hp.hpl.jena.rdf.model.Resource providerRes = model.createResource(provider.getUri().toASCIIString());
            current.addProperty(SCHEMA.provider, providerRes);
            providerRes.addProperty(RDF.type,SCHEMA.Organization);
            addResourceMetadata(model,provider);
            // Create Popularity Value
            if(provider.getPopularity() != null){
                Literal popularity = model.createTypedLiteral(provider.getPopularity());
                providerRes.addProperty(MSM_NFP.hasPopularity,popularity);
            }

        }
    }

    private void addTwitterAccount(Model model, Service service) {
        // Exit early if null
        if (service == null)
            return;

        // Add Twitter Account if present
        TwitterAccount twitterAccount = service.getTwitterAccount();
        if (twitterAccount != null) {
            // Create the resource
            com.hp.hpl.jena.rdf.model.Resource current = model.createResource(service.getUri().toASCIIString());
            com.hp.hpl.jena.rdf.model.Resource twitterAccountRes = model.createResource(twitterAccount.getUri().toASCIIString());
            twitterAccountRes.addProperty(RDF.type,MSM_NFP.TwitterAccount);
            current.addProperty(MSM_NFP.hasTwitterAccount, twitterAccountRes);
            addResourceMetadata(model,twitterAccount);
        }
    }

    private void addForum(Model model, Service service) {
        // Exit early if null
        if (service == null)
            return;

        // Add Forum if present
        Forum forum = service.getForum();
        if (forum != null) {
            // Create the resource
            com.hp.hpl.jena.rdf.model.Resource current = model.createResource(service.getUri().toASCIIString());
            com.hp.hpl.jena.rdf.model.Resource forumRes = model.createResource(forum.getUri().toASCIIString());
            forumRes.addProperty(RDF.type, SIOC.Forum);
            current.addProperty(MSM_NFP.hasForum, forumRes);
            addResourceMetadata(model,forum);
            if(forum.getSite() != null){
                forumRes.addProperty(SIOC.has_host,forum.getSite().toString());
            }
            if(forum.getVitality() != null){
                Literal vitality = model.createTypedLiteral(forum.getVitality());
                forumRes.addProperty(MSM_NFP.hasVitality,vitality);
            }
        }
    }


    private void addOperationToModel(Model model, Operation op) {

        // Exit early if null
        if (op == null)
            return;

        com.hp.hpl.jena.rdf.model.Resource current = model.createResource(op.getUri().toASCIIString());
        current.addProperty(RDF.type, MSM.Operation);
        // Add the base annotations
        addResourceMetadata(model, op);
        // Add the modelReferences
        addModelReferences(model, op);
        // Add the grounding
        addGrounding(model, op);

        // Process inputs
        for (MessageContent input : op.getInputs()) {
            current.addProperty(MSM.hasInput,
                    model.createResource(input.getUri().toASCIIString()));
            addMessageContent(model, input);
        }

        // Process outputs
        for (MessageContent output : op.getOutputs()) {
            current.addProperty(MSM.hasOutput,
                    model.createResource(output.getUri().toASCIIString()));
            addMessageContent(model, output);
        }

        // Process Input Faults
        for (MessageContent fault : op.getInputFaults()) {
            current.addProperty(MSM.hasInputFault,
                    model.createResource(fault.getUri().toASCIIString()));
            addMessageContent(model, fault);
        }

        // Process Output Faults
        for (MessageContent fault : op.getOutputFaults()) {
            current.addProperty(MSM.hasOutputFault,
                    model.createResource(fault.getUri().toASCIIString()));
            addMessageContent(model, fault);
        }

    }

    private void addMessageContent(Model model, MessageContent messageContent) {

        // Exit early if null
        if (messageContent == null)
            return;

        com.hp.hpl.jena.rdf.model.Resource current = model.createResource(messageContent.getUri().toASCIIString());
        current.addProperty(RDF.type, MSM.MessageContent);
        addMessagePart(model, messageContent);

        // Add the grounding
        addGrounding(model, messageContent);

        // Process liftings
        for (URI mapping : messageContent.getLiftingSchemaMappings()) {
            current.addProperty(SAWSDL.liftingSchemaMapping, mapping.toASCIIString());
        }

        // Process lowerings
        for (URI mapping : messageContent.getLoweringSchemaMappings()) {
            current.addProperty(SAWSDL.loweringSchemaMapping, mapping.toASCIIString());
        }
    }

    private void addMessagePart(Model model, MessagePart messagePart) {

        // Exit early if null
        if (messagePart == null)
            return;

        com.hp.hpl.jena.rdf.model.Resource current = model.createResource(messagePart.getUri().toASCIIString());
        current.addProperty(RDF.type, MSM.MessagePart);
        // Add the base annotations
        addResourceMetadata(model, messagePart);
        // Add the modelReferences
        addModelReferences(model, messagePart);

        // Process mandatory parts
        for (MessagePart part : messagePart.getMandatoryParts()) {
            current.addProperty(MSM.hasMandatoryPart,
                    model.createResource(part.getUri().toASCIIString()));
            addMessagePart(model, part);
        }
        // Process optional parts
        for (MessagePart part : messagePart.getOptionalParts()) {
            current.addProperty(MSM.hasOptionalPart,
                    model.createResource(part.getUri().toASCIIString()));
            addMessagePart(model, part);
        }

    }

    private void addModelReferences(Model model, AnnotableResource annotableResource) {
        // Exit early if null
        if (annotableResource == null || model == null)
            return;

        com.hp.hpl.jena.rdf.model.Resource current = model.createResource(annotableResource.getUri().toASCIIString());

        // Process general model references
        for (Resource mr : annotableResource.getModelReferences()) {
            com.hp.hpl.jena.rdf.model.Resource refResource;
            if (mr.getUri() == null) {
                log.warn("Not expecting a generic model reference to a blank node. ");
            } else {
                refResource = model.createResource(mr.getUri().toASCIIString());
                current.addProperty(SAWSDL.modelReference, refResource);
            }
        }

        // Process NFPs
        for (Resource nfp : annotableResource.getNfps()) {
            com.hp.hpl.jena.rdf.model.Resource refResource = model.createResource(nfp.getUri().toASCIIString());
            current.addProperty(SAWSDL.modelReference, refResource);
            refResource.addProperty(RDF.type, WSMO_LITE.NonfunctionalParameter);
        }

        if (annotableResource instanceof InvocableEntity) {
            InvocableEntity invEnt = (InvocableEntity) annotableResource;
            // Process Conditions
            for (Condition cond : invEnt.getConditions()) {
                com.hp.hpl.jena.rdf.model.Resource refResource = createRdfResource(model, cond);
                current.addProperty(SAWSDL.modelReference, refResource);
                refResource.addProperty(RDF.type, WSMO_LITE.Condition);
                refResource.addLiteral(RDF.value, cond.getTypedValue());
            }

            // Process Effects
            for (Effect effect : invEnt.getEffects()) {
                com.hp.hpl.jena.rdf.model.Resource refResource = createRdfResource(model, effect);
                current.addProperty(SAWSDL.modelReference, refResource);
                refResource.addProperty(RDF.type, WSMO_LITE.Effect);
                refResource.addLiteral(RDF.value, effect.getTypedValue());
            }
        }
    }

    /**
     * Creates an RDF Resource in a given model. Takes care of dealing with nonUri resources
     * and creates a blank node in these cases.
     *
     * @param model    the model
     * @param resource the MSM resource to be mapped to Jena
     * @return a URI resource or a blank node otherwise
     */
    private com.hp.hpl.jena.rdf.model.Resource createRdfResource(Model model, Resource resource) {

        com.hp.hpl.jena.rdf.model.Resource result;
        if (resource.getUri() != null) {
            result = model.createResource(resource.getUri().toASCIIString());
        } else {
            // create blank node
            result = model.createResource();
        }

        return result;
    }

    private void addGrounding(Model model, Resource resource) {
        // Exit early if null
        if (resource == null)
            return;

        // Add WSDL grounding if present
        URI grounding = resource.getWsdlGrounding();
        if (grounding != null) {
            // Create the resource
            com.hp.hpl.jena.rdf.model.Resource current = model.createResource(resource.getUri().toASCIIString());
            com.hp.hpl.jena.rdf.model.Resource gdgRes = model.createResource(grounding.toASCIIString());
            current.addProperty(MSM_WSDL.isGroundedIn, gdgRes);
        }

    }

    private void addResourceMetadata(Model model, Resource basicResource) {
        // Exit early if null
        if (basicResource == null)
            return;

        com.hp.hpl.jena.rdf.model.Resource current = model.createResource(basicResource.getUri().toASCIIString());

        // Label
        String label = basicResource.getLabel();
        if (label != null) {
            current.addProperty(RDFS.label, label);
        }

        // Comment
        String comment = basicResource.getComment();
        if (comment != null) {
            current.addProperty(RDFS.comment, comment);
        }

        // Creator
        URI creator = basicResource.getCreator();
        if (creator != null) {
            current.addProperty(DCTerms.creator,
                    model.createResource(creator.toASCIIString()));
        }

        // seeAlso
        if(basicResource.getSeeAlsos() != null){
            for(URI seeAlso:basicResource.getSeeAlsos()){
                current.addProperty(RDFS.seeAlso,
                        model.createResource(seeAlso.toASCIIString()));
            }
        }

        // Source
        URI source = basicResource.getSource();
        if (source != null) {
            current.addProperty(DCTerms.source,
                    model.createResource(source.toASCIIString()));
        }

        // Created
        Date created = basicResource.getCreated();
        Literal createdLiteral = createDateLiteral(created);
        if (createdLiteral != null) {
            current.addProperty(DCTerms.created, createdLiteral);
        }

        // Issued
        Date issued = basicResource.getIssued();
        Literal issuedLiteral = createDateLiteral(issued);
        if (issuedLiteral != null) {
            current.addProperty(DCTerms.issued, issuedLiteral);
        }

        // Licenses
        if(basicResource.getLicenses() != null){
            for(URI license:basicResource.getLicenses()){
                current.addProperty(DCTerms.license, model.createResource(license.toASCIIString()));
            }
        }

    }

    private Literal createDateLiteral(Date date) {

        if (date != null) {
            // Serialise the simple form
            DateFormat outputFormatter = new SimpleDateFormat("yyyy-MM-dd");
            String dateStr = outputFormatter.format(date);
            return ResourceFactory.createTypedLiteral(dateStr, XSDDatatype.XSDdate);
        }

        return null;

    }

}
