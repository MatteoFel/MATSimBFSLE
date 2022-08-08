/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
@author      Michael Balmer, Senozon AG
@author      Matteo Felder
*/


package org.ivt.linkpenaltybfslezurich;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.ivt.tools.AnalysisTools;
import org.ivt.tools.Triple;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.collections.Tuple;

public class ChoiceSetWriterLPBFSLEZurich {
	
	//variables
	
	private static final Logger log = Logger.getLogger(ChoiceSetWriterLPBFSLEZurich.class);
	
	//methods
	
	public static final void choiceSetWriter(String outputFileName, Network network, Map<String,Triple<Node,Node,Path>> ods_chosenRoutes, int choice_set_size, double variationFactor, long timeout) throws IOException {
		FileWriter fw = new FileWriter(outputFileName);
		BufferedWriter out = new BufferedWriter(fw);
		
		BrouterCostFunctionZurich cost_function = new BrouterCostFunctionZurich();
		
		
		Map<String,Double> used_links = new HashMap<>();
		
		for (Link l : network.getLinks().values()) {
			used_links.put(l.getId().toString(), 0.0);
		}
		
		cost_function.setUsedLinks(used_links);
		
		LPBFSLEZurich bfsle = new LPBFSLEZurich(network);
					

		bfsle.setChoiceSetSize(choice_set_size);
		bfsle.setVariationFactor(variationFactor);
		bfsle.setTimeout(timeout);
		
		
		// csv file header
		
		out.write("od_pair_id" + ",");
		out.write("internal_id" + ",");
		out.write("link_nodetonode_id" + ",");
		out.append('\n');
		
		int skip = 0;

		for (Entry<String, Triple<Node, Node, Path>> entry : ods_chosenRoutes.entrySet()) {

			long startTime = System.currentTimeMillis();
			String id = entry.getKey();
			
			skip ++;
			
			// if you only want to analyse a subset of your sample, this can be imposed here.
			if (skip < 0) {
				continue;
			}
			
			if (skip > 5000) {
				break;
			}
				
			
			Triple<Node, Node, Path> od_cr = entry.getValue();
			// generate paths

			log.debug("----------------------------------------------------------------------");
			log.debug("generating path sets for segment id="+id+", O="+od_cr.getFirst().getId()+" and D="+od_cr.getSecond().getId()+"...");
			if (bfsle.setODPair(od_cr.getFirst(),od_cr.getSecond()))
				try {
					Tuple<Path,List<Path>> paths = bfsle.generateChoiceSet(od_cr.getFirst(), od_cr.getSecond(), choice_set_size);
					log.debug("done.");
					long endTime = System.currentTimeMillis();
					long calcTime = endTime - startTime;
					
					
					// the chosen route
					
					Path chosen_route = od_cr.getThird();
					
					// the least cost path
					
					Path least_cost_path = paths.getFirst();
					
					// list of other alternatives
					
					List<Path> alternatives = paths.getSecond();
					
					//initialise analysis tools
					
					AnalysisTools analysis_tools = new AnalysisTools(chosen_route, paths);
					
					// set up internal id's of choice set
					
					Map <Integer, Path> internal_ids = new HashMap<>();
					
					internal_ids.put(0, chosen_route);
					internal_ids.put(1, least_cost_path);
					
					int j = 2;
					for (Path alternative : alternatives) {
						internal_ids.put(j, alternative);
						j ++;
					}
					
					///// OVERLAP //////
					
					
					Map <Integer, Double> overlap = new HashMap<>();
					
					for (Map.Entry<Integer, Path> choice_set_entry : internal_ids.entrySet()) {
						int internal_id = choice_set_entry.getKey();
						Path path = choice_set_entry.getValue();
						
						// overlap 
						Double link_for_link_overlap_with_cr = 0.0;
						for (Link l : path.links) {
							if (chosen_route.links.contains(l)) {
								link_for_link_overlap_with_cr += l.getLength(); 
							}
						}
						Double overlap_percentage = link_for_link_overlap_with_cr / analysis_tools.getPathLength(chosen_route);
						overlap.put(internal_id, overlap_percentage);
					}
					
					// maximal overlap
					
					Double max_overlap = 0.0;
					int id_to_remove = -100;
					
					for (Map.Entry<Integer, Double> overlap_entry : overlap.entrySet()) {
						int internal_id = overlap_entry.getKey();
						Double overlap_percentage = overlap_entry.getValue();
						
						if (internal_id == 0) {
							continue;
						}
						else {
							if (overlap_percentage > max_overlap) {
								max_overlap = Math.max(overlap_percentage, max_overlap);
								id_to_remove = internal_id;
							}
						}
					}
					
					// we remove the trip which has the greatest overlap with the chosen route if the coincide 
					// at least 70% - otherwise, the last route is removed.
					
					if (id_to_remove != -100 && max_overlap >= 0.7) {
						internal_ids.remove(id_to_remove);
					}
					else {
						id_to_remove = choice_set_size;
						internal_ids.remove(choice_set_size);
					}
					
					log.warn("removed alternative id is " + id_to_remove);
					log.warn("max overlap is " + max_overlap);
					
					
					
					
					for (Map.Entry<Integer, Path> choice_set_entry : internal_ids.entrySet()) {
						int internal_id = choice_set_entry.getKey();
						Path path = choice_set_entry.getValue();
						
						out.write(id + ",");
						out.write(internal_id + ",");
						
						//write out the route links are represent by node-to-node ids
						for (Link l : path.links) {
							out.write(String.valueOf(l.getId()) + ",");
							}
					
						out.append('\n');
						
					}
					
				} catch (NumberFormatException | IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			else {
				log.warn("triple id="+id+", O="+od_cr.getFirst().getId()+" and D="+od_cr.getSecond().getId()+" is omitted.");
			}
			Gbl.printMemoryUsage();
			log.debug("----------------------------------------------------------------------");
		}
		out.close();
		fw.close();
		}

}





