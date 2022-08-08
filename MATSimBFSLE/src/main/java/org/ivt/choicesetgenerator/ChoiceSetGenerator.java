/* *********************************************************************** *
 * project: org.matsim.*
 * BFSLE.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

/**
@author      Matteo Felder 
*/


package org.ivt.choicesetgenerator;

import java.util.List;

import org.matsim.api.core.v01.network.Node;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.collections.Tuple;

public interface ChoiceSetGenerator {
	
	/**
	 * 
	 * @param origin
	 *        The node at which all routes should start.
	 * @param destination
	 *        The node at which all routes should end.
	 * @param choiceSetSize
	 *        The number of alternatives that are to be generated.
	 * @return
	 *        The output is a tuple consisting of the least cost path, and a list containing all other alternatives. Maybe this needs to be done differently.
	 */
	Tuple<Path, List<Path>> generateChoiceSet(Node origin, Node destination, int choiceSetSize);
	
}