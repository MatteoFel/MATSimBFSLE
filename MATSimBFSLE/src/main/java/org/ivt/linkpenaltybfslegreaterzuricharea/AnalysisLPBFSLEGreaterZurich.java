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


package org.ivt.linkpenaltybfslegreaterzuricharea;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.ivt.tools.AnalysisTools;
import org.ivt.tools.ParseInputFile;
import org.ivt.tools.Triple;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;


public class AnalysisLPBFSLEGreaterZurich {
	
	//variables
	
	private static final Logger log = Logger.getLogger(AnalysisLPBFSLEGreaterZurich.class);
	
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
	
	public static final void analysis(String outputFileName, Network network, Map<String, Triple<Node,Node,Path>> ods_chosenRoutes, int choice_set_size, double variationFactor, long timeout) throws IOException, NumberFormatException {
		FileWriter fw = new FileWriter(outputFileName);
		BufferedWriter out = new BufferedWriter(fw);
		
		BrouterCostFunctionGreaterZurich cost_function = new BrouterCostFunctionGreaterZurich();
		
		Map<String,Double> used_links = new HashMap<>();
		
		for (Link l : network.getLinks().values()) {
			used_links.put(l.getId().toString(), 0.0);
		}
		
		cost_function.setUsedLinks(used_links);
		
		LPBFSLEGreaterZurich bfsle = new LPBFSLEGreaterZurich(network);
					

		bfsle.setChoiceSetSize(choice_set_size);
		bfsle.setVariationFactor(variationFactor);
		bfsle.setTimeout(timeout);
		
		
		
		String[] osm_highway_tags = {"primary", "secondary", "tertiary", "footway", "pedestrian",
                "path", "track", "cycleway", "unclassified", "residential", 
                "living_street", "service", "motorway", "motorway_link",
                "primary_link", "secondary_link", "tertiary_link"};
		
		String[] osm_surface_tags = {"asphalt", "dirt", "ground", "gravel", "paved",
				"woodchips", "concrete", "sett", "grass", "paving_stones", "cobblestone", 
				"compacted", "fine_gravel", "unpaved", "pebblestone", "grass_paver",
				"plastic_grate", "concrete:plates", "wood", "metal", "mud", "earth",
				"artificial_turf", "stone", "plastic", "laminate","nan"};
		
		String[] number_of_lanes = {"0_lanes", "1_lanes", "2_lanes", "3_lanes", "4_lanes"}; 
		
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
			out.write("ln_length_" + String.valueOf(i) +  ",");
			out.write("number_of_links_" + String.valueOf(i) + ",");
			out.write("travel_disutility_" + String.valueOf(i) + ",");
			out.write("path_size_" + String.valueOf(i) + ",");
			out.write("ln_path_size_" + String.valueOf(i) + ",");
			out.write("bierlaire_ben_akiva_path_size_" + String.valueOf(i) + ",");
			out.write("ln_bierlaire_ben_akiva_path_size_" + String.valueOf(i) + ",");
			out.write("bovy_path_size_correction_" + String.valueOf(i) + ",");
			out.write("commonality_factor_" + String.valueOf(i) + ",");
			for (String tag : osm_highway_tags) {
				out.write(tag + "_" + String.valueOf(i) + ",");
			}
			//for (String tag : osm_surface_tags) {
			//	out.write(tag + "_" + String.valueOf(i) + ",");
			//}
			for (String lanes : number_of_lanes) {
				out.write(lanes + "_" + String.valueOf(i) + ",");
			}
			out.write("max_slope" + "_" + String.valueOf(i) + ",");
			out.write("avg_slope" + "_" + String.valueOf(i) + ",");
			out.write("avg_link_slope" + "_" + String.valueOf(i) + ",");
			out.write("total_elevation_gain" + "_"+ String.valueOf(i) + ",");
			out.write("uphill_distance" + "_" + String.valueOf(i) + ",");
			out.write("elevation_gain_total_length" + "_" + String.valueOf(i) + ",");
			out.write("elevation_gain_uphill_length" + "_" + String.valueOf(i) + ",");
			out.write("grade_1_2" + "_" + String.valueOf(i) + ",");
			out.write("grade_2_3" + "_" + String.valueOf(i) + ",");
			out.write("grade_3_4" + "_" + String.valueOf(i) + ",");
			out.write("grade_4_5" + "_" + String.valueOf(i) + ",");
			out.write("grade_5_6" + "_" + String.valueOf(i) + ",");
			out.write("grade_6_7" + "_" + String.valueOf(i) + ",");
			out.write("grade_7_8" + "_" + String.valueOf(i) + ",");
			out.write("grade_8_9" + "_" + String.valueOf(i) + ",");
			out.write("grade_9_10" + "_" + String.valueOf(i) + ",");
			out.write("grade_10_infty" + "_" + String.valueOf(i) + ",");
			out.write("max_maxspeed" + "_" + String.valueOf(i) + ",");
			out.write("distance_lower_30" + "_" + String.valueOf(i) + ",");
			out.write("distance_30" + "_" + String.valueOf(i) + ",");
			out.write("max_ldv_count" + "_" + String.valueOf(i) + ",");
			out.write("avg_ldv_count" + "_" + String.valueOf(i) + ",");
			out.write("ldv_count_0_2500" + "_" + String.valueOf(i) + ",");
			out.write("ldv_count_2500_10000" + "_" + String.valueOf(i) + ",");
			out.write("ldv_count_100000_infty" + "_" + String.valueOf(i) + ",");
			//out.write("max_hdv_count" + "_" + String.valueOf(i) + ",");
			//out.write("os_parking_pd" + "_" + String.valueOf(i) + ",");
			//out.write("trees_pd" + "_" + String.valueOf(i) + ",");
			out.write("veloweg" + "_" + String.valueOf(i) + ",");
			out.write("velostreifen" + "_" + String.valueOf(i) + ",");
			//out.write("park" + "_" + String.valueOf(i) + ",");
			//out.write("forest" + "_" + String.valueOf(i) + ",");
			//out.write("water" + "_" + String.valueOf(i) + ",");
			//out.write("shops_pd" + "_" + String.valueOf(i) + ",");
			//out.write("max_shops_pd" + "_" + String.valueOf(i) + ",");
			//node attributes
			//out.write("distinct_ts_ids" + "_" + String.valueOf(i) + ",");
			out.write("ts_osm" + "_" + String.valueOf(i) + ",");
			out.write("ts_osm_2" + "_" + String.valueOf(i) + ",");
			//out.write("ts_bike" + "_" + String.valueOf(i) + ",");
			//out.write("max_ts_complexity" + "_" + String.valueOf(i) + ",");
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
			
			if (skip > 50000) {
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
						
						// set up internal ids of choice set
						
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
							
							out.write(Math.log(length) + ",");
							
							
							// number of links
							
							int number_of_links = path.links.size();
							
							out.write(number_of_links + ",");
							
							// travel disutility
							
							Double travel_disutility = 0.0;
							for (Link l : path.links) {
								travel_disutility += cost_function.getLinkTravelDisutility(l,endTime, null,null);
							}
							out.write(travel_disutility + ",");
							
							// PATH SIZE
							
							// path sizes 
							
							Double path_size = analysis_tools.getPathSize(path, chosen_route, paths);
							
							out.write(path_size + ",");
							
							out.write(Math.log(path_size) + ",");
							
							Double path_size_bierlaire_ben_akiva = analysis_tools.getBierlaireBenAkivaPathSize(path, chosen_route, paths);
							
							out.write(path_size_bierlaire_ben_akiva + ",");
							
							out.write(Math.log(path_size_bierlaire_ben_akiva) + ",");
							
							Double path_size_correction = analysis_tools.getPathSizeCorrection(path, chosen_route, paths);
							
							out.write(path_size_correction + ",");
							
							// COMMONALITY FACTOR
							
							// commonality factor
							
							Double commonality_factor = analysis_tools.getCommonalityFactor(path, chosen_route, paths);
							
							out.write(commonality_factor + ",");
							
							// OSM tags
							
							// for each highway type, return the percentage of the route consisting of that highway type
							
							Map<String, Double> highway_share = new HashMap<String, Double>();
							for (String tag : osm_highway_tags) {
								highway_share.put(tag, 0.0);
							}
							
							for (String tag : osm_highway_tags) {
								for (Link l: path.links) {
									if (l.getAttributes().getAttribute("highway").equals(tag)) {
										highway_share.put(tag, highway_share.get(tag) + l.getLength());
									};
									
								}
								out.write(highway_share.get(tag)/length + ",");
							}
							
							
							// LANES
							
							// for each possible number of lanes, return the percentage consisting of that number of lanes
							
							Map<String, Double> lanes_percentages = new HashMap<String, Double>();
							for (String lanes : number_of_lanes) {
								lanes_percentages.put(lanes, 0.0);
							}
							
							for (Link l : path.links) {
								if (l.getNumberOfLanes() == 0.0) {
									lanes_percentages.put("0_lanes", lanes_percentages.get("0_lanes") + l.getLength());
								}
								else if (l.getNumberOfLanes() == 1.0) {
									lanes_percentages.put("1_lanes", lanes_percentages.get("1_lanes") + l.getLength());
								}
								else if (l.getNumberOfLanes() == 2.0) {
									lanes_percentages.put("2_lanes", lanes_percentages.get("2_lanes") + l.getLength());
								}
								else if (l.getNumberOfLanes() == 3.0) {
									lanes_percentages.put("3_lanes", lanes_percentages.get("3_lanes") + l.getLength());
								}
								else if (l.getNumberOfLanes() == 4.0) {
									lanes_percentages.put("4_lanes", lanes_percentages.get("4_lanes") + l.getLength());
								}
							}
							
							for (String lanes : number_of_lanes) {
								out.write(lanes_percentages.get(lanes)/length + ",");
							}

							// MAX, AVG link and AVG GRADE
							
							// compute the slope of the steepest link
							// and the average slope of any link and the average slope
							
							Double max_gradient = (double) -10;
							Double avg_gradient = 0.0;
							Double avg_link_gradient = 0.0;
							for (Link l : path.links) {
								String temp_gradient_str = (l.getAttributes().getAttribute("grade")).toString();
								Double temp_gradient = Double.valueOf(temp_gradient_str).doubleValue();
								max_gradient = Math.max(temp_gradient, max_gradient);
								avg_gradient += temp_gradient * l.getLength();
								avg_link_gradient += temp_gradient;
							}
							
							avg_gradient /= length;
							avg_link_gradient /= number_of_links;
							
							out.write(String.valueOf(max_gradient) + ",");
							out.write(String.valueOf(avg_gradient) + ",");
							out.write(String.valueOf(avg_link_gradient) + ",");
							
							// elevation gain
							
							Double elevation_gain = 0.0;
							Double uphill_distance = 0.0;
							
							for (Link l : path.links) {
								String temp_gradient_str = (l.getAttributes().getAttribute("grade")).toString();
								Double temp_gradient = Double.valueOf(temp_gradient_str).doubleValue();
								if (temp_gradient > 0.0) {
									elevation_gain += temp_gradient * l.getLength();
									uphill_distance += l.getLength();
								}
							}
							
							out.write(elevation_gain + ",");
							out.write(uphill_distance + ",");
							
							out.write(elevation_gain/length + ",");
							if (uphill_distance > 0.0) {
								out.write(elevation_gain/uphill_distance + ",");
							}
							else {
								out.write(elevation_gain + ","); // this is 0.0
							}
							
							
							
							// GRADIENT CATEGORIES
							
							Double grade_1_2 = 0.0;
							Double grade_2_3 = 0.0;
							Double grade_3_4 = 0.0;
							Double grade_4_5 = 0.0;
							Double grade_5_6 = 0.0;
							Double grade_6_7 = 0.0;
							Double grade_7_8 = 0.0;
							Double grade_8_9 = 0.0;
							Double grade_9_10 = 0.0;
							Double grade_10_infty = 0.0;
							
							
							
							for (Link l : path.links) {
								String grade_str = (l.getAttributes().getAttribute("grade")).toString();
								Double grade = Double.valueOf(grade_str).doubleValue();
								if (grade >= 0.01 && grade <= 0.02) {
									grade_1_2 += l.getLength();
								}
								else if (grade > 0.02 && grade <= 0.03) {
									grade_2_3 += l.getLength();
								}
								else if (grade > 0.03 && grade <= 0.04) {
									grade_3_4 += l.getLength();
								}
								else if (grade > 0.04 && grade <= 0.05) {
									grade_4_5 += l.getLength();
								}
								else if (grade > 0.05 && grade <= 0.06) {
									grade_5_6 += l.getLength();
								}
								else if (grade > 0.06 && grade <= 0.07) {
									grade_6_7 += l.getLength();
								}
								else if (grade > 0.07 && grade <= 0.08) {
									grade_7_8 += l.getLength();
								}
								else if (grade > 0.08 && grade <= 0.09) {
									grade_8_9 += l.getLength();
								}
								else if (grade > 0.09 && grade <= 0.10) {
									grade_9_10 += l.getLength();
								}
								else if (grade > 0.10) {
									grade_10_infty += l.getLength();
								}
								else {
									continue;
								}
							}
							

							out.write(grade_1_2/length + ",");
							out.write(grade_2_3/length + ",");
							out.write(grade_3_4/length + ",");
							out.write(grade_4_5/length + ",");
							out.write(grade_5_6/length + ",");
							out.write(grade_6_7/length + ",");
							out.write(grade_7_8/length + ",");
							out.write(grade_8_9/length + ",");
							out.write(grade_9_10/length + ",");
							out.write(grade_10_infty/length + ",");
							
							
							
							// MAXIMAL MAXSPEED
							
							// compute the maximal allowed car speed
							
							Double max_maxspeed = 0.0;
							for (Link l : path.links) {
								String temp_maxspeed_str = (l.getAttributes().getAttribute("max_speed")).toString();
								if (temp_maxspeed_str.equals("nan") || temp_maxspeed_str.equals("walk")) {
									max_maxspeed = Math.max(0.0, max_maxspeed);
								}
								else {
									Double temp_maxspeed = Double.valueOf(temp_maxspeed_str).doubleValue();
									max_maxspeed = Math.max(temp_maxspeed, max_maxspeed);
								}
							}
							
							out.write(max_maxspeed + ",");
							
							// Distance with maxspeed lower than 30
							
							Double maxspeed_lower_than_30 = 0.0;
							for (Link l : path.links) {
								String temp_maxspeed_str = (l.getAttributes().getAttribute("max_speed")).toString();
								if (temp_maxspeed_str.equals("nan") || temp_maxspeed_str.equals("walk")) {
									continue;
								}
								else if (Double.valueOf(temp_maxspeed_str).doubleValue() <= 30.0) {
									maxspeed_lower_than_30 += l.getLength();
								}
								else {
									continue;
								}
							}
							
							out.write(maxspeed_lower_than_30/length + ",");
							
							
							// Distance with maxspeed 30
							
							Double maxspeed_30 = 0.0;
							for (Link l : path.links) {
								String temp_maxspeed_str = (l.getAttributes().getAttribute("max_speed")).toString();
								if (temp_maxspeed_str.equals("nan") || temp_maxspeed_str.equals("walk")) {
									continue;
								}
								else if (Double.valueOf(temp_maxspeed_str).doubleValue() == 30.0) {
									maxspeed_30 += l.getLength();
								}
								else {
									continue;
								}
							}
							
							out.write(maxspeed_30/length + ",");
							
							
							// MAX and AVG LDV COUNT
							
							Double max_ldv_count = 0.0;
							Double avg_ldv_count = 0.0;
							for (Link l : path.links) {
								String temp_ldv_count_str = (l.getAttributes().getAttribute("ldv_count")).toString();
								if (temp_ldv_count_str.equals("nan")) {
									avg_ldv_count += 0.0;
									max_ldv_count = Math.max(0.0, max_ldv_count);
								}
								else {
									Double temp_ldv_count_cr = Double.valueOf(temp_ldv_count_str).doubleValue();
									max_ldv_count = Math.max(temp_ldv_count_cr, max_ldv_count);
									avg_ldv_count += temp_ldv_count_cr * l.getLength()/length;
								}
							}
							
							out.write(String.valueOf(max_ldv_count) + ",");
							out.write(avg_ldv_count + ",");
							
							
							
							
							// LDV COUNT CATEGORIES
							
							Double ldv_count_0_2500 = 0.0;
							Double ldv_count_2500_10000 = 0.0;
							Double ldv_count_10000_infty = 0.0;
							
							for (Link l : path.links) {
								String ldv_count = (l.getAttributes().getAttribute("ldv_count")).toString();
								if (ldv_count.equals("nan") || (double) Double.valueOf(ldv_count).doubleValue() <= 2500.0) {
									ldv_count_0_2500 += l.getLength();
								}
								else if ((double) Double.valueOf(ldv_count).doubleValue() > 2500.0 && (double) Double.valueOf(ldv_count).doubleValue() <= 10000.0) {
									ldv_count_2500_10000 += l.getLength();
								}
								else {
									ldv_count_10000_infty += l.getLength();
								}
							}
							
							out.write(ldv_count_0_2500/length + ",");
							out.write(ldv_count_2500_10000/length + ",");
							out.write(ldv_count_10000_infty/length + ",");
							
							
							
							// MAX HDV COUNT
							
//							Double max_hdv_count = 0.0;
//							for (Link l : path.links) {
//								String temp_hdv_count_str = (l.getAttributes().getAttribute("hdv_count")).toString();
//								if (temp_hdv_count_str.equals("nan")) {
//									max_hdv_count = Math.max(0.0, max_hdv_count);
//								}
//								else {
//									Double temp_hdv_count = Double.valueOf(temp_hdv_count_str).doubleValue();
//									max_hdv_count = Math.max(temp_hdv_count, max_hdv_count);
//								}
//							}
//							
//							out.write(String.valueOf(max_hdv_count) + ",");
//							
//							// ON-STREET PARKING
//							
//							Double number_of_parking_spots = 0.0;
//							for (Link l : path.links) {
//								number_of_parking_spots += Math.min(100.0, Double.valueOf((String) l.getAttributes().getAttribute("os_park_pd").toString()).doubleValue()*l.getLength()/100);
//							}
//							out.write(number_of_parking_spots/length * 100 + ",");
//							
//							
//							// Avg number of TREES per 100m
//							
//							Double number_of_trees = 0.0;
//							for (Link l : path.links) {
//								number_of_trees += Math.min(100.0, Double.valueOf((String) l.getAttributes().getAttribute("trees_pd").toString()).doubleValue()*l.getLength()/100);
//							}
//							out.write(number_of_trees/length * 100 + ",");
//							
//							
							// VELOWEG
							
							Double sep_biking_lanes_length = 0.0;
							for (Link l : path.links) {
								if (l.getAttributes().getAttribute("veloweg").equals("1") || l.getAttributes().getAttribute("cycleway").equals("track")) {
									sep_biking_lanes_length += l.getLength();
								}
							}
							out.write(sep_biking_lanes_length/length + ",");
							
							
							// VELOSTREIFEN
							
							Double biking_lanes_length = 0.0;
							for (Link l : path.links) {
								if (l.getAttributes().getAttribute("velostreifen").equals("1")) {
									biking_lanes_length += l.getLength();
								}
							}
							out.write(biking_lanes_length/length + ",");
							
							
							
//							// Percentage of route along parks
//							
//							Double park_length = 0.0;
//							
//							for (Link l : path.links) {
//								if(l.getAttributes().getAttribute("park").equals(1.0)) {
//									park_length += l.getLength();
//								}
//							}
//							
//							out.write(park_length/length + ",");
//							
//							// Percentage of route along a forest
//							
//							Double forest_length = 0.0;
//							
//							for (Link l : path.links) {
//								if(l.getAttributes().getAttribute("forest").equals(1.0)) {
//									forest_length += l.getLength();
//								}
//							}
//							
//							out.write(forest_length/length + ",");
//							
//							
//							// Percentage of route along water
//							
//							Double water_length = 0.0;
//							
//							for (Link l : path.links) {
//								if(l.getAttributes().getAttribute("water").equals(1.0)) {
//									water_length += l.getLength();
//								}
//							}
//							
//							out.write(water_length/length + ",");
//							
//							
//							// avg number of SHOPS per 100m
//							
//							Double number_of_shops = 0.0;
//							Double max_shop_pd = 0.0;
//							for (Link l : path.links) {
//								max_shop_pd = Math.max(max_shop_pd, (double) l.getAttributes().getAttribute("shops_pd"));
//								number_of_shops += Math.min((double) l.getAttributes().getAttribute("shops_pd") * l.getLength()/100, 50.0);
//							}
//							out.write(number_of_shops/length * 100 + ",");
//							
//							
//							// highest density of shops along the route (i.e. link with highest shops per 100m value)
//							
//							out.write(max_shop_pd + ",");
//							
							
							
							////////// NODE ATTRIBUTES ///////
							
							
							// NUMBER OF TRAFFIC SIGNALS
							
							
//							List<Double> ts_ids = new ArrayList<Double>();
//							
//							for (Link l : path.links) {
//								String temp_ts_id_str = (l.getToNode().getAttributes().getAttribute("ts_id")).toString();
//								if (temp_ts_id_str.equals("nan")) {
//									continue;
//								}
//								else {
//									ts_ids.add(Double.valueOf(temp_ts_id_str).doubleValue());
//								}
//							}
//							Set<Double> unique_ts_ids = new HashSet<Double>(ts_ids);
//							out.write(unique_ts_ids.size() + ",");
//							
//							// TRAFFIC SIGNALS CAR
//							
//							Double number_of_ts_car = 0.0;
//							for (Link l : path.links) {
//								if (l.getToNode().getAttributes().getAttribute("ts_car").equals("1.0")) {
//									number_of_ts_car ++;
//								}
//							}
//							out.write(number_of_ts_car + ",");
							
							// TRAFFIC SIGNALS OSM
							
							Double number_of_ts_osm = 0.0;
							for (Link l : path.links) {
								if (l.getToNode().getAttributes().getAttribute("ts_osm").equals("1")) {
									number_of_ts_osm ++;
								}
							}
							out.write(number_of_ts_osm + ",");
							
							Double number_of_ts_osm_2 = 0.0;
							for (Link l : path.links) {
								if (l.getToNode().getAttributes().getAttribute("ts_osm_2").equals("1")) {
									number_of_ts_osm_2 ++;
								}
							}
							out.write(number_of_ts_osm_2 + ",");
//							
//							// TRAFFIC SIGNALS BIKE
//							
//							Double number_of_ts_bike = 0.0;
//							for (Link l : path.links) {
//								if (l.getToNode().getAttributes().getAttribute("ts_bike").equals("1.0")) {
//									number_of_ts_bike ++;
//								}
//							}
//							out.write(number_of_ts_bike + ",");
//							
//							
//							// MAXIMAL TRAFFIC SIGNAL (CROSSING) COMPLEXITY
//							
//							Double max_ts_complexity = 0.0;
//							for (Link l : path.links) {
//								String temp_ts_complexity_str = (l.getToNode().getAttributes().getAttribute("ts_complex")).toString();
//								if (temp_ts_complexity_str.equals("nan")) {
//									max_ts_complexity = Math.max(0.0, max_ts_complexity);
//								}
//								else {
//									Double temp_ts_complexity = Double.valueOf(temp_ts_complexity_str).doubleValue();
//									max_ts_complexity = Math.max(temp_ts_complexity, max_ts_complexity);
//								}
//								
//							}
//							
//							out.write(String.valueOf(max_ts_complexity) + ",");
//							
							
						
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


