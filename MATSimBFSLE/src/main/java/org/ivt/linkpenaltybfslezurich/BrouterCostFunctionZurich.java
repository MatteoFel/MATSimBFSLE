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
* A link cost function inspired by the open source cycling router Brouter (http://brouter.de)
*/

package org.ivt.linkpenaltybfslezurich;

import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

public class BrouterCostFunctionZurich implements TravelDisutility, TravelTime {

	Map<String,Double> used_links;
	
	private static final Logger log = Logger.getLogger(BrouterCostFunctionZurich.class);
	
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
		
		cost += Math.random() * 0.0;
		
		Double elevation_cost = 0.0;
		
		Double uphillcost = 80.0;
		
		Double downhillcost = 0.0;
		
		Double uphillcutoff = 1.5;
		
		Double downhillcutoff = 1.5;
		
		Double average_slope_after_cut_off_up = Math.max(0.0, (double) link.getAttributes().getAttribute("grade_abs") * 100 - uphillcutoff);
		
		Double average_slope_after_cut_off_down = Math.max(0.0, (double) link.getAttributes().getAttribute("grade_abs") * 100 - downhillcutoff);
		
		if ((double) link.getAttributes().getAttribute("grade") >= 0.0 ) {
			elevation_cost += link.getLength() * uphillcost * average_slope_after_cut_off_up / 100;
		}
		else {
			elevation_cost += link.getLength() * downhillcost * average_slope_after_cut_off_down / 100;
		}
		
		
		cost += elevation_cost;
		
		int isbike = 0;
		
		if (link.getAttributes().getAttribute("bicycle").equals("yes") ||
				link.getAttributes().getAttribute("bicycle").equals("official") ||
				link.getAttributes().getAttribute("bicycle").equals("permissive") ||
				link.getAttributes().getAttribute("bicycle").equals("designated") ||
				link.getAttributes().getAttribute("velomaster").equals("1.0") || 
				link.getAttributes().getAttribute("veloweg").equals("1.0") || 
				link.getAttributes().getAttribute("velostreifen").equals("1.0") ||
				link.getAttributes().getAttribute("cycleway").equals("|lane|") ||
				link.getAttributes().getAttribute("cycleway").equals("track") ||
				link.getAttributes().getAttribute("cycleway").equals("opposite_lane") ||
				link.getAttributes().getAttribute("cycleway").equals("opposite")) {
			isbike += 1;
		}
		
		
		int ispaved = 0;
		
		if (link.getAttributes().getAttribute("surface").equals("asphalt") ||
				link.getAttributes().getAttribute("surface").equals("paved") ||
				link.getAttributes().getAttribute("surface").equals("concrete") ||
				link.getAttributes().getAttribute("surface").equals("paving_stones") ||
				link.getAttributes().getAttribute("surface").equals("sett") || 
				link.getAttributes().getAttribute("surface").equals("concrete:plates") ||
				link.getAttributes().getAttribute("surface").equals("laminate") ||
				link.getAttributes().getAttribute("surface").equals("compacted")||
				link.getAttributes().getAttribute("surface").equals("fine_gravel")||
				link.getAttributes().getAttribute("surface").equals("gravel")||
				link.getAttributes().getAttribute("surface").equals("nan")) {
			ispaved += 1;
		}
		
		
		int badoneway = 0;
		
		if (link.getAttributes().getAttribute("reverse_direction").equals("yes")) {
			badoneway += 1;
		}
		
		
		Double onewaypenalty = 0.0;
		
		if (badoneway == 1) {
			if (link.getAttributes().getAttribute("cycleway").equals("opposite") || link.getAttributes().getAttribute("cycleway").equals("opposite_lane") ) {
				onewaypenalty += 0.0;
			}
			else if (link.getAttributes().getAttribute("highway").equals("primary") || link.getAttributes().getAttribute("highway").equals("primary_link")) {
				onewaypenalty += 50.0;
			}
			else if (link.getAttributes().getAttribute("highway").equals("secondary") || link.getAttributes().getAttribute("highway").equals("secondary_link")) {
				onewaypenalty += 30.0;
			}
			else if (link.getAttributes().getAttribute("highway").equals("tertiary") || link.getAttributes().getAttribute("highway").equals("tertiary_link")) {
				onewaypenalty += 20.0;
			}
			else {
				onewaypenalty += 6.0;
			}
		}
		
		//cost += onewaypenalty;
		
		
		Double stepspenalty = 0.0;
		
		if (link.getAttributes().getAttribute("highway").equals("steps")) {
			stepspenalty += 40.0;
		}

		//cost += stepspenalty;
		
		
		Double costfactor = 0.0;
		
		costfactor += onewaypenalty;
		costfactor += stepspenalty;
		
		if (link.getAttributes().getAttribute("highway").equals("pedestrian") ||
				link.getAttributes().getAttribute("highway").equals("track") ||
				link.getAttributes().getAttribute("highway").equals("road") ||
				link.getAttributes().getAttribute("highway").equals("path") ||
				(link.getAttributes().getAttribute("highway").equals("footway"))) {
			if (ispaved == 1 && isbike == 1) {
				costfactor += 1.0;
			}
			else {
				costfactor += 3.0;
			}
		}
		else if (link.getAttributes().getAttribute("highway").equals("cycleway")) {
			costfactor += 1.0;
		}
		else if (link.getAttributes().getAttribute("highway").equals("residential") || link.getAttributes().getAttribute("highway").equals("living_street")) {
			if (isbike == 1) {
				costfactor += 1.0;
			}
			else if (isbike == 0 && ispaved == 1) {
				costfactor += 1.1;// 1.1
			}
			else {
				costfactor += 1.4;//1.4
			}
		}
		else if (link.getAttributes().getAttribute("highway").equals("service")) {
			if (isbike == 1) {
				costfactor += 1.0;
			}
			else if (isbike == 0 && ispaved == 1) {
				costfactor += 1.1;//1.2
			}
			else {
				costfactor += 1.6;//1.6;
			}
		}
		
		else if (link.getAttributes().getAttribute("highway").equals("primary") || link.getAttributes().getAttribute("highway").equals("primary_link")) {
			if (isbike == 1) {
				costfactor += 1.2;
			}
			else {
				costfactor += 3.0;
			}
		}
		else if (link.getAttributes().getAttribute("highway").equals("secondary") || link.getAttributes().getAttribute("highway").equals("secondary_link")) {
			if (isbike == 1) {
				costfactor += 1.1;
			}
			else {
				costfactor += 1.6;
			}
		}
		else if (link.getAttributes().getAttribute("highway").equals("tertiary") || link.getAttributes().getAttribute("highway").equals("tertiary_link")) {
			if (isbike == 1) {
				costfactor += 1.0;
			}
			else {
				costfactor += 1.3;//1.4
			}
		}
		else if (link.getAttributes().getAttribute("highway").equals("unclassified")) {
			if (isbike == 1) {
				costfactor += 1.0;
			}
			else {
				costfactor += 1.3;
			}
		}
		else if (link.getAttributes().getAttribute("highway").equals("motorway")) {
			costfactor += 1000.0;
		}
		else {
			costfactor += 2.0;
		}
		
		
		
		
		//optional (avoid unsafe roads)
		//if (isbike == 0) {
		//	cost += 2;
		//}
		//else {
		//	cost += 0;
		//}
		
		cost += costfactor * link.getLength();
		
		return cost;
	}
	
	

	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		
		Double cost = 0.0;
		
		cost = link.getLength();
		
		
		return cost;
//		
//		cost += used_links.get(link.getId().toString())*link.getLength();
//		
//		
//		Double elevation_cost = 0.0;
//		
//		Double uphillcost = 80.0;
//		
//		Double downhillcost = 80.0;
//		
//		Double uphillcutoff = 1.5;
//		
//		Double downhillcutoff = 1.5;
//		
//		Double average_slope_after_cut_off_up = Math.max(0.0, (double) link.getAttributes().getAttribute("grade_abs") * 100 - uphillcutoff);
//		
//		Double average_slope_after_cut_off_down = Math.max(0.0, (double) link.getAttributes().getAttribute("grade_abs") * 100 - downhillcutoff);
//		
//		if ((double) link.getAttributes().getAttribute("grade") >= 0.0 ) {
//			elevation_cost += link.getLength() * uphillcost * average_slope_after_cut_off_up / 100;
//		}
//		else {
//			elevation_cost += link.getLength() * downhillcost * average_slope_after_cut_off_down / 100;
//		}
//		
//		
//		cost += elevation_cost;
//		
//		int isbike = 0;
//		
//		if (link.getAttributes().getAttribute("bicycle").equals("yes") ||
//				link.getAttributes().getAttribute("bicycle").equals("permissive") ||
//				link.getAttributes().getAttribute("bicycle").equals("designated") ||
//				link.getAttributes().getAttribute("velomaster").equals("1.0") || 
//				link.getAttributes().getAttribute("veloweg").equals("1.0") || 
//				link.getAttributes().getAttribute("velostreifen").equals("1.0") ||
//				link.getAttributes().getAttribute("cycleway").equals("|lane|") ||
//				link.getAttributes().getAttribute("cycleway").equals("track") ||
//				link.getAttributes().getAttribute("cycleway").equals("opposite_lane")) {
//			isbike += 1;
//		}
//		
//		
//		int ispaved = 0;
//		
//		if (link.getAttributes().getAttribute("surface").equals("asphalt") ||
//				link.getAttributes().getAttribute("surface").equals("paved") ||
//				link.getAttributes().getAttribute("surface").equals("concrete") ||
//				link.getAttributes().getAttribute("surface").equals("paving_stones") ||
//				link.getAttributes().getAttribute("surface").equals("sett") || 
//				link.getAttributes().getAttribute("surface").equals("concrete:plates") ||
//				link.getAttributes().getAttribute("surface").equals("laminate")) {
//			ispaved += 1;
//		}
//		
//		
//		
//		
//		int badoneway = 0;
//		
//		if (link.getAttributes().getAttribute("reverse_direction").equals("yes")) {
//			badoneway += 1;
//		}
//		
//		
//		Double onewaypenalty = 0.0;
//		
//		if (badoneway == 1) {
//			if (link.getAttributes().getAttribute("cycleway").equals("opposite") || link.getAttributes().getAttribute("cycleway").equals("opposite_lane") ) {
//				onewaypenalty += 0.0;
//			}
//			else if (link.getAttributes().getAttribute("highway").equals("primary") || link.getAttributes().getAttribute("highway").equals("primary_link")) {
//				onewaypenalty += 50.0;
//			}
//			else if (link.getAttributes().getAttribute("highway").equals("secondary") || link.getAttributes().getAttribute("highway").equals("secondary_link")) {
//				onewaypenalty += 30.0;
//			}
//			else if (link.getAttributes().getAttribute("highway").equals("tertiary") || link.getAttributes().getAttribute("highway").equals("tertiary_link")) {
//				onewaypenalty += 20.0;
//			}
//			else {
//				onewaypenalty += 6.0;
//			}
//		}
//		
//		cost += onewaypenalty;
//		
//		
//		Double stepspenalty = 0.0;
//		
//		if (link.getAttributes().getAttribute("highway").equals("steps")) {
//			stepspenalty += 40;
//		}
//
//		cost += stepspenalty;
//		
//		
//		Double costfactor = 0.0;
//		
//		if (link.getAttributes().getAttribute("highway").equals("pedestrian")) {
//			costfactor += 3.0;
//		}	
//		else if (link.getAttributes().getAttribute("highway").equals("track") ||
//				link.getAttributes().getAttribute("highway").equals("road") ||
//				(link.getAttributes().getAttribute("highway").equals("footway"))) {
//			if (ispaved == 1 && isbike == 1) {
//				costfactor += 1.0;
//			}
//			else {
//				costfactor += 3.0;
//			}
//		}
//		else if (link.getAttributes().getAttribute("highway").equals("cycleway")) {
//			costfactor += 1.0;
//		}
//		else if (link.getAttributes().getAttribute("highway").equals("residential") || link.getAttributes().getAttribute("highway").equals("living_street")) {
//			if (ispaved == 1) {
//				costfactor += 1.1;
//			}
//			else {
//				costfactor += 1.5;
//			}
//		}
//		else if (link.getAttributes().getAttribute("highway").equals("service")) {
//			if (ispaved == 1) {
//				costfactor += 1.3;
//			}
//			else {
//				costfactor += 1.6;
//			}
//		}
//		
//		else if (link.getAttributes().getAttribute("highway").equals("primary") || link.getAttributes().getAttribute("highway").equals("primary_link")) {
//			if (isbike == 1) {
//				costfactor += 1.2;
//			}
//			else {
//				costfactor += 3.0;
//			}
//		}
//		else if (link.getAttributes().getAttribute("highway").equals("secondary") || link.getAttributes().getAttribute("highway").equals("secondary_link")) {
//			if (isbike == 1) {
//				costfactor += 1.1;
//			}
//			else {
//				costfactor += 1.6;
//			}
//		}
//		else if (link.getAttributes().getAttribute("highway").equals("tertiary") || link.getAttributes().getAttribute("highway").equals("tertiary_link")) {
//			if (isbike == 1) {
//				costfactor += 1.0;
//			}
//			else {
//				costfactor += 1.4;
//			}
//		}
//		else if (link.getAttributes().getAttribute("highway").equals("unclassified")) {
//			if (isbike == 1) {
//				costfactor += 1.0;
//			}
//			else {
//				costfactor += 1.3;
//			}
//		}
//		else {
//			costfactor += 2.0;
//		}
//		
//		
//		
//		
//		//optional (avoid unsafe roads)
//		if (isbike == 0) {
//			cost += 2;
//		}
//		else {
//			cost += 0;
//		}
//		
//		
//		cost += costfactor * link.getLength();
//
//		
//		
//		return cost;
	}

}
