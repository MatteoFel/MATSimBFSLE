import pandas as pd
import math

import xml.etree.ElementTree as ET
from pyproj import Transformer
from pyproj import CRS
crs_4326 = CRS("WGS84")
crs_proj = CRS("EPSG:25832")
transformer = Transformer.from_crs(crs_4326, crs_proj)


#Tobler's hiking function, returns km/h thus we need to divide by 3.6
def tobler(slope):
    speed = 6 * math.exp(-3.5 * abs(slope + 0.05))/3.6
    return speed


#based on oslo paper
def compute_free_speed_oslo(slope):
    # grade = delta_elevation/length [m]
    if slope <= -0.09:
        slope_factor = 0.0491
    elif -0.07 >= slope > -0.09:
        slope_factor = 0.1081
    elif -0.06 >= slope > -0.07:
        slope_factor = 0.1357
    elif -0.05 >= slope > -0.06:
        slope_factor = 0.1795
    elif -0.04 >= slope > -0.05:
        slope_factor = 0.1802
    elif -0.03 >= slope > -0.04:
        slope_factor = 0.1494
    elif -0.02 >= slope > -0.03:
        slope_factor = 0.1124
    elif -0.01 >= slope > -0.02:
        slope_factor = 0.0589
    elif 0.0 >= slope > -0.01:
        slope_factor = 0.0412
    elif 0.01 >= slope > 0.0:
        slope_factor = 0.0
    elif 0.02 >= slope > 0.01:
        slope_factor = -0.0973
    elif 0.03 >= slope > 0.02:
        slope_factor = -0.1299
    elif 0.04 >= slope > 0.03:
        slope_factor = -0.1951
    elif 0.05 >= slope > 0.04:
        slope_factor = -0.2669
    elif 0.06 >= slope > 0.05:
        slope_factor = -0.3034
    elif 0.07 >= slope > 0.06:
        slope_factor = -0.3854
    elif 0.09 >= slope > 0.07:
        slope_factor = -0.3949
    elif 0.09 < slope:
        slope_factor = -0.4267

    speed = math.exp(3.008 + slope_factor - 0.2087)
    return speed


def compute_slope_factor(slope):
    # for regular bikes
    # grade = delta_elevation/length [m]
    # source: OSMBicycleReader
    if slope > 0.10:
        slope_factor = 0.1
    elif 0.1 >= slope > 0.05:
        slope_factor = 0.4
    elif 0.05 >= slope > 0.03:
        slope_factor = 0.6
    elif 0.03 >= slope > 0.01:
        slope_factor = 0.8
    elif 0.01 >= slope > -0.01:
        slope_factor = 1
    elif -0.01 >= slope > -0.03:
        slope_factor = 1.2
    elif -0.03 >= slope > -0.05:
        slope_factor = 1.3
    elif -0.05 >= slope > -0.1:
        slope_factor = 1.4
    elif -0.1 >= slope:
        slope_factor = 1.5
    return slope_factor

#due to inprecisions in the map matching there might be links in the chosen route that are not part of the network. Add these links.
import pandas as pd
import numpy as np

df = pd.read_csv('/Users/matteofelder/Documents/IVT/ped_zurich/chosen_routes/od_pairs_iwi_trb.tsv', sep='\t', on_bad_lines='skip')
chosen_links_list = []

df = df.replace(np.nan, 0)
for index, row in df.iterrows():
    for link in row[3:-1]:
        if link != 0:
            chosen_links_list.append(link)



tree = ET.parse('/Users/matteofelder/Documents/IVT/OsmToMatsim/stadt_zh_final_6_AOT.xml')
root = tree.getroot()

#to make xml files prettier, use (it is stored in a file ..._pretty.xml)
#ET.indent(tree, space="\t", level=0)

#tree.write("/Users/matteofelder/Documents/IVT/OsmToMatsim/stadt_zh_final_AOT_pretty.xml", encoding='utf-8', xml_declaration=True)

root_new = ET.Element("network")
nodes = ET.SubElement(root_new, "nodes")



for elem in root.findall('node'):
    x,y = transformer.transform(float(elem.get('lat')),float(elem.get('lon')))
    node = ET.SubElement(nodes, "node", id=str(elem.get('id')), x=str(x), y=str(y))
    list = []
    for subelem in elem.findall('tag'):
        sublist = []
        sublist.append(str(subelem.get('k')))
        sublist.append(str(subelem.get('v')))
        list.append(sublist)
        #if str(subelem.get('v'))=="nan":
        #   list[-1][1] = "unknown"
    node_attributes = ET.SubElement(node, "attributes")
    node_attributes.text = "\n\t\t\t"
    node_attribute_highway = ET.SubElement(node_attributes, "attribute", name="highway")
    node_attribute_highway.set("class", "java.lang.String")
    node_attribute_highway.text = list[0][1]
    node_attribute_ts_complex = ET.SubElement(node_attributes, "attribute", name="ts_complex")
    node_attribute_ts_complex.set("class", "java.lang.String")
    node_attribute_ts_complex.text = list[1][1]
    node_attribute_ts_bike = ET.SubElement(node_attributes, "attribute", name="ts_bike")
    node_attribute_ts_bike.set("class", "java.lang.String")
    node_attribute_ts_bike.text = list[2][1]
    node_attribute_ts_car = ET.SubElement(node_attributes, "attribute", name="ts_car")
    node_attribute_ts_car.set("class", "java.lang.String")
    node_attribute_ts_car.text = list[3][1]
    node_attribute_ts_id = ET.SubElement(node_attributes, "attribute", name="ts_id")
    node_attribute_ts_id.set("class", "java.lang.String")
    node_attribute_ts_id.text = list[4][1]
    node.text = "\n\t\t"

links = ET.SubElement(root_new, "links")

