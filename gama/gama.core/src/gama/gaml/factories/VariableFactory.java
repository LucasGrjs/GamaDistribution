/*******************************************************************************************************
 *
 * VariableFactory.java, in gama.core, is part of the source code of the GAMA modeling and simulation platform
 * .
 *
 * (c) 2007-2024 UMI 209 UMMISCO IRD/SU & Partners (IRIT, MIAT, TLU, CTU)
 *
 * Visit https://github.com/gama-platform/gama for license information and contacts.
 *
 ********************************************************************************************************/
package gama.gaml.factories;

import static gama.core.common.interfaces.IKeyword.ON_CHANGE;
import static gama.core.common.interfaces.IKeyword.PARAMETER;
import static gama.core.common.interfaces.IKeyword.VAR;

import java.util.Map;

import org.eclipse.emf.ecore.EObject;

import gama.gaml.descriptions.ExperimentDescription;
import gama.gaml.descriptions.FacetProto;
import gama.gaml.descriptions.IDescription;
import gama.gaml.descriptions.IExpressionDescription;
import gama.gaml.descriptions.SymbolProto;
import gama.gaml.descriptions.VariableDescription;
import gama.gaml.statements.Facets;

/**
 * Written by drogoul Modified on 26 nov. 2008
 *
 * @todo Description
 */
// @factory (
// handles = { ISymbolKind.Variable.CONTAINER, ISymbolKind.Variable.NUMBER, ISymbolKind.Variable.REGULAR,
// ISymbolKind.PARAMETER })
public class VariableFactory extends SymbolFactory {

	@Override
	protected IDescription buildDescription(final String keyword, final Facets facets, final EObject element,
			final Iterable<IDescription> children, final IDescription enclosing, final SymbolProto proto) {
		if (PARAMETER.equals(keyword)) {

			final Map<String, FacetProto> possibleFacets = proto.getPossibleFacets();
			// We copy the relevant facets from the targeted var of the
			// parameter
			VariableDescription targetedVar = enclosing.getModelDescription().getAttribute(facets.getLabel(VAR));
			if (targetedVar == null && enclosing instanceof ExperimentDescription) {
				targetedVar = ((ExperimentDescription) enclosing).getAttribute(facets.getLabel(VAR));
			}
			if (targetedVar != null) {
				for (final String key : possibleFacets.keySet()) {
					if (ON_CHANGE.equals(key)) { continue; }
					final IExpressionDescription expr = targetedVar.getFacet(key);
					if (expr != null) {
						IExpressionDescription copy = expr.cleanCopy();
						facets.putIfAbsent(key, copy);
					}
				}

			}
		}
		return new VariableDescription(keyword, enclosing, element, facets);
	}

}
