import pandas as pd
import re
import logging

#df = pd.read_csv('/Users/matteofelder/Documents/IVT/attributed_network/cbff/20210910_100832/12807989_edges.txt', sep=")", header=None)



import os
#velo
#rootdir = '/Users/matteofelder/Documents/IVT/bike_zurich/stadt_zh_network_car/chosen_routes/c_filtered'

#pedestrian
rootdir = '/Users/matteofelder/Documents/IVT/bike_zurich/greater_zurich/ebike/chosen_routes/ebike_filtered'

def parse_route(df):
    id_list = []
    for index in range(df.shape[1]):
        from_node_to_node = re.findall(r'\d+', str(df.iloc[0, index]))
        if (len(from_node_to_node)!=2):
            logging.warning('This is a warning message')
            print(file)
            print(index)
        if index == 0:
            origin = from_node_to_node[0]
        if index == df.shape[1]-1:
            destination = from_node_to_node[1]
        link_node_to_node_id = from_node_to_node[0] + "_" + from_node_to_node[1]
        id_list.append(link_node_to_node_id)
    rows = []
    cols = []
    df_output = pd.DataFrame(rows, columns = cols)
    df_output["link_id"] = id_list

    return [origin, destination, df_output]



rows = []
cols = []
df_id = pd.DataFrame(rows, columns=cols)
df_temp = pd.DataFrame(rows, columns=cols)
df_route = pd.DataFrame(rows, columns=cols)



for subdir, dirs, files in os.walk(rootdir):
    for file in files:
        if "edges" in file:
            df = pd.read_csv(os.path.join(subdir, file), sep=")", header=None)
            df.drop(df.columns[-1], axis=1, inplace=True)
            origin = parse_route(df)[0]
            destination = parse_route(df)[1]
            df_output = parse_route(df)[2].transpose()
            df_output.reset_index(drop=True, inplace=True)
            df_output.columns = df_output.columns.astype(str)
            filename = re.findall(r'\d+', str(file))[0]
            df_output.insert(loc=0, column='destination', value=destination)
            df_output.insert(loc=0, column='origin', value=origin)
            df_output.insert(loc=0, column='route_id', value=[filename])
            output_filename = str(filename) + "_route.tsv"
            df_output.to_csv(os.path.join('/Users/matteofelder/Documents/IVT/bike_zurich/greater_zurich/ebike/chosen_routes/chosen_routes_separate', output_filename), sep = '\t', index=False)
            df_route = df_route.append(df_output, ignore_index=True)
            df_route = df_route.sort_values(by=['route_id'])


df_route.to_csv('/Users/matteofelder/Documents/IVT/bike_zurich/greater_zurich/ebike/chosen_routes/chosen_routes_ebike.tsv', sep = '\t', index=False)


