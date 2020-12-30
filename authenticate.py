"""Authenticate with strava"""

import webbrowser
import os

from process_strava import get_token, refresh_token, save_token
from process_strava import strava_clean
from stravalib.client import Client
from stravalib.util import limiter

# This file contains the id and secrets
# Trying with a new free account
client_id, client_secret = open('secrets-chaya.txt').read().strip().split(',')

client = Client(rate_limiter=limiter.DefaultRateLimiter())
path_to_save = None
# ****This only needs to happen once. Once we have the token we can simply refresh ****

# Generate the URL
# QUESTION: do we need to hide the CLIENT_ID?
print("Authenticating with Strava. Be sure you are logged into Strava before running this!")
print("I am launching a web browser. Please return the code following code= in the resulting url.")
print("i know this is clunky but it's a starting place")
# NOTE - this allows access to a lot of profile info AND private activities. we could scope this back to read all easily
url = client.authorization_url(client_id=client_id,
                               redirect_uri='http://127.0.0.1:5000/authorization',
                               scope=['read_all', 'profile:read_all', 'activity:read_all'])
webbrowser.open(url)
print("""You will see a url that looks like this. """,
      """http://127.0.0.1:5000/authorization?state=&code=45fe4353d6f8d04fd6033a00923dd04972760550&scope=read,activity:read_all,profile:read_all,read_all")""",
      """copy the code after code= in the url. do not include the & in this """)

code = input("Please enter the code that you received: ")
print("great! your code is ", code)

# Somehow our web app needs to return a code and then we can grab it.
# potentially using request
#code = request.args.get('code') # e.g.

# Once we have this setup we can exchange the code for a token
# The token i think will need to be stored in a (secure) database.
# Can combine the two lines below just keeping things separate for now
path_to_save = os.path.join("access_token.pickle")
access_token = get_token(client, client_id, client_secret, code=code)
save_token(access_token, path_to_save)

# Begin Good Times
# Once we have the token we will need to check that it's current
if path_to_save:
    refresh_token(client, client_id, client_secret, token_path_pickle=path_to_save)

# This workflow is really assuming it's the first time we're doing this
# In reality i'm guessing we'd be storing data somewhere and would have a
# Date of last activity. I haven't built out that functionality yet so
# let's hold on that for now and just get this going first.
athlete_info = client.get_athlete()

print("hey there {}, how ya doin on this fine day? Im gonna grab your"
      " Strava activities next. You hold on now, ya hear?!".format(athlete_info.firstname))

# Grab activities and turn into df
print("Be patient - this may take a minute")
strava_clean.get_activities(client)

