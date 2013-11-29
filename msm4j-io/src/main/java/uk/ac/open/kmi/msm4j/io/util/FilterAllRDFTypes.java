package uk.ac.open.kmi.msm4j.io.util;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.iterator.Filter;

import java.util.Set;

/**
 * FilterAllRDFTypes
 * This filter accepts elements that have ALL RDF Types it is looking for.
 * When used for keeping, it will keep any instances of ALL types contemplated
 * When used for dropping, it will drop those elements that are instances of SOME of the types
 * contemplated
 *
 * @author <a href="mailto:carlos.pedrinaci@open.ac.uk">Carlos Pedrinaci</a> (KMi - The Open University)
 * @since 29/11/2013
 */
public class FilterAllRDFTypes<T extends RDFNode> extends Filter<T> {

    private final Set<Resource> rdfTypes;

    public FilterAllRDFTypes(Set<Resource> rdfTypes) {
        this.rdfTypes = rdfTypes;
    }

    @Override
    public boolean accept(T node) {
        if (node.canAs(Individual.class)) {
            Individual individual = node.as(Individual.class);
            for (Resource rdfType : rdfTypes) {
                if (!individual.hasRDFType(rdfType))
                    return false;
            }
        }
        return true;
    }
}