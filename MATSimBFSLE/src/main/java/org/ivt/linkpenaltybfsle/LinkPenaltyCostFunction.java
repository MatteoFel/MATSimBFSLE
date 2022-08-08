/* *********************************************************************** *
 * project: org.matsim.*
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
*
* An example of a link penalty based cost function. The used_links dictionary records the number of times n(l)
* a link l has been used in the choice set. The cost of a link l is then given by 
* <p>
* cost(l) = length(l) + n(l) * length(l) * link_penalty
* <p>
* where the link penalty link_penalty is specified by the analyst.
*/


package org.ivt.linkpenaltybfsle;

import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

public class LinkPenaltyCostFunction implements TravelDisutility, TravelTime {

	Map<String,Double> used_links;
	
	private static final Logger log = Logger.getLogger(LinkPenaltyCostFunction.class);
	
	@Override
	public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
		
		return link.getLength()/link.getFreespeed();
	}

	/**
	 * 
	 * @param used_links    a dictionary mapping each link id to the number of times a certain link is used by other
	 *                      alternatives within the choice set up to that point
	 */
	public void setUsedLinks(Map<String,Double> used_links) {
		this.used_links = used_links;
	}
	
	@Override
	public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {

		Double cost = 0.0;
		
		Double link_penalty = 1.0;
		
		cost += used_links.get(link.getId().toString()) * link.getLength() * link_penalty;
		
		cost += link.getLength();
		
		return cost;
	}


	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		
		Double cost = 0.0;
		
		cost = link.getLength();
		
		return cost;

	}

}
