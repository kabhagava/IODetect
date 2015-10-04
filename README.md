# IODetect: Context Sensing for Determining Indoor/Outdoor Localization
Kavita Bhagavatula's Masters Project at UCSD
March 2014


# Abstract:
Many mobile computing applications can benefit from monitoring
a user’s indoor/outdoor location context. Such knowledge
can be leveraged both internally by systems to adjust
sensor processing loads and reduce energy consumption,
and externally by consumers to adopt healthier lifestyles.
In this paper, I propose IODetect, a service that infers indoor/
outdoor localization. IODetect uses data from a phone’s
accelerometer, light sensor, colors extracted from images, and
WiFi scanner to form ambient fingerprints of a user’s activity
and logical location. The system is controlled by a decision
manager, which oversees sensor data collection and processing,
and an inference module, which infers indoor/outdoor
decisions via a place detector, activity state machine, and
Bayesian network model. My approach is evaluated by two
data sets, including individual activity experiments conducted
in targeted ambiences, as well as longer traces collected over
a 2-day period while going about normal routines. Experimental
results show that IODetect can achieve an average
indoor/outdoor decision accuracy of 77% when a user continuously
enagages in a range of activities while visiting varied
places.
