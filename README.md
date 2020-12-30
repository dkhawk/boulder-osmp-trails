# boulder-osmp-trails
Project for working wth the Boulder County Open Space and Mountain Parks data.

* [The Trail Challenge](https://bouldercolorado.gov/osmp/osmp-trail-challenge)
* [The trail data](https://open-data.bouldercolorado.gov/datasets/d7ad8e150c164c32ab1690658f3fa662_4)
* [A Google map showing the data](https://www.google.com/maps/d/edit?hl=en&mid=1pea5QmxLo4xAqzJNh9LNSFe05a5Te8LR&ll=40.02018708097168%2C-105.2118237148843&z=12)

## Python Strava API Access

1. To run, first install `python 3.6` or above. I prefer the miniconda
installation but any Python installation will do.  if you are a pypi user.

### Environment Setup
You will need an environment with two Python packages to run this code:

1.  stravalib `pip install stravalib`
2.  pandas (if it is not already in your distribution) `pip install pandas`

Make sure the environment is active before proceeding. If you aren't using conda,
you can use the `requirements.txt` file to install both packages:

 `pip install -r dev-requirements.txt`

3. To run the script, open bash and run:
  `$ python authenticate.py` NOTE: if you run into any issues with missing packages
  please let me know. My `conda` install may have some things a classic python
  install doesn't.
4. Follow the instructions. The output should be a `.csv` file with all of your
strava activities as a proof of concept.

**IMPORTANT** you will need the `secrets-chaya.txt` file in order for this to works
more later on how we will share secrets on this project!
If you want, you can create a file from an api app you already have. SImply

1. create a text file called `secrets-chaya.txt`
2. Then in that file add two values: `client_id,client_secret`

This should work!
