package uk.ac.open.kmi.msm4j.io.util;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.iterator.Filter;

import java.util.Set;

/**
 * FilterSomeRDFTypes
 * This filter accepts elements that have one of RDF Types it is looking for.
 * When used for keeping, it will keep any instances of SOME types contemplated
 * When used for dropping, it will drop those elements that are instances of ALL of the types
 * contemplated
 *
 * @author <a href="mailto:carlos.pedrinaci@open.ac.uk">Carlos Pedrinaci</a> (KMi - The Open University)
 * @since 29/11/2013
 */
public class FilterSomeRDFTypes<T extends RDFNode> extends Filter<T> {

    private final Set<Resource> rdfTypes;

    public FilterSomeRDFTypes(Set<Resource> rdfTypes) {
        this.rdfTypes = rdfTypes;
    }

    @Override
    public boolean accept(T node) {
        if (node.canAs(Individual.class)) {
            Individual individual = node.as(Individual.class);
            for (Resource rdfType : rdfTypes) {
                if (individual.hasRDFType(rdfType))
                    return true;
            }
        }
        return false;
    }
}