i=0
k=0
j=0
l=0
for elem in root.findall('way'):
    k+=1
    id = str(elem.get('id'))
    list=[]
    for subelem in elem.findall('nd'):
        list.append(str(subelem.get('ref')))
    for subelem in elem.findall('tag'):
        sublist = []
        sublist.append(str(subelem.get('k')))
        sublist.append(str(subelem.get('v')))
        list.append(sublist)
        if str(subelem.get('k'))=='oneway':
            if str(subelem.get('v'))=="yes":
                list[-1][1] = 'yes'
            else:
                list[-1][1] = 'no'
        #if str(subelem.get('v'))=="nan":
        #   list[-1][1] = "unknown"
    if list[4][1] == 'yes':#i.e. oneway == '1', create the link as in Meister's network (exception will come later)
        link = ET.SubElement(links, "link", id=list[0] + "_" + list[1])
        link.set('from', list[0])
        link.set('to', list[1])
        link.set('length', list[5][1])
        #link.set('freespeed', str(compute_free_speed_oslo(float(list[11][1]))))
        link.set('freespeed', str(tobler(float(list[11][1]))))
        link.set("capacity", '300')  # default
        link.set("permlanes", list[14][1])
        # modes_one = []
        # for i in range(17,20):
        #     if list[i][1]=='True':
        #         modes_one.append(list[i][0])
        # link.set("modes", ", ".join(modes_one))  # this might cause some trouble / might not be ideal.
        link.set("modes", "walk")

        # if list[26][1]=='1.0' and list[27][1]=='1.0' and list[28][1]=='nan':# and list[28][1] == 'yes' and list[14][1]>'1': #and list[18][1]=='True' and list[17][1]=='False' and list[4][1]=='1':
        # i+=1
        # print(id)
        # print(list)

        # if list[26][1]=='1.0':
        # j+=1

        # if list[26][1] == '1.0' and list[27][1]=='1.0':
        #    l+=1
        #    print(id)

        attributes = ET.SubElement(link, "attributes")
        attributes.text = "\n\t\t\t"
        attribute_highway = ET.SubElement(attributes, "attribute", name="highway")
        attribute_highway.set("class", "java.lang.String")
        attribute_highway.text = list[2][1]
        attribute_name = ET.SubElement(attributes, "attribute", name="name")
        attribute_name.set("class", "java.lang.String")
        attribute_name.text = list[3][1]
        attribute_oneway = ET.SubElement(attributes, "attribute", name="oneway")
        attribute_oneway.set("class", "java.lang.String")
        attribute_oneway.text = list[4][1]   # this tells us whether there is a matsim link in the opposite direction
        attribute_length = ET.SubElement(attributes, "attribute", name="length")
        attribute_length.set("class", "java.lang.Double")
        attribute_length.text = list[5][1]
        attribute_service = ET.SubElement(attributes, "attribute", name="service")
        attribute_service.set("class", "java.lang.String")
        attribute_service.text = list[6][1]
        attribute_cycleway = ET.SubElement(attributes, "attribute", name="cycleway")
        attribute_cycleway.set("class", "java.lang.String")
        attribute_cycleway.text = list[7][1]
        attribute_bicycle = ET.SubElement(attributes, "attribute", name="bicycle")
        attribute_bicycle.set("class", "java.lang.String")
        attribute_bicycle.text = list[8][1]
        attribute_access = ET.SubElement(attributes, "attribute", name="access")
        attribute_access.set("class", "java.lang.String")
        attribute_access.text = list[9][1]
        attribute_surface = ET.SubElement(attributes, "attribute", name="surface")
        attribute_surface.set("class", "java.lang.String")
        attribute_surface.text = list[10][1]
        attribute_grade = ET.SubElement(attributes, "attribute", name="grade")
        attribute_grade.set("class", "java.lang.Double")
        attribute_grade.text = list[11][1]
        attribute_grade_abs = ET.SubElement(attributes, "attribute", name="grade_abs")
        attribute_grade_abs.set("class", "java.lang.Double")
        attribute_grade_abs.text = list[12][1]
        attribute_maxspeed = ET.SubElement(attributes, "attribute", name="max_speed")
        attribute_maxspeed.set("class", "java.lang.String")
        attribute_maxspeed.text = list[13][1]
        attribute_lanes = ET.SubElement(attributes, "attribute", name="lanes")
        attribute_lanes.set("class", "java.lang.Double")
        attribute_lanes.text = list[14][1]
        attribute_ffspeed = ET.SubElement(attributes, "attribute", name="ffspeed_bike")
        attribute_ffspeed.set("class", "java.lang.Double")
        attribute_ffspeed.text = list[15][1]
        # attribute_ffspeed_slope = ET.SubElement(attributes, "attribute", name="ffspeed_slope")
        # attribute_ffspeed_slope.set("class", "java.lang.Double")
        # attribute_ffspeed_slope.text = list[16][1]
        attribute_car = ET.SubElement(attributes, "attribute", name="car")
        attribute_car.set("class", "java.lang.String")
        attribute_car.text = list[17][1]
        attribute_bike = ET.SubElement(attributes, "attribute", name="bike")
        attribute_bike.set("class", "java.lang.String")
        attribute_bike.text = list[18][1]
        attribute_foot = ET.SubElement(attributes, "attribute", name="foot")
        attribute_foot.set("class", "java.lang.String")
        attribute_foot.text = list[19][1]
        attribute_ldv_count = ET.SubElement(attributes, "attribute", name="ldv_count")
        attribute_ldv_count.set("class", "java.lang.String")
        attribute_ldv_count.text = list[20][1]
        attribute_hdv_count = ET.SubElement(attributes, "attribute", name="hdv_count")
        attribute_hdv_count.set("class", "java.lang.String")
        attribute_hdv_count.text = list[21][1]
        attribute_os_park_pd = ET.SubElement(attributes, "attribute", name="os_park_pd")
        attribute_os_park_pd.set("class", "java.lang.String")
        attribute_os_park_pd.text = list[22][1]
        attribute_trees = ET.SubElement(attributes, "attribute", name="trees_pd")
        attribute_trees.set("class", "java.lang.String")
        attribute_trees.text = list[23][1]
        attribute_veloweg = ET.SubElement(attributes, "attribute", name="veloweg")
        attribute_veloweg.set("class", "java.lang.String")
        attribute_veloweg.text = list[24][1]
        attribute_velostreif = ET.SubElement(attributes, "attribute", name="velostreifen")
        attribute_velostreif.set("class", "java.lang.String")
        attribute_velostreif.text = list[25][1]
        #attribute_oneway_bike = ET.SubElement(attributes, "attribute", name="oneway_bike")
        #attribute_oneway_bike.set("class", "java.lang.String")
        #attribute_oneway_bike.text = list[28][1]
        attribute_velomaster = ET.SubElement(attributes, "attribute", name="velomaster")
        attribute_velomaster.set("class", "java.lang.String")
        attribute_velomaster.text = list[27][1]
        attribute_park = ET.SubElement(attributes, "attribute", name="park")
        attribute_park.set("class", "java.lang.Double")
        attribute_park.text = list[28][1]
        attribute_forest = ET.SubElement(attributes, "attribute", name="forest")
        attribute_forest.set("class", "java.lang.Double")
        attribute_forest.text = list[29][1]
        attribute_water = ET.SubElement(attributes, "attribute", name="water")
        attribute_water.set("class", "java.lang.Double")
        attribute_water.text = list[30][1]
        attribute_shops_pd = ET.SubElement(attributes, "attribute", name="shops_pd")
        attribute_shops_pd.set("class", "java.lang.Double")
        attribute_shops_pd.text = list[31][1]
        attribute_reversedirection = ET.SubElement(attributes, "attribute", name="reverse_direction")
        attribute_reversedirection.set("class", "java.lang.String")
        attribute_reversedirection.text = 'no'


        link.text = "\n\t\t"

        #some of the pedestrian and pure bike links in Meister's network are not bidirectional - add a link in the opposite direction if this is the case
        #also those links with cycleway = opposite/opposite_lane are allowed to used in the opposite direction by bikes
        if list[19][1] == 'True' or (list[18][1] =='True' and list[17][1]=='False' and list[19][1] == 'False')\
                or (list[7][1] == 'opposite' or list[7][1] == 'opposite_lane'):
            link = ET.SubElement(links, "link", id=list[1] + "_" + list[0])
            link.set('from', list[1])
            link.set('to', list[0])
            link.set('length', list[5][1])
            #link.set('freespeed', str(compute_free_speed_oslo(-float(list[11][1]))))
            #link.set('freespeed', str(compute_slope_factor(float(-float(list[11][1]))) * float(list[15][1])/3.6))
            link.set('freespeed', str(tobler(-float(list[11][1]))))
            link.set("capacity", '300')  # default
            link.set("permlanes", str(int(list[14][1]) // 2))  # the link in Meister's direction gets one lane more than the link in the opposite direction
            # modes_four = []
            # for i in range(17, 20):
            #     if list[i][1] == 'True':
            #         modes_four.append(list[i][0])
            # link.set("modes", ", ".join(modes_four))  # this might not be ideal
            link.set("modes", "walk")

            attributes = ET.SubElement(link, "attributes")
            attributes.text = "\n\t\t\t"
            attribute_highway = ET.SubElement(attributes, "attribute", name="highway")
            attribute_highway.set("class", "java.lang.String")
            attribute_highway.text = list[2][1]
            attribute_name = ET.SubElement(attributes, "attribute", name="name")
            attribute_name.set("class", "java.lang.String")
            attribute_name.text = list[3][1]
            attribute_oneway = ET.SubElement(attributes, "attribute", name="oneway")
            attribute_oneway.set("class", "java.lang.String")
            attribute_oneway.text = list[4][1]   # this tells us whether there is a matsim link in the opposite direction
            attribute_length = ET.SubElement(attributes, "attribute", name="length")
            attribute_length.set("class", "java.lang.Double")
            attribute_length.text = list[5][1]
            attribute_service = ET.SubElement(attributes, "attribute", name="service")
            attribute_service.set("class", "java.lang.String")
            attribute_service.text = list[6][1]
            attribute_cycleway = ET.SubElement(attributes, "attribute", name="cycleway")
            attribute_cycleway.set("class", "java.lang.String")
            attribute_cycleway.text = list[7][1]
            attribute_bicycle = ET.SubElement(attributes, "attribute", name="bicycle")
            attribute_bicycle.set("class", "java.lang.String")
            attribute_bicycle.text = list[8][1]
            attribute_access = ET.SubElement(attributes, "attribute", name="access")
            attribute_access.set("class", "java.lang.String")
            attribute_access.text = list[9][1]
            attribute_surface = ET.SubElement(attributes, "attribute", name="surface")
            attribute_surface.set("class", "java.lang.String")
            attribute_surface.text = list[10][1]
            attribute_grade = ET.SubElement(attributes, "attribute", name="grade")
            attribute_grade.set("class", "java.lang.Double")
            attribute_grade.text = str(float(-float(list[11][1])))
            attribute_grade_abs = ET.SubElement(attributes, "attribute", name="grade_abs")
            attribute_grade_abs.set("class", "java.lang.Double")
            attribute_grade_abs.text = list[12][1]
            attribute_maxspeed = ET.SubElement(attributes, "attribute", name="max_speed")
            attribute_maxspeed.set("class", "java.lang.String")
            attribute_maxspeed.text = list[13][1]
            attribute_lanes = ET.SubElement(attributes, "attribute", name="lanes")
            attribute_lanes.set("class", "java.lang.Double")
            attribute_lanes.text = str(int(list[14][1]) // 2)
            attribute_ffspeed = ET.SubElement(attributes, "attribute", name="ffspeed_bike")
            attribute_ffspeed.set("class", "java.lang.Double")
            attribute_ffspeed.text = list[15][1]
            # attribute_ffspeed_slope = ET.SubElement(attributes, "attribute", name="ffspeed_slope")
            # attribute_ffspeed_slope.set("class", "java.lang.Double")
            # attribute_ffspeed_slope.text = str(compute_slope_factor(float(-float(list[11][1]))) * float(list[15][1]))
            attribute_car = ET.SubElement(attributes, "attribute", name="car")
            attribute_car.set("class", "java.lang.String")
            attribute_car.text = list[17][1]
            attribute_bike = ET.SubElement(attributes, "attribute", name="bike")
            attribute_bike.set("class", "java.lang.String")
            attribute_bike.text = list[18][1]
            attribute_foot = ET.SubElement(attributes, "attribute", name="foot")
            attribute_foot.set("class", "java.lang.String")
            attribute_foot.text = list[19][1]
            attribute_ldv_count = ET.SubElement(attributes, "attribute", name="ldv_count")
            attribute_ldv_count.set("class", "java.lang.String")
            attribute_ldv_count.text = list[20][1]
            attribute_hdv_count = ET.SubElement(attributes, "attribute", name="hdv_count")
            attribute_hdv_count.set("class", "java.lang.String")
            attribute_hdv_count.text = list[21][1]
            attribute_os_park_pd = ET.SubElement(attributes, "attribute", name="os_park_pd")
            attribute_os_park_pd.set("class", "java.lang.String")
            attribute_os_park_pd.text = list[22][1]
            attribute_trees = ET.SubElement(attributes, "attribute", name="trees_pd")
            attribute_trees.set("class", "java.lang.String")
            attribute_trees.text = list[23][1]
            attribute_veloweg = ET.SubElement(attributes, "attribute", name="veloweg")
            attribute_veloweg.set("class", "java.lang.String")
            attribute_veloweg.text = list[24][1]
            attribute_velostreif = ET.SubElement(attributes, "attribute", name="velostreifen")
            attribute_velostreif.set("class", "java.lang.String")
            if list[26][
                1] == 'counter_yes':  # oneway_bike (i.e. list[28][1]) tells us whether we have a velostreifen in the digitalization direction
                attribute_velostreif.text = list[25][1]
            elif list[26][1] == 'yes':
                attribute_velostreif.text = "0.0"
            else:
                attribute_velostreif.text = list[25][1]
            # attribute_oneway_bike = ET.SubElement(attributes, "attribute", name="oneway_bike")
            # attribute_oneway_bike.set("class", "java.lang.String")
            # attribute_oneway_bike.text = list[28][1]
            attribute_velomaster = ET.SubElement(attributes, "attribute", name="velomaster")
            attribute_velomaster.set("class", "java.lang.String")
            attribute_velomaster.text = list[27][1]
            attribute_park = ET.SubElement(attributes, "attribute", name="park")
            attribute_park.set("class", "java.lang.Double")
            attribute_park.text = list[28][1]
            attribute_forest = ET.SubElement(attributes, "attribute", name="forest")
            attribute_forest.set("class", "java.lang.Double")
            attribute_forest.text = list[29][1]
            attribute_water = ET.SubElement(attributes, "attribute", name="water")
            attribute_water.set("class", "java.lang.Double")
            attribute_water.text = list[30][1]
            attribute_shops_pd = ET.SubElement(attributes, "attribute", name="shops_pd")
            attribute_shops_pd.set("class", "java.lang.Double")
            attribute_shops_pd.text = list[31][1]
            attribute_reversedirection = ET.SubElement(attributes, "attribute", name="reverse_direction")
            attribute_reversedirection.set("class", "java.lang.String")
            if list[7][1] == 'opposite' or list[7][1] == 'opposite_lane':
                attribute_reversedirection.text = 'yes'
            else:
                attribute_reversedirection.text = 'no'




        if list[1] + "_" + list[0] in chosen_links_list:
            print(list)
            link = ET.SubElement(links, "link", id=list[1] + "_" + list[0])
            link.set('from', list[1])
            link.set('to', list[0])
            link.set('length', list[5][1])
            #link.set('freespeed', str(compute_free_speed_oslo(-float(list[11][1]))))
            #link.set('freespeed', str(compute_slope_factor(float(-float(list[11][1]))) * float(list[15][1])/3.6))
            link.set('freespeed', str(tobler(-float(list[11][1]))))
            link.set("capacity", '300')  # default
            if int(list[14][1]) == 1:
                link.set("permlanes", "1")
            else:
                link.set("permlanes", str(int(list[14][1]) // 2))  # the link in Meister's direction gets one lane more than the link in the opposite direction
                # modes_four = []
                # for i in range(17, 20):
                #     if list[i][1] == 'True':
                #         modes_four.append(list[i][0])
                # link.set("modes", ", ".join(modes_four))  # this might not be ideal
            link.set("modes", "walk")

            attributes = ET.SubElement(link, "attributes")
            attributes.text = "\n\t\t\t"
            attribute_highway = ET.SubElement(attributes, "attribute", name="highway")
            attribute_highway.set("class", "java.lang.String")
            attribute_highway.text = list[2][1]
            attribute_name = ET.SubElement(attributes, "attribute", name="name")
            attribute_name.set("class", "java.lang.String")
            attribute_name.text = list[3][1]
            attribute_oneway = ET.SubElement(attributes, "attribute", name="oneway")
            attribute_oneway.set("class", "java.lang.String")
            attribute_oneway.text = list[4][
                    1]  # this tells us whether there is a matsim link in the opposite direction
            attribute_length = ET.SubElement(attributes, "attribute", name="length")
            attribute_length.set("class", "java.lang.Double")
            attribute_length.text = list[5][1]
            attribute_service = ET.SubElement(attributes, "attribute", name="service")
            attribute_service.set("class", "java.lang.String")
            attribute_service.text = list[6][1]
            attribute_cycleway = ET.SubElement(attributes, "attribute", name="cycleway")
            attribute_cycleway.set("class", "java.lang.String")
            attribute_cycleway.text = list[7][1]
            attribute_bicycle = ET.SubElement(attributes, "attribute", name="bicycle")
            attribute_bicycle.set("class", "java.lang.String")
            attribute_bicycle.text = list[8][1]
            attribute_access = ET.SubElement(attributes, "attribute", name="access")
            attribute_access.set("class", "java.lang.String")
            attribute_access.text = list[9][1]
            attribute_surface = ET.SubElement(attributes, "attribute", name="surface")
            attribute_surface.set("class", "java.lang.String")
            attribute_surface.text = list[10][1]
            attribute_grade = ET.SubElement(attributes, "attribute", name="grade")
            attribute_grade.set("class", "java.lang.Double")
            attribute_grade.text = str(float(-float(list[11][1])))
            attribute_grade_abs = ET.SubElement(attributes, "attribute", name="grade_abs")
            attribute_grade_abs.set("class", "java.lang.Double")
            attribute_grade_abs.text = list[12][1]
            attribute_maxspeed = ET.SubElement(attributes, "attribute", name="max_speed")
            attribute_maxspeed.set("class", "java.lang.String")
            attribute_maxspeed.text = list[13][1]
            attribute_lanes = ET.SubElement(attributes, "attribute", name="lanes")
            attribute_lanes.set("class", "java.lang.Double")
            if list[14][1] == "1":
                attribute_lanes.text = "1"
            else:
                attribute_lanes.text = str(int(list[14][1]) // 2)
            attribute_ffspeed = ET.SubElement(attributes, "attribute", name="ffspeed_bike")
            attribute_ffspeed.set("class", "java.lang.Double")
            attribute_ffspeed.text = list[15][1]
                # attribute_ffspeed_slope = ET.SubElement(attributes, "attribute", name="ffspeed_slope")
                # attribute_ffspeed_slope.set("class", "java.lang.Double")
                # attribute_ffspeed_slope.text = str(compute_slope_factor(float(-float(list[11][1]))) * float(list[15][1]))
            attribute_car = ET.SubElement(attributes, "attribute", name="car")
            attribute_car.set("class", "java.lang.String")
            attribute_car.text = list[17][1]
            attribute_bike = ET.SubElement(attributes, "attribute", name="bike")
            attribute_bike.set("class", "java.lang.String")
            attribute_bike.text = list[18][1]
            attribute_foot = ET.SubElement(attributes, "attribute", name="foot")
            attribute_foot.set("class", "java.lang.String")
            attribute_foot.text = list[19][1]
            attribute_ldv_count = ET.SubElement(attributes, "attribute", name="ldv_count")
            attribute_ldv_count.set("class", "java.lang.String")
            attribute_ldv_count.text = list[20][1]
            attribute_hdv_count = ET.SubElement(attributes, "attribute", name="hdv_count")
            attribute_hdv_count.set("class", "java.lang.String")
            attribute_hdv_count.text = list[21][1]
            attribute_os_park_pd = ET.SubElement(attributes, "attribute", name="os_park_pd")
            attribute_os_park_pd.set("class", "java.lang.String")
            attribute_os_park_pd.text = list[22][1]
            attribute_trees = ET.SubElement(attributes, "attribute", name="trees_pd")
            attribute_trees.set("class", "java.lang.String")
            attribute_trees.text = list[23][1]
            attribute_veloweg = ET.SubElement(attributes, "attribute", name="veloweg")
            attribute_veloweg.set("class", "java.lang.String")
            attribute_veloweg.text = list[24][1]
            attribute_velostreif = ET.SubElement(attributes, "attribute", name="velostreifen")
            attribute_velostreif.set("class", "java.lang.String")
            if list[26][
                1] == 'counter_yes':  # oneway_bike (i.e. list[28][1]) tells us whether we have a velostreifen in the digitalization direction
                attribute_velostreif.text = list[25][1]
            elif list[26][1] == 'yes':
                attribute_velostreif.text = "0.0"
            else:
                attribute_velostreif.text = list[25][1]
            # attribute_oneway_bike = ET.SubElement(attributes, "attribute", name="oneway_bike")
            # attribute_oneway_bike.set("class", "java.lang.String")
            # attribute_oneway_bike.text = list[28][1]
            attribute_velomaster = ET.SubElement(attributes, "attribute", name="velomaster")
            attribute_velomaster.set("class", "java.lang.String")
            attribute_velomaster.text = list[27][1]
            attribute_park = ET.SubElement(attributes, "attribute", name="park")
            attribute_park.set("class", "java.lang.Double")
            attribute_park.text = list[28][1]
            attribute_forest = ET.SubElement(attributes, "attribute", name="forest")
            attribute_forest.set("class", "java.lang.Double")
            attribute_forest.text = list[29][1]
            attribute_water = ET.SubElement(attributes, "attribute", name="water")
            attribute_water.set("class", "java.lang.Double")
            attribute_water.text = list[30][1]
            attribute_shops_pd = ET.SubElement(attributes, "attribute", name="shops_pd")
            attribute_shops_pd.set("class", "java.lang.Double")
            attribute_shops_pd.text = list[31][1]
            #if we are reversing a car link, this should be noted here
            if list[17][1] == 'True':
                attribute_reversedirection = ET.SubElement(attributes, "attribute", name="reverse_direction")
                attribute_reversedirection.set("class", "java.lang.String")
                attribute_reversedirection.text = 'yes'
            else:
                attribute_reversedirection = ET.SubElement(attributes, "attribute", name="reverse_direction")
                attribute_reversedirection.set("class", "java.lang.String")
                attribute_reversedirection.text = 'no'




    else:#i.e. oneway == 'no', we create two links, one in each direction
        #first, the one in Meister's direction
        link = ET.SubElement(links, "link", id=list[0] + "_" + list[1])
        link.set('from', list[0])
        link.set('to', list[1])
        link.set('length', list[5][1])
        #link.set('freespeed', str(compute_free_speed_oslo(float(list[11][1]))))
        #link.set('freespeed', str(float(list[16][1])/3.6))
        link.set('freespeed', str(tobler(float(list[11][1]))))
        link.set("capacity", '300')  # default
        if int(list[14][1]) % 2 == 0: #number of lanes is even
            link.set("permlanes", str(int(list[14][1]) / 2))
        else:#number of lanes is odd
            link.set("permlanes", str(int(list[14][1]) // 2 + 1)) #the link in Meister's direction gets one lane more than the link in the opposite direction
        # modes_two = []
        # for i in range(17,20):
        #     if list[i][1]=='True':
        #         modes_two.append(list[i][0])
        # link.set("modes", ", ".join(modes_two)) #this might not be ideal
        link.set("modes", "walk")


        # if list[26][1]=='1.0' and list[27][1]=='1.0' and list[28][1]=='nan':# and list[28][1] == 'yes' and list[14][1]>'1': #and list[18][1]=='True' and list[17][1]=='False' and list[4][1]=='1':
        # i+=1
        # print(id)
        # print(list)

        # if list[26][1]=='1.0':
        # j+=1

        #if list[4][1] == '1' and list[28][1]=='counter_yes':
          #  l+=1
          #  print(list)

        attributes = ET.SubElement(link, "attributes")
        attributes.text = "\n\t\t\t"
        attribute_highway = ET.SubElement(attributes, "attribute", name="highway")
        attribute_highway.set("class", "java.lang.String")
        attribute_highway.text = list[2][1]
        attribute_name = ET.SubElement(attributes, "attribute", name="name")
        attribute_name.set("class", "java.lang.String")
        attribute_name.text = list[3][1]
        attribute_oneway = ET.SubElement(attributes, "attribute", name="oneway")
        attribute_oneway.set("class", "java.lang.String")
        attribute_oneway.text = list[4][1]   # this tells us whether there is a matsim link in the opposite direction
        attribute_length = ET.SubElement(attributes, "attribute", name="length")
        attribute_length.set("class", "java.lang.Double")
        attribute_length.text = list[5][1]
        attribute_service = ET.SubElement(attributes, "attribute", name="service")
        attribute_service.set("class", "java.lang.String")
        attribute_service.text = list[6][1]
        attribute_cycleway = ET.SubElement(attributes, "attribute", name="cycleway")
        attribute_cycleway.set("class", "java.lang.String")
        attribute_cycleway.text = list[7][1]
        attribute_bicycle = ET.SubElement(attributes, "attribute", name="bicycle")
        attribute_bicycle.set("class", "java.lang.String")
        attribute_bicycle.text = list[8][1]
        attribute_access = ET.SubElement(attributes, "attribute", name="access")
        attribute_access.set("class", "java.lang.String")
        attribute_access.text = list[9][1]
        attribute_surface = ET.SubElement(attributes, "attribute", name="surface")
        attribute_surface.set("class", "java.lang.String")
        attribute_surface.text = list[10][1]
        attribute_grade = ET.SubElement(attributes, "attribute", name="grade")
        attribute_grade.set("class", "java.lang.Double")
        attribute_grade.text = list[11][1]
        attribute_grade_abs = ET.SubElement(attributes, "attribute", name="grade_abs")
        attribute_grade_abs.set("class", "java.lang.Double")
        attribute_grade_abs.text = list[12][1]
        attribute_maxspeed = ET.SubElement(attributes, "attribute", name="max_speed")
        attribute_maxspeed.set("class", "java.lang.String")
        attribute_maxspeed.text = list[13][1]
        attribute_lanes = ET.SubElement(attributes, "attribute", name="lanes")
        attribute_lanes.set("class", "java.lang.Double")
        if int(list[14][1]) % 2 == 0:  # number of lanes is even
            attribute_lanes.text = str(int(list[14][1]) / 2)
        else:  # number of lanes is odd
            attribute_lanes.text = str(int(list[14][1]) // 2 + 1)  # the link in Meister's direction gets one lane more than the link in the opposite direction
        attribute_ffspeed = ET.SubElement(attributes, "attribute", name="ffspeed_bike")
        attribute_ffspeed.set("class", "java.lang.Double")
        attribute_ffspeed.text = list[15][1]
        # attribute_ffspeed_slope = ET.SubElement(attributes, "attribute", name="ffspeed_slope")
        # attribute_ffspeed_slope.set("class", "java.lang.Double")
        # attribute_ffspeed_slope.text = list[16][1]
        attribute_car = ET.SubElement(attributes, "attribute", name="car")
        attribute_car.set("class", "java.lang.String")
        attribute_car.text = list[17][1]
        attribute_bike = ET.SubElement(attributes, "attribute", name="bike")
        attribute_bike.set("class", "java.lang.String")
        attribute_bike.text = list[18][1]
        attribute_foot = ET.SubElement(attributes, "attribute", name="foot")
        attribute_foot.set("class", "java.lang.String")
        attribute_foot.text = list[19][1]
        attribute_ldv_count = ET.SubElement(attributes, "attribute", name="ldv_count")
        attribute_ldv_count.set("class", "java.lang.String")
        attribute_ldv_count.text = list[20][1]
        attribute_hdv_count = ET.SubElement(attributes, "attribute", name="hdv_count")
        attribute_hdv_count.set("class", "java.lang.String")
        attribute_hdv_count.text = list[21][1]
        attribute_os_park_pd = ET.SubElement(attributes, "attribute", name="os_park_pd")
        attribute_os_park_pd.set("class", "java.lang.String")
        attribute_os_park_pd.text = list[22][1]
        attribute_trees = ET.SubElement(attributes, "attribute", name="trees_pd")
        attribute_trees.set("class", "java.lang.String")
        attribute_trees.text = list[23][1]
        attribute_veloweg = ET.SubElement(attributes, "attribute", name="veloweg")
        attribute_veloweg.set("class", "java.lang.String")
        attribute_veloweg.text = list[24][1]
        attribute_velostreif = ET.SubElement(attributes, "attribute", name="velostreifen")
        attribute_velostreif.set("class", "java.lang.String")
        if list[26][1] == 'yes': #oneway_bike (i.e. list[28][1]) tells us whether we have a velostreifen in the digitalization direction
            attribute_velostreif.text = list[25][1]
        elif list[26][1] == 'counter_yes':
            attribute_velostreif = "0.0"
        else:
            attribute_velostreif.text = list[25][1] #there might be some links for which oneway_bike is 'nan', but still velostreif = '1'
            # attribute_oneway_bike = ET.SubElement(attributes, "attribute", name="oneway_bike")
            # attribute_oneway_bike.set("class", "java.lang.String")
            # attribute_oneway_bike.text = list[28][1]
        attribute_velomaster = ET.SubElement(attributes, "attribute", name="velomaster")
        attribute_velomaster.set("class", "java.lang.String")
        attribute_velomaster.text = list[27][1]
        attribute_park = ET.SubElement(attributes, "attribute", name="park")
        attribute_park.set("class", "java.lang.Double")
        attribute_park.text = list[28][1]
        attribute_forest = ET.SubElement(attributes, "attribute", name="forest")
        attribute_forest.set("class", "java.lang.Double")
        attribute_forest.text = list[29][1]
        attribute_water = ET.SubElement(attributes, "attribute", name="water")
        attribute_water.set("class", "java.lang.Double")
        attribute_water.text = list[30][1]
        attribute_shops_pd = ET.SubElement(attributes, "attribute", name="shops_pd")
        attribute_shops_pd.set("class", "java.lang.Double")
        attribute_shops_pd.text = list[31][1]
        attribute_reversedirection = ET.SubElement(attributes, "attribute", name="reverse_direction")
        attribute_reversedirection.set("class", "java.lang.String")
        attribute_reversedirection.text = 'no'


        link.text = "\n\t\t"

        #second, the link in the opposite direction
        link = ET.SubElement(links, "link", id=list[1] + "_" + list[0])
        link.set('from', list[1])
        link.set('to', list[0])
        link.set('length', list[5][1])
        #link.set('freespeed', str(compute_free_speed_oslo(-float(list[11][1]))))
        #link.set('freespeed', str(compute_slope_factor(float(-float(list[11][1])))*float(list[15][1])/3.6))
        link.set('freespeed', str(tobler(-float(list[11][1]))))
        link.set("capacity", '300')  # default
        if int(list[14][1]) == 1:
            link.set("permlanes", "1")
        else:
            link.set("permlanes", str(int(list[14][1]) // 2)) # the link in Meister's direction gets one lane more than the link in the opposite direction
        # modes_three =[]
        # for i in range(17, 20):
        #     if list[i][1] == 'True':
        #         modes_three.append(list[i][0])
        # link.set("modes", ", ".join(modes_three))  # this might not be ideal
        link.set("modes", "walk")


        attributes = ET.SubElement(link, "attributes")
        attributes.text = "\n\t\t\t"
        attribute_highway = ET.SubElement(attributes, "attribute", name="highway")
        attribute_highway.set("class", "java.lang.String")
        attribute_highway.text = list[2][1]
        attribute_name = ET.SubElement(attributes, "attribute", name="name")
        attribute_name.set("class", "java.lang.String")
        attribute_name.text = list[3][1]
        attribute_oneway = ET.SubElement(attributes, "attribute", name="oneway")
        attribute_oneway.set("class", "java.lang.String")
        attribute_oneway.text = list[4][1]   # this tells us whether there is a matsim link in the opposite direction
        attribute_length = ET.SubElement(attributes, "attribute", name="length")
        attribute_length.set("class", "java.lang.Double")
        attribute_length.text = list[5][1]
        attribute_service = ET.SubElement(attributes, "attribute", name="service")
        attribute_service.set("class", "java.lang.String")
        attribute_service.text = list[6][1]
        attribute_cycleway = ET.SubElement(attributes, "attribute", name="cycleway")
        attribute_cycleway.set("class", "java.lang.String")
        attribute_cycleway.text = list[7][1]
        attribute_bicycle = ET.SubElement(attributes, "attribute", name="bicycle")
        attribute_bicycle.set("class", "java.lang.String")
        attribute_bicycle.text = list[8][1]
        attribute_access = ET.SubElement(attributes, "attribute", name="access")
        attribute_access.set("class", "java.lang.String")
        attribute_access.text = list[9][1]
        attribute_surface = ET.SubElement(attributes, "attribute", name="surface")
        attribute_surface.set("class", "java.lang.String")
        attribute_surface.text = list[10][1]
        attribute_grade = ET.SubElement(attributes, "attribute", name="grade")
        attribute_grade.set("class", "java.lang.Double")
        attribute_grade.text = str(float(-float(list[11][1])))
        attribute_grade_abs = ET.SubElement(attributes, "attribute", name="grade_abs")
        attribute_grade_abs.set("class", "java.lang.Double")
        attribute_grade_abs.text = list[12][1]
        attribute_maxspeed = ET.SubElement(attributes, "attribute", name="max_speed")
        attribute_maxspeed.set("class", "java.lang.String")
        attribute_maxspeed.text = list[13][1]
        attribute_lanes = ET.SubElement(attributes, "attribute", name="lanes")
        attribute_lanes.set("class", "java.lang.Double")
        if int(list[14][1]) == 1:
            attribute_lanes.text = "1"
        else:
            attribute_lanes.text = str(int(list[14][1]) // 2)
        attribute_ffspeed = ET.SubElement(attributes, "attribute", name="ffspeed_bike")
        attribute_ffspeed.set("class", "java.lang.Double")
        attribute_ffspeed.text = list[15][1]
        # attribute_ffspeed_slope = ET.SubElement(attributes, "attribute", name="ffspeed_slope")
        # attribute_ffspeed_slope.set("class", "java.lang.Double")
        # attribute_ffspeed_slope.text = str(compute_slope_factor(float(-float(list[11][1])))*float(list[15][1]))
        attribute_car = ET.SubElement(attributes, "attribute", name="car")
        attribute_car.set("class", "java.lang.String")
        attribute_car.text = list[17][1]
        attribute_bike = ET.SubElement(attributes, "attribute", name="bike")
        attribute_bike.set("class", "java.lang.String")
        attribute_bike.text = list[18][1]
        attribute_foot = ET.SubElement(attributes, "attribute", name="foot")
        attribute_foot.set("class", "java.lang.String")
        attribute_foot.text = list[19][1]
        attribute_ldv_count = ET.SubElement(attributes, "attribute", name="ldv_count")
        attribute_ldv_count.set("class", "java.lang.String")
        attribute_ldv_count.text = list[20][1]
        attribute_hdv_count = ET.SubElement(attributes, "attribute", name="hdv_count")
        attribute_hdv_count.set("class", "java.lang.String")
        attribute_hdv_count.text = list[21][1]
        attribute_os_park_pd = ET.SubElement(attributes, "attribute", name="os_park_pd")
        attribute_os_park_pd.set("class", "java.lang.String")
        attribute_os_park_pd.text = list[22][1]
        attribute_trees = ET.SubElement(attributes, "attribute", name="trees_pd")
        attribute_trees.set("class", "java.lang.String")
        attribute_trees.text = list[23][1]
        attribute_veloweg = ET.SubElement(attributes, "attribute", name="veloweg")
        attribute_veloweg.set("class", "java.lang.String")
        attribute_veloweg.text = list[24][1]
        attribute_velostreif = ET.SubElement(attributes, "attribute", name="velostreifen")
        attribute_velostreif.set("class", "java.lang.String")
        if list[26][
            1] == 'counter_yes':  # oneway_bike (i.e. list[28][1]) tells us whether we have a velostreifen in the digitalization direction
            attribute_velostreif.text = list[25][1]
        elif list[26][1] == 'yes':
            attribute_velostreif.text = "0.0"
        else:
            attribute_velostreif.text = list[25][1]
            # attribute_oneway_bike = ET.SubElement(attributes, "attribute", name="oneway_bike")
            # attribute_oneway_bike.set("class", "java.lang.String")
            # attribute_oneway_bike.text = list[28][1]
        attribute_velomaster = ET.SubElement(attributes, "attribute", name="velomaster")
        attribute_velomaster.set("class", "java.lang.String")
        attribute_velomaster.text = list[27][1]
        attribute_park = ET.SubElement(attributes, "attribute", name="park")
        attribute_park.set("class", "java.lang.Double")
        attribute_park.text = list[28][1]
        attribute_forest = ET.SubElement(attributes, "attribute", name="forest")
        attribute_forest.set("class", "java.lang.Double")
        attribute_forest.text = list[29][1]
        attribute_water = ET.SubElement(attributes, "attribute", name="water")
        attribute_water.set("class", "java.lang.Double")
        attribute_water.text = list[30][1]
        attribute_shops_pd = ET.SubElement(attributes, "attribute", name="shops_pd")
        attribute_shops_pd.set("class", "java.lang.Double")
        attribute_shops_pd.text = list[31][1]
        attribute_reversedirection = ET.SubElement(attributes, "attribute", name="reverse_direction")
        attribute_reversedirection.set("class", "java.lang.String")
        attribute_reversedirection.text = 'no'


        link.text = "\n\t\t"
print(i)
#print(k)
#print(j)
#print(l)

list_of_link_ids=[]
for elem in root_new.findall('links'):
    for subelem in elem.findall('link'):
        list_of_link_ids.append(str(subelem.get('id')))

#print(len(list_of_link_ids))

seen = {}
doubles = []

for id in list_of_link_ids:
    if id not in seen:
        seen[id] = 1
    else:
        if seen[id] == 1:
            doubles.append(id)
        seen[id] += 1

print(doubles)
print(len(doubles))

for id in doubles:
    i=0
    for elem in root_new.findall("links/link"):
        if elem.get('id')==id:
            i += 1
            if i !=1:
                links.remove(elem)


# m=0
# for id in chosen_links_list:
#     if id not in list_of_link_ids:
#         m += 1
# print(m)

tree_new = ET.ElementTree(root_new)
ET.indent(tree_new, space="\t", level=0)

with open('/Users/matteofelder/Documents/IVT/OsmToMatsim/stadt_zh_matsim_ped.xml', 'wb') as f:
    f.write('<?xml version="1.0" encoding="UTF-8" ?>\n<!DOCTYPE network SYSTEM "http://www.matsim.org/files/dtd/network_v2.dtd">\n'.encode('utf8'))
    tree_new.write(f, 'utf-8')