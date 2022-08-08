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
@author      Matteo Felder
*/



package org.ivt.linkpenaltybfsle;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.ivt.tools.AnalysisTools;
import org.ivt.tools.Triple;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.collections.Tuple;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;



public class AnalysisLPBFSLE {
	
	//variables
	
	private static final Logger log = Logger.getLogger(AnalysisLPBFSLE.class);
	
	//methods
	/**
	 * generates alternatives using LPBFSLE algorithm and writes the specified attributes to a csv file
	 * in a biogeme compatible format.
	 * 
	 * @param outputFileName               csv file to write analysis
	 * @param network                      MATSim network
	 * @param ods_chosenRoutes             triple consisting of origin, destination, chosen route
	 * @param choice_set_size              number of routes to be generated
	 * @param variationFactor              variation factor
	 * @param timeout                      max computation time
	 * @throws IOException
	 * @throws NumberFormatException
	 */
	
	
	public static final void analysis(String outputFileName, Network network, Map<String,Triple<Node,Node,Path>> ods_chosenRoutes, int choice_set_size, double variationFactor, long timeout) throws IOException, NumberFormatException {
		FileWriter fw = new FileWriter(outputFileName);
		BufferedWriter out = new BufferedWriter(fw);
		
		LinkPenaltyCostFunction cost_function = new LinkPenaltyCostFunction();
		
		Map<String,Double> used_links = new HashMap<>();
		
		for (Link l : network.getLinks().values()) {
			used_links.put(l.getId().toString(), 0.0);
		}
		
		cost_function.setUsedLinks(used_links);
		
		LPBFSLE bfsle = new LPBFSLE(network);
					
		bfsle.setChoiceSetSize(choice_set_size);
		bfsle.setVariationFactor(variationFactor);
		bfsle.setTimeout(timeout);
		
		// header of csv file
		
		out.write("OD_pair_id" + ",");
		out.write("computation_time" + ",");
		out.write("maximal_overlap" + ",");
		out.write("excluded_alternative" + ",");
		out.write("maximal_buffer_overlap" + ",");
		out.write("max_buffer_alternative" + ",");
		
		
		for (int i = 1; i <= choice_set_size; i++){
			out.write("internal_id_" + String.valueOf(i) +  ",");
			out.write("overlap_" + String.valueOf(i) + ",");
			out.write("buffer_overlap_" + String.valueOf(i) + ",");
			out.write("length_" + String.valueOf(i) +  ",");
			out.write("number_of_links_" + String.valueOf(i) + ",");
			out.write("path_size_" + String.valueOf(i) + ",");
			}
		//for biogeme path size logit we need to specify which route is the chosen one
		out.write("CHOICE" + "\n");
		
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
						
						
						// od pair id
						
						out.write(id + ",");
						
						// computation time
						
						out.write(calcTime + ",");
						
						// the chosen route
						
						Path chosen_route = od_cr.getThird();
						
						// the least cost path
						
						Path least_cost_path = paths.getFirst();
						
						// list of other alternatives
						
						List<Path> alternatives = paths.getSecond();
						
						Map <Integer, Path> internal_ids = new HashMap<>();
						
						internal_ids.put(0, chosen_route);
						internal_ids.put(1, least_cost_path);
						
						int j = 2;
						for (Path alternative : alternatives) {
							internal_ids.put(j, alternative);
							j ++;
						}
						
						//initialise analysis tools
						
						AnalysisTools analysis_tools = new AnalysisTools(chosen_route, paths);
						
						
						
						////////
						// CHOSEN ROUTE BUFFER
						////////



						ArrayList<Coordinate> coords_cr = new ArrayList<>();

						for (Node node : chosen_route.nodes) {
							coords_cr.add(new Coordinate(node.getCoord().getX(), node.getCoord().getY(), 0.0));
						}

						Coordinate[] coordinates_cr = coords_cr.toArray(new Coordinate[coords_cr.size()]);
						Geometry lineString_cr = new GeometryFactory().createLineString(coordinates_cr);
						Double length_test_cr = lineString_cr.getLength();

						Geometry BufferChosenRoute = ((Geometry) lineString_cr).buffer(20.0);
						
						

						
						///// OVERLAP //////
						
						
						Map <Integer, Double> overlap = new HashMap<>();
						Map <Integer, Double> buffer_overlap = new HashMap<>();
						
						
						
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
							
							// buffer overlap
							ArrayList<Coordinate> coords_path = new ArrayList<>();

							for (Node node : path.nodes) {
								coords_path.add(new Coordinate(node.getCoord().getX(), node.getCoord().getY(), 0.0));
							}

							Coordinate[] coordinates_path = coords_path.toArray(new Coordinate[coords_path.size()]);
							Geometry lineString_path = new GeometryFactory().createLineString(coordinates_path);

							Geometry intersection_cr_path = BufferChosenRoute.intersection((Geometry) lineString_path);

							Double buffer_overlap_length = intersection_cr_path.getLength();
							Double buffer_overlap_percentage = buffer_overlap_length / length_test_cr;
							//Double buffer_overlap_percentage = buffer_overlap_length / analysis_tools.getPathLength(chosen_route);
							
							buffer_overlap.put(internal_id, buffer_overlap_percentage);
							
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
						
						Double max_buffer_overlap = 0.0;
						int id_max_buffer = -100;
						
						for (Map.Entry<Integer, Double> buffer_overlap_entry : buffer_overlap.entrySet()) {
							int internal_id = buffer_overlap_entry.getKey();
							Double buffer_overlap_percentage = buffer_overlap_entry.getValue();
							
							if (internal_id == 0) {
								continue;
							}
							else {
								if (buffer_overlap_percentage > max_buffer_overlap) {
									max_buffer_overlap = Math.max(buffer_overlap_percentage, max_buffer_overlap);
									id_max_buffer = internal_id;
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

						out.write(max_overlap + ",");
						out.write(id_to_remove + ",");
						out.write(max_buffer_overlap + ",");
						out.write(id_max_buffer + ",");
						
						
///////////////////////// START ANALYSIS ///////////////////////////////
						
						for (Map.Entry<Integer, Path> choice_set_entry : internal_ids.entrySet()) {
							int internal_id = choice_set_entry.getKey();
							Path path = choice_set_entry.getValue();
							
							
							
							out.write(internal_id + ",");
							
							out.write(overlap.get(internal_id) + ",");
							
							out.write(buffer_overlap.get(internal_id) + ",");
							
							// length
							
							Double length = analysis_tools.getPathLength(path);
							
							out.write(length + ",");
							
							// number of links
							
							int number_of_links = path.links.size();
							
							out.write(number_of_links + ",");
							
							// PATH SIZE
							
							// path sizes 
							
							Double path_size = analysis_tools.getPathSize(path, chosen_route, paths);
							
							out.write(path_size + ",");
							
						}
				out.write("1" + "\n");
				
					
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
		//out.append('\n');
		out.close();
		fw.close();
		
	}

}


