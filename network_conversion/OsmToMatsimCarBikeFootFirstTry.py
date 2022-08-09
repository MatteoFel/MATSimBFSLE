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



tree = ET.parse('/Users/matteofelder/polybox/Shared/Matteo/car_bike_foot_full_poly_enriched.osm')
root = tree.getroot()

#to make xml files prettier, use (it is stored in a file test_2)
#ET.indent(tree, space="\t", level=0)

#tree.write("/Users/matteofelder/Documents/IVT/OsmToMatsim/test_2.xml", encoding='utf-8', xml_declaration=True)


root_new = ET.Element("network")
nodes = ET.SubElement(root_new, "nodes")


for elem in root.findall('node'):
    x,y = transformer.transform(float(elem.get('lat')),float(elem.get('lon')))
    node = ET.SubElement(nodes, "node", id=str(elem.get('id')), x=str(x), y=str(y))
    node.text = "\n\t\t"


links = ET.SubElement(root_new, "links")

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
    link.set('length', list[7][1])
    link.set('freespeed', "8")
    link.set("capacity", "300")
    link.set("permlanes","1")
    link.set("modes", "bike")
    attributes = ET.SubElement(link, "attributes")
    attributes.text = "\n\t\t\t"
    attribute_highway = ET.SubElement(attributes, "attribute", name="highway")
    attribute_highway.set("class", "java.lang.String")
    attribute_highway.text = list[2][1]
    attribute_grade = ET.SubElement(attributes, "attribute", name="grade")
    attribute_grade.set("class", "java.lang.String")
    attribute_grade.text = list[8][1]
    attribute_grade_abs = ET.SubElement(attributes, "attribute", name="grade_abs")
    attribute_grade_abs.set("class", "java.lang.String")
    attribute_grade_abs.text = list[9][1]
    attribute_trees = ET.SubElement(attributes, "attribute", name="trees")
    attribute_trees.set("class", "java.lang.String")
    attribute_trees.text = list[11][1]
    attribute_park_count = ET.SubElement(attributes, "attribute", name="park_count")
    attribute_park_count.set("class", "java.lang.String")
    attribute_park_count.text = list[13][1]
    attribute_velostreifen = ET.SubElement(attributes, "attribute", name="velostreifen")
    attribute_velostreifen.set("class", "java.lang.String")
    attribute_velostreifen.text = list[14][1]

    link.text = "\n\t\t"
    #if not oneway, create link in opposite direction
    if list[6][1] != 'yes':
        link = ET.SubElement(links, "link", id=list[1] + "_" + list[0])
        link.set('from', list[1])
        link.set('to', list[0])
        link.set('length', list[7][1])
        link.set('freespeed', "8")
        link.set("capacity", "300")
        link.set("permlanes", "1")
        link.set("modes", "bike")
        attributes = ET.SubElement(link, "attributes")
        attributes.text = "\n\t\t\t"
        attribute_highway = ET.SubElement(attributes, "attribute", name="highway")
        attribute_highway.set("class", "java.lang.String")
        attribute_highway.text = list[2][1]
        attribute_grade = ET.SubElement(attributes, "attribute", name="grade")
        attribute_grade.set("class", "java.lang.String")
        attribute_grade.text = str(float(-float(list[8][1])))
        attribute_grade_abs = ET.SubElement(attributes, "attribute", name="grade_abs")
        attribute_grade_abs.set("class", "java.lang.String")
        attribute_grade_abs.text = list[9][1]
        attribute_trees = ET.SubElement(attributes, "attribute", name="trees")
        attribute_trees.set("class", "java.lang.String")
        attribute_trees.text = list[11][1]
        attribute_park_count = ET.SubElement(attributes, "attribute", name="park_count")
        attribute_park_count.set("class", "java.lang.String")
        attribute_park_count.text = list[13][1]
        attribute_velostreifen = ET.SubElement(attributes, "attribute", name="velostreifen")
        attribute_velostreifen.set("class", "java.lang.String")
        attribute_velostreifen.text = list[14][1]

        link.text = "\n\t\t"



tree_new = ET.ElementTree(root_new)
ET.indent(tree_new, space="\t", level=0)

#Check for duplicate link id's
list_of_links=[]
for elem in root_new.findall('links'):
    for subelem in elem.findall('link'):
        list_of_links.append(str(subelem.get('id')))


seen = {}
doubles = []

for id in list_of_links:
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




with open('/Users/matteofelder/Documents/IVT/OsmToMatsim/car_bike_foot_first_try.xml', 'wb') as f:
    f.write('<?xml version="1.0" encoding="UTF-8" ?>\n<!DOCTYPE network SYSTEM "http://www.matsim.org/files/dtd/network_v2.dtd">\n'.encode('utf8'))
    tree_new.write(f, 'utf-8')

