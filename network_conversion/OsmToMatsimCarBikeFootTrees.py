import xml.etree.ElementTree as ET
from pyproj import Transformer
from pyproj import CRS
crs_4326 = CRS("WGS84")
crs_proj = CRS("EPSG:25832")


x1 = 47.3941155
y1 = 8.4893416
transformer = Transformer.from_crs(crs_4326, crs_proj)
x2,y2 = transformer.transform(x1, y1)
print(x2)



tree = ET.parse('/Users/matteofelder/polybox/Shared/Matteo/car_bike_foot_AOF.xml')
root = tree.getroot()

#to make xml files prettier, use (it is stored in a file test_2)
#ET.indent(tree, space="\t", level=0)

#tree.write("/Users/matteofelder/Documents/IVT/OsmToMatsim/test_2.xml", encoding='utf-8', xml_declaration=True)


root_new = ET.Element("network")
nodes = ET.SubElement(root_new, "nodes")


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

for elem in root.findall('node'):
    x,y = transformer.transform(float(elem.get('lat')),float(elem.get('lon')))
    node = ET.SubElement(nodes, "node", id=str(elem.get('id')), x=str(x), y=str(y))
    node.text = "\n\t\t"


links = ET.SubElement(root_new, "links")
i=0
for elem in root.findall('way'):
    id = str(elem.get('id'))
    list=[]
    for subelem in elem.findall('nd'):
        list.append(str(subelem.get('ref')))
    for subelem in elem.findall('tag'):
        sublist = []
        sublist.append(str(subelem.get('k')))
        sublist.append(str(subelem.get('v')))
        list.append(sublist)
        if str(subelem.get('v'))=="nan":
            list[-1][1] = "unknown"



    link = ET.SubElement(links, "link", id=list[0] + "_" + list[1])
    link.set('from', list[0])
    link.set('to', list[1])
    link.set('length', list[5][1])
    link.set('freespeed', list[15][1])
    link.set("capacity", "300")
    link.set("permlanes","1")
    #link.set("oneway", list[4][1])
    link.set("modes", "bike")
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
    attribute_oneway.text = list[4][1]
    attribute_grade = ET.SubElement(attributes, "attribute", name="grade")
    attribute_grade.set("class", "java.lang.String")
    attribute_grade.text = list[11][1]
    attribute_grade_abs = ET.SubElement(attributes, "attribute", name="grade_abs")
    attribute_grade_abs.set("class", "java.lang.String")
    attribute_grade_abs.text = list[12][1]
    if ("d" in list[3][1]):
        attribute_trees = ET.SubElement(attributes, "attribute", name="trees")
        attribute_trees.set("class", "java.lang.String")
        attribute_trees.text = "10"
    else:
        attribute_trees = ET.SubElement(attributes, "attribute", name="trees")
        attribute_trees.set("class", "java.lang.String")
        attribute_trees.text = "1"
    #attribute_trees = ET.SubElement(attributes, "attribute", name="trees")
    #attribute_trees.set("class", "java.lang.String")
    #attribute_trees.text = list[11][1]
    #attribute_park_count = ET.SubElement(attributes, "attribute", name="park_count")
    #attribute_park_count.set("class", "java.lang.String")
    #attribute_park_count.text = list[13][1]
    attribute_cycleway = ET.SubElement(attributes, "attribute", name="cycleway")
    attribute_cycleway.set("class", "java.lang.String")
    attribute_cycleway.text = list[7][1]
    attribute_bicycle = ET.SubElement(attributes, "attribute", name="bicycle")
    attribute_bicycle.set("class", "java.lang.String")
    attribute_bicycle.text = list[8][1]
    attribute_surface = ET.SubElement(attributes, "attribute", name="surface")
    attribute_surface.set("class", "java.lang.String")
    attribute_surface.text = list[10][1]
    attribute_maxspeed = ET.SubElement(attributes, "attribute", name="maxspeed")
    attribute_maxspeed.set("class", "java.lang.String")
    attribute_maxspeed.text = list[13][1]
    attribute_ffspeed = ET.SubElement(attributes, "attribute", name="ffspeed")
    attribute_ffspeed.set("class", "java.lang.String")
    attribute_ffspeed.text = list[14][1]


    link.text = "\n\t\t"
    #if not oneway, create link in opposite direction
    if list[4][1] == 'yes':
        i+=1
        link = ET.SubElement(links, "link", id=list[1] + "_" + list[0])
        link.set('from', list[1])
        link.set('to', list[0])
        link.set('length', list[5][1])
        link.set('freespeed', str(compute_slope_factor(float(-float(list[11][1])))*float(list[14][1])))
        link.set("capacity", "300")
        link.set("permlanes", "1")
        link.set("modes", "bike")
        attributes = ET.SubElement(link, "attributes")
        attributes.text = "\n\t\t\t"
        attribute_highway = ET.SubElement(attributes, "attribute", name="highway")
        attribute_highway.set("class", "java.lang.String")
        attribute_highway.text = list[2][1]
        attribute_name = ET.SubElement(attributes, "attribute", name="name")
        attribute_name.set("class", "java.lang.String")
        attribute_name.text = list[3][1]
        attribute_grade = ET.SubElement(attributes, "attribute", name="grade")
        attribute_grade.set("class", "java.lang.String")
        attribute_oneway = ET.SubElement(attributes, "attribute", name="oneway")
        attribute_oneway.set("class", "java.lang.String")
        attribute_oneway.text = list[4][1] + "_opposite"
        attribute_grade.text = str(float(-float(list[11][1])))
        attribute_grade_abs = ET.SubElement(attributes, "attribute", name="grade_abs")
        attribute_grade_abs.set("class", "java.lang.String")
        attribute_grade_abs.text = list[12][1]
        if ("d" in list[3][1]):
            attribute_trees = ET.SubElement(attributes, "attribute", name="trees")
            attribute_trees.set("class", "java.lang.String")
            attribute_trees.text = "10"
        else:
            attribute_trees = ET.SubElement(attributes, "attribute", name="trees")
            attribute_trees.set("class", "java.lang.String")
            attribute_trees.text = "1"
        # attribute_trees = ET.SubElement(attributes, "attribute", name="trees")
        # attribute_trees.set("class", "java.lang.String")
        # attribute_trees.text = list[11][1]
        # attribute_park_count = ET.SubElement(attributes, "attribute", name="park_count")
        # attribute_park_count.set("class", "java.lang.String")
        # attribute_park_count.text = list[13][1]
        attribute_cycleway = ET.SubElement(attributes, "attribute", name="cycleway")
        attribute_cycleway.set("class", "java.lang.String")
        attribute_cycleway.text = list[7][1]
        attribute_bicycle = ET.SubElement(attributes, "attribute", name="bicycle")
        attribute_bicycle.set("class", "java.lang.String")
        attribute_bicycle.text = list[8][1]
        attribute_surface = ET.SubElement(attributes, "attribute", name="surface")
        attribute_surface.set("class", "java.lang.String")
        attribute_surface.text = list[10][1]
        attribute_maxspeed = ET.SubElement(attributes, "attribute", name="maxspeed")
        attribute_maxspeed.set("class", "java.lang.String")
        attribute_maxspeed.text = list[13][1]
        attribute_ffspeed = ET.SubElement(attributes, "attribute", name="ffspeed")
        attribute_ffspeed.set("class", "java.lang.String")
        attribute_ffspeed.text = list[14][1]

        link.text = "\n\t\t"

