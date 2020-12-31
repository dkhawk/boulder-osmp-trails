"""A module with helper functions to process data."""

from datetime import datetime

from shapely.geometry import LineString, Point
import pandas as pd

def swap(x):
    coords = list(x.coords)
    # Swap each coordinate using list comprehension and create Points
    coords = [Point(t[1], t[0]) for t in coords]
    return LineString(coords)


def get_activities(client):
    """Grab all activities and turn into df.
     Finally export to a csv file
     TODO: add a limit or date span to this as an input
     Grabs all activities after Dec 1 2020 for the time being
     Does this need to be flexible to allow a date input?

     Parameters
     ----------
        client : Stravalib client object


     Returns
     -------
        Pandas DataFrame w all activities collected from strava
        Writes out a csv file to the users current working dir.
     """
    # Specify date range to return activities for
    # TODO - decide on activity date to return activities for
    # after = "2020-01-01T00:00:00Z"
    activities = client.get_activities(after="2020-11-30T00:00:00Z")

    # Generate a dataframe of all activities
    my_cols = ['name',
               'average_speed',
               'distance',
               'elapsed_time',
               'total_elevation_gain',
               'start_date_local',
               'type']
    data = []
    # You have to grab the activity id from the activity iterator object
    for activity in activities:
        my_dict = activity.to_dict()
        data.append([activity.id] + [my_dict.get(x) for x in my_cols])

    my_cols.insert(0, "activity_id")
    all_activities_df = pd.DataFrame(data,
                                     columns=my_cols)

    # Note - we will want to parse out runs  & hikes vs other types?
    # Can we do that using the request?
    print("Looks like you've been a busy bee. "
          "I've found {} activities.".format(len(all_activities_df)))

    csv_path = client.get_athlete().firstname + "_all_activities.csv"
    print("Great! i have everything I need - I'm saving your data to a "
          "file called {}. \n Our business is done here. Bye for now!".format(csv_path))
    all_activities_df.to_csv(csv_path)

    return all_activities_df


def get_act_gps(client, act_df, athlete_info):
    """

    :param act_dr:
    :return:
    """
    # Next, grab all spatial data
    # TODO: We may not need distance or time?
    types = ['time', 'distance', 'latlng']

    print("Next I will get your run GPS data.")
    gdf_list = []
    for i, act in enumerate(act_df["activity_id"].values):

        act_data = client.get_activity_streams(act,
                                               types=types)
        # print(act)
        # Some activities have no information associated with them
        if act_data:
            try:
                gdf_list.append([act,
                                 act_data["latlng"].data])
            except KeyError:
                # some activities have no gps data like swimming and short activities
                print(
                    "LatLon is missing from activity {}. Moving to next activity".format(act))

    print("You have made {} requests. Strava limits requests to 600 every 15 mins".format(i))
    print(datetime.now())
    act_gps_df = pd.DataFrame(gdf_list,
                              columns=["activity_id", "xy"])
    print("Next, I'll export your hiking & running GPS data. Hold on".format(i))

    gps_data_path = athlete_info.firstname + "_gps_data.csv"
    act_gps_df.to_csv(gps_data_path)
    print("I've saved a file called {} for you.".format(gps_data_path))
