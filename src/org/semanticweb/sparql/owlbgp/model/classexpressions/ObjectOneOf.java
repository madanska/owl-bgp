/* Copyright 2010 by the Oxford University Computing Laboratory
   
   This file is part of OWL-BGP.

   OWL-BGP is free software: you can redistribute it and/or modify
   it under the terms of the GNU Lesser General Public License as published by
   the Free Software Foundation, either version 3 of the License, or
   (at your option) any later version.
   
   OWL-BGP is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU Lesser General Public License for more details.
   
   You should have received a copy of the GNU Lesser General Public License
   along with OWL-BGP.  If not, see <http://www.gnu.org/licenses/>.
*/
package org.semanticweb.sparql.owlbgp.model.classexpressions;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.sparql.owlbgp.model.AbstractExtendedOWLObject;
import org.semanticweb.sparql.owlbgp.model.Atomic;
import org.semanticweb.sparql.owlbgp.model.ExtendedOWLObject;
import org.semanticweb.sparql.owlbgp.model.ExtendedOWLObjectVisitorEx;
import org.semanticweb.sparql.owlbgp.model.InterningManager;
import org.semanticweb.sparql.owlbgp.model.OWLAPIConverter;
import org.semanticweb.sparql.owlbgp.model.Prefixes;
import org.semanticweb.sparql.owlbgp.model.Variable;
import org.semanticweb.sparql.owlbgp.model.Variable.VarType;
import org.semanticweb.sparql.owlbgp.model.individuals.Individual;

public class ObjectOneOf extends AbstractExtendedOWLObject implements ClassExpression {
    private static final long serialVersionUID = -3492317792968371893L;

    protected static InterningManager<ObjectOneOf> s_interningManager=new InterningManager<ObjectOneOf>() {
        protected boolean equal(ObjectOneOf objectOneOf1,ObjectOneOf objectOneOf2) {
            if (objectOneOf1.m_enumeration.size()!=objectOneOf2.m_enumeration.size())
                return false;
            for (Individual individual : objectOneOf1.m_enumeration) {
                if (!contains(individual, objectOneOf2.m_enumeration))
                    return false;
            } 
            return true;
        }
        protected boolean contains(Individual individual ,Set<Individual> individuals) {
            for (Individual oneOf : individuals)
                if (individual==oneOf)
                    return true;
            return false;
        }
        protected int getHashCode(ObjectOneOf oneOf) {
            int hashCode=0;
            for (Individual individual : oneOf.m_enumeration)
                hashCode+=individual.hashCode();
            return hashCode;
        }
    };
    
    protected final Set<Individual> m_enumeration;
    
    protected ObjectOneOf(Set<Individual> enumeration) {
        m_enumeration=Collections.unmodifiableSet(enumeration);
    }
    public Set<Individual> getIndividuals() {
        return m_enumeration;
    }
    public String toString(Prefixes prefixes) {
        StringBuffer buffer=new StringBuffer();
        buffer.append("ObjectOneOf(");
        boolean notFirst=false;
        for (Individual individual : m_enumeration) {
            if (notFirst)
                buffer.append(' ');
            else 
                notFirst=true;
            buffer.append(individual.toString(prefixes));
        }
        buffer.append(")");
        return buffer.toString();
    }
    protected Object readResolve() {
        return s_interningManager.intern(this);
    }
    public static ObjectOneOf create(Individual... individuals) {
        return create(new HashSet<Individual>(Arrays.asList(individuals)));
    }
    public static ObjectOneOf create(Set<Individual> oneOfs) {
        return s_interningManager.intern(new ObjectOneOf(oneOfs));
    }
    public <O> O accept(ExtendedOWLObjectVisitorEx<O> visitor) {
        return visitor.visit(this);
    }
    protected OWLObject convertToOWLAPIObject(OWLAPIConverter converter) {
        return converter.visit(this);
    }
    public Set<Variable> getVariablesInSignature(VarType varType) {
        Set<Variable> variables=new HashSet<Variable>();
        for (Individual individual : m_enumeration) {
            variables.addAll(individual.getVariablesInSignature(varType));
        }
        return variables;
    }
    public ExtendedOWLObject getBoundVersion(Map<Variable,Atomic> variablesToBindings) {
        Set<Individual> individuals=new HashSet<Individual>();
        for (Individual individual : m_enumeration) {
            individuals.add((Individual)individual.getBoundVersion(variablesToBindings));
        }
        return create(individuals);
    }
}