"""A module with helper functions to process data."""

from shapely.geometry import LineString, Point
import pandas as pd

def swap(x):
    coords = list(x.coords)
    # Swap each coordinate using list comprehension and create Points
    coords = [Point(t[1], t[0]) for t in coords]
    return LineString(coords)


def get_activities(client):
    """Grab all activities and turn into df
     TODO: add a limit or date span to this as an input"""
    activities = client.get_activities()

    # Generate a dataframe of all activities
    my_cols = ["activity_id",
               'name',
               'average_speed',
               'distance',
               'elapsed_time',
               'total_elevation_gain',
               'start_date_local',
               'type']
    data = []
    for activity in activities:
        my_dict = activity.to_dict()
        data.append([my_dict.get(x) for x in my_cols])

    all_data_df = pd.DataFrame(data,
                               columns=my_cols)

    # Note - we will want to parse out runs  & hikes vs other types?
    # Can we do that using the request?
    print("Looks like you've been a busy bee. "
          "I've found {} activities.".format(len(all_data_df)))

    csv_path = "all_activities.csv"
    print("Great! i have everything I need - I'm saving your data to a "
          "file called {}. \n Our business is done here. Bye for now!".format(csv_path))
    all_data_df.to_csv(csv_path)
