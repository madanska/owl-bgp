package org.semanticweb.sparql.bgpevaluation;

import java.util.List;

import org.semanticweb.sparql.bgpevaluation.monitor.Monitor;
import org.semanticweb.sparql.bgpevaluation.queryobjects.QueryObject;
import org.semanticweb.sparql.owlbgp.model.axioms.Axiom;

public class QueryReordering {
    public static QueryObject<? extends Axiom> getCheapest(CostEstimationVisitor estimator, List<QueryObject<? extends Axiom>> atoms, Monitor monitor) {
        QueryObject<? extends Axiom> cheapest=null;
        double cheapestCost=0;
        boolean first=true;
        for (QueryObject<? extends Axiom> qo : atoms) {
            monitor.costEvaluationStarted(qo);
            double[] costs=qo.accept(estimator);
            monitor.costEvaluationFinished(costs[0], costs[1]);
            double totalCost=costs[0]+costs[1];
            if (first || totalCost<cheapestCost) {
                first=false;
                cheapestCost=totalCost;
                cheapest=qo;
            }
        } 
        return cheapest;
    }
}