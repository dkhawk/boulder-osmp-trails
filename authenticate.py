"""Authenticate with strava"""

import webbrowser
import os
from datetime import datetime

import pandas as pd
from stravalib.client import Client
from stravalib.util import limiter

from process_strava import get_token, refresh_token, save_token
from process_strava import strava_clean

# This file contains the id and secrets
# Trying with a new free account
client_id, client_secret = open('secrets-chaya.txt').read().strip().split(',')

client = Client(rate_limiter=limiter.DefaultRateLimiter())
# ****This only needs to happen once. Once we have the token we can simply refresh ****

path_to_save = os.path.join("access_token.pickle")

if not os.path.exists(path_to_save):
    # Generate the URL
    # QUESTION: do we need to hide the CLIENT_ID?
    print("Authenticating with Strava. Be sure you are logged into Strava before running this!")
    print("I am launching a web browser. Please return the code following code= in the resulting url.")
    print("i know this is clunky but it's a starting place")

    # TODO: Adjust scope - probably only need activity:read_all
    # TODO: move this into a one time event that looks for a key - i presume the tokens will be stored in the DB
    # NOTE - this allows access to a lot of profile info AND private activities. we could scope this back to read all easily
    url = client.authorization_url(client_id=client_id,
                                   redirect_uri='http://127.0.0.1:5000/authorization',
                                   scope=['read_all', 'profile:read_all', 'activity:read_all'])
    webbrowser.open(url)
    print("""You will see a url that looks like this. """,
          """http://127.0.0.1:5000/authorization?state=&code=45fe4353d6f8d04fd6033a00923dd04972760550&scope=read,activity:read_all,profile:read_all,read_all")""",
          """copy the code after code= in the url. do not include the & in this """)

    code = input("Please enter the code that you received: ")
    print("Great! Your code is ", code, "Next I will exchange that code for a token.\n"
                                        "I only have to do this once.")

    # Somehow our web app needs to return a code and then we can grab it.
    # potentially using request
    #code = request.args.get('code') # e.g.

    # Once we have this setup we can exchange the code for a token
    # The token I think will need to be stored in a (secure) database for each user.
    # Can combine the two lines below just keeping things separate for now
    path_to_save = os.path.join("access_token.pickle")
    access_token = get_token(client, client_id, client_secret, code=code)
    save_token(access_token, path_to_save)

# Begin Good Times -
# TODO: this maybe should be a try statement so it can fail gracefully
# Once we have the token we will need to check that it's current
if os.path.exists(path_to_save):
    refresh_token(client, client_id, client_secret, token_path_pickle=path_to_save)

# This workflow is really assuming it's the first time we're doing this
# In reality i'm guessing we'd be storing data somewhere and would have a
# Date of last activity. I haven't built out that functionality yet so
# let's hold on that for now and just get this going first.
athlete_info = client.get_athlete()

print("hey there {}, how ya doin on this fine day? Im gonna grab your"
      "\nStrava activities next. You hold on now, ya hear?!".format(athlete_info.firstname))

# Grab activities and turn into df
print("Be patient - this may take a minute")
# TODO: Add time range limit to reduce data download
all_activities = strava_clean.get_activities(client)

# Only grab runs, walks and hikes
act_types = ["Run", "Hike", "Walk"]
all_runs_hikes = all_activities[all_activities.type.isin(act_types)]

# Next, grab all spatial data
# TODO: We may not need distance or time?
types = ['time', 'distance', 'latlng']

gdf_list=[]
for i, act in enumerate(all_runs_hikes["activity_id"].values[0:2]):
        # Turn this into a small helper
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

print("you have made {} requests. Strava limits requests to 600 every 15 mins".format(i))
print(datetime.now())
act_gps_df = pd.DataFrame(gdf_list,
                          columns=["activity_id", "xy"])
print("Next, I'll export your hiking & running GPS data. Hold on".format(i))

gps_data_path = athlete_info.firstname + "_gps_data.csv"
act_gps_df.to_csv(gps_data_path)
print("I've saved a file called {} for you. ".format(gps_data_path))



