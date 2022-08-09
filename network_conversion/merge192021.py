import pandas as pd

df_2019_bike = pd.read_csv('/Users/matteofelder/Documents/IVT/bike_zurich/stadt_zh_network_car_bike/chosen_routes/chosen_routes_car_bike.tsv', sep='\t')

df_2020_bike = pd.read_csv('/Users/matteofelder/Documents/IVT/bike_zurich/2020/bike/chosen_routes/chosen_routes_bike_2020_clean.tsv', sep='\t')
df_2020_ebike = pd.read_csv('/Users/matteofelder/Documents/IVT/bike_zurich/2020/ebike/chosen_routes/chosen_routes_ebike_2020_clean.tsv', sep='\t')
df_2021_bike = pd.read_csv('/Users/matteofelder/Documents/IVT/bike_zurich/2021/bike/chosen_routes/chosen_routes_bike_2021.tsv', sep='\t')
df_2021_ebike = pd.read_csv('/Users/matteofelder/Documents/IVT/bike_zurich/2021/ebike/chosen_routes/chosen_routes_ebike_2021.tsv', sep='\t')

frames_bike = [df_2019_bike, df_2020_bike, df_2021_bike]
frames_ebike = [df_2020_ebike, df_2021_ebike]

df_bike = pd.concat(frames_bike, ignore_index=True, sort=False)
df_ebike = pd.concat(frames_ebike, ignore_index=True, sort=False)

df_bike.to_csv('/Users/matteofelder/Documents/IVT/bike_zurich/2019_2021/chosen_routes/chosen_routes_19_21_bike.tsv', sep='\t', index=False)
df_ebike.to_csv('/Users/matteofelder/Documents/IVT/bike_zurich/2019_2021/chosen_routes/chosen_routes_20_21_ebike.tsv', sep='\t', index=False)