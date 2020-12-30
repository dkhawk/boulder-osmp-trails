"""A module with helper functions to process data."""

import pickle
import time

from stravalib.client import Client


def get_token(client, client_id, client_secret, code):
    """A one time authentication to get a user token"""
    # Should I instantiate a new client or just pass one in?

    return client.exchange_code_for_token(client_id=client_id,
                                          client_secret=client_secret,
                                          code=code)


def save_token(access_token, path_to_save):
    """Function to save the token somewhere. For now i'm using a
    pickle to do it. Don't laugh at pickles."""

    with open(path_to_save, 'wb') as f:
        pickle.dump(access_token, f)

    print("Token saved - hooray!")


def refresh_token(client, client_id, client_secret, token_path_pickle=None, code=None):
    """A function that refreshes the users token once it's been
    created. The tokens expire every 6 hours."""

    # Here i'm using a saved pickle
    try:
        with open(token_path_pickle, 'rb') as f:
            access_token = pickle.load(f)
    except FileNotFoundError:
        print("Oops - looks like you haven't created an access token yet. Aborting.")
    
    if time.time() > access_token['expires_at']:
        print('Oops! Your token has expired. No worries - I will refresh it for you.')
        refresh_response = client.refresh_access_token(client_id=client_id,
                                                       client_secret=client_secret,
                                                       refresh_token=access_token['refresh_token'])
        access_token = refresh_response
        with open(token_path_pickle, 'wb') as f:
            pickle.dump(refresh_response, f)
        print("I've refreshed your token and saved it to a file. Aren't I the best?")

        client.access_token = refresh_response['access_token']
        client.refresh_token = refresh_response['refresh_token']
        client.token_expires_at = refresh_response['expires_at']

    else:
        print('Token still valid, expires at {}'
              .format(time.strftime("%a, %d %b %Y %H:%M:%S %Z", time.localtime(access_token['expires_at']))))

        client.access_token = access_token['access_token']
        client.refresh_token = access_token['refresh_token']
        client.token_expires_at = access_token['expires_at']
        
        return client