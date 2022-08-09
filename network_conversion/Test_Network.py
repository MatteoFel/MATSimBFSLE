import xml.etree.ElementTree as ET

root_new = ET.Element("network")
nodes = ET.SubElement(root_new, "nodes")

for row in range(10):
    for col in range(10):
        node = ET.SubElement(nodes, "node", id=str(col+row*10), x=str(row), y=str(col))
        node.text = "\n\t\t"


links = ET.SubElement(root_new, "links")

for row in range(10):
    for col in range(9):
        link = ET.SubElement(links, "link", id=str(col + row * 10) + "_" + str(col + row * 10 + 1))
        link.set('from', str(col + row * 10))
        link.set('to', str(col + row * 10 + 1))
        link.set('length', "1000")
        link.set('freespeed', "1000")
        link.set("capacity", "300")
        link.set("permlanes", "1")
        link.set("modes", "bike")
        attributes = ET.SubElement(link, "attributes")
        attributes.text = "\n\t\t\t"
        if (col+row*10 == 0 or col+row*10 == 11 or col+row*10 == 22 or col+row*10 == 33 or col+row*10 == 44
            or col + row * 10 == 55 or col+row*10 == 66 or col+row*10 == 77 or col+row*10 == 88 or col+row*10 == 99):
            attribute_trees = ET.SubElement(attributes, "attribute", name="trees")
            attribute_trees.set("class", "java.lang.String")
            attribute_trees.text = "100"
            attribute_trees = ET.SubElement(attributes, "attribute", name="bikelane")
            attribute_trees.set("class", "java.lang.String")
            attribute_trees.text = "no"
        elif (col+row*10 == 90 or col+row*10 == 91 or col+row*10 == 92 or col+row*10 == 93 or col+row*10 == 94
            or col + row * 10 == 95 or col+row*10 == 96 or col+row*10 == 97 or col+row*10 == 98):
            attribute_trees = ET.SubElement(attributes, "attribute", name="trees")
            attribute_trees.set("class", "java.lang.String")
            attribute_trees.text = "1"
            attribute_trees = ET.SubElement(attributes, "attribute", name="bikelane")
            attribute_trees.set("class", "java.lang.String")
            attribute_trees.text = "yes"
        else:
            attribute_trees = ET.SubElement(attributes, "attribute", name="trees")
            attribute_trees.set("class", "java.lang.String")
            attribute_trees.text = "1"
            attribute_trees = ET.SubElement(attributes, "attribute", name="bikelane")
            attribute_trees.set("class", "java.lang.String")
            attribute_trees.text = "no"
        link.text = "\n\t\t"

        link = ET.SubElement(links, "link", id=str(col + row * 10+1) + "_" + str(col + row * 10))
        link.set('from', str(col + row * 10+1))
        link.set('to', str(col + row * 10))
        link.set('length', "1000")
        link.set('freespeed', "1000")
        link.set("capacity", "300")
        link.set("permlanes", "1")
        link.set("modes", "bike")
        attributes = ET.SubElement(link, "attributes")
        attributes.text = "\n\t\t\t"
        attribute_trees = ET.SubElement(attributes, "attribute", name="trees")
        attribute_trees.set("class", "java.lang.String")
        attribute_trees.text = "1"
        attribute_trees = ET.SubElement(attributes, "attribute", name="bikelane")
        attribute_trees.set("class", "java.lang.String")
        attribute_trees.text = "no"
        link.text = "\n\t\t"


for row in range(9):
    for col in range(10):
        link = ET.SubElement(links, "link", id=str(col + row * 10) + "_" + str(col + row * 10 + 10 ))
        link.set('from', str(col + row * 10))
        link.set('to', str(col + row * 10 + 10))
        link.set('length', "1000")
        link.set('freespeed', "1000")
        link.set("capacity", "300")
        link.set("permlanes", "1")
        link.set("modes", "bike")
        attributes = ET.SubElement(link, "attributes")
        attributes.text = "\n\t\t\t"
        if (
                col + row * 10 == 1 or col + row * 10 == 12 or col + row * 10 == 23 or col + row * 10 == 34 or col + row * 10 == 45
                or col + row * 10 == 56 or col + row * 10 == 67 or col + row * 10 == 78 or col + row * 10 == 89):
            attribute_trees = ET.SubElement(attributes, "attribute", name="trees")
            attribute_trees.set("class", "java.lang.String")
            attribute_trees.text = "100"
            attribute_trees = ET.SubElement(attributes, "attribute", name="bikelane")
            attribute_trees.set("class", "java.lang.String")
            attribute_trees.text = "no"
        elif(
                col + row * 10 == 0 or col + row * 10 == 10 or col + row * 10 == 20 or col + row * 10 == 30 or col + row * 10 == 40
                or col + row * 10 == 50 or col + row * 10 == 60 or col + row * 10 == 70 or col + row * 10 == 80):
            attribute_trees = ET.SubElement(attributes, "attribute", name="trees")
            attribute_trees.set("class", "java.lang.String")
            attribute_trees.text = "1"
            attribute_trees = ET.SubElement(attributes, "attribute", name="bikelane")
            attribute_trees.set("class", "java.lang.String")
            attribute_trees.text = "yes"
        else:
            attribute_trees = ET.SubElement(attributes, "attribute", name="trees")
            attribute_trees.set("class", "java.lang.String")
            attribute_trees.text = "1"
            attribute_trees = ET.SubElement(attributes, "attribute", name="bikelane")
            attribute_trees.set("class", "java.lang.String")
            attribute_trees.text = "no"
        link.text = "\n\t\t"

        link = ET.SubElement(links, "link", id=str(col + row * 10+10) + "_" + str(col + row * 10))
        link.set('from', str(col + row * 10+10))
        link.set('to', str(col + row * 10))
        link.set('length', "1000")
        link.set('freespeed', "1000")
        link.set("capacity", "300")
        link.set("permlanes", "1")
        link.set("modes", "bike")
        attributes = ET.SubElement(link, "attributes")
        attributes.text = "\n\t\t\t"
        attribute_trees = ET.SubElement(attributes, "attribute", name="trees")
        attribute_trees.set("class", "java.lang.String")
        attribute_trees.text = "1"
        attribute_trees = ET.SubElement(attributes, "attribute", name="bikelane")
        attribute_trees.set("class", "java.lang.String")
        attribute_trees.text = "no"
        link.text = "\n\t\t"

tree_new = ET.ElementTree(root_new)
ET.indent(tree_new, space="\t", level=0)
with open('/Users/matteofelder/Documents/IVT/OsmToMatsim/test_network.xml', 'wb') as f:
    f.write('<?xml version="1.0" encoding="UTF-8" ?>\n<!DOCTYPE network SYSTEM "http://www.matsim.org/files/dtd/network_v2.dtd">\n'.encode('utf8'))
    tree_new.write(f, 'utf-8')