print(i)

tree_new = ET.ElementTree(root_new)
ET.indent(tree_new, space="\t", level=0)

# list_of_link_one_and_two_way = []
# list_of_oneway_links=[]
# list_of_twoway_links=[]
# for elem in root_new.findall('links'):
#     for subelem in elem.findall('link'):
#         list_of_link_one_and_two_way.append(str(subelem.get('oneway')))
#
# for way in list_of_link_one_and_two_way:
#     if way=='yes':
#         list_of_oneway_links.append(way)
#     elif way=='no':
#         list_of_twoway_links.append(way)
#
# print(len(list_of_oneway_links))
# print(len(list_of_twoway_links))



#Check for duplicate link id's
list_of_link_ids=[]
for elem in root_new.findall('links'):
    for subelem in elem.findall('link'):
        list_of_link_ids.append(str(subelem.get('id')))

print(len(list_of_link_ids))
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




with open('/Users/matteofelder/Documents/IVT/OsmToMatsim/car_bike_foot_trees_matsim.xml', 'wb') as f:
    f.write('<?xml version="1.0" encoding="UTF-8" ?>\n<!DOCTYPE network SYSTEM "http://www.matsim.org/files/dtd/network_v2.dtd">\n'.encode('utf8'))
    tree_new.write(f, 'utf-8')

