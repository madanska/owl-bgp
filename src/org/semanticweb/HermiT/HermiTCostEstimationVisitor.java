package org.semanticweb.HermiT;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.semanticweb.HermiT.hierarchy.InstanceManager;
import org.semanticweb.HermiT.model.AtomicConcept;
import org.semanticweb.HermiT.model.AtomicRole;
import org.semanticweb.HermiT.model.DLClause;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.sparql.arq.OWLOntologyGraph;
import org.semanticweb.sparql.bgpevaluation.CostEstimationVisitor;
import org.semanticweb.sparql.owlbgp.model.Atomic;
import org.semanticweb.sparql.owlbgp.model.Variable;
import org.semanticweb.sparql.owlbgp.model.classexpressions.ClassExpression;
import org.semanticweb.sparql.owlbgp.model.classexpressions.Clazz;
import org.semanticweb.sparql.owlbgp.model.individuals.Individual;
import org.semanticweb.sparql.owlbgp.model.properties.ObjectProperty;

public class HermiTCostEstimationVisitor extends CostEstimationVisitor {

    protected double POSSIBLE_INSTANCE_SUCCESS=0.5;
    
    protected final Reasoner m_hermit;
    protected final InstanceManager m_instanceManager;
    protected final double m_numDisjunctions;
    protected Integer m_classHierarchyDepth;
    protected Integer m_opHierarchyDepth;
    
    public HermiTCostEstimationVisitor(OWLOntologyGraph graph, Map<Variable,Integer> bindingPositions, List<Atomic[]> candidateBindings) {
        super(graph, bindingPositions, candidateBindings);
        if (m_reasoner instanceof Reasoner) {
            m_hermit=(Reasoner)m_reasoner;
            m_instanceManager=m_hermit.m_instanceManager;
            double numDisjunctions=0;
            for (DLClause clause : m_hermit.getDLOntology().getDLClauses())
                if (clause.getHeadLength()>1)
                    numDisjunctions+=clause.getHeadLength();
            m_numDisjunctions=numDisjunctions;
        } else 
            throw new IllegalArgumentException("Error: The HermiT cost estimator can only be instantiated with a graph that has a (HermiT) Reasoner instance attached to it.");
    }
    protected double[] getClassAssertionCost(ClassExpression ce, Individual ind, Set<Variable> unbound, Variable indVar) {
        double cost=0;
        if (ce instanceof Atomic && (m_instanceManager==null || !m_instanceManager.areClassesInitialised()))
            cost+=(m_classCount*m_indCount*COST_LOOKUP+COST_ENTAILMENT); // initialization required
        if (unbound.size()==0)
            if (ce instanceof Atomic) {
                if (m_instanceManager!=null 
                        && m_instanceManager.areClassesInitialised() 
                        && !m_instanceManager.hasType(org.semanticweb.HermiT.model.Individual.create(((Atomic)ind).getIdentifierString()), AtomicConcept.create(((Atomic)ce).getIdentifierString()), false))
                    return new double[] { cost+COST_LOOKUP, 0 };
                else 
                    return new double[] { cost+COST_LOOKUP, 1 }; // initialization required
            } else { 
                return new double[] { COST_ENTAILMENT, 1 };
            }
        else if (unbound.size()==1 && indVar!=null) {// C(?x)
            if (ce instanceof Atomic) 
                if (m_instanceManager!=null && m_instanceManager.areClassesInitialised()) { 
                    int[] estimate=m_instanceManager.getNumberOfInstances(AtomicConcept.create(((Clazz)ce).getIRIString()));
                    return new double[] { estimate[0]*COST_LOOKUP+estimate[1]*COST_ENTAILMENT*0.5*m_hermit.getClassHierarchyDepth(), estimate[0]+(POSSIBLE_INSTANCE_SUCCESS*estimate[1]) };
                } 
                return new double[] { cost+m_indCount, m_indCount }; // needs refinement 
        } else if (indVar==null && unbound.size()==1 && ce instanceof Atomic) {// ?x(:a)
            if (m_instanceManager!=null && m_instanceManager.areClassesInitialised()) {
                int[] estimate=m_instanceManager.getNumberOfTypes(org.semanticweb.HermiT.model.Individual.create(((Atomic)ind).getIdentifierString()));
                return new double[] { estimate[0]*COST_LOOKUP+estimate[1]*COST_ENTAILMENT*0.5*m_hermit.getClassHierarchyDepth(), estimate[0]+(POSSIBLE_INSTANCE_SUCCESS*estimate[1]) };
            }
            return new double[] { cost+m_classCount, m_classCount }; // needs refinement 
        } else {
            double tests=complex(unbound);
            return new double[] { tests, (double)(POSSIBLE_INSTANCE_SUCCESS*tests) }; // assume all tests succeed
        }
    }
    protected double[] getObjectPropertyAssertionCost(ObjectProperty op, Individual ind1, Individual ind2, Set<Variable> unbound, Variable opVar) {
        double cost=0;
        if (m_instanceManager==null || !m_instanceManager.arePropertiesInitialised())
            cost+=(m_opCount*m_indCount*COST_LOOKUP+COST_ENTAILMENT); // initialization required
        if (unbound.size()==0)
            if (m_instanceManager!=null && m_instanceManager.arePropertiesInitialised()) { 
                boolean[] result=m_instanceManager.hasSuccessor(AtomicRole.create(((Atomic)op).getIdentifierString()), org.semanticweb.HermiT.model.Individual.create(((Atomic)ind1).getIdentifierString()), org.semanticweb.HermiT.model.Individual.create(((Atomic)ind2).getIdentifierString()));
                if (result[0])
                    return new double[] { cost+COST_LOOKUP, 1 };
                else if (result[1])
                    return new double[] { cost+((double)0.5*COST_ENTAILMENT*getOPHierarchyDepth()), 1 };
            } else 
                return new double[] { cost+((double)0.5*COST_ENTAILMENT*getOPHierarchyDepth()), 1 }; // initialization required
        else if (unbound.size()==1 && opVar!=null) {// ?x(:a :b) 
            return new double[] { cost+m_opCount*COST_LOOKUP*(double)(0.5*COST_ENTAILMENT*getOPHierarchyDepth()), m_opCount};  
        } else if (unbound.size()==1 && opVar==null) {// op(:a ?x) or op(?x :a)
            if (m_instanceManager!=null && m_instanceManager.arePropertiesInitialised()) {
                int[] estimate;
                if (ind2 instanceof Variable)
                    estimate=m_instanceManager.getNumberOfSuccessors(AtomicRole.create(op.getIRIString()), org.semanticweb.HermiT.model.Individual.create(ind1.getIdentifierString()));
                else
                    estimate=m_instanceManager.getNumberOfPredecessors(AtomicRole.create(op.getIRIString()), org.semanticweb.HermiT.model.Individual.create(ind2.getIdentifierString()));
                //double[] estimate=m_instanceManager.getAverageNumberOfSuccessors(AtomicRole.create(op.getIRIString()));
                return new double[] { estimate[0]*COST_LOOKUP+(estimate[1]*COST_ENTAILMENT*0.5*getOPHierarchyDepth()), estimate[0]+(POSSIBLE_INSTANCE_SUCCESS*estimate[1]) };
            }
            return new double[] { cost+m_indCount*COST_LOOKUP, m_indCount }; // needs refinement 
        } else if (unbound.size()==2 && opVar!=null) {// ?x(:a ?y) or ?x(?y :a)
            if (m_instanceManager!=null && m_instanceManager.arePropertiesInitialised()) {
                double[] estimate=new double[2];
                for (ObjectProperty prop : m_graph.getObjectPropertiesInSignature()) {
                    int[] thisEstimate=m_instanceManager.getNumberOfPropertyInstances(AtomicRole.create(prop.getIRIString()));
                    estimate[0]+=((double)thisEstimate[0]/(double)thisEstimate[2]);
                    estimate[1]+=((double)thisEstimate[1]/(double)thisEstimate[2]);
                }
                return new double[] { m_opCount*COST_LOOKUP+(estimate[0]*COST_LOOKUP)+(estimate[1]*COST_ENTAILMENT*0.5*getOPHierarchyDepth()), estimate[0]+(POSSIBLE_INSTANCE_SUCCESS*estimate[1]) };
            }
            return new double[] { cost+m_opCount*m_indCount*COST_LOOKUP*(double)(0.5*COST_ENTAILMENT*getOPHierarchyDepth()), m_opCount*m_indCount};  
        } else if (unbound.size()==2 && opVar==null) {// op(?x ?y)
            if (m_instanceManager!=null && m_instanceManager.arePropertiesInitialised()) {
                int[] estimate=m_instanceManager.getNumberOfPropertyInstances(AtomicRole.create(op.getIRIString()));
                return new double[] { estimate[0]*COST_LOOKUP+(estimate[1]*COST_ENTAILMENT*0.5*getOPHierarchyDepth()), estimate[0]+(POSSIBLE_INSTANCE_SUCCESS*estimate[1]) };
            }
            return new double[] { cost+m_opCount*m_indCount*COST_LOOKUP*0.5*COST_ENTAILMENT*getOPHierarchyDepth(), m_indCount*m_indCount}; 
        } 
        // ?x(?y ?z)
        if (m_instanceManager!=null && m_instanceManager.arePropertiesInitialised()) {
            double[] estimate=new double[2];
            for (ObjectProperty prop : m_graph.getObjectPropertiesInSignature()) {
                int[] thisEstimate=m_instanceManager.getNumberOfPropertyInstances(AtomicRole.create(prop.getIRIString()));
                estimate[0]+=thisEstimate[0];
                estimate[1]+=thisEstimate[1];
            }
            return new double[] { m_opCount*COST_LOOKUP+estimate[0]*COST_LOOKUP+estimate[1]*COST_ENTAILMENT*(double)(0.5*getOPHierarchyDepth()), estimate[0]+POSSIBLE_INSTANCE_SUCCESS*(double)estimate[1] };
        }
        return new double[] { cost+m_opCount*m_indCount*COST_LOOKUP*(double)(0.5*COST_ENTAILMENT*getOPHierarchyDepth()), m_opCount*m_indCount};
    }
    protected double getClassHierarchyDepth() {
        if (m_classHierarchyDepth==null && m_hermit.isPrecomputed(InferenceType.CLASS_HIERARCHY))
            m_classHierarchyDepth=m_hermit.getClassHierarchyDepth();
        if (m_classHierarchyDepth==null)
            return m_classCount;
        return m_classHierarchyDepth;
    } 
    protected double getOPHierarchyDepth() {
        if (m_opHierarchyDepth==null && m_hermit.isPrecomputed(InferenceType.OBJECT_PROPERTY_HIERARCHY))
            m_opHierarchyDepth=m_hermit.getObjectPropertyHierarchyDepth();
        if (m_opHierarchyDepth==null)
            return m_opCount;
        return m_opHierarchyDepth;
    } 
